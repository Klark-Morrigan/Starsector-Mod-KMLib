package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LinesTest {

    @Nested
    class IntersectLines {

        @Test
        void intersectLinesFindsWhereTwoCrossingLinesMeet() {
            // The x-axis (through the origin, direction +x) and the vertical line
            // x = 2 (through (2, -5), direction +y) cross at (2, 0).
            assertThat(Lines.intersectLines(
                    new double[] {0, 0},
                    1,
                    0,
                    new double[] {2, -5},
                    0,
                    1))
                .containsExactly(2.0, 0.0);
        }

        @Test
        void intersectLinesReturnsTheCrossingBeyondBothGivenPoints() {
            // The intersection is on the infinite lines, not a segment: it can land
            // past the points that define each line. These two meet at (4, 4), off
            // to one side of both (0, 0) and (4, 0).
            assertThat(Lines.intersectLines(
                    new double[] {0, 0},
                    1,
                    1,
                    new double[] {4, 0},
                    0,
                    1))
                .containsExactly(4.0, 4.0);
        }

        @Test
        void intersectLinesIsNullForParallelLines() {
            // Same direction, different points - the lines never cross.
            assertThat(Lines.intersectLines(
                    new double[] {0, 0},
                    1,
                    0,
                    new double[] {0, 5},
                    2,
                    0))
                .isNull();
        }

        @Test
        void intersectLinesIsNullForCollinearLines() {
            // Coincident lines have no single crossing, and the parallel guard
            // reports null rather than dividing by a zero cross product.
            assertThat(Lines.intersectLines(
                    new double[] {0, 0},
                    1,
                    0,
                    new double[] {3, 0},
                    1,
                    0))
                .isNull();
        }
    }

    @Nested
    class ComputePerpendicularDistance {

        @Test
        void computePerpendicularDistanceMeasuresHeightAboveTheLine() {
            // The point (3, 4) sits four units above the x-axis (the line through
            // (0, 0) and (10, 0)).
            assertThat(Lines.computePerpendicularDistance(
                    new double[] {3, 4},
                    new double[] {0, 0},
                    new double[] {10, 0}))
                .isEqualTo(4.0);
        }

        @Test
        void computePerpendicularDistanceIsUnsignedForAPointBelowTheLine() {
            // Distance is a magnitude, so a point on the far side of the line reads
            // the same height as one the same distance above it.
            assertThat(Lines.computePerpendicularDistance(
                    new double[] {3, -4},
                    new double[] {0, 0},
                    new double[] {10, 0}))
                .isEqualTo(4.0);
        }

        @Test
        void computePerpendicularDistanceIsZeroForAPointOnTheLine() {
            assertThat(Lines.computePerpendicularDistance(
                    new double[] {7, 0},
                    new double[] {0, 0},
                    new double[] {10, 0}))
                .isZero();
        }

        @Test
        void computePerpendicularDistanceFallsBackToTheEndpointWhenTheLineHasNoDirection() {
            // Coincident line points give no direction, so the distance is measured
            // straight to that point instead: (3, 4) is five from (0, 0).
            assertThat(Lines.computePerpendicularDistance(
                    new double[] {3, 4},
                    new double[] {0, 0},
                    new double[] {0, 0}))
                .isEqualTo(5.0);
        }
    }

    @Nested
    class ComputeSignedOffsetFromLine {

        @Test
        void computeSignedOffsetFromLineIsPositiveOnTheNormalSide() {
            // Line x = 5 with the normal pointing +x: a point at x = 8 is three
            // units into the kept side.
            assertThat(Lines.computeSignedOffsetFromLine(new double[] {8, 0}, new HalfPlane(5, 0, 1, 0)))
                .isEqualTo(3.0);
        }

        @Test
        void computeSignedOffsetFromLineIsNegativeOnTheFarSide() {
            assertThat(Lines.computeSignedOffsetFromLine(new double[] {2, 0}, new HalfPlane(5, 0, 1, 0)))
                .isEqualTo(-3.0);
        }

        @Test
        void computeSignedOffsetFromLineIsZeroOnTheLine() {
            assertThat(Lines.computeSignedOffsetFromLine(new double[] {5, 100}, new HalfPlane(5, 0, 1, 0)))
                .isZero();
        }

        @Test
        void computeSignedOffsetFromLineScalesWithANonUnitNormal() {
            // Only the sign is reliable when the normal is not unit length: a
            // normal of length two doubles the magnitude but keeps the side.
            assertThat(Lines.computeSignedOffsetFromLine(new double[] {8, 0}, new HalfPlane(5, 0, 2, 0)))
                .isEqualTo(6.0);
        }
    }
}
