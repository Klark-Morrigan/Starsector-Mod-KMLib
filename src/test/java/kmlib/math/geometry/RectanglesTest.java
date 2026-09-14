package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link Rectangles#findIndexContaining}: it returns the first box a point falls in (the
 * earlier one on a shared edge), {@link Rectangles#NONE} when none contains it, and reads the box
 * out of an arbitrary element through the bounds extractor.
 *
 * <p>And {@link Rectangles#describe}, which is shared for a reason worth holding: the widget probes
 * and the raw GL reads both write boxes into the same log, so two spellings would make two lines
 * describing one box look like two boxes.
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

    @Nested
    class Describe {

        @Test
        void describeNamesThePositionAndSizeInOrder() {
            assertThat(Rectangles.describe(new Rectangle(10f, 20f, 30f, 40f)))
                .isEqualTo("x=10 y=20 w=30 h=40");
        }

        @Test
        void describeRoundsToWholeUnits() {
            // Read off a log by eye against the game's own pixel grid, where a fractional coordinate
            // is noise rather than precision.
            assertThat(Rectangles.describe(new Rectangle(10.4f, 20.5f, 30.6f, 40.49f)))
                .isEqualTo("x=10 y=21 w=31 h=40");
        }

        @Test
        void describeSaysThereWasNothingToMeasureForNoBox() {
            // A caller with no box at all must not have that read as a box at the origin.
            assertThat(Rectangles.describe(null))
                .isEqualTo("none");
        }
    }
}
