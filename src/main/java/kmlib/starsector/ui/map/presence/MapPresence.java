package kmlib.starsector.ui.map.presence;

import kmlib.starsector.compatibility.GameReachReporter;
import kmlib.starsector.ui.coreui.ClosingCoreTabs;
import kmlib.starsector.ui.intel.IntelScreenView;
import kmlib.starsector.ui.intel.MapVisorState;
import kmlib.starsector.ui.intel.VanillaIntelScreenView;
import kmlib.starsector.ui.map.probes.ShownMapTab;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Answers what the game is showing a map on, over both hosts that can be showing one: the full
 * sector map, and the intel screen's embedded map preview (its "map visor"). Each host keeps its own
 * Starscape filter, so every question here is a disjunction over the two, and code reached through a
 * hook that is not told which host invoked it asks once here instead of repeating an OR that a third
 * host would have to be added to in each caller.
 *
 * <p>Three reads rather than one with a mode argument, because they are three states the game is
 * actually in and not three settings of one: a screen showing no map at all leaves all three false,
 * so none is derivable as another's negation. They share one object because they share their two
 * sources - separate classes per read would build a second binding to the same live widget tree, and
 * a caller wanting two answers would pay for the walk behind them twice.
 *
 * <p>{@link #isAnyVanillaMapShowing()} is the cheapest of the three and not merely their disjunction: no
 * filter is read on either host, because the filter chooses which look a showing map wears and never
 * whether one is showing. So it resolves a step earlier than the look-aware pair, asking each host
 * one question where they ask two.
 *
 * <p>{@link #isAnyVanillaMapDrawn()} widens the any-look read for one gap the game leaves. The core UI
 * stops naming a tab as current before it stops drawing it, so a map the player has just left is
 * still on screen for the length of its fade while the showing reads say it is gone. Drawn answers
 * the questions about painting, such as whether a render pass belongs to a vanilla map. Showing
 * answers the questions about input, since a closing map takes none.
 *
 * <p>Host-blind is what these answers are for and the limit of what they can be used for: each says
 * such a map is somewhere on screen, never which host is showing it. A question about one host -
 * whether to draw into that host, or where - is answered by reading that host instead.
 *
 * <p>Both sources arrive as injected ports - the sector map as a supplier of its
 * {@link SectorMapState}, the intel screen as {@link IntelScreenView} - because the live readings
 * are statics and game-only widget walks that nothing outside a running game can stand up. The
 * sector side arrives as that one enumerated state rather than as a boolean per question, so no
 * caller can describe a map that is absent and filtered at once. The intel side answers the
 * look-aware pair with a state of the same shape and for the same reason, with one more behind it:
 * its two signals are read off a walk down the live widget tree, so asking them separately would
 * take that walk twice to describe one frame. The closing tabs and the visor test applied to them
 * arrive as ports for the same reason. Every port fails closed, so an unreadable host reports no map.
 *
 * <p>The visor's filter cannot be read against its presence in any case. The preview's panel
 * survives a switch to the sibling sub-tabs that share the intel tab, so its filter state still
 * reads either way while no visor is on screen at all - which is why the intel side's states carry
 * the presence within them rather than leaving a caller to pair two answers.
 */
public final class MapPresence {

    private final IntelScreenView intelScreen;
    private final Supplier<SectorMapState> readSectorMapState;
    private final Supplier<List<Object>> readClosingTabs;
    private final Predicate<Object> isMapVisorLitBelow;

    /**
     * Reads the live sector map and the live intel screen - the pairing a running game gets.
     *
     * @param reporter where a failed read of either host is filed, for the mod gating on the answers.
     *                 One for both: a host that cannot be read costs that mod its map on that host,
     *                 which is one loss whichever host it was
     */
    public MapPresence(GameReachReporter reporter) {
        this(reporter, new VanillaIntelScreenView(reporter));
    }

    private MapPresence(GameReachReporter reporter, VanillaIntelScreenView intelScreen) {
        this(
            () -> CampaignMapView.resolveSectorMapState(reporter),
            intelScreen,
            MapPresence::readClosingTabsOrNone,
            intelScreen::isMapVisorLitBelow);
    }

    MapPresence(
            Supplier<SectorMapState> readSectorMapState,
            IntelScreenView intelScreen,
            Supplier<List<Object>> readClosingTabs,
            Predicate<Object> isMapVisorLitBelow) {

        this.readSectorMapState = readSectorMapState;
        this.intelScreen = intelScreen;
        this.readClosingTabs = readClosingTabs;
        this.isMapVisorLitBelow = isMapVisorLitBelow;
    }

    /**
     * Whether a vanilla map is being painted: one showing, or one inside a tab the player has just
     * left, still fading out.
     *
     * <p>The game stops naming a tab as current before it stops drawing it, so for that fade every
     * other read here answers no map while one is on screen. Ask this for a question about painting,
     * such as whether a render pass belongs to a vanilla map. Ask {@link #isAnyVanillaMapShowing()} for
     * a question about input, since a closing map takes none and nothing should resolve against it.
     *
     * <p>A closing map counts whichever sub-view it was left on. The sub-view decides what that map
     * draws, and a caller asking about a pass is already inside the draw it chose.
     *
     * @return whether a map is showing on either host, or a closing tab holds the sector map or an
     *         intel screen with its visor lit
     */
    public boolean isAnyVanillaMapDrawn() {

        if (isAnyVanillaMapShowing()) {
            return true;
        }

        for (var closingTab : readClosingTabs.get()) {

            // Each test is the one its own read already applies to the tab that is up.
            if (ShownMapTab.isSectorMapTab(closingTab) || isMapVisorLitBelow.test(closingTab)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return whether either host is showing a map - the sector map in its Sector sub-view, or a lit
     *         map visor - with either host's Starscape filter set either way
     */
    public boolean isAnyVanillaMapShowing() {

        if (readSectorMapState.get().isShowing()) {
            return true;
        }

        // The rectangle rather than the combined state, which is the whole of the saving this read
        // is described as making: the filter is never asked, so the visor's own mode is never read.
        return intelScreen.getMapVisorRect() != null;
    }

    /**
     * @return whether either host is showing a map drawing the ordinary schematic - the sector map
     *         with its own filter off, or a lit map visor with the intel screen's filter off
     */
    public boolean isSchematicVanillaMapShowing() {

        if (readSectorMapState.get() == SectorMapState.SHOWING_WITH_STARSCAPE_OFF) {
            return true;
        }
        return intelScreen.readMapVisorState() == MapVisorState.SHOWING_WITH_STARSCAPE_OFF;
    }

    /**
     * @return whether either host is showing a map in Starscape mode - the sector map
     *         with its own filter on, or a lit map visor with the intel screen's filter on
     */
    public boolean isStarscapeVanillaMapShowing() {

        if (readSectorMapState.get() == SectorMapState.SHOWING_IN_STARSCAPE_MODE) {
            return true;
        }
        return intelScreen.readMapVisorState() == MapVisorState.SHOWING_IN_STARSCAPE_MODE;
    }

    // The closing tabs, or none where the core UI cannot be reached. Fails closed like both host
    // ports, and silently: the reach it fails on is the one the intel screen's own walk takes first
    // on every call that gets here, and that walk already says once that it broke.
    private static List<Object> readClosingTabsOrNone() {

        // Over Throwable, as CoreUiTree asks of the callers of its reach.
        try {
            return ClosingCoreTabs.readClosingTabs();

        } catch (Throwable exception) {
            return List.of();
        }
    }
}
