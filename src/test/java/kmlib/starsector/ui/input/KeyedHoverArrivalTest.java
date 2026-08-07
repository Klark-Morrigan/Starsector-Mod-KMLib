package kmlib.starsector.ui.input;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link KeyedHoverArrival} over a row: an arrival is the element under the pointer changing to an
 * element, so crossing straight from one to its neighbour counts and leaving the row does not.
 */
final class KeyedHoverArrivalTest {

    private static final int FIRST_ELEMENT = 0;
    private static final int SECOND_ELEMENT = 1;

    // What the row's hit-test reports with the pointer on none of its elements, named so a call reads as a
    // pointer position rather than as a bare null.
    private static final Integer NO_ELEMENT_HOVERED = null;

    private final KeyedHoverArrival<Integer> hoverArrival = new KeyedHoverArrival<>();

    @Nested
    class DetectArrivalAt {

        @Test
        void detectArrivalAtReportsThePointerComingOntoTheRow() {
            assertThat(hoverArrival.detectArrivalAt(FIRST_ELEMENT))
                .isTrue();
        }

        @Test
        void detectArrivalAtReportsCrossingStraightToTheNeighbouringElement() {
            // The common move on a row of abutting elements: the pointer never leaves the row, so an
            // arrival detected only from off the row would answer the first element and then nothing else.
            hoverArrival.detectArrivalAt(FIRST_ELEMENT);

            assertThat(hoverArrival.detectArrivalAt(SECOND_ELEMENT))
                .isTrue();
        }

        @Test
        void detectArrivalAtReportsNothingWhileThePointerRestsOnAnElement() {
            hoverArrival.detectArrivalAt(FIRST_ELEMENT);

            assertThat(hoverArrival.detectArrivalAt(FIRST_ELEMENT))
                .isFalse();
        }

        @Test
        void detectArrivalAtReportsNothingAsThePointerLeavesTheRow() {
            hoverArrival.detectArrivalAt(FIRST_ELEMENT);

            assertThat(hoverArrival.detectArrivalAt(NO_ELEMENT_HOVERED))
                .isFalse();
        }

        @Test
        void detectArrivalAtReportsTheSameElementReachedAgainAfterLeavingTheRow() {
            // Leaving clears which element was reached, so coming back to it is a fresh arrival rather
            // than a continuation of the last one.
            hoverArrival.detectArrivalAt(FIRST_ELEMENT);
            hoverArrival.detectArrivalAt(NO_ELEMENT_HOVERED);

            assertThat(hoverArrival.detectArrivalAt(FIRST_ELEMENT))
                .isTrue();
        }
    }

    @Nested
    class ResetArrival {

        @Test
        void resetArrivalMakesAPointerParkedOnAnElementArriveAfresh() {
            // The row came to the pointer rather than the other way about, which the player reads as an
            // arrival even though the pointer never moved.
            hoverArrival.detectArrivalAt(FIRST_ELEMENT);
            hoverArrival.resetArrival();

            assertThat(hoverArrival.detectArrivalAt(FIRST_ELEMENT))
                .isTrue();
        }
    }
}
