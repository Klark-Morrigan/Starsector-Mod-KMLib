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
    class SplitIntoGrid {

        @Test
        void placesOptionsColumnMajorAcrossTwoColumns() {
            // Five options across two columns: the first column holds three rows (0,1,2) and the
            // second the remaining two (3,4), each column a divide-rounding-up three rows tall. A
            // 60-wide, 90-tall list gives 30-wide columns and 30-tall rows.
            var grid = RadioRow.splitIntoGrid(new Rectangle(0f, 0f, 60f, 90f), 5, 2);
            assertThat(grid).containsExactly(
                new Rectangle(0f, 60f, 30f, 30f),
                new Rectangle(0f, 30f, 30f, 30f),
                new Rectangle(0f, 0f, 30f, 30f),
                new Rectangle(30f, 60f, 30f, 30f),
                new Rectangle(30f, 30f, 30f, 30f));
        }

        @Test
        void reducesToThePlainVerticalStackForOneColumn() {
            // One column is the ordinary top-to-bottom split, so it matches splitIntoSegments vertical
            // rectangle for rectangle - the grid is the general case the single stack is a case of.
            var bounds = new Rectangle(0f, 0f, 40f, 100f);
            assertThat(RadioRow.splitIntoGrid(bounds, 4, 1))
                .isEqualTo(RadioRow.splitIntoSegments(bounds, 4, RadioAlignment.VERTICAL));
        }

        @Test
        void yieldsNoSegmentsForANonPositiveOptionOrColumnCount() {
            var bounds = new Rectangle(0f, 0f, 40f, 100f);
            assertThat(RadioRow.splitIntoGrid(bounds, 0, 2)).isEmpty();
            assertThat(RadioRow.splitIntoGrid(bounds, 4, 0)).isEmpty();
        }
    }

    @Nested
    class ComputeRowsPerColumn {

        @Test
        void roundsUpSoAPartlyFilledLastColumnStillGetsARow() {
            // Five options across two columns need three cells per column (the first column holds three,
            // the second two), so the divide rounds up rather than truncating the last option off the
            // grid.
            assertThat(RadioRow.computeRowsPerColumn(5, 2)).isEqualTo(3);
        }

        @Test
        void isTheOptionCountForASingleColumn() {
            // One column stacks every option, so the tallest column is the whole option count.
            assertThat(RadioRow.computeRowsPerColumn(4, 1)).isEqualTo(4);
        }

        @Test
        void isZeroForANonPositiveOptionOrColumnCount() {
            assertThat(RadioRow.computeRowsPerColumn(0, 2)).isZero();
            assertThat(RadioRow.computeRowsPerColumn(4, 0)).isZero();
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
}
