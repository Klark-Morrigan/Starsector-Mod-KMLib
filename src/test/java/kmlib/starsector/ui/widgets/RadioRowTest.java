package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link RadioRow}'s geometry: the row divides into equal-width segments left to right, a
 * point resolves to the segment it falls in (the left one on a shared edge, none when outside the
 * row), and a press on the already-selected segment resolves to no actionable element.
 */
class RadioRowTest {

    @Nested
    class SplitIntoSegments {
        private final Rectangle row = new Rectangle(0f, 0f, 100f, 20f);

        @Test
        void dividesTheRowIntoEqualWidthSegmentsLeftToRight() {
            assertThat(RadioRow.splitIntoSegments(row, 2))
                    .containsExactly(new Rectangle(0f, 0f, 50f, 20f),
                            new Rectangle(50f, 0f, 50f, 20f));
        }

        @Test
        void yieldsNoSegmentsForANonPositiveCount() {
            assertThat(RadioRow.splitIntoSegments(row, 0)).isEmpty();
        }
    }

    @Nested
    class FindSegmentIndexAt {
        private final Rectangle row = new Rectangle(0f, 0f, 100f, 20f);

        @Test
        void findsTheSegmentAPointFallsIn() {
            assertThat(RadioRow.findSegmentIndexAt(row, 2, 75f, 10f)).isEqualTo(1);
        }

        @Test
        void resolvesASharedEdgeToTheLeftSegment() {
            assertThat(RadioRow.findSegmentIndexAt(row, 2, 50f, 10f)).isEqualTo(0);
        }

        @Test
        void reportsNoSegmentForAPointOutsideTheRow() {
            assertThat(RadioRow.findSegmentIndexAt(row, 2, 150f, 10f))
                    .isEqualTo(RadioRow.NO_SEGMENT);
        }
    }

    @Nested
    class FindHitElement {
        private final Rectangle row = new Rectangle(0f, 0f, 100f, 20f);

        @Test
        void findsTheHitSegmentWhenItIsNotTheSelectedOne() {
            assertThat(RadioRow.findHitElement(row, 2, 0, 75f, 10f)).isEqualTo(1);
        }

        @Test
        void reportsNoElementWhenTheHitIsTheAlreadySelectedSegment() {
            assertThat(RadioRow.findHitElement(row, 2, 1, 75f, 10f))
                    .isEqualTo(RadioRow.NO_SEGMENT);
        }

        @Test
        void reportsNoElementForAPointOutsideTheRow() {
            assertThat(RadioRow.findHitElement(row, 2, 0, 150f, 10f))
                    .isEqualTo(RadioRow.NO_SEGMENT);
        }
    }
}
