package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.LabelledControlSpecs;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.scroll.ScrollbarThickness;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what counts as being on a laid-out tab panel. The three pieces are the whole of the question: the
 * tab row stands above the box, the handle is drawn past its right border edge, and a bodyless panel is
 * the row alone - so a footprint that stopped at the box would disown two of them, and a panel that
 * disowns the screen it paints on lets whatever is behind go on reading the pointer.
 *
 * <p>Pins the enclosing bound over the same three pieces, since a pass that needs one region for the whole
 * panel must not get a rect that leaves a piece outside it.
 */
final class TabPanelPlacementTest {

    // A box away from the origin, so a point outside it is outside on both axes rather than by a
    // coordinate that happens to be zero.
    private static final Rectangle BODY_BOX = new Rectangle(100f, 200f, 300f, 400f);

    // The tab row standing on the box's top edge (y 600), as the layout hangs it.
    private static final Rectangle HEADER_BAND = new Rectangle(100f, 600f, 300f, 20f);

    // Clear of the body box's right edge (x 400), the side the collapse handle rides.
    private static final Rectangle NOTCH = new Rectangle(400f, 300f, 20f, 40f);

    private static final float BORDER_WIDTH = 1f;

    private static final float INSIDE_BODY_X = 150f;
    private static final float INSIDE_BODY_Y = 250f;
    private static final float INSIDE_HEADER_X = 150f;
    private static final float INSIDE_HEADER_Y = 610f;
    private static final float INSIDE_NOTCH_X = 410f;
    private static final float INSIDE_NOTCH_Y = 320f;
    private static final float OUTSIDE_X = 900f;
    private static final float OUTSIDE_Y = 900f;

    // A body control, so the placement reads as having a body at all; what it is never matters here.
    private static final ControlSpec BODY_CONTROL =
        LabelledControlSpecs.buildCheckbox("X", false, ControlAction.NONE);

    @Nested
    class ContainsPoint {

        @Test
        void containsPointAnswersYesInsideTheBody() {
            assertThat(placePanel(NOTCH, HEADER_BAND).containsPoint(INSIDE_BODY_X, INSIDE_BODY_Y))
                .isTrue();
        }

        @Test
        void containsPointAnswersYesOnTheTabRowAboveTheBox() {
            // The row is drawn above the box, so a footprint that stopped at the box would leave the
            // surface behind the panel reading a pointer parked on the tabs.
            assertThat(placePanel(NOTCH, HEADER_BAND).containsPoint(INSIDE_HEADER_X, INSIDE_HEADER_Y))
                .isTrue();
        }

        @Test
        void containsPointAnswersYesOnTheHandleOutsideTheBox() {
            // The handle is the part still on screen once the body is docked, so a footprint that
            // stopped at the box would report a point on it as being off the panel.
            assertThat(placePanel(NOTCH, HEADER_BAND).containsPoint(INSIDE_NOTCH_X, INSIDE_NOTCH_Y))
                .isTrue();
        }

        @Test
        void containsPointAnswersNoOffAllThree() {
            assertThat(placePanel(NOTCH, HEADER_BAND).containsPoint(OUTSIDE_X, OUTSIDE_Y))
                .isFalse();
        }

        @Test
        void containsPointAnswersNoWhereTheFoldHasWipedTheRow() {
            // Mid-fold the drawn band is narrower than the row was laid out: the panel claims only what it
            // still paints, so the screen the tabs have wiped off goes back to whatever is behind.
            var wiped = new Rectangle(HEADER_BAND.x(), HEADER_BAND.y(), 10f, HEADER_BAND.height());

            assertThat(placePanel(NOTCH, wiped).containsPoint(INSIDE_HEADER_X, INSIDE_HEADER_Y))
                .isFalse();
        }

        @Test
        void containsPointAnswersForABodylessPanelByItsRowAlone() {
            // No box and no handle, so the row is the whole footprint - and it still is one, which is what
            // stops a tab row with nothing under it blocking nothing at all.
            var bodyless = placeBodylessPanel();

            assertThat(bodyless.containsPoint(INSIDE_HEADER_X, INSIDE_HEADER_Y))
                .isTrue();
            assertThat(bodyless.containsPoint(INSIDE_BODY_X, INSIDE_BODY_Y))
                .isFalse();
            assertThat(bodyless.containsPoint(INSIDE_NOTCH_X, INSIDE_NOTCH_Y))
                .isFalse();

            // Not even the corner the empty box is parked on, which is outside the row: a box standing for
            // a body that is not there is not a place the panel paints, so it claims nothing.
            assertThat(bodyless.containsPoint(HEADER_BAND.x() - BORDER_WIDTH, HEADER_BAND.y()))
                .isFalse();
        }
    }

    @Nested
    class ComputeOuterBound {

        @Test
        void computeOuterBoundEnclosesTheRowTheBodyAndTheHandle() {
            // Left and bottom come from the box (x 100, y 200), the right edge from the handle
            // (x 400 + 20), the top from the row (y 600 + 20).
            assertThat(placePanel(NOTCH, HEADER_BAND).computeOuterBound())
                .isEqualTo(new Rectangle(100f, 200f, 320f, 420f));
        }

        @Test
        void computeOuterBoundWidensToARowOverhangingTheBody() {
            // The row is not clipped to the body's span at rest, so a wider row pushes both side edges
            // of the bound out past the box - the piece that reaches furthest sets each edge.
            var wideBand = new Rectangle(80f, 600f, 400f, 20f);

            assertThat(placePanel(NOTCH, wideBand).computeOuterBound())
                .isEqualTo(new Rectangle(80f, 200f, 400f, 420f));
        }

        @Test
        void computeOuterBoundForABodylessPanelIsItsRow() {
            // The row is the whole of the bound: no handle to reach past, and the empty box standing for the
            // missing body is left out rather than dragging the bound to the outer edge it is parked on.
            assertThat(placeBodylessPanel().computeOuterBound())
                .isEqualTo(new Rectangle(100f, 600f, 300f, 20f));
        }

        @Test
        void computeOuterBoundEnclosesEveryPointTheFootprintClaims() {
            // The invariant a clip taken from the bound rests on: nothing the panel draws, and so nothing
            // the pointer can be on, falls outside it. A bound missing a piece would clip a pass to a
            // region that stops short exactly where that piece is drawn.
            var bound = placePanel(NOTCH, HEADER_BAND).computeOuterBound();

            assertThat(bound.containsPoint(INSIDE_BODY_X, INSIDE_BODY_Y))
                .isTrue();
            assertThat(bound.containsPoint(INSIDE_HEADER_X, INSIDE_HEADER_Y))
                .isTrue();
            assertThat(bound.containsPoint(INSIDE_NOTCH_X, INSIDE_NOTCH_Y))
                .isTrue();
        }
    }

    @Nested
    class ContainsPointInNotch {

        @Test
        void containsPointInNotchAnswersYesOnTheHandle() {
            assertThat(placePanel(NOTCH, HEADER_BAND).containsPointInNotch(INSIDE_NOTCH_X, INSIDE_NOTCH_Y))
                .isTrue();
        }

        @Test
        void containsPointInNotchAnswersNoOnTheBody() {
            // The two are disjoint: the handle rides the box's outer edge, so a body hit is never
            // also a handle hit and a press cannot fire both.
            assertThat(placePanel(NOTCH, HEADER_BAND).containsPointInNotch(INSIDE_BODY_X, INSIDE_BODY_Y))
                .isFalse();
        }

        @Test
        void containsPointInNotchAnswersNoForABodylessPanelWithNoHandle() {
            // With no rect to be over, no point is over it - the null a bodyless panel carries is
            // absorbed here rather than at each caller.
            assertThat(placeBodylessPanel().containsPointInNotch(INSIDE_NOTCH_X, INSIDE_NOTCH_Y))
                .isFalse();
        }
    }

    @Nested
    class HasBody {

        @Test
        void hasBodyAnswersYesWhenTheBodyCarriesControls() {
            assertThat(placePanel(NOTCH, HEADER_BAND).hasBody())
                .isTrue();
        }

        @Test
        void hasBodyAnswersNoForABodylessPanel() {
            // The row is the whole panel: nothing to frame, fold, or fill beneath it.
            assertThat(placeBodylessPanel().hasBody())
                .isFalse();
        }
    }

    // A placement carrying what a containment test reads: the drawn row, the body's box, and the notch.
    private static TabPanelPlacement placePanel(Rectangle notch, Rectangle drawnHeaderBand) {
        return new TabPanelPlacement(
            buildHeaderControl(),
            drawnHeaderBand,
            new PanelPlacement(
                BODY_BOX,
                BODY_BOX,
                List.of(buildBodyControl()),
                BODY_BOX,
                0f,
                0f,
                ScrollbarThickness.DEFAULT),
            new BoxBorder(BORDER_WIDTH),
            notch);
    }

    // A tab whose body is empty: an empty box at the anchor, no controls, and no handle. The box is parked
    // a border-width left of the row, where the layout leaves it - it is anchored at the panel's outer edge
    // while the row starts inside the border - so a footprint that read it would be visibly wider than the
    // row rather than coinciding with it by accident of the fixture.
    private static TabPanelPlacement placeBodylessPanel() {

        var emptyBox = new Rectangle(HEADER_BAND.x() - BORDER_WIDTH, HEADER_BAND.y(), 0f, 0f);

        return new TabPanelPlacement(
            buildHeaderControl(),
            HEADER_BAND,
            new PanelPlacement(
                emptyBox,
                emptyBox,
                List.of(),
                emptyBox,
                0f,
                0f,
                ScrollbarThickness.DEFAULT),
            new BoxBorder(BORDER_WIDTH),
            null);
    }

    private static Control buildHeaderControl() {
        return new Control(
            new ControlSpec.Tabs(List.of("A"), List.of(), 0, ControlAction.NONE),
            HEADER_BAND,
            List.of(HEADER_BAND));
    }

    private static Control buildBodyControl() {
        return new Control(BODY_CONTROL, BODY_BOX, List.of());
    }
}
