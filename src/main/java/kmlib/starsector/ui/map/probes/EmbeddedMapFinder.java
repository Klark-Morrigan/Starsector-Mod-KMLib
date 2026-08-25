package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.SectorMapAPI;

import kmlib.logging.SessionWarning;
import kmlib.starsector.ui.coreui.CoreUiTree;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Where the sector maps that are not the screen's own stand in the live widget tree.
 *
 * <p>A map widget renders terrain, and terrain rendering is a hook any mod's map can drive, so a
 * mod that composites a map into a panel of its own puts a second map surface on screen that
 * nothing running inside that hook can see. Anything having to reason about such a surface - what
 * is drawn where, and whether the pointer is on it - needs the widget itself, which is what this
 * hands back.
 *
 * <p>Identification is structural and never by class name. An embedded map is a
 * {@link SectorMapAPI} that is not the map tab on screen and does not hang under it. That keeps the
 * rule working for the next mod that embeds one, and it is the only rule available in any case: a
 * minimap built entirely out of vanilla API carries no mod-owned class to match on, so a walk
 * looking for a name would find nothing to look at.
 *
 * <p>The map on screen is pruned rather than filtered out afterwards, so its whole subtree goes
 * unwalked. That is most of the tree - the map's own content hangs below it - and none of it can
 * hold an embedded map by the rule above.
 *
 * <p>Rooted at the core UI rather than at the current tab, since the thing being looked for is by
 * definition not in a tab: it hangs off the campaign HUD or whatever that raises. A walk rooted at
 * a tab would come back empty and read as "there is none" rather than "looked in the wrong place".
 *
 * <p>An answer is remembered against the tree it was walked out of, because a caller asking per
 * frame cannot pay for a walk of the core UI per frame, while a widget a mod built once stays where
 * it was put. The tree is what the memo is keyed on: the core UI in force changes when an
 * interaction dialog stands up its own, so keying on it re-walks exactly when the tree being
 * described is a different one.
 *
 * <p>It is also remembered for a moment only, and the reason is that the root outlives changes
 * <em>within</em> the tree. A core screen the player closes is still in that tree while it fades,
 * and it is no longer the map on screen, so a walk taken across those frames counts it as somebody
 * else's map - a true reading of that instant and a wrong one a moment later. Keyed on the root
 * alone that reading would stand for as long as the root does, which in the campaign's own core UI
 * is the rest of the session: a caller acting on there being exactly one embedded map would go on
 * seeing two, and nothing the player did with a screen afterwards could correct it. A bounded
 * memory costs one walk per interval instead of one per frame and heals in that interval.
 *
 * <p>What bounds how long a remembered answer holds a widget tree alive is that replacement and
 * nothing else. Every find carries its map and the whole chain it hangs under, and the outermost of
 * those ancestors is the root itself, so the memo pins the tree it came from for as long as it
 * stands - which lasts until the first ask after that tree changed or the interval elapsed, and for
 * a caller asking per frame is a frame.
 *
 * <p>Only a walk that found something is remembered, and a walk that found nothing is taken again.
 * Nothing orders a mod's widget building against ours, so a first walk can legitimately run before
 * the widget it is looking for exists - and remembering that emptiness would answer "there is no
 * embedded map" for the rest of the session on precisely the installs this exists for.
 *
 * <p>Both live reads arrive as injected ports rather than being taken statically here. What is kept
 * and when it is taken again is a rule with behaviour of its own, and a walk that reached into a
 * running game for its own root would carry that rule nowhere else.
 *
 * <p>Answers nothing rather than throwing. A caller is typically in the middle of a frame, and a
 * read taken to refine what that frame draws must not be able to take the frame down; the reach it
 * rests on is by-name reflection into classes no game build is obliged to keep, so that it can fail
 * is a fact about the reach rather than a remote possibility.
 */
public final class EmbeddedMapFinder {

    private static final Logger LOG = Global.getLogger(EmbeddedMapFinder.class);

    // How long an answer is reused before the tree is read again. Long enough that a caller asking
    // per frame pays a walk every few dozen frames rather than one per frame, and short enough that
    // a reading taken while a screen was fading out of the tree corrects itself before a player
    // pointing at a map surface could notice it had not.
    static final long MEMO_LIFETIME_NANOS = 500_000_000L;

    private final LongSupplier readElapsedNanos;
    private final Supplier<Object> readShownMapTab;
    private final Supplier<Object> readTreeRoot;

    // Says once per session that the reach stopped working, since a caller handed an empty list
    // cannot tell a tree with no embedded map in it from a walk that never ran.
    private final SessionWarning warning = new SessionWarning(LOG);

    private List<EmbeddedMap> foundMaps = List.of();

    // The tree the answer above was walked out of. Identity rather than equality: two roots are the
    // same tree only by being the same object, and a widget's equals is the obfuscated class's
    // business.
    //
    // Held plainly rather than weakly, since a weak reference here would buy nothing it appears to:
    // the finds beside it hold every widget on each map's chain, this root among them, so the memo
    // pins the tree whatever this field does. Replacement is what releases it - see the class note.
    private Object walkedTreeRoot;

    // When that walk was made, on the elapsed clock this was handed. Read against the lifetime above
    // rather than against a frame count, since nothing here is told when a frame begins.
    private long walkedAtNanos;

    /** Reads the live core UI, the live map tab and the elapsed clock - what a running game gets. */
    public EmbeddedMapFinder() {
        this(CoreUiTree::resolveActiveCoreUi, ShownMapTab::resolveShownMapTab, System::nanoTime);
    }

    /**
     * @param readTreeRoot     the widget tree to search, above any one tab
     * @param readShownMapTab  the map tab the game is showing, which is the one map that is not
     *                         embedded - and null on every screen showing none
     * @param readElapsedNanos the elapsed-time clock the memo's life is measured on, which is a
     *                         monotonic one rather than a wall clock: only differences are read, and
     *                         a wall clock stepping back would hold an answer past its interval
     */
    EmbeddedMapFinder(
            Supplier<Object> readTreeRoot,
            Supplier<Object> readShownMapTab,
            LongSupplier readElapsedNanos) {

        this.readElapsedNanos = readElapsedNanos;
        this.readShownMapTab = readShownMapTab;
        this.readTreeRoot = readTreeRoot;
    }

    /**
     * Every sector map in the live tree that is not the one the player has open.
     *
     * <p>Walks once per tree and reuses the answer after, so a caller in a render pass can ask per
     * frame. A tree with nothing embedded in it is the exception, being walked again at every ask
     * for the reason this class states.
     *
     * @return the maps found, or an empty list when there is no tree to walk, nothing is embedded
     *         in it, or the reach into it failed - none of which a caller can act on differently
     */
    public List<EmbeddedMap> findEmbeddedMaps() {
        try {

            var treeRoot = readTreeRoot.get();

            // No tree is the ordinary state before a campaign is stood up. Answered as nothing
            // rather than with what a previous tree held, since what was found was found in there.
            if (treeRoot == null) {
                return List.of();
            }
            if (walkedTreeRoot == treeRoot && !foundMaps.isEmpty() && !hasMemoExpired()) {
                return foundMaps;
            }
            var embeddedMaps = collectEmbeddedMapsUnder(treeRoot, readShownMapTab.get());

            rememberIfFound(treeRoot, embeddedMaps);
            return embeddedMaps;

        } catch (Throwable failure) {
            // Swallowed rather than raised: a caller is in the middle of a frame, and a read taken
            // to refine what that frame draws must not take the frame down with it.
            warnOnce(failure);
            return List.of();
        }
    }

    /**
     * The live tree walked afresh, for a caller that must see it as it stands rather than as it was
     * remembered.
     *
     * <p>Beside the memoised read rather than in whatever wants it, so which tree is searched and
     * which map is excluded are stated once. A second statement of that pairing could only drift
     * from this one, and would do it silently - the two would go on answering, about different
     * trees.
     *
     * @return the maps found, or null when there is no tree to walk - which a caller reporting on
     *         the tree can tell apart from a walk that found nothing in it
     */
    static List<EmbeddedMap> collectLiveEmbeddedMaps() {
        var treeRoot = CoreUiTree.resolveActiveCoreUi();
        return treeRoot == null
            ? null
            : collectEmbeddedMapsUnder(treeRoot, ShownMapTab.resolveShownMapTab());
    }

    /**
     * The rule the live read applies, over a tree read elsewhere.
     *
     * @param treeRoot    the widget to search from
     * @param shownMapTab the map tab on screen, pruned along with everything under it, or null when
     *                    no screen is showing one
     * @return the maps found, in the order the walk met them, each with the chain it hangs under
     */
    static List<EmbeddedMap> collectEmbeddedMapsUnder(Object treeRoot, Object shownMapTab) {

        var embeddedMaps = new ArrayList<EmbeddedMap>();

        collectEmbeddedMaps(treeRoot, shownMapTab, new ArrayList<>(), 0, embeddedMaps);
        return List.copyOf(embeddedMaps);
    }

    // Walks depth-first, carrying the path down so a map that is found can report what it hangs
    // under. The path is the point of the walk rather than a by-product: a flat hit says a map
    // exists somewhere, which is already known by the time anything asks.
    private static void collectEmbeddedMaps(
            Object component,
            Object shownMapTab,
            List<Object> ancestors,
            int depth,
            List<EmbeddedMap> embeddedMaps) {

        if (component == null
                || component == shownMapTab
                || depth > ProbeLimits.MAX_SEARCH_DEPTH
                || embeddedMaps.size() >= ProbeLimits.MAX_REPORTED_ITEMS) {
            return;
        }
        if (component instanceof SectorMapAPI map) {
            embeddedMaps.add(new EmbeddedMap(map, ancestors));
        }
        ancestors.add(component);
        for (var child : CoreUiTree.readChildrenOf(component)) {
            collectEmbeddedMaps(child, shownMapTab, ancestors, depth + 1, embeddedMaps);
        }
        ancestors.remove(ancestors.size() - 1);
    }

    // Records a walk for reuse, and only one that found something - see the retry this class states.
    private void rememberIfFound(Object treeRoot, List<EmbeddedMap> embeddedMaps) {
        if (embeddedMaps.isEmpty()) {
            return;
        }
        foundMaps = embeddedMaps;
        walkedAtNanos = readElapsedNanos.getAsLong();
        walkedTreeRoot = treeRoot;
    }

    // Whether what is remembered has stood long enough to be worth reading the tree again. Stated as
    // a difference so it holds wherever the clock's own zero is.
    private boolean hasMemoExpired() {
        return readElapsedNanos.getAsLong() - walkedAtNanos >= MEMO_LIFETIME_NANOS;
    }

    // Warns on this library's own logger rather than the caller's, since a reach that broke is the
    // library's news to report.
    private void warnOnce(Throwable failure) {
        warning.warnOnce(
            "Could not walk the core UI tree by reflection; maps embedded outside the "
                + "game's own map screens will go unfound this session.",
            failure);
    }
}
