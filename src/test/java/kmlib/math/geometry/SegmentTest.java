package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.math.geometry.GeometryTestSupport.buildAssertionSlack;
import static org.assertj.core.api.Assertions.assertThat;

class SegmentTest {

    @Nested
    class ComputeCrossingPoint {
        @Test
        void computeCrossingPointLandsWhereTheSignedValueReachesZero() {
            // Signed +4 at (0,0) and -4 at (8,0) crosses zero at the midpoint.
            assertThat(Segment.computeCrossingPoint(new double[] {0, 0}, new double[] {8, 0}, 4, -4))
                .containsExactly(4.0, 0.0);
        }

        @Test
        void computeCrossingPointIsProportionalToTheSignedMagnitudes() {
            // +1 at start, -3 at end: the zero is a quarter of the way along.
            assertThat(Segment.computeCrossingPoint(new double[] {0, 0}, new double[] {8, 4}, 1, -3))
                .containsExactly(2.0, 1.0);
        }

        @Test
        void computeCrossingPointIsOrderIndependentForTheSameSegment() {
            // Walking the segment the other way with swapped signs finds the same
            // point on it.
            assertThat(Segment.computeCrossingPoint(new double[] {8, 0}, new double[] {0, 0}, -4, 4))
                .containsExactly(4.0, 0.0);
        }
    }

    @Nested
    class ComputeDistanceToPoint {

        @Test
        void computeDistanceToPointIsThePerpendicularWhereTheFootLandsOnTheSegment() {
            // (4,3) drops onto the segment at (4,0), three away.
            assertThat(Segment.computeDistanceToPoint(
                    new double[] {0, 0},
                    new double[] {8, 0},
                    new double[] {4, 3}))
                .isCloseTo(3.0, buildAssertionSlack());
        }

        @Test
        void computeDistanceToPointMeasuresToTheNearerEndPastTheSegment() {
            // (12,0) sits four past the end. The perpendicular foot is off the segment,
            // so the distance is to the end itself - the bound that separates this from
            // a distance to the infinite line, which would answer zero.
            assertThat(Segment.computeDistanceToPoint(
                    new double[] {0, 0},
                    new double[] {8, 0},
                    new double[] {12, 0}))
                .isCloseTo(4.0, buildAssertionSlack());
        }

        @Test
        void computeDistanceToPointMeasuresToTheStartBeforeTheSegment() {
            assertThat(Segment.computeDistanceToPoint(
                    new double[] {0, 0},
                    new double[] {8, 0},
                    new double[] {-3, 4}))
                .isCloseTo(5.0, buildAssertionSlack());
        }

        @Test
        void computeDistanceToPointIsZeroOnTheSegment() {
            assertThat(Segment.computeDistanceToPoint(
                    new double[] {0, 0},
                    new double[] {8, 0},
                    new double[] {5, 0}))
                .isCloseTo(0.0, buildAssertionSlack());
        }

        @Test
        void computeDistanceToPointMeasuresToTheSpotASegmentTooShortToAimSitsAt() {
            // No direction to project onto, so the answer is the distance to the point
            // the degenerate segment occupies rather than a projection onto noise.
            assertThat(Segment.computeDistanceToPoint(
                    new double[] {3, 4},
                    new double[] {3, 4},
                    new double[] {0, 0}))
                .isCloseTo(5.0, buildAssertionSlack());
        }
    }
}
