package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link Rectangles#findIndexContaining}: it returns the first box a point falls in (the
 * earlier one on a shared edge), {@link Rectangles#NONE} when none contains it, and reads the box
 * out of an arbitrary element through the bounds extractor.
 */
class RectanglesTest {

    @Nested
    class FindIndexContaining {
        private final List<Rectangle> boxes = List.of(new Rectangle(0f, 0f, 10f, 10f),
                new Rectangle(10f, 0f, 10f, 10f));

        @Test
        void findsTheFirstBoxContainingThePoint() {
            assertThat(Rectangles.findIndexContaining(boxes, 15f, 5f)).isEqualTo(1);
        }

        @Test
        void resolvesASharedEdgeToTheEarlierBox() {
            assertThat(Rectangles.findIndexContaining(boxes, 10f, 5f)).isEqualTo(0);
        }

        @Test
        void reportsNoneWhenNoBoxContainsThePoint() {
            assertThat(Rectangles.findIndexContaining(boxes, 25f, 5f)).isEqualTo(Rectangles.NONE);
        }

        @Test
        void readsBoundsOutOfAnElementThroughTheExtractor() {
            record Labelled(String name, Rectangle bounds) {
            }
            var items = List.of(new Labelled("a", new Rectangle(0f, 0f, 10f, 10f)),
                    new Labelled("b", new Rectangle(10f, 0f, 10f, 10f)));
            assertThat(Rectangles.findIndexContaining(items, Labelled::bounds, 15f, 5f))
                    .isEqualTo(1);
        }
    }
}
