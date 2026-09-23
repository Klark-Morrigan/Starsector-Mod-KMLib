package kmlib.starsector.ui.coreui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the holder that lets an overlay of this library's own be seen as a modal.
 *
 * <p>The holder outlives a case, so every reading handed over here is taken back afterwards;
 * one left behind would make the next suite to read the modal presence see a screen held by
 * nobody.
 */
final class ModalOverlaysTest {

    private static final OverlayPresence RAISED = new OverlayPresence(true, 1f);

    // An overlay that has handed the screen back and is still painting its way out.
    private static final OverlayPresence FALLING = new OverlayPresence(false, 0.4f);

    private final Supplier<OverlayPresence> raisedOverlay = () -> RAISED;

    private final Supplier<OverlayPresence> fallingOverlay = () -> FALLING;

    private final Supplier<OverlayPresence> loweredOverlay = () -> OverlayPresence.NONE;

    @AfterEach
    void releaseEverythingHeld() {

        ModalOverlays.releaseScreen(raisedOverlay);
        ModalOverlays.releaseScreen(fallingOverlay);
        ModalOverlays.releaseScreen(loweredOverlay);
    }

    @Nested
    class ResolveShowingPresence {

        @Test
        void answersNothingWhereNothingIsHeld() {

            assertThat(ModalOverlays.resolveShowingPresence())
                .isEqualTo(OverlayPresence.NONE);
        }

        @Test
        void answersTheReadingOfAnOverlayThatIsUp() {

            ModalOverlays.holdScreen(raisedOverlay);

            assertThat(ModalOverlays.resolveShowingPresence())
                .isEqualTo(RAISED);
        }

        @Test
        void answersNothingForAnOverlayHeldButNotUp() {

            // A reading is asked each time rather than believed once: a panel that has come down
            // but not yet taken its reading back reports itself lowered.
            ModalOverlays.holdScreen(loweredOverlay);

            assertThat(ModalOverlays.resolveShowingPresence())
                .isEqualTo(OverlayPresence.NONE);
        }

        @Test
        void answersTheReadingOfAnOverlayThatHasLetGoAndIsStillFading() {

            // The fall is the half that matters here. Whatever thinned itself under the overlay
            // rides this fraction back up, so cutting the reading off at the press would snap it.
            ModalOverlays.holdScreen(fallingOverlay);

            assertThat(ModalOverlays.resolveShowingPresence())
                .isEqualTo(FALLING);
        }

        @Test
        void answersNothingOnceTheOverlayIsReleased() {

            ModalOverlays.holdScreen(raisedOverlay);
            ModalOverlays.releaseScreen(raisedOverlay);

            assertThat(ModalOverlays.resolveShowingPresence())
                .isEqualTo(OverlayPresence.NONE);
        }
    }

    @Nested
    class HoldScreen {

        @Test
        void holdsAReadingOnceHoweverOftenItIsHandedOver() {

            ModalOverlays.holdScreen(raisedOverlay);
            ModalOverlays.holdScreen(raisedOverlay);
            ModalOverlays.releaseScreen(raisedOverlay);

            // One release undoes both holds, or a panel raised twice would hold the screen after
            // it came down.
            assertThat(ModalOverlays.resolveShowingPresence())
                .isEqualTo(OverlayPresence.NONE);
        }
    }
}
