package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link Checkbox#computeTickBox}: the tick box is a square the height of the row, flush
 * with the row's left edge, so it lines up with the row and leaves the rest of the width for the
 * label.
 */
class CheckboxTest {

    @Nested
    class ComputeTickBox {
        private final Rectangle row = new Rectangle(30f, 40f, 150f, 20f);

        @Test
        void makesASquareTheHeightOfTheRow() {
            assertThat(Checkbox.computeTickBox(row)).isEqualTo(new Rectangle(30f, 40f, 20f, 20f));
        }

        @Test
        void sitsFlushWithTheRowsLeftEdge() {
            var box = Checkbox.computeTickBox(row);
            assertThat(box.x()).isEqualTo(row.x());
            assertThat(box.y()).isEqualTo(row.y());
        }
    }
}
