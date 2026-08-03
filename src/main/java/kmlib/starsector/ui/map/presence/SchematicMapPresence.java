package kmlib.starsector.ui.map.presence;

import kmlib.starsector.ui.intel.IntelScreenView;
import kmlib.starsector.ui.intel.VanillaIntelScreenView;

import java.util.function.BooleanSupplier;

/**
 * Answers whether a map drawing the ordinary schematic - the map proper, not the Starscape starfield
 * - is on screen, on whichever of the two hosts is up: the full sector map, or the intel screen's
 * embedded map preview (its "map visor"). Each host keeps its own Starscape filter, so the question is
 * a disjunction over both. Code that draws for the map wherever the game is showing one therefore asks
 * once here instead of every caller repeating an OR that a third host would have to be added to in
 * each of them.
 *
 * <p>This is the counterpart of {@link StarscapeMapPresence} and not its negation: a screen showing no
 * map at all leaves both false, so neither read can be derived from the other. Each names a state the
 * game is actually in, which is why both are asked positively.
 *
 * <p>Host-blind is what this answer is for and the limit of what it can be used for: it says a
 * schematic map is somewhere on screen, never which host is showing it. A question about one host -
 * whether to draw into that host, or where - is answered by reading that host instead.
 *
 * <p>Both sources arrive as injected ports - the sector read as a boolean supplier, the intel screen
 * as {@link IntelScreenView} - because the live readings are statics and game-only widget walks that
 * cannot be stood up in a test. The intel side pairs the starscape read with the visor rectangle: the
 * preview's panel survives a switch to the sibling sub-tabs that share the intel tab, so its filter
 * state can still read as off while no visor is on screen at all, and the rectangle is the signal that
 * says one is. Both ports fail closed, so an unreadable host reports no schematic map.
 */
public final class SchematicMapPresence {
    private final IntelScreenView intelScreen;
    private final BooleanSupplier isSectorMapWithStarscapeOff;

    /** Reads the live sector map and the live intel screen - the pairing outside a test. */
    public SchematicMapPresence() {
        this(CampaignMapView::isSectorMapWithStarscapeOff, new VanillaIntelScreenView());
    }

    SchematicMapPresence(BooleanSupplier isSectorMapWithStarscapeOff, IntelScreenView intelScreen) {
        this.isSectorMapWithStarscapeOff = isSectorMapWithStarscapeOff;
        this.intelScreen = intelScreen;
    }

    /**
     * @return whether either host is showing a map with its Starscape filter off - the sector map in
     *         its Sector sub-view, or a lit map visor drawing the ordinary map
     */
    public boolean isSchematicMapShowing() {
        if (isSectorMapWithStarscapeOff.getAsBoolean()) {
            return true;
        }
        return intelScreen.getMapVisorRect() != null && !intelScreen.isMapStarscapeModeOn();
    }
}
