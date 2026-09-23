package kmlib.starsector.ui.coreui;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * The overlays this library has raised over a core screen and that hold it while they are up, so
 * that anything standing aside for a modal stands aside for these too.
 *
 * <p>A panel stood in the core UI's own tree through {@link CoreUiOverlayPanels} is an ordinary
 * child, and nothing reading the game's modal base sees it. So a panel that means to be modal
 * says so here, and {@link CoreUiDialogView} reads this beside the game's own modal - one answer
 * to "is something holding the screen", whichever raised it. Without that, a host that stands its
 * input down for the game's dialogs would keep dispatching under a dialog of ours.
 *
 * <p>Held as presences rather than as flags, so a caller that fades against a modal fades against
 * these on the same curve. Each overlay hands over its own reading and takes it back when it comes
 * down; what is stored is the reading, never the panel, so nothing here outlives the screen a panel
 * was on.
 *
 * <p>One holder for the session, like the record the compatibility channel drains: there is one
 * screen and one player in front of it, and a second overlay raised over the first is a fact this
 * has to be able to see.
 */
public final class ModalOverlays {

    // Every reading currently handed over, in the order they were. Copy-on-write because holds and
    // releases are rare and reads are per frame from whatever stands aside.
    private static final List<Supplier<OverlayPresence>> RAISED_OVERLAYS = new CopyOnWriteArrayList<>();

    private ModalOverlays() {
    }

    /**
     * Hands over an overlay's reading, for as long as it stays handed over.
     *
     * @param presence what the overlay is doing this frame, asked each time anything reads here
     */
    public static void holdScreen(Supplier<OverlayPresence> presence) {

        Objects.requireNonNull(presence, "An overlay with no reading could not say whether it is up.");

        if (!RAISED_OVERLAYS.contains(presence)) {
            RAISED_OVERLAYS.add(presence);
        }
    }

    /**
     * Takes an overlay's reading back, and does nothing where it was never handed over.
     *
     * @param presence the same reading that was handed over
     */
    public static void releaseScreen(Supplier<OverlayPresence> presence) {

        RAISED_OVERLAYS.remove(presence);
    }

    /**
     * What the overlay on screen is doing, or nothing where none is.
     *
     * <p>An overlay counts while it is still fading as well as while it holds the screen, so that
     * whatever rides its fade has a curve to ride all the way down. A reading is asked each time
     * rather than believed once, so a panel that has come down but not yet handed its reading back
     * reports itself gone.
     *
     * <p>The first showing reading wins where two are up, which is the one raised first: an overlay
     * over another is the second's problem to stand aside for, not this read's to arbitrate.
     *
     * @return the showing overlay's presence, or {@link OverlayPresence#NONE}
     */
    public static OverlayPresence resolveShowingPresence() {

        for (var presence : RAISED_OVERLAYS) {
            var reading = presence.get();

            if (reading.isShowing()) {
                return reading;
            }
        }
        return OverlayPresence.NONE;
    }
}
