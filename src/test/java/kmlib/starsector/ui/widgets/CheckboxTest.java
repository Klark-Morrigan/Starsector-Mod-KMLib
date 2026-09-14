package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link Checkbox}'s geometry: the tick box is a square the height of the row, flush with the
 * row's left edge, the label anchors past the box and the shared column gap, and the measured row
 * width reserves exactly that - so the drawn box, the drawn label, and the sized row read the same
 * layout.
 */
class CheckboxTest {

    @Nested
    class ComputeTickBox {
        private final Rectangle row = new Rectangle(30f, 40f, 150f, 20f);

        @Test
        void makesASquareTheHeightOfTheRow() {
            assertThat(Checkbox.computeTickBox(row))
                .isEqualTo(new Rectangle(30f, 40f, 20f, 20f));
        }

        @Test
        void sitsFlushWithTheRowsLeftEdge() {

            var box = Checkbox.computeTickBox(row);

            assertThat(box.x())
                .isEqualTo(row.x());
            assertThat(box.y())
                .isEqualTo(row.y());
        }
    }

    @Nested
    class ComputeLabelAnchorX {
        @Test
        void anchorsPastTheTickBoxAndTheColumnGap() {
            // Row left 30 + box side (row height) 20 + the 6-unit gap parting a leading column from
            // the label -> 56, the same offset an icon row starts its label at past its own leading
            // column.
            var row = new Rectangle(30f, 40f, 150f, 20f);

            assertThat(Checkbox.computeLabelAnchorX(row))
                .isEqualTo(56f);
        }
    }

    @Nested
    class MeasureRowWidth {
        @Test
        void reservesTheTickBoxTheGapAndTheLabel() {
            // Box side 20 + gap 6 + label 50 = 76: no padding at either end, since the box is flush
            // with the row's left edge and nothing follows the label.
            assertThat(Checkbox.measureRowWidth(20f, 50f))
                .isEqualTo(76f);
        }

        @Test
        void reservesExactlyTheRoomTheLabelAnchorNeeds() {
            // A row snapped to the measured width holds its label to the last unit: the 50-wide label
            // starts at 56 and ends at 106, the row's own right edge (30 + 76). The two methods are
            // read by different callers - one sizes the row, the other places the text in it - so what
            // is reserved and what is drawn have to meet here or the label clips.
            var sizedRow = new Rectangle(30f, 40f, 76f, 20f);
            assertThat(Checkbox.computeLabelAnchorX(sizedRow) + 50f)
                .isEqualTo(106f);
        }
    }
}
