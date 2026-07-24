package kmlib.starsector.ui.map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the reconstructed star-icon hit test against hand-placed points: a point on the anchor is
 * over the icon and one well past its world radius is not, the world radius holds as the map zooms
 * (the whole point of the reduced test - the icon is world-sized, not screen-sized), and the small
 * screen-pixel floor keeps a zoomed-out icon hittable a touch beyond that radius. Also pins the
 * world-radius reconstruction: a star drawn smaller than another body, a nebula centre off its
 * fixed base, and the per-spec scale multiplier.
 */
class StarIconHitTestTest {

    // A round world icon radius, so a point's offset reads straight against it.
    private static final float ICON_WORLD_RADIUS = 100f;

    // An anchor away from the origin so a point that only coincides with it by dropping a
    // coordinate cannot pass.
    private static final Vector2f ANCHOR = new Vector2f(500f, -300f);

    // A cursor offset the given distance along x from the anchor, so a test states how far off the
    // icon centre the cursor sits and reads the verdict off that alone.
    private static Vector2f cursorOffsetFromAnchorBy(float worldOffsetX) {
        return new Vector2f(ANCHOR.x + worldOffsetX, ANCHOR.y);
    }

    @Nested
    class IsWorldPointOverStarIcon {

        @Test
        void isWorldPointOverStarIconIsTrueOnTheAnchor() {
            assertThat(StarIconHitTest.isWorldPointOverStarIcon(ANCHOR, ANCHOR, ICON_WORLD_RADIUS, 1f))
                    .isTrue();
        }

        @Test
        void isWorldPointOverStarIconIsTrueJustInsideTheWorldRadius() {
            assertThat(StarIconHitTest.isWorldPointOverStarIcon(
                    cursorOffsetFromAnchorBy(90f), ANCHOR, ICON_WORLD_RADIUS, 1f))
                    .isTrue();
        }

        @Test
        void isWorldPointOverStarIconIsFalseWellOutsideTheWorldRadius() {
            assertThat(StarIconHitTest.isWorldPointOverStarIcon(
                    cursorOffsetFromAnchorBy(110f), ANCHOR, ICON_WORLD_RADIUS, 1f))
                    .isFalse();
        }

        @Test
        void isWorldPointOverStarIconHoldsItsWorldRadiusAcrossZoom() {
            // A point inside the world radius stays inside whether the map is zoomed right in or
            // right out - the world radius does not shrink with zoom, which is the bug the reduced
            // test fixes (a screen-space radius would have flipped this between the two).
            var point = cursorOffsetFromAnchorBy(90f);
            assertThat(StarIconHitTest.isWorldPointOverStarIcon(point, ANCHOR, ICON_WORLD_RADIUS, 100f))
                    .isTrue();
            assertThat(StarIconHitTest.isWorldPointOverStarIcon(point, ANCHOR, ICON_WORLD_RADIUS, 0.1f))
                    .isTrue();
        }

        @Test
        void isWorldPointOverStarIconLetsTheScreenFloorReachPastTheRadiusWhenZoomedOut() {
            // A point just past the world radius: zoomed in the screen floor is negligible so it is
            // outside, zoomed out the floor (5 pixels, / factor in world units) grows enough to
            // catch it - the widget keeping a small icon hittable.
            var point = cursorOffsetFromAnchorBy(102f);
            assertThat(StarIconHitTest.isWorldPointOverStarIcon(point, ANCHOR, ICON_WORLD_RADIUS, 10f))
                    .isFalse();
            assertThat(StarIconHitTest.isWorldPointOverStarIcon(point, ANCHOR, ICON_WORLD_RADIUS, 1f))
                    .isTrue();
        }
    }

    @Nested
    class ComputeIconWorldRadius {

        @Test
        void computeIconWorldRadiusDrawsAStarSmallerThanAnEqualSizedBody() {
            // 5 * 100 * 1 * 0.75 = 375, three-quarters of the 500 a non-star body of the same
            // radius gets.
            assertThat(StarIconHitTest.computeIconWorldRadius(100f, 1f, StarIconBodyKind.STAR))
                    .isEqualTo(375f, within(1e-3f));
        }

        @Test
        void computeIconWorldRadiusLeavesANonStarBodyAtFullScale() {
            // 5 * 100 * 1 = 500, no star reduction.
            assertThat(StarIconHitTest.computeIconWorldRadius(100f, 1f, StarIconBodyKind.OTHER))
                    .isEqualTo(500f, within(1e-3f));
        }

        @Test
        void computeIconWorldRadiusSizesANebulaCentreOffTheFixedBase() {
            // The (large, diffuse) radius is ignored for a nebula centre; it is sized off the fixed
            // base: 5 * 200 * 1 = 1000.
            assertThat(StarIconHitTest.computeIconWorldRadius(
                    9999f, 1f, StarIconBodyKind.NEBULA_CENTRE))
                    .isEqualTo(1000f, within(1e-3f));
        }

        @Test
        void computeIconWorldRadiusAppliesTheMapIconScaleMultiplier() {
            // 5 * 100 * 2 = 1000: the per-spec multiplier scales the whole radius.
            assertThat(StarIconHitTest.computeIconWorldRadius(100f, 2f, StarIconBodyKind.OTHER))
                    .isEqualTo(1000f, within(1e-3f));
        }

        @Test
        void computeIconWorldRadiusReducesABlackHoleLikeAStar() {
            // A black hole takes the same star reduction as a star: 5 * 100 * 1 * 0.75 = 375.
            assertThat(StarIconHitTest.computeIconWorldRadius(100f, 1f, StarIconBodyKind.BLACK_HOLE))
                    .isEqualTo(375f, within(1e-3f));
        }
    }
}
