package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the pure sizing and edge-clamp contract of {@link TooltipBoxLayout#computeBox}:
 *  - the box grows to the measured content plus padding on all sides,
 *  - it sits offset up-and-right of the cursor when there is room,
 *  - it clamps back inside the bound it is handed at that bound's far edges,
 *  - and it pins to the bound's near corner when it is larger than the bound rather than sliding off.
 *
 * <p>Alongside it, {@link TooltipBoxLayout#computeBoxHeight} answers that same wrapping on its own, for
 * a caller weighing a height before it has anywhere to put a box - so what is pinned there is that the
 * two agree on the padding.
 *
 * <p>How tall the content itself stacks is its own caller's rule and is pinned with that caller
 * ({@code CursorTooltipTest}); what is fixed here is that the box wraps whatever height it is given.
 *
 * <p>Plain numbers throughout - the layout reads no font, GL, or engine state - so the arithmetic
 * and the clamp are asserted directly on the returned box.
 */
final class TooltipBoxLayoutTest {

    // A single 15-tall line and a two-line stack (two lines plus the 4 gap between them), the content
    // heights the cases below wrap.
    private static final double ONE_LINE_HEIGHT = 15d;
    private static final double TWO_LINE_HEIGHT = 34d;
    private static final float TOLERANCE = 0.001f;

    // The whole screen, which is what the box ordinarily clamps inside, and an inset region standing in
    // for any other bound - the two together are what fix the clamp reading off the bound's own edges
    // rather than off an assumed origin.
    private static final Rectangle SCREEN = new Rectangle(0f, 0f, 1920f, 1080f);
    private static final Rectangle INSET_BOUND = new Rectangle(500f, 400f, 300f, 200f);

    @Nested
    class ComputeBox {

        @Test
        void grows_the_width_to_the_content_plus_padding_on_both_sides() {

            var box = TooltipBoxLayout.computeBox(100d, ONE_LINE_HEIGHT, 200f, 300f, SCREEN);

            // 100 content + 8 padding on each side.
            assertThat(box.width())
                .isCloseTo(116f, within(TOLERANCE));
        }

        @Test
        void grows_the_height_to_the_content_plus_padding_top_and_bottom() {

            var box = TooltipBoxLayout.computeBox(100d, ONE_LINE_HEIGHT, 200f, 300f, SCREEN);

            // One 15-tall line + 8 padding top and bottom.
            assertThat(box.height())
                .isCloseTo(31f, within(TOLERANCE));
        }

        @Test
        void wraps_a_taller_content_stack_without_reinterpreting_it() {

            var box = TooltipBoxLayout.computeBox(100d, TWO_LINE_HEIGHT, 200f, 300f, SCREEN);

            // The 34 the caller measured - two lines and their gap - plus the same 16 of padding.
            assertThat(box.height())
                .isCloseTo(50f, within(TOLERANCE));
        }

        @Test
        void places_the_box_up_and_right_of_the_cursor_when_there_is_room() {

            var box = TooltipBoxLayout.computeBox(100d, ONE_LINE_HEIGHT, 200f, 300f, SCREEN);

            // Cursor + the 18 offset, clear of both edges.
            assertThat(box.x())
                .isCloseTo(218f, within(TOLERANCE));
            assertThat(box.y())
                .isCloseTo(318f, within(TOLERANCE));
        }

        @Test
        void clamps_to_the_right_edge_when_the_cursor_is_near_it() {

            var box = TooltipBoxLayout.computeBox(100d, ONE_LINE_HEIGHT, 1850f, 300f, SCREEN);

            // 1850 + 18 would overrun; the box pulls left to the bound's right edge - width (1920 - 116).
            assertThat(box.x())
                .isCloseTo(1804f, within(TOLERANCE));
        }

        @Test
        void clamps_to_the_top_edge_when_the_cursor_is_near_it() {

            var box = TooltipBoxLayout.computeBox(100d, ONE_LINE_HEIGHT, 200f, 1060f, SCREEN);

            // 1060 + 18 would overrun; the box drops to the bound's top edge - height (1080 - 31).
            assertThat(box.y())
                .isCloseTo(1049f, within(TOLERANCE));
        }

        @Test
        void clamps_inside_a_bound_that_does_not_start_at_the_origin() {
            // The bound is a region, not a size: a box near its far corner pulls back to that corner
            // (500 + 300 - 116, 400 + 200 - 31), which a clamp reading only the extents would miss.
            var box = TooltipBoxLayout.computeBox(100d, ONE_LINE_HEIGHT, 780f, 580f, INSET_BOUND);

            assertThat(box.x())
                .isCloseTo(684f, within(TOLERANCE));
            assertThat(box.y())
                .isCloseTo(569f, within(TOLERANCE));
        }

        @Test
        void pins_to_the_bounds_near_corner_when_the_box_is_larger_than_the_bound() {
            // A box wider and taller than the tiny bound: the clamp would push it past the near corner,
            // so the floor keeps it anchored there rather than sliding off the far edge.
            var box = TooltipBoxLayout.computeBox(
                100d,
                ONE_LINE_HEIGHT,
                0f,
                0f,
                new Rectangle(0f, 0f, 100f, 20f));

            assertThat(box.x())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(box.y())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ComputeBoxHeight {
        
        @Test
        void wraps_the_content_height_in_the_padding_above_and_below_it() {

            // One 15-tall line + 8 padding top and bottom, the same height the placed box comes to.
            assertThat(TooltipBoxLayout.computeBoxHeight(ONE_LINE_HEIGHT))
                .isCloseTo(31f, within(TOLERANCE));
        }

        @Test
        void wraps_a_taller_content_stack_without_reinterpreting_it() {

            // The 34 the caller measured - two lines and their gap - plus the same 16 of padding.
            assertThat(TooltipBoxLayout.computeBoxHeight(TWO_LINE_HEIGHT))
                .isCloseTo(50f, within(TOLERANCE));
        }

        @Test
        void wraps_empty_content_in_the_padding_alone() {

            // A box with nothing in it is still its own chrome, so the padding stands whatever it holds.
            assertThat(TooltipBoxLayout.computeBoxHeight(0d))
                .isCloseTo(16f, within(TOLERANCE));
        }
    }
}
