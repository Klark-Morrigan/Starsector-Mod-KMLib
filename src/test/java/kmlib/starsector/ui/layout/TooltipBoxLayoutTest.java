package kmlib.starsector.ui.layout;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the pure sizing and edge-clamp contract of {@link TooltipBoxLayout#computeBox}:
 *  - the box grows to the widest measured line plus padding on both sides,
 *  - its height counts each line plus the gap stacked between lines, plus padding,
 *  - it sits offset up-and-right of the cursor when there is room,
 *  - it clamps back onto the screen at the right and top edges,
 *  - and it pins to the origin when it is larger than the screen rather than sliding off.
 *
 * <p>Plain numbers throughout - the layout reads no font, GL, or engine state - so the arithmetic
 * and the clamp are asserted directly on the returned box.
 */
final class TooltipBoxLayoutTest {
    private static final double LINE_HEIGHT = 15d;
    private static final float SCREEN_WIDTH = 1920f;
    private static final float SCREEN_HEIGHT = 1080f;
    private static final float TOLERANCE = 0.001f;

    @Nested
    class ComputeBox {
        @Test
        void grows_the_width_to_the_content_plus_padding_on_both_sides() {
            var box = TooltipBoxLayout.computeBox(100d, 1, LINE_HEIGHT, 200f, 300f,
                    SCREEN_WIDTH, SCREEN_HEIGHT);

            // 100 content + 8 padding on each side.
            assertThat(box.width()).isCloseTo(116f, within(TOLERANCE));
        }

        @Test
        void sizes_the_height_for_a_single_line_plus_padding() {
            var box = TooltipBoxLayout.computeBox(100d, 1, LINE_HEIGHT, 200f, 300f,
                    SCREEN_WIDTH, SCREEN_HEIGHT);

            // One 15-tall line, no line gap, + 8 padding top and bottom.
            assertThat(box.height()).isCloseTo(31f, within(TOLERANCE));
        }

        @Test
        void sizes_the_height_for_two_lines_including_the_line_gap() {
            var box = TooltipBoxLayout.computeBox(100d, 2, LINE_HEIGHT, 200f, 300f,
                    SCREEN_WIDTH, SCREEN_HEIGHT);

            // Two 15-tall lines + one 4 gap between them + 8 padding top and bottom.
            assertThat(box.height()).isCloseTo(50f, within(TOLERANCE));
        }

        @Test
        void places_the_box_up_and_right_of_the_cursor_when_there_is_room() {
            var box = TooltipBoxLayout.computeBox(100d, 1, LINE_HEIGHT, 200f, 300f,
                    SCREEN_WIDTH, SCREEN_HEIGHT);

            // Cursor + the 18 offset, clear of both edges.
            assertThat(box.x()).isCloseTo(218f, within(TOLERANCE));
            assertThat(box.y()).isCloseTo(318f, within(TOLERANCE));
        }

        @Test
        void clamps_to_the_right_edge_when_the_cursor_is_near_it() {
            var box = TooltipBoxLayout.computeBox(100d, 1, LINE_HEIGHT, 1850f, 300f,
                    SCREEN_WIDTH, SCREEN_HEIGHT);

            // 1850 + 18 would overrun; the box pulls left to screenWidth - width (1920 - 116).
            assertThat(box.x()).isCloseTo(1804f, within(TOLERANCE));
        }

        @Test
        void clamps_to_the_top_edge_when_the_cursor_is_near_it() {
            var box = TooltipBoxLayout.computeBox(100d, 1, LINE_HEIGHT, 200f, 1060f,
                    SCREEN_WIDTH, SCREEN_HEIGHT);

            // 1060 + 18 would overrun; the box drops to screenHeight - height (1080 - 31).
            assertThat(box.y()).isCloseTo(1049f, within(TOLERANCE));
        }

        @Test
        void pins_to_the_origin_when_the_box_is_larger_than_the_screen() {
            // A box wider and taller than the tiny screen: the clamp would push it negative, so the
            // origin floor keeps it anchored at the corner rather than sliding off the far edge.
            var box = TooltipBoxLayout.computeBox(100d, 1, LINE_HEIGHT, 0f, 0f, 100f, 20f);

            assertThat(box.x()).isCloseTo(0f, within(TOLERANCE));
            assertThat(box.y()).isCloseTo(0f, within(TOLERANCE));
        }
    }
}
