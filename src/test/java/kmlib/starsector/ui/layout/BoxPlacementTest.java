package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the box placement math: the anchor slides the box across the screen's free space after
 * the margins, so a corner sits margin-in from two edges and the centre lands mid-screen.
 */
class BoxPlacementTest {

    // A 1000x800 screen holding a 180x116 box with a 12-unit margin. Free span is
    // 1000-180-24 = 796 horizontally and 800-116-24 = 660 vertically.
    private static final float SCREEN_WIDTH = 1000f;
    private static final float SCREEN_HEIGHT = 800f;
    private static final float BOX_WIDTH = 180f;
    private static final float BOX_HEIGHT = 116f;
    private static final float MARGIN = 12f;

    @Nested
    class PlaceBox {
        @Test
        void pinsTheTopLeftAnchorMarginInFromTheLeftAndTop() {
            var box = place(ScreenAnchor.TOP_LEFT);

            assertThat(box.x()).isEqualTo(12f);
            assertThat(box.y()).isEqualTo(672f);
            assertThat(box.width()).isEqualTo(BOX_WIDTH);
            assertThat(box.height()).isEqualTo(BOX_HEIGHT);
        }

        @Test
        void pinsTheBottomRightAnchorMarginInFromTheRightAndBottom() {
            var box = place(ScreenAnchor.BOTTOM_RIGHT);

            assertThat(box.x()).isEqualTo(808f);
            assertThat(box.y()).isEqualTo(12f);
        }

        @Test
        void centresTheCenterAnchorOnBothAxes() {
            var box = place(ScreenAnchor.CENTER);

            assertThat(box.x()).isEqualTo(410f);
            assertThat(box.y()).isEqualTo(342f);
        }

        @Test
        void centresAnEdgeMidpointOnItsFreeAxisOnly() {
            var box = place(ScreenAnchor.LEFT_CENTER);

            assertThat(box.x()).isEqualTo(12f);
            assertThat(box.y()).isEqualTo(342f);
        }

        private Rectangle place(ScreenAnchor anchor) {
            return BoxPlacement.placeBox(SCREEN_WIDTH, SCREEN_HEIGHT, BOX_WIDTH, BOX_HEIGHT,
                MARGIN, anchor);
        }
    }
}
