package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.BoxEdge;
import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link BorderedBox#computeContentBounds}: the content area insets by the border on each stroked
 * edge, an open edge leaves the content flush to that side, a zero border leaves the box untouched, and
 * an over-wide border collapses the content to zero rather than inverting it.
 */
class BorderedBoxTest {

    @Nested
    class ComputeContentBounds {
        private final Rectangle outer = new Rectangle(10f, 20f, 100f, 50f);

        @Test
        void insetsByTheBorderOnEveryStrokedEdge() {
            assertThat(BorderedBox.computeContentBounds(outer, new BoxBorder(4f)))
                    .isEqualTo(new Rectangle(14f, 24f, 92f, 42f));
        }

        @Test
        void keepsTheContentFlushOnAnOpenEdge() {
            // With the left edge open (the intel panel's flush side), the content keeps the box's left at
            // x, insetting only the three stroked edges - so nothing clips a border-width strip off a side
            // that draws no border.
            var content = BorderedBox.computeContentBounds(outer,
                    new BoxBorder(4f, EnumSet.of(BoxEdge.TOP, BoxEdge.RIGHT, BoxEdge.BOTTOM)));
            assertThat(content.x()).isEqualTo(outer.x());
            assertThat(content.width()).isEqualTo(outer.width() - 4f);
        }

        @Test
        void returnsTheOuterBoxWhenTheBorderIsZero() {
            assertThat(BorderedBox.computeContentBounds(outer, new BoxBorder(0f))).isEqualTo(outer);
        }

        @Test
        void collapsesToZeroWhenTheBorderExceedsHalfTheBox() {
            var content = BorderedBox.computeContentBounds(outer, new BoxBorder(40f));
            assertThat(content.height()).isEqualTo(0f);
        }
    }
}
