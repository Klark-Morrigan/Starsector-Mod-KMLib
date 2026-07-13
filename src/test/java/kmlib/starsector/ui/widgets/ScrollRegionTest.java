package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link ScrollRegion#computeContentHeight}: the scrolled content is the visible viewport plus
 * how far it overruns, so the scrollbar sizes the thumb from the region without the region carrying
 * the content height as a separate field.
 */
final class ScrollRegionTest {
    private static final float TOLERANCE = 0.01f;
    private static final Rectangle CONTAINER = new Rectangle(0f, 0f, 100f, 300f);
    private static final Rectangle VIEWPORT = new Rectangle(0f, 0f, 100f, 120f);

    @Nested
    class ComputeContentHeight {

        @Test
        void computeContentHeightIsTheViewportPlusItsOverflow() {
            // The visible viewport (120) plus how far the content overruns it (80) is the full content.
            var region = new ScrollRegion(CONTAINER, VIEWPORT, 0f, 80f);
            assertThat(region.computeContentHeight()).isCloseTo(200f, within(TOLERANCE));
        }

        @Test
        void computeContentHeightIsTheViewportWhenNothingOverflows() {
            // A content that fits is exactly its viewport tall.
            var region = new ScrollRegion(CONTAINER, VIEWPORT, 0f, 0f);
            assertThat(region.computeContentHeight()).isCloseTo(120f, within(TOLERANCE));
        }
    }
}
