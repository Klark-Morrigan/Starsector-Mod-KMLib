package kmlib.starsector.ui.input;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link KeyedHoverArrival} over a row: an arrival is the element under the pointer changing to an
 * element, so crossing straight from one to its neighbour counts and leaving the row does not.
 *
 * <p>And the other way a frame steps it - taking what is under the pointer without announcing it, for a
 * row that moved rather than a pointer that did. What those cases pin is that it stays a latch: silent for
 * the element it took, and answering the pointer's own moves afterwards.
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
    class AdoptArrivalAt {

        @Test
        void adoptArrivalAtReportsNothingForTheElementItTook() {
            // The row moved under a still pointer, so what is now under it was reached by nobody - and the
            // very next frame must not report it either, the adoption being what stops the moment coming
            // one frame late instead of not at all.
            hoverArrival.adoptArrivalAt(FIRST_ELEMENT);

            assertThat(hoverArrival.detectArrivalAt(FIRST_ELEMENT))
                .isFalse();
        }

        @Test
        void adoptArrivalAtLeavesTheLatchAnsweringThePointersOwnMoves() {
            // Adopted rather than gone deaf: the pointer genuinely moving on to what the row carried under
            // it is an arrival like any other, so the element has to be reachable again once left.
            hoverArrival.adoptArrivalAt(FIRST_ELEMENT);
            hoverArrival.detectArrivalAt(NO_ELEMENT_HOVERED);

            assertThat(hoverArrival.detectArrivalAt(FIRST_ELEMENT))
                .isTrue();
        }

        @Test
        void adoptArrivalAtDropsTheElementThePointerWasOnBeforeTheRowMoved() {
            // A row that moves away from under the pointer leaves it on nothing, and the element it was on
            // must not stay latched - coming back to that element afterwards is an arrival.
            hoverArrival.detectArrivalAt(FIRST_ELEMENT);
            hoverArrival.adoptArrivalAt(NO_ELEMENT_HOVERED);

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
