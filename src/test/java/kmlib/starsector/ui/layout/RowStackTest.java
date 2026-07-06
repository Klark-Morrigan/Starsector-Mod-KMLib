package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the row-stack math: rows hang from the top edge, drop by one height plus one gap each, share
 * the origin's left edge, and take their own widths, so a column reads evenly top to bottom.
 */
class RowStackTest {

    // Three rows, 20 tall with a 4-unit gap, hung from y=200 at x=20. Row tops fall at 200, 176,
    // 152, so their bottom-left y values are 180, 156, 132.
    private static final float ORIGIN_X = 20f;
    private static final float TOP_Y = 200f;
    private static final float ROW_HEIGHT = 20f;
    private static final float ROW_GAP = 4f;
    private static final List<Float> ROW_WIDTHS = List.of(100f, 60f, 80f);

    @Nested
    class LayoutRows {

        @Test
        void hangsTheFirstRowFromTheTopEdge() {
            var rows = RowStack.layoutRows(ORIGIN_X, TOP_Y, ROW_HEIGHT, ROW_GAP, ROW_WIDTHS);

            var first = rows.get(0);
            assertThat(first.y() + first.height()).isEqualTo(TOP_Y);
            assertThat(first.height()).isEqualTo(ROW_HEIGHT);
        }

        @Test
        void dropsEachLaterRowByOneHeightPlusOneGap() {
            var rows = RowStack.layoutRows(ORIGIN_X, TOP_Y, ROW_HEIGHT, ROW_GAP, ROW_WIDTHS);

            for (var index = 1; index < rows.size(); index++) {
                var previous = rows.get(index - 1);
                var current = rows.get(index);
                assertThat(current.y() + current.height()).isEqualTo(previous.y() - ROW_GAP);
            }
        }

        @Test
        void leftAlignsEveryRowAtTheOrigin() {
            var rows = RowStack.layoutRows(ORIGIN_X, TOP_Y, ROW_HEIGHT, ROW_GAP, ROW_WIDTHS);

            assertThat(rows).extracting(Rectangle::x).containsOnly(ORIGIN_X);
        }

        @Test
        void snapsEachRowToItsOwnWidth() {
            var rows = RowStack.layoutRows(ORIGIN_X, TOP_Y, ROW_HEIGHT, ROW_GAP, ROW_WIDTHS);

            assertThat(rows).extracting(Rectangle::width).containsExactly(100f, 60f, 80f);
        }

        @Test
        void returnsNoRowsForNoWidths() {
            var rows = RowStack.layoutRows(ORIGIN_X, TOP_Y, ROW_HEIGHT, ROW_GAP, List.of());

            assertThat(rows).isEmpty();
        }
    }
}
