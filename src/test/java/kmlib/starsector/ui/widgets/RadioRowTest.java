package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.RadioAlignment;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link RadioRow}'s geometry: the row divides into equal cells in its flow direction
 * (horizontal columns left to right, vertical rows top to bottom), a point resolves to the segment
 * it falls in (the earlier one on a shared edge, none when outside), and a press on the
 * already-selected segment resolves to no actionable element while a raw hit still reports it.
 */
class RadioRowTest {

    @Nested
    class SplitIntoSegments {
        private final Rectangle row = new Rectangle(0f, 0f, 100f, 20f);

        @Test
        void dividesTheRowIntoEqualWidthSegmentsLeftToRightWhenHorizontal() {
            assertThat(RadioRow.splitIntoSegments(row, 2, RadioAlignment.HORIZONTAL))
                    .containsExactly(new Rectangle(0f, 0f, 50f, 20f),
                            new Rectangle(50f, 0f, 50f, 20f));
        }

        @Test
        void stacksEqualHeightSegmentsTopToBottomWhenVertical() {
            // A 100-tall column split in two: element 0 is the top half (y=50), element 1 the
            // bottom half (y=0), each full row width.
            assertThat(RadioRow.splitIntoSegments(new Rectangle(0f, 0f, 40f, 100f), 2,
                    RadioAlignment.VERTICAL))
                    .containsExactly(new Rectangle(0f, 50f, 40f, 50f),
                            new Rectangle(0f, 0f, 40f, 50f));
        }

        @Test
        void yieldsNoSegmentsForANonPositiveCount() {
            assertThat(RadioRow.splitIntoSegments(row, 0, RadioAlignment.HORIZONTAL)).isEmpty();
        }
    }

    @Nested
    class FindSegmentIndexAt {
        private final List<Rectangle> segments =
                RadioRow.splitIntoSegments(new Rectangle(0f, 0f, 100f, 20f), 2,
                        RadioAlignment.HORIZONTAL);

        @Test
        void findsTheSegmentAPointFallsIn() {
            assertThat(RadioRow.findSegmentIndexAt(segments, 75f, 10f)).isEqualTo(1);
        }

        @Test
        void resolvesASharedEdgeToTheEarlierSegment() {
            assertThat(RadioRow.findSegmentIndexAt(segments, 50f, 10f)).isEqualTo(0);
        }

        @Test
        void reportsNoSegmentForAPointOutsideTheRow() {
            assertThat(RadioRow.findSegmentIndexAt(segments, 150f, 10f))
                    .isEqualTo(RadioRow.NO_SEGMENT);
        }

        @Test
        void findsTheStackedSegmentAPointFallsInWhenVertical() {
            // Top row is element 0 (y in [50,100]), bottom is element 1 (y in [0,50]).
            var vertical = RadioRow.splitIntoSegments(new Rectangle(0f, 0f, 40f, 100f), 2,
                    RadioAlignment.VERTICAL);
            assertThat(RadioRow.findSegmentIndexAt(vertical, 20f, 75f)).isEqualTo(0);
            assertThat(RadioRow.findSegmentIndexAt(vertical, 20f, 25f)).isEqualTo(1);
        }
    }

    @Nested
    class FindHitElement {
        private final List<Rectangle> segments =
                RadioRow.splitIntoSegments(new Rectangle(0f, 0f, 100f, 20f), 2,
                        RadioAlignment.HORIZONTAL);

        @Test
        void findsTheHitSegmentWhenItIsNotTheSelectedOne() {
            assertThat(RadioRow.findHitElement(segments, 0, 75f, 10f)).isEqualTo(1);
        }

        @Test
        void reportsNoElementWhenTheHitIsTheAlreadySelectedSegment() {
            assertThat(RadioRow.findHitElement(segments, 1, 75f, 10f))
                    .isEqualTo(RadioRow.NO_SEGMENT);
        }

        @Test
        void reportsNoElementForAPointOutsideTheRow() {
            assertThat(RadioRow.findHitElement(segments, 0, 150f, 10f))
                    .isEqualTo(RadioRow.NO_SEGMENT);
        }
    }
}
