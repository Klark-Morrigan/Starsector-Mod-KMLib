package kmlib.starsector.ui.map.presence;

import kmlib.starsector.ui.intel.IntelScreenView;
import kmlib.starsector.ui.intel.VanillaIntelScreenView;

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
 * <p>{@link #isAnyMapShowing()} is the cheapest of the three and not merely their disjunction: no
 * filter is read on either host, because the filter chooses which look a showing map wears and never
 * whether one is showing. So it resolves a step earlier than the look-aware pair, asking each host
 * one question where they ask two.
 *
 * <p>Host-blind is what these answers are for and the limit of what they can be used for: each says
 * such a map is somewhere on screen, never which host is showing it. A question about one host -
 * whether to draw into that host, or where - is answered by reading that host instead.
 *
 * <p>Both sources arrive as injected ports - the sector map as a supplier of its
 * {@link SectorMapState}, the intel screen as {@link IntelScreenView} - because the live readings
 * are statics and game-only widget walks that cannot be stood up in a test. The sector side arrives
 * as that one enumerated state rather than as a boolean per question, so no caller and no test can
 * describe a map that is absent and filtered at once. The intel side pairs its filter read with the
 * visor rectangle: the preview's panel survives a switch to the sibling sub-tabs that share the
 * intel tab, so its filter state can still read either way while no visor is on screen at all, and
 * the rectangle is the signal that says one is. Both ports fail closed, so an unreadable host
 * reports no map.
 */
public final class MapPresence {
    private final IntelScreenView intelScreen;
    private final Supplier<SectorMapState> readSectorMapState;

    /** Reads the live sector map and the live intel screen - the pairing outside a test. */
    public MapPresence() {
        this(CampaignMapView::resolveSectorMapState, new VanillaIntelScreenView());
    }

    MapPresence(Supplier<SectorMapState> readSectorMapState, IntelScreenView intelScreen) {
        this.readSectorMapState = readSectorMapState;
        this.intelScreen = intelScreen;
    }

    /**
     * @return whether either host is showing a map - the sector map in its Sector sub-view, or a lit
     *         map visor - with either host's Starscape filter set either way
     */
    public boolean isAnyMapShowing() {
        if (readSectorMapState.get().isShowing()) {
            return true;
        }
        return intelScreen.getMapVisorRect() != null;
    }

    /**
     * @return whether either host is showing a map drawing the ordinary schematic - the sector map
     *         with its own filter off, or a lit map visor with the intel screen's filter off
     */
    public boolean isSchematicMapShowing() {
        if (readSectorMapState.get() == SectorMapState.SHOWING_WITH_STARSCAPE_OFF) {
            return true;
        }
        return intelScreen.getMapVisorRect() != null && !intelScreen.isMapStarscapeModeOn();
    }

    /**
     * @return whether either host is showing a map drawing the Starscape starfield - the sector map
     *         with its own filter on, or a lit map visor with the intel screen's filter on
     */
    public boolean isStarscapeMapShowing() {
        if (readSectorMapState.get() == SectorMapState.SHOWING_IN_STARSCAPE_MODE) {
            return true;
        }
        return intelScreen.getMapVisorRect() != null && intelScreen.isMapStarscapeModeOn();
    }
}
