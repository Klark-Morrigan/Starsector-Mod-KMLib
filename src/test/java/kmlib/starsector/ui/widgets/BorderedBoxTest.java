package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link BorderedBox#computeContentBounds}: the content area insets by the border on every
 * edge, a zero border leaves the box untouched, and an over-wide border collapses the content to
 * zero rather than inverting it.
 */
class BorderedBoxTest {

    @Nested
    class ComputeContentBounds {
        private final Rectangle outer = new Rectangle(10f, 20f, 100f, 50f);

        @Test
        void insetsByTheBorderOnEveryEdge() {
            assertThat(BorderedBox.computeContentBounds(outer, 4f))
                    .isEqualTo(new Rectangle(14f, 24f, 92f, 42f));
        }

        @Test
        void returnsTheOuterBoxWhenTheBorderIsZero() {
            assertThat(BorderedBox.computeContentBounds(outer, 0f)).isEqualTo(outer);
        }

        @Test
        void collapsesToZeroWhenTheBorderExceedsHalfTheBox() {
            var content = BorderedBox.computeContentBounds(outer, 40f);
            assertThat(content.height()).isEqualTo(0f);
        }
    }
}
