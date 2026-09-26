package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the rectangle a list of vectors is enclosed by: its corner at their lowest x and y, its size
 * spanning to their highest, and no rectangle at all for no points.
 *
 * <p>Pins the rectangle's point test: interior points are inside, exterior points are not, and
 * the edges count as inside so a hairline-precise hit does not fall through. Also pins the centre
 * accessors used to place a centred element, and the inset box an element drawn inside a frame is
 * placed from - the floor that keeps a box too small for its own inset from inverting included.
 */
class RectangleTest {

    private static final float TOLERANCE = 0.01f;

    @Nested
    class ComputeEnclosingRectangle {

        @Test
        void placesTheCornerAtTheLowestPointsAndSpansToTheHighest() {
            // The extremes come from three different points, so a walk that read the first and
            // last, or that carried x for y, would come back with a plausible smaller box.
            var rectangle = Rectangle.computeEnclosingRectangle(List.of(
                new Vector2f(5f, -2f),
                new Vector2f(-3f, 7f),
                new Vector2f(1f, 1f)));

            assertThat(rectangle)
                .isEqualTo(new Rectangle(-3f, -2f, 8f, 9f));
        }

        @Test
        void enclosesASinglePointAsARectangleWithNoArea() {

            assertThat(Rectangle.computeEnclosingRectangle(List.of(new Vector2f(4f, 6f))))
                .isEqualTo(new Rectangle(4f, 6f, 0f, 0f));
        }

        @Test
        void refusesToEncloseNoPointsAtAll() {

            assertThatThrownBy(() -> Rectangle.computeEnclosingRectangle(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ContainsPoint {

        private final Rectangle rectangle = new Rectangle(10f, 20f, 100f, 50f);

        @Test
        void reportsAnInteriorPointAsInside() {

            assertThat(rectangle.containsPoint(50f, 40f))
                .isTrue();
        }

        @Test
        void reportsAPointLeftOfTheRectangleAsOutside() {

            assertThat(rectangle.containsPoint(5f, 40f))
                .isFalse();
        }

        @Test
        void reportsAPointAboveTheRectangleAsOutside() {

            assertThat(rectangle.containsPoint(50f, 80f))
                .isFalse();
        }

        @Test
        void countsTheLowerLeftCornerAsInside() {

            assertThat(rectangle.containsPoint(10f, 20f))
                .isTrue();
        }

        @Test
        void countsTheUpperRightCornerAsInside() {

            assertThat(rectangle.containsPoint(110f, 70f))
                .isTrue();
        }
    }

    @Nested
    class IntersectWith {

        @Test
        void narrowsToTheBoundThatClipsAnOverhangingRectangle() {

            var box = new Rectangle(0f, 0f, 100f, 40f);
            var viewport = new Rectangle(20f, 10f, 200f, 200f);

            assertThat(viewport.intersectWith(box))
                .isEqualTo(new Rectangle(20f, 10f, 80f, 30f));
        }

        @Test
        void keepsARectangleWhollyInsideTheOtherUnchanged() {

            var box = new Rectangle(0f, 0f, 100f, 100f);
            var viewport = new Rectangle(10f, 20f, 30f, 40f);

            assertThat(viewport.intersectWith(box))
                .isEqualTo(new Rectangle(10f, 20f, 30f, 40f));
        }

        @Test
        void narrowsToTheOtherWhenItLiesWhollyInsideThisRectangle() {

            var viewport = new Rectangle(0f, 0f, 200f, 200f);
            var box = new Rectangle(30f, 40f, 20f, 20f);

            assertThat(viewport.intersectWith(box))
                .isEqualTo(new Rectangle(30f, 40f, 20f, 20f));
        }

        @Test
        void collapsesToAZeroExtentRectangleWhenTheTwoDoNotOverlap() {

            var box = new Rectangle(0f, 0f, 50f, 50f);
            var viewport = new Rectangle(200f, 200f, 30f, 30f);

            assertThat(viewport.intersectWith(box))
                .isEqualTo(new Rectangle(200f, 200f, 0f, 0f));
        }

        @Test
        void yieldsAZeroWidthRectangleWhenADockedBoxSharesOnlyAnEdge() {

            var viewport = new Rectangle(20f, 0f, 0f, 100f);
            var box = new Rectangle(0f, 0f, 20f, 100f);

            assertThat(viewport.intersectWith(box))
                .isEqualTo(new Rectangle(20f, 0f, 0f, 100f));
        }
    }

    @Nested
    class OverlapsBox {

        // The surface a box is tested against - a screen, a panel, a viewport.
        private final Rectangle surface = new Rectangle(0f, 0f, 800f, 600f);

        @Test
        void reportsABoxWhollyInsideAsOverlapping() {

            assertThat(surface.overlapsBox(new Rectangle(100f, 100f, 200f, 150f)))
                .isTrue();
        }

        @Test
        void reportsABoxPartlyOverhangingAsOverlapping() {
            // The state a panel sliding on or off screen passes through: part of it is visible, so
            // it counts as present rather than as having arrived or gone.
            assertThat(surface.overlapsBox(new Rectangle(-100f, 100f, 200f, 150f)))
                .isTrue();
        }

        @Test
        void reportsABoxWhollyOutsideAsNotOverlapping() {

            assertThat(surface.overlapsBox(new Rectangle(-400f, 100f, 200f, 150f)))
                .isFalse();
        }

        @Test
        void reportsABoxAbuttingAnEdgeAsNotOverlapping() {
            // Edge-to-edge shares a line rather than an area, and a line of a widget is nothing the
            // player can see.
            assertThat(surface.overlapsBox(new Rectangle(-200f, 100f, 200f, 150f)))
                .isFalse();
        }

        @Test
        void reportsABoxWithNoExtentAsNotOverlapping() {
            // A collapsed box sits inside the surface and still occupies none of it.
            assertThat(surface.overlapsBox(new Rectangle(400f, 300f, 0f, 0f)))
                .isFalse();
        }
    }

    @Nested
    class UnionWith {

        @Test
        void enclosesBothRectanglesAndTheGapBetweenThem() {
            // Disjoint pieces of one widget: the bound spans from the lower-left of the one to the
            // upper-right of the other, covering the screen neither piece paints on.
            var row = new Rectangle(0f, 0f, 10f, 10f);
            var handle = new Rectangle(20f, 30f, 10f, 10f);

            assertThat(row.unionWith(handle))
                .isEqualTo(new Rectangle(0f, 0f, 30f, 40f));
        }

        @Test
        void widensOnlyTheSidesTheOtherRectangleOverhangs() {

            var box = new Rectangle(10f, 20f, 100f, 50f);
            var overhang = new Rectangle(50f, 60f, 100f, 50f);

            assertThat(box.unionWith(overhang))
                .isEqualTo(new Rectangle(10f, 20f, 140f, 90f));
        }

        @Test
        void keepsTheEnclosingRectangleWhenTheOtherLiesWhollyInside() {

            var box = new Rectangle(0f, 0f, 100f, 100f);
            var inner = new Rectangle(30f, 40f, 20f, 20f);

            assertThat(box.unionWith(inner))
                .isEqualTo(new Rectangle(0f, 0f, 100f, 100f));
        }

        @Test
        void growsToTheOtherWhenItEnclosesThisRectangle() {

            var inner = new Rectangle(30f, 40f, 20f, 20f);
            var box = new Rectangle(0f, 0f, 100f, 100f);

            assertThat(inner.unionWith(box))
                .isEqualTo(new Rectangle(0f, 0f, 100f, 100f));
        }

        @Test
        void reachesAZeroExtentRectangleAsAPointToEnclose() {
            // A collapsed piece is still somewhere: a degenerate rect widens the bound to its corner
            // rather than being ignored for having no area.

            var box = new Rectangle(10f, 20f, 100f, 50f);
            var collapsed = new Rectangle(200f, 20f, 0f, 0f);

            assertThat(box.unionWith(collapsed))
                .isEqualTo(new Rectangle(10f, 20f, 190f, 50f));
        }
    }

    @Nested
    class ComputeInsetBox {

        @Test
        void pullsEverySideInwardByTheInset() {
            // Each side moves in by 2, so the corner shifts by 2 and each extent loses 4.
            assertThat(new Rectangle(10f, 20f, 100f, 50f).computeInsetBox(2f))
                .isEqualTo(new Rectangle(12f, 22f, 96f, 46f));
        }

        @Test
        void returnsTheBoxItselfAtNoInset() {

            assertThat(new Rectangle(10f, 20f, 100f, 50f).computeInsetBox(0f))
                .isEqualTo(new Rectangle(10f, 20f, 100f, 50f));
        }

        @Test
        void collapsesAtTheCentreWhenTheBoxCannotHoldItsOwnInset() {
            // A box narrower than twice the inset would invert into a rectangle drawn back-to-front
            // across whatever it was inside, so the extent floors at zero and the box holds its centre.
            assertThat(new Rectangle(10f, 20f, 6f, 50f).computeInsetBox(4f))
                .isEqualTo(new Rectangle(13f, 24f, 0f, 42f));
        }
    }

    @Nested
    class ComputeCenterX {

        @Test
        void isHalfTheWidthInFromTheLeftEdge() {
            // A 100-wide rectangle from x=10 centres at x=60.
            assertThat(new Rectangle(10f, 20f, 100f, 50f).computeCenterX())
                .isCloseTo(60f, within(TOLERANCE));
        }
    }

    @Nested
    class ComputeCenterY {

        @Test
        void isHalfTheHeightUpFromTheBottomEdge() {
            // A 50-tall rectangle from y=20 centres at y=45.
            assertThat(new Rectangle(10f, 20f, 100f, 50f).computeCenterY())
                .isCloseTo(45f, within(TOLERANCE));
        }
    }
}
