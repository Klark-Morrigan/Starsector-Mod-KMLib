package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the box a point cloud is enclosed by and the overlap test that reads it.
 *
 * <p>The overlap cases are what the type exists for: a box stands in for the shape inside it while
 * something decides what is worth measuring properly, so the answer has to be right at the two
 * places a stand-in can mislead - boxes that miss on one axis alone, and boxes that meet along an
 * edge without covering any area between them.
 */
final class BoundsTest {

    // A box from (0, 0) to (10, 10), the fixed one every overlap case is posed against.
    private static final Bounds UNIT_BOX = new Bounds(0, 0, 10, 10);

    @Nested
    class ComputeEnclosingBounds {

        @Test
        void enclosesTheExtremesOfThePointsRatherThanTheirOrder() {
            // The extremes come from three different points, so a walk that read the first and
            // last, or that carried x for y, would come back with a plausible smaller box.
            assertThat(Bounds.computeEnclosingBounds(List.of(
                    new double[] {5, -2},
                    new double[] {-3, 7},
                    new double[] {1, 1})))
                .isEqualTo(new Bounds(-3, -2, 5, 7));
        }

        @Test
        void enclosesASinglePointAsABoxWithNoArea() {
            assertThat(Bounds.computeEnclosingBounds(List.of(new double[] {4, 6})))
                .isEqualTo(new Bounds(4, 6, 4, 6));
        }

        @Test
        void refusesToEncloseNoPointsAtAll() {
            // No box is the honest answer, and an empty cloud's min/max walk would otherwise
            // hand back an inverted box that reads as real to everything downstream.
            assertThatThrownBy(() -> Bounds.computeEnclosingBounds(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Overlaps {

        @Test
        void reportsTwoBoxesSharingAreaAsOverlapping() {
            assertThat(UNIT_BOX.overlaps(new Bounds(5, 5, 15, 15)))
                .isTrue();
        }

        @Test
        void reportsAnEnclosedBoxAsOverlapping() {
            assertThat(UNIT_BOX.overlaps(new Bounds(2, 2, 3, 3)))
                .isTrue();
        }

        @Test
        void reportsBoxesMeetingAlongAnEdgeAsOverlapping() {
            // Touching counts: the shapes inside two boxes that meet may still meet, and the
            // point of the test is to decide what is worth measuring rather than to measure.
            assertThat(UNIT_BOX.overlaps(new Bounds(10, 0, 20, 10)))
                .isTrue();
        }

        @Test
        void reportsBoxesApartAlongTheXAxisAsMissing() {
            assertThat(UNIT_BOX.overlaps(new Bounds(11, 0, 20, 10)))
                .isFalse();
        }

        @Test
        void reportsBoxesApartAlongTheYAxisAloneAsMissing() {
            // Overlapping in x and clear in y: the one case a test that checked either axis
            // rather than both would get wrong.
            assertThat(UNIT_BOX.overlaps(new Bounds(0, 11, 10, 20)))
                .isFalse();
        }

        @Test
        void answersTheSameWhicheverBoxIsAsked() {
            assertThat(new Bounds(5, 5, 15, 15).overlaps(UNIT_BOX))
                .isTrue();
        }
    }
}
