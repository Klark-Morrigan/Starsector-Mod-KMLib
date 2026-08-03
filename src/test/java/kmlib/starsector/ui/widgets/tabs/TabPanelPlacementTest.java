package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.PanelPlacement;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what counts as being on a laid-out tab panel. The notch is the whole of the question: it is
 * drawn past the box's right border edge, so the footprint is not the box, and it is absent on a
 * bodyless panel, so the test has to answer for a placement carrying no handle at all.
 */
final class TabPanelPlacementTest {

    // A box away from the origin, so a point outside it is outside on both axes rather than by a
    // coordinate that happens to be zero.
    private static final Rectangle BODY_BOX = new Rectangle(100f, 200f, 300f, 400f);

    // Clear of the body box's right edge (x 400), the side the collapse handle rides.
    private static final Rectangle NOTCH = new Rectangle(400f, 300f, 20f, 40f);

    private static final float BORDER_WIDTH = 1f;

    private static final float INSIDE_BODY_X = 150f;
    private static final float INSIDE_BODY_Y = 250f;
    private static final float INSIDE_NOTCH_X = 410f;
    private static final float INSIDE_NOTCH_Y = 320f;
    private static final float OUTSIDE_X = 900f;
    private static final float OUTSIDE_Y = 900f;

    @Nested
    class ContainsPoint {

        @Test
        void containsPointAnswersYesInsideTheBody() {
            assertThat(placePanel(NOTCH).containsPoint(INSIDE_BODY_X, INSIDE_BODY_Y))
                .isTrue();
        }

        @Test
        void containsPointAnswersYesOnTheHandleOutsideTheBox() {
            // The handle is the part still on screen once the body is docked, so a footprint that
            // stopped at the box would report a point on it as being off the panel.
            assertThat(placePanel(NOTCH).containsPoint(INSIDE_NOTCH_X, INSIDE_NOTCH_Y))
                .isTrue();
        }

        @Test
        void containsPointAnswersNoOffBoth() {
            assertThat(placePanel(NOTCH).containsPoint(OUTSIDE_X, OUTSIDE_Y))
                .isFalse();
        }

        @Test
        void containsPointStillAnswersForABodylessPanelWithNoHandle() {
            assertThat(placePanel(null).containsPoint(INSIDE_BODY_X, INSIDE_BODY_Y))
                .isTrue();
            assertThat(placePanel(null).containsPoint(INSIDE_NOTCH_X, INSIDE_NOTCH_Y))
                .isFalse();
        }
    }

    @Nested
    class ContainsPointInNotch {

        @Test
        void containsPointInNotchAnswersYesOnTheHandle() {
            assertThat(placePanel(NOTCH).containsPointInNotch(INSIDE_NOTCH_X, INSIDE_NOTCH_Y))
                .isTrue();
        }

        @Test
        void containsPointInNotchAnswersNoOnTheBody() {
            // The two are disjoint: the handle rides the box's outer edge, so a body hit is never
            // also a handle hit and a press cannot fire both.
            assertThat(placePanel(NOTCH).containsPointInNotch(INSIDE_BODY_X, INSIDE_BODY_Y))
                .isFalse();
        }

        @Test
        void containsPointInNotchAnswersNoForABodylessPanelWithNoHandle() {
            // With no rect to be over, no point is over it - the null a bodyless panel carries is
            // absorbed here rather than at each caller.
            assertThat(placePanel(null).containsPointInNotch(INSIDE_NOTCH_X, INSIDE_NOTCH_Y))
                .isFalse();
        }
    }

    // A placement carrying only what a containment test reads: the body's box and the notch.
    private static TabPanelPlacement placePanel(Rectangle notch) {
        return new TabPanelPlacement(
            null,
            new PanelPlacement(BODY_BOX, BODY_BOX, List.of(), BODY_BOX, 0f, 0f),
            new BoxBorder(BORDER_WIDTH),
            notch);
    }
}
