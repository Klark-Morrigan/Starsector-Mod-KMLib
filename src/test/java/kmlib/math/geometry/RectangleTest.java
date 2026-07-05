package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the rectangle's point test: interior points are inside, exterior points are not, and
 * the edges count as inside so a hairline-precise hit does not fall through.
 */
class RectangleTest {

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
}
