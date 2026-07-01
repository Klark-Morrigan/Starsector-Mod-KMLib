package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.assertj.core.api.Assertions.assertThat;

class PointsTest {

    @Nested
    class ComputeDistance {
        @Test
        void computeDistanceIsEuclidean() {
            assertThat(Points.computeDistance(0, 0, 3, 4)).isEqualTo(5.0);
        }

        @Test
        void computeDistanceIsZeroForCoincidentPoints() {
            assertThat(Points.computeDistance(2, 7, 2, 7)).isZero();
        }

        @Test
        void computeDistanceArrayOverloadMatchesCoordinateForm() {
            assertThat(Points.computeDistance(new double[] {1, 1}, new double[] {4, 5}))
                    .isEqualTo(5.0);
        }

        @Test
        void computeDistanceVectorOverloadMatchesCoordinateForm() {
            assertThat(Points.computeDistance(new Vector2f(1, 1), new Vector2f(4, 5)))
                    .isEqualTo(5.0);
        }
    }

    @Nested
    class ComputeAngleDegrees {
        @Test
        void computeAngleDegreesVectorOverloadMatchesCoordinateForm() {
            assertThat(Points.computeAngleDegrees(new Vector2f(0, 0), new Vector2f(1, 1)))
                    .isEqualTo(45.0);
        }

        @Test
        void computeAngleDegreesIsCounterClockwiseFromPositiveX() {
            assertThat(Points.computeAngleDegrees(0, 0, 1, 1)).isEqualTo(45.0);
            assertThat(Points.computeAngleDegrees(0, 0, 0, 1)).isEqualTo(90.0);
            assertThat(Points.computeAngleDegrees(0, 0, -1, 0)).isEqualTo(180.0);
        }

        @Test
        void computeAngleDegreesIsRelativeToTheFirstPoint() {
            assertThat(Points.computeAngleDegrees(2, 2, 5, 6)).isCloseTo(53.13,
                    org.assertj.core.api.Assertions.within(0.01));
        }
    }

    @Nested
    class ComputeCrossingPoint {
        @Test
        void computeCrossingPointLandsWhereTheSignedValueReachesZero() {
            // Signed +4 at (0,0) and -4 at (8,0) crosses zero at the midpoint.
            assertThat(Points.computeCrossingPoint(new double[] {0, 0}, new double[] {8, 0}, 4, -4))
                    .containsExactly(4.0, 0.0);
        }

        @Test
        void computeCrossingPointIsProportionalToTheSignedMagnitudes() {
            // +1 at a, -3 at b: the zero is a quarter of the way along.
            assertThat(Points.computeCrossingPoint(new double[] {0, 0}, new double[] {8, 4}, 1, -3))
                    .containsExactly(2.0, 1.0);
        }

        @Test
        void computeCrossingPointIsOrderIndependentForTheSameSegment() {
            // Walking the segment the other way with swapped signs finds the same
            // point on the line.
            assertThat(Points.computeCrossingPoint(new double[] {8, 0}, new double[] {0, 0}, -4, 4))
                    .containsExactly(4.0, 0.0);
        }
    }
}
