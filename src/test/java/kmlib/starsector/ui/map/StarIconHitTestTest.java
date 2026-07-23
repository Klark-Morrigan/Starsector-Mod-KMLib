package kmlib.starsector.ui.map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the reconstructed star-icon hit test against hand-placed points: a point on the anchor is
 * over the icon and one past its radius is not, the boundary is where the widget's own test puts
 * it, a bigger star widens the hittable icon, and a higher map zoom shrinks it in world terms - so
 * the same cursor can be over a small star's icon zoomed in and off it zoomed out. Also pins the
 * icon-radius reconstruction's floor and cap directly, since those are what keep a tiny star
 * hittable and a huge one bounded.
 */
class StarIconHitTestTest {

    // A star at the scale reference radius, so its icon is full size and the radius maths is a
    // round number: full scale gives an icon radius of 40 / 2 = 20 pixels.
    private static final float FULL_SIZE_STAR_RADIUS = 100f;
    private static final float FULL_SIZE_ICON_RADIUS = 20f;

    // An anchor away from the origin so a point that only coincides with it by dropping a
    // coordinate cannot pass, and a unit map scale so a world distance reads straight as pixels.
    private static final Vector2f ANCHOR = new Vector2f(500f, -300f);
    private static final float UNIT_FACTOR = 1f;

    // A cursor offset the given distance along x from the anchor, so a test states how far off the
    // icon centre the cursor sits and reads the verdict off that alone.
    private static Vector2f cursorOffsetFromAnchorBy(float worldOffsetX) {
        return new Vector2f(ANCHOR.x + worldOffsetX, ANCHOR.y);
    }

    @Nested
    class IsWorldPointOverStarIcon {

        @Test
        void isWorldPointOverStarIconIsTrueOnTheAnchor() {
            assertThat(StarIconHitTest.isWorldPointOverStarIcon(
                    ANCHOR, ANCHOR, FULL_SIZE_STAR_RADIUS, UNIT_FACTOR))
                    .isTrue();
        }

        @Test
        void isWorldPointOverStarIconIsTrueJustInsideTheIconRadius() {
            // A pixel short of the 20-pixel radius, at unit scale so the world offset is the pixel
            // distance.
            assertThat(StarIconHitTest.isWorldPointOverStarIcon(
                    cursorOffsetFromAnchorBy(FULL_SIZE_ICON_RADIUS - 1f), ANCHOR,
                    FULL_SIZE_STAR_RADIUS, UNIT_FACTOR))
                    .isTrue();
        }

        @Test
        void isWorldPointOverStarIconIsFalseJustOutsideTheIconRadius() {
            assertThat(StarIconHitTest.isWorldPointOverStarIcon(
                    cursorOffsetFromAnchorBy(FULL_SIZE_ICON_RADIUS + 1f), ANCHOR,
                    FULL_SIZE_STAR_RADIUS, UNIT_FACTOR))
                    .isFalse();
        }

        @Test
        void isWorldPointOverStarIconGrowsTheHittableIconWithTheStar() {
            // The same point sits just outside a floor-sized star's icon but inside a full-sized
            // one's, so the star's radius is what decides it - a tiny star's icon (radius 10) does
            // not reach 15 world units out, a full star's (radius 20) does.
            var point = cursorOffsetFromAnchorBy(15f);
            assertThat(StarIconHitTest.isWorldPointOverStarIcon(point, ANCHOR, 0f, UNIT_FACTOR))
                    .isFalse();
            assertThat(StarIconHitTest.isWorldPointOverStarIcon(
                    point, ANCHOR, FULL_SIZE_STAR_RADIUS, UNIT_FACTOR))
                    .isTrue();
        }

        @Test
        void isWorldPointOverStarIconShrinksTheIconInWorldTermsAsTheMapZoomsIn() {
            // The icon radius is a fixed pixel size, so a higher factor packs it into fewer world
            // units: a point 15 world units out is inside the 20-pixel icon at unit zoom but past
            // it once the zoom doubles the pixels each world unit spans.
            var point = cursorOffsetFromAnchorBy(15f);
            assertThat(StarIconHitTest.isWorldPointOverStarIcon(
                    point, ANCHOR, FULL_SIZE_STAR_RADIUS, UNIT_FACTOR))
                    .isTrue();
            assertThat(StarIconHitTest.isWorldPointOverStarIcon(
                    point, ANCHOR, FULL_SIZE_STAR_RADIUS, 2f))
                    .isFalse();
        }
    }

    @Nested
    class ComputeIconRadius {

        @Test
        void computeIconRadiusScalesLinearlyWithTheStarBetweenTheBounds() {
            // Half the reference radius sits between the floor and the cap, so the scale is the
            // straight ratio: 40 * 0.5 / 2 = 10 pixels.
            assertThat(StarIconHitTest.computeIconRadius(50f)).isEqualTo(10f, within(1e-4f));
        }

        @Test
        void computeIconRadiusHoldsTheFloorForATinyStar() {
            // Below the floor the scale does not keep shrinking, so a near-zero star still gets the
            // minimum icon: 40 * 0.5 / 2 = 10 pixels, the same as at the floor.
            assertThat(StarIconHitTest.computeIconRadius(0f)).isEqualTo(10f, within(1e-4f));
        }

        @Test
        void computeIconRadiusHoldsTheCapForAHugeStar() {
            // Above the reference radius the scale saturates, so a giant star gets no more than the
            // full icon: 40 * 1 / 2 = 20 pixels.
            assertThat(StarIconHitTest.computeIconRadius(10000f)).isEqualTo(20f, within(1e-4f));
        }
    }
}
