package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the rectangle's point test: interior points are inside, exterior points are not, and
 * the edges count as inside so a hairline-precise hit does not fall through. Also pins the centre
 * accessors used to place a centred element.
 */
class RectangleTest {
    private static final float TOLERANCE = 0.01f;

    @Nested
    class ContainsPoint {
        private final Rectangle rectangle = new Rectangle(10f, 20f, 100f, 50f);

        @Test
        void reportsAnInteriorPointAsInside() {
            assertThat(rectangle.containsPoint(50f, 40f)).isTrue();
        }

        @Test
        void reportsAPointLeftOfTheRectangleAsOutside() {
            assertThat(rectangle.containsPoint(5f, 40f)).isFalse();
        }

        @Test
        void reportsAPointAboveTheRectangleAsOutside() {
            assertThat(rectangle.containsPoint(50f, 80f)).isFalse();
        }

        @Test
        void countsTheLowerLeftCornerAsInside() {
            assertThat(rectangle.containsPoint(10f, 20f)).isTrue();
        }

        @Test
        void countsTheUpperRightCornerAsInside() {
            assertThat(rectangle.containsPoint(110f, 70f)).isTrue();
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
