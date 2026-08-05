package kmlib.starsector.ui.map.presence;

import kmlib.starsector.ui.intel.IntelScreenView;
import kmlib.starsector.ui.intel.VanillaIntelScreenView;

import java.util.function.BooleanSupplier;

/**
 * Answers whether a map is on screen at all, on whichever of the two hosts is up - the full sector
 * map, or the intel screen's embedded map preview (its "map visor") - and whichever of the two looks
 * that map is drawing. Code that follows the map wherever the game is showing one, without caring
 * which picture is under it, asks here.
 *
 * <p>This reads as the disjunction of {@link SchematicMapPresence} and {@link StarscapeMapPresence}
 * and is deliberately not built as one. Neither host's Starscape filter is consulted: the filter
 * chooses which look a showing map wears, never whether a map is showing, so the question resolves a
 * step earlier than either sibling's does. The gain is not only brevity - each host is asked once
 * instead of twice, and the intel side's answer costs a walk of the live widget tree, which a
 * disjunction of the two siblings pays for up to four times over where this pays once.
 *
 * <p>Host-blind is what this answer is for and the limit of what it can be used for: it says a map
 * is somewhere on screen, never which host is showing it, and never which look it wears. A question
 * about one host - whether to draw into that host, or where - is answered by reading that host
 * instead, and a question about the look is one of the siblings'.
 *
 * <p>Both sources arrive as injected ports - the sector read as a boolean supplier, the intel screen
 * as {@link IntelScreenView} - because the live readings are statics and game-only widget walks that
 * cannot be stood up in a test. The visor rectangle is the whole of the intel term: the preview's
 * panel survives a switch to the sibling sub-tabs that share the intel tab, so the rectangle is the
 * signal that says one is actually on screen. Both ports fail closed, so an unreadable host reports
 * no map.
 */
public final class AnyMapPresence {
    private final IntelScreenView intelScreen;
    private final BooleanSupplier isSectorMapShowing;

    /** Reads the live sector map and the live intel screen - the pairing outside a test. */
    public AnyMapPresence() {
        this(CampaignMapView::isSectorMapShowing, new VanillaIntelScreenView());
    }

    AnyMapPresence(BooleanSupplier isSectorMapShowing, IntelScreenView intelScreen) {
        this.isSectorMapShowing = isSectorMapShowing;
        this.intelScreen = intelScreen;
    }

    /**
     * @return whether either host is showing a map - the sector map in its Sector sub-view, or a lit
     *         map visor - with either host's Starscape filter set either way
     */
    public boolean isAnyMapShowing() {
        if (isSectorMapShowing.getAsBoolean()) {
            return true;
        }
        return intelScreen.getMapVisorRect() != null;
    }
}
