package kmlib.starsector.ui.layout;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the pure sizing and edge-clamp contract of {@link TooltipBoxLayout#computeBox}:
 *  - the box grows to the measured content plus padding on all sides,
 *  - it sits offset up-and-right of the cursor when there is room,
 *  - it clamps back onto the screen at the right and top edges,
 *  - and it pins to the origin when it is larger than the screen rather than sliding off.
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
    private static final float SCREEN_WIDTH = 1920f;
    private static final float SCREEN_HEIGHT = 1080f;
    private static final float TOLERANCE = 0.001f;

    @Nested
    class ComputeBox {
        @Test
        void grows_the_width_to_the_content_plus_padding_on_both_sides() {
            var box = TooltipBoxLayout.computeBox(100d, ONE_LINE_HEIGHT, 200f, 300f,
                    SCREEN_WIDTH, SCREEN_HEIGHT);

            // 100 content + 8 padding on each side.
            assertThat(box.width()).isCloseTo(116f, within(TOLERANCE));
        }

        @Test
        void grows_the_height_to_the_content_plus_padding_top_and_bottom() {
            var box = TooltipBoxLayout.computeBox(100d, ONE_LINE_HEIGHT, 200f, 300f,
                    SCREEN_WIDTH, SCREEN_HEIGHT);

            // One 15-tall line + 8 padding top and bottom.
            assertThat(box.height()).isCloseTo(31f, within(TOLERANCE));
        }

        @Test
        void wraps_a_taller_content_stack_without_reinterpreting_it() {
            var box = TooltipBoxLayout.computeBox(100d, TWO_LINE_HEIGHT, 200f, 300f,
                    SCREEN_WIDTH, SCREEN_HEIGHT);

            // The 34 the caller measured - two lines and their gap - plus the same 16 of padding.
            assertThat(box.height()).isCloseTo(50f, within(TOLERANCE));
        }

        @Test
        void places_the_box_up_and_right_of_the_cursor_when_there_is_room() {
            var box = TooltipBoxLayout.computeBox(100d, ONE_LINE_HEIGHT, 200f, 300f,
                    SCREEN_WIDTH, SCREEN_HEIGHT);

            // Cursor + the 18 offset, clear of both edges.
            assertThat(box.x()).isCloseTo(218f, within(TOLERANCE));
            assertThat(box.y()).isCloseTo(318f, within(TOLERANCE));
        }

        @Test
        void clamps_to_the_right_edge_when_the_cursor_is_near_it() {
            var box = TooltipBoxLayout.computeBox(100d, ONE_LINE_HEIGHT, 1850f, 300f,
                    SCREEN_WIDTH, SCREEN_HEIGHT);

            // 1850 + 18 would overrun; the box pulls left to screenWidth - width (1920 - 116).
            assertThat(box.x()).isCloseTo(1804f, within(TOLERANCE));
        }

        @Test
        void clamps_to_the_top_edge_when_the_cursor_is_near_it() {
            var box = TooltipBoxLayout.computeBox(100d, ONE_LINE_HEIGHT, 200f, 1060f,
                    SCREEN_WIDTH, SCREEN_HEIGHT);

            // 1060 + 18 would overrun; the box drops to screenHeight - height (1080 - 31).
            assertThat(box.y()).isCloseTo(1049f, within(TOLERANCE));
        }

        @Test
        void pins_to_the_origin_when_the_box_is_larger_than_the_screen() {
            // A box wider and taller than the tiny screen: the clamp would push it negative, so the
            // origin floor keeps it anchored at the corner rather than sliding off the far edge.
            var box = TooltipBoxLayout.computeBox(100d, ONE_LINE_HEIGHT, 0f, 0f, 100f, 20f);

            assertThat(box.x()).isCloseTo(0f, within(TOLERANCE));
            assertThat(box.y()).isCloseTo(0f, within(TOLERANCE));
        }
    }
}
