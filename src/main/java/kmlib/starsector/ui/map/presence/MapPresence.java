package kmlib.starsector.ui.map.presence;

import kmlib.starsector.compatibility.GameReachReporter;
import kmlib.starsector.ui.coreui.ClosingCoreTabs;
import kmlib.starsector.ui.intel.IntelScreenView;
import kmlib.starsector.ui.intel.MapVisorState;
import kmlib.starsector.ui.intel.VanillaIntelScreenView;
import kmlib.starsector.ui.map.probes.ShownMapTab;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Answers whether the game is showing a map, over both hosts that can show one: the full sector map,
 * and the intel screen's embedded map preview (its "map visor"). Code reached through a hook that is
 * not told which host invoked it asks here, instead of repeating an OR over the hosts in each caller.
 *
 * <p>Showing and drawn are two questions. The core UI stops naming a tab as current before it stops
 * drawing it, so a map the player has just left is still on screen for the length of its fade. The
 * drawn reads count that map and answer the questions about painting, such as whether a render pass
 * belongs to a vanilla map. The showing reads do not count it and answer the questions about input,
 * since a closing map takes none.
 *
 * <p>Each read is a state the game is in, not a setting of one: a screen with no map leaves every read
 * false, so none is another's negation. The any-look reads ask no filter, which chooses how a map
 * looks and never whether it is there.
 *
 * <p>Host-blind is what these answers are for and the limit of their use: each says such a map is on
 * screen, never which host has it. A question about one host is answered by reading that host.
 *
 * <p>Every source arrives as a port, since the live reads walk a widget tree that only a running game
 * has. Each host answers in one enumerated state, presence and filter together, so no caller can pose
 * a map that is absent and filtered at once - which matters on the intel screen, whose filter still
 * reads while a sibling sub-tab hides the visor. Every port fails closed, so an unreadable host
 * reports no map.
 */
public final class MapPresence {

    private final IntelScreenView intelScreen;
    private final Supplier<SectorMapState> readSectorMapState;
    private final Supplier<SectorMapState> readClosingSectorMapState;
    private final Supplier<List<Object>> readClosingTabs;
    private final Function<Object, MapVisorState> readMapVisorStateBelow;

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

    /**
     * Reads the live sector map and the live intel screen through a view the caller already holds,
     * so a mod reading the intel screen directly as well keeps one view and its warnings once.
     *
     * @param reporter    where a failed read of the sector map is filed, for the mod gating on the
     *                    answers
     * @param intelScreen the live intel screen view, filing its own failures under its own reporter
     */
    public MapPresence(GameReachReporter reporter, VanillaIntelScreenView intelScreen) {
        this(
            () -> CampaignMapView.resolveSectorMapState(reporter),
            () -> CampaignMapView.resolveClosingSectorMapState(reporter),
            intelScreen,
            MapPresence::readClosingTabsOrNone,
            intelScreen::readMapVisorStateBelow);
    }

    MapPresence(
            Supplier<SectorMapState> readSectorMapState,
            Supplier<SectorMapState> readClosingSectorMapState,
            IntelScreenView intelScreen,
            Supplier<List<Object>> readClosingTabs,
            Function<Object, MapVisorState> readMapVisorStateBelow) {

        this.readSectorMapState = readSectorMapState;
        this.readClosingSectorMapState = readClosingSectorMapState;
        this.intelScreen = intelScreen;
        this.readClosingTabs = readClosingTabs;
        this.readMapVisorStateBelow = readMapVisorStateBelow;
    }

    /**
     * @return whether either host is drawing a map, either look: one showing, or one in a tab the
     *         player has just left, still fading out
     */
    public boolean isAnyVanillaMapDrawn() {

        return isAnyVanillaMapShowing()
            || isClosingMapIn(SectorMapState::isShowing, visorState -> visorState != MapVisorState.NOT_SHOWING);
    }

    /**
     * @return whether either host is drawing a map in Starscape mode: one showing, or one in a tab the
     *         player has just left, still fading out
     */
    public boolean isStarscapeVanillaMapDrawn() {

        return isStarscapeVanillaMapShowing()
            || isClosingMapIn(
                sectorMapState -> sectorMapState == SectorMapState.SHOWING_IN_STARSCAPE_MODE,
                visorState -> visorState == MapVisorState.SHOWING_IN_STARSCAPE_MODE);
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

    // Whether a tab still fading out holds a map in a state the caller accepts. A closing sector map
    // is classified off the persisted UI data the closed map left behind, and a closing intel screen
    // off its own visor, each by the read that classifies the tab that is up.
    private boolean isClosingMapIn(
            Predicate<SectorMapState> isSectorMapStateAccepted,
            Predicate<MapVisorState> isVisorStateAccepted) {

        for (var closingTab : readClosingTabs.get()) {

            var isAccepted = ShownMapTab.isSectorMapTab(closingTab)
                ? isSectorMapStateAccepted.test(readClosingSectorMapState.get())
                : isVisorStateAccepted.test(readMapVisorStateBelow.apply(closingTab));

            if (isAccepted) {
                return true;
            }
        }

        return false;
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
