package kmlib.starsector.ui.input;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link HoverArrival} as a moment rather than a state: it reports the frame the pointer reaches an
 * element and nothing on the frames it rests there, which is the whole difference between this and the
 * hover fade an element keeps beside it.
 */
final class HoverArrivalTest {

    private static final boolean HOVERED = true;
    private static final boolean NOT_HOVERED = false;

    private final HoverArrival hoverArrival = new HoverArrival();

    @Nested
    class DetectArrival {

        @Test
        void detectArrivalReportsTheFrameThePointerReachesTheElement() {
            assertThat(hoverArrival.detectArrival(HOVERED))
                .isTrue();
        }

        @Test
        void detectArrivalReportsNothingWhileThePointerRestsOnTheElement() {
            // The pointer parked on an element holds its fade at the top for as long as it stays; an
            // arrival read that way would fire every frame, which as a sound is a tone rather than a tick.
            hoverArrival.detectArrival(HOVERED);

            assertThat(hoverArrival.detectArrival(HOVERED))
                .isFalse();
        }

        @Test
        void detectArrivalReportsNothingAsThePointerLeavesTheElement() {
            // Leaving reaches nothing, so it is not an arrival.
            hoverArrival.detectArrival(HOVERED);

            assertThat(hoverArrival.detectArrival(NOT_HOVERED))
                .isFalse();
        }

        @Test
        void detectArrivalReportsThePointerComingBackAfterLeaving() {

            hoverArrival.detectArrival(HOVERED);
            hoverArrival.detectArrival(NOT_HOVERED);

            assertThat(hoverArrival.detectArrival(HOVERED))
                .isTrue();
        }
    }

    @Nested
    class ResetArrival {

        @Test
        void resetArrivalMakesAPointerParkedOnTheElementArriveAfresh() {
            // The element came to the pointer rather than the other way about - it was not there a moment
            // ago - so the player reads that as an arrival however still the pointer was.
            hoverArrival.detectArrival(HOVERED);
            hoverArrival.resetArrival();

            assertThat(hoverArrival.detectArrival(HOVERED))
                .isTrue();
        }
    }
}
