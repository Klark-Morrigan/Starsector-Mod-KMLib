package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.BoxEdge;
import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link BoxBorder}: what one edge reserves follows from whether that edge is stroked, so a layout
 * growing a box outward and a renderer insetting content inward cannot disagree about how much room the
 * frame takes - and where the four quads that stroke it go, cut so that no two of them cover one pixel.
 */
final class BoxBorderTest {
    private static final float WIDTH = 4f;

    // A box whose corner and extents are all distinct and none a multiple of the width, so an edge quad
    // landing on the wrong side or at the wrong length cannot match an expected rectangle by coincidence.
    private static final Rectangle BOX = new Rectangle(100f, 50f, 60f, 30f);

    @Nested
    class ComputeEdgeInset {

        @Test
        void computeEdgeInsetReservesTheWidthOnAStrokedEdge() {
            assertThat(new BoxBorder(WIDTH).computeEdgeInset(BoxEdge.LEFT)).isEqualTo(WIDTH);
        }

        @Test
        void computeEdgeInsetReservesNothingOnAnOpenEdge() {
            // The intel panel's flush side: the edge draws no border, so it reserves no strip either and the
            // content sits hard against whatever the box abuts.
            var border = new BoxBorder(WIDTH, EnumSet.of(BoxEdge.TOP, BoxEdge.RIGHT, BoxEdge.BOTTOM));
            assertThat(border.computeEdgeInset(BoxEdge.LEFT)).isZero();
            assertThat(border.computeEdgeInset(BoxEdge.TOP)).isEqualTo(WIDTH);
        }

        @Test
        void computeEdgeInsetReservesNothingWhenTheWidthIsZero() {
            // A zero-width frame strokes nothing, so even a listed edge costs no room - a box with no border
            // is its own content.
            assertThat(new BoxBorder(0f).computeEdgeInset(BoxEdge.TOP)).isZero();
        }

        @Test
        void computeEdgeInsetReservesNothingWhenNoEdgeIsStroked() {
            assertThat(new BoxBorder(WIDTH, Set.of()).computeEdgeInset(BoxEdge.BOTTOM)).isZero();
        }
    }

    @Nested
    class ComputeStrokeBoxes {

        @Test
        void computeStrokeBoxesCutsTheSidesShortOfTheEdgesTheyMeet() {
            // The corners are the whole point: every shade composites, so a translucent frame painted twice
            // where two edges cross comes out with four bright corner pixels. The horizontals run the full
            // width and the verticals give up that width at each end, so no pixel is covered by two quads.
            var strokes = new BoxBorder(WIDTH).computeStrokeBoxes(BOX);

            assertThat(strokes)
                .containsExactlyInAnyOrder(
                    new Rectangle(100f, 50f, 60f, 4f),
                    new Rectangle(100f, 76f, 60f, 4f),
                    new Rectangle(100f, 54f, 4f, 22f),
                    new Rectangle(156f, 54f, 4f, 22f));
        }

        @Test
        void computeStrokeBoxesRunsASideToTheBoxsEdgeWhereItMeetsAnOpenSide() {
            // An open edge draws no quad, so there is nothing at that end for a side to leave room for -
            // the intel panel's flush left is exactly this, and a side stopping short of it would leave a
            // notch out of the frame at the corner.
            var strokes = new BoxBorder(WIDTH, EnumSet.of(BoxEdge.TOP, BoxEdge.RIGHT, BoxEdge.LEFT))
                .computeStrokeBoxes(BOX);

            assertThat(strokes)
                .contains(new Rectangle(100f, 50f, 4f, 26f));
        }

        @Test
        void computeStrokeBoxesCollapsesTheSidesOfABoxShorterThanItsOwnEdges() {
            // A negative height would draw a side upside down through the corners it was cut to clear, so a
            // box with no room between its two horizontals yields sides with nothing in them.
            var strokes = new BoxBorder(WIDTH).computeStrokeBoxes(new Rectangle(100f, 50f, 60f, 6f));

            assertThat(strokes)
                .contains(new Rectangle(100f, 54f, 4f, 0f));
        }

        @Test
        void computeStrokeBoxesStrokesNothingAtZeroWidth() {
            // A zero-width frame is a box with no border at all, so the pass that fills these draws nothing
            // rather than four quads of no thickness.
            assertThat(new BoxBorder(0f).computeStrokeBoxes(BOX))
                .isEmpty();
        }
    }

    @Nested
    class WidthOnlyConstructor {

        @Test
        void widthOnlyConstructorStrokesEveryEdge() {
            // The common case - a panel floating free, framed all round - so a caller naming no edges gets a
            // full frame rather than an unstroked one.
            var border = new BoxBorder(WIDTH);
            assertThat(border.edges()).isEqualTo(BoxEdge.ALL);
            for (var edge : BoxEdge.values()) {
                assertThat(border.computeEdgeInset(edge)).isEqualTo(WIDTH);
            }
        }
    }
}
