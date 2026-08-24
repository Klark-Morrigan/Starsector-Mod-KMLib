package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.Global;

import kmlib.logging.SessionWarning;
import kmlib.starsector.ui.coreui.CoreUiTree;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Answers "which tooltip is the vanilla map screen showing right now?" for an overlay that must
 * step aside for it. When the cursor is over a star (or any entity) the map draws its own tooltip;
 * an overlay drawing its own box wants to suppress it then so only one shows.
 *
 * <p>The found component is handed back rather than reduced to a yes/no, because stepping aside is
 * not the only way to cohabit with a tooltip: a caller that draws over the tooltip instead needs the
 * component itself to act on. The probe stays a pure locator - it finds, it never draws.
 *
 * <p>There is no API for this, so it is read off the live core-UI tree. A tooltip is not a node in
 * that tree and not a typed field on the map widget - the widget that draws it is a tooltip <em>host</em>
 * (the core UI's own {@code getTooltip()} contract), which holds the currently shown tooltip and hands
 * it back from {@code getTooltip()}, clearing it to null when the tooltip hides. So the read searches a
 * subtree for any host whose {@code getTooltip()} returns a live {@code StandardTooltipV2}. That is the
 * same read the core UI does itself to render a child's tooltip, so it tracks exactly when a tooltip is
 * up.
 *
 * <p>Where that search starts is the caller's, taken as a port. A map surface is not always a core tab -
 * a mod that docks one adds a panel to the core UI itself, and on the frames such a panel is up there is
 * no tab at all - so a walk fixed at the current tab would be unreachable on exactly the surface a
 * tooltip was raised on. What the search root should be is a question about which surfaces exist, which
 * is answered where those surfaces are known rather than here; what a vanilla tooltip <em>is</em> is the
 * same wherever it hangs, and that is what this class owns. The no-arg pairing binds the current tab,
 * which is the root a caller reading a vanilla map host wants.
 *
 * <p>The private core-UI methods and the private tooltip host are reached through {@link CoreUiTree},
 * which is where the by-name reach and the library behind it are answered for; nothing here names that
 * library itself. The tooltip is matched by the returned value's runtime class <em>name</em> walked up
 * its hierarchy, not by a field name or {@code Class} identity, so an obfuscated rename or a classloader
 * mismatch cannot make a real tooltip read as foreign.
 *
 * <p>The whole read is best-effort, since reflecting into obfuscated internals can fail on any game
 * build. On any failure it reports no tooltip - the overlay then draws, so a broken read costs a
 * possible double tooltip rather than a missing overlay - and warns once per session so the failure is
 * visible without flooding the log. A missing MagicLib is not among those failures - it is a
 * declared {@code mod_info.json} dependency.
 *
 * <p>Because the reach is fragile and only observable in-engine, the read narrates itself at DEBUG:
 * each time its outcome changes it logs one line naming the search root, how many nodes it walked,
 * the tooltips it saw shown, and the verdict. That is what turns "it does not suppress" from a guess
 * into a diagnosis - which hop failed, or whether the map's host was reached - without flooding the
 * log frame to frame. With DEBUG off it builds none of that, so the walk stays a bare tree search.
 * The level is this library's own, so a consuming mod's verbosity setting does not reach it; the
 * warning below is the part that survives the default level, and it is the one that matters.
 *
 * <p>An instance rather than a static holder so a caller can put its own probe in place: whether
 * the map is drawing its own tooltip decides whether that caller draws at all, which is too much
 * to hang on a reach nothing can substitute.
 */
public final class VanillaMapTooltipProbe {

    private static final Logger LOG = Global.getLogger(VanillaMapTooltipProbe.class);

    // The map's tooltip type, matched by name up the returned value's class hierarchy. Its expandable
    // subclass is what the map actually shows, so the walk up from the runtime class finds this.
    private static final String TOOLTIP_CLASS_NAME = "com.fs.starfarer.ui.impl.StandardTooltipV2";

    // The accessors this probe names for itself: a host's current tooltip, and (to tell a shown tooltip
    // from a merely-configured one) the tooltip's fade state. The hops down to the tab and a component's
    // children are {@link CoreUiTree}'s. All are part of the core UI's contract, so they survive
    // obfuscation.
    private static final String GET_TOOLTIP_METHOD = "getTooltip";
    private static final String GET_FADER_METHOD = "getFader";
    private static final String IS_FADED_OUT_METHOD = "isFadedOut";

    // The widget to search under, read afresh at every ask: which surface owns the frame changes as
    // the player opens and closes screens, so a root taken once would describe a screen that is gone.
    private final Supplier<Object> readSearchRoot;

    // Says once per session that this read broke, rather than every frame. Per instance rather than
    // per class, since each consumer holds its own probe and a shared flag would let one consumer's
    // broken read silence the news of another's.
    private final SessionWarning warning = new SessionWarning(LOG);

    // The last diagnostic line logged, so the probe narrates only when its outcome changes rather
    // than every frame the map is up.
    private String lastLoggedOutcome;

    /** Searches the core tab the player has open - the root a caller reading a vanilla host wants. */
    public VanillaMapTooltipProbe() {
        this(CoreUiTree::resolveCurrentTab);
    }

    /**
     * @param readSearchRoot the widget whose subtree holds the map surface being reasoned about, and
     *                       null on a frame that shows none; it may raise, which counts as a failed
     *                       read like any other hop into the live tree
     */
    public VanillaMapTooltipProbe(Supplier<Object> readSearchRoot) {
        this.readSearchRoot = readSearchRoot;
    }

    /**
     * @return the core-UI component the vanilla map screen is currently showing as a tooltip, or
     *         {@code null} when none is up; {@code null} on any read failure too, so the caller
     *         falls back to its own drawing rather than acting on a broken read
     */
    public Object findShownTooltip() {
        try {

            var searchRoot = readSearchRoot.get();

            if (searchRoot == null) {
                reportReachFailure("no search root");
                return null;
            }
            // Build the diagnostic trace only when DEBUG is on, so a normal frame is a bare tree walk
            // with no per-node string work.
            var trace = LOG.isDebugEnabled()
                ? new WalkTrace(searchRoot.getClass().getName())
                : null;

            var tooltip = searchSubtreeForShownTooltip(searchRoot, ProbeLimits.MAX_SEARCH_DEPTH, trace);

            reportWalkOutcome(tooltip != null, trace);
            return tooltip;

        } catch (Throwable failure) {

            warnOnce(failure);
            return null;
        }
    }

    /**
     * @return whether the vanilla map screen is currently drawing a tooltip; {@code false} on any
     *         read failure, so the caller draws rather than hides on a broken read
     */
    public boolean isTooltipShowing() {
        return findShownTooltip() != null;
    }

    // The tooltip a component is currently showing, via the same getTooltip the core UI reads; null for
    // a component that is not a tooltip host (no such method - the common leaf) or that shows none now. A
    // host clears this to null when its tooltip hides, so a non-null value means one is up.
    //
    // The forgiving hop, because both ways it comes back empty are ordinary here: most components
    // carry no getTooltip and are simply not hosts, and a host failing from inside its own accessor
    // shows nothing this frame while staying a host on the next one. Neither is remembered against
    // the widget, so one bad frame cannot blind the walk to that widget's tooltips for the rest of
    // the run.
    static Object findTooltipShownBy(Object component) {
        return CoreUiTree.readHopIfOffered(component, GET_TOOLTIP_METHOD);
    }

    // Whether the tooltip is actually on screen rather than merely configured on a widget: its fader is
    // not faded out. A tooltip a widget holds but has never shown sits idle at zero brightness, which
    // reads as faded out and so does not suppress our overlay. Any unreadable fader returns false, so an
    // uncertain read leaves our overlay drawing (fail-open) rather than hiding it on a guess.
    static boolean isTooltipVisible(Object tooltip) {
        try {
            var fader = CoreUiTree.invokeNoArg(tooltip, GET_FADER_METHOD);

            return fader != null
                && CoreUiTree.invokeNoArg(fader, IS_FADED_OUT_METHOD) instanceof Boolean fadedOut
                && !fadedOut;

        } catch (Throwable cannotReadFader) {
            return false;
        }
    }

    // Whether a class, or any class it descends from, carries the given name. By name up the hierarchy
    // rather than by Class identity, so a classloader mismatch between the mod and the game class cannot
    // make a real widget read as foreign, and the game's own subclass of a type reads as that type. The
    // walk stops at Object, which every class reaches and none is identified by.
    static boolean isNamedInHierarchy(Class<?> type, String className) {

        for (var clazz = type;
                clazz != null && clazz != Object.class;
                clazz = clazz.getSuperclass()) {

            if (clazz.getName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    // The first live vanilla tooltip in this subtree, or null when none is up. Recurses the panel's
    // children by depth, stopping at the bound so a malformed tree cannot loop the walk. Records every
    // shown tooltip into the trace (when one is given, i.e. DEBUG is on) so a walk that finds no vanilla
    // tooltip still says what it saw.
    private static Object searchSubtreeForShownTooltip(
            Object component,
            int depthRemaining,
            WalkTrace trace) {

        if (component == null || depthRemaining < 0) {
            return null;
        }
        if (trace != null) {
            trace.nodesVisited++;
        }

        var tooltip = findTooltipShownBy(component);

        if (tooltip != null && isStandardTooltip(tooltip)) {

            // A widget can hold a configured tooltip whose fader sits idle at zero (never hovered), which
            // must not suppress our overlay - only a tooltip actually faded in should. So gate on the
            // fader, the same read vanilla does before it renders a child's tooltip.
            var visible = isTooltipVisible(tooltip);

            if (trace != null) {
                trace.recordShownTooltip(tooltip, visible);
            }
            if (visible) {
                return tooltip;
            }
        }
        for (var child : CoreUiTree.readChildrenOf(component)) {

            var found = searchSubtreeForShownTooltip(child, depthRemaining - 1, trace);

            if (found != null) {
                return found;
            }
        }
        return null;
    }

    // Whether the tooltip's runtime class is, or descends from, the vanilla tooltip class. Its
    // expandable subclass is what the map actually shows, so the walk up from the runtime class is what
    // finds the type.
    private static boolean isStandardTooltip(Object tooltip) {
        return isNamedInHierarchy(tooltip.getClass(), TOOLTIP_CLASS_NAME);
    }

    // The walk never started: the root to search under answered nothing, so there is no verdict to
    // explain beyond why. Separate from the outcome below because the two say different things with
    // different material - one names a hop, the other describes a completed walk - and a single
    // reporter taking both would take one of them as null at each of its call sites.
    private void reportReachFailure(String reachFailure) {

        // Guarded before the line is composed rather than inside the emit, because this runs per frame
        // and the composition is the only cost either reporter has when nobody is listening.
        if (LOG.isDebugEnabled()) {
            logOutcomeChange("verdict=false (" + reachFailure + ")");
        }
    }

    // The walk ran: the verdict, and what it saw on the way when the trace was built.
    private void reportWalkOutcome(boolean verdict, WalkTrace trace) {

        if (LOG.isDebugEnabled()) {
            logOutcomeChange("verdict=" + verdict
                + " " + (trace == null ? "" : trace.describeWalk()));
        }
    }

    // Emits one outcome the first time the probe reaches it and on every change after. A stable outcome
    // logs once; a tooltip appearing or disappearing logs the transition, so the log shows what the
    // probe saw without a per-frame flood.
    private void logOutcomeChange(String outcome) {

        if (outcome.equals(lastLoggedOutcome)) {
            return;
        }
        lastLoggedOutcome = outcome;
        LOG.debug("Vanilla map-tooltip probe: " + outcome);
    }

    private void warnOnce(Throwable failure) {
        warning.warnOnce(
            "Could not read the vanilla map tooltip state by reflection; "
                + "the overlay tooltip will not suppress for it. "
                + "This is safe but means both may show over a star icon.",
            failure);
    }

    // Accumulates what one walk saw - the root it started at, how many nodes it visited, and the
    // tooltips it found shown - so a walk that finds no vanilla tooltip can still say whether it
    // reached the map's host at all and what it held. The root is named because it is now the
    // caller's choice, so a walk that searched the wrong surface says which one it searched.
    private static final class WalkTrace {

        private final String rootClassName;
        private final List<String> shownTooltips = new ArrayList<>();
        private int nodesVisited;

        private WalkTrace(String rootClassName) {
            this.rootClassName = rootClassName;
        }

        // Notes one found tooltip's class and whether it read as visible, up to the trace cap so a busy
        // tree logs a readable sample. The visibility is what separates "found a tooltip but it was
        // faded out" from "found a shown one", the distinction a wrong suppression is diagnosed against.
        private void recordShownTooltip(Object tooltip, boolean visible) {

            if (shownTooltips.size() < ProbeLimits.MAX_REPORTED_ITEMS) {
                shownTooltips.add(tooltip.getClass().getName() + "(visible=" + visible + ")");
            }
        }

        private String describeWalk() {
            return "root=" + rootClassName
                + " visited=" + nodesVisited
                + " shownTooltips=" + shownTooltips;
        }
    }
}
