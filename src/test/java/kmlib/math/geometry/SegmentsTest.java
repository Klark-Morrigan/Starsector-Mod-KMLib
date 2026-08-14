package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.math.geometry.GeometryTestSupport.buildAssertionSlack;
import static org.assertj.core.api.Assertions.assertThat;

class SegmentsTest {

    @Nested
    class IntersectSegments {

        @Test
        void intersectSegmentsFindsWhereTwoCrossingSpansMeet() {
            // An X: the two diagonals of the unit-10 square cross at its centre.
            assertThat(Segments.intersectSegments(
                    new double[] {0, 0},
                    new double[] {10, 10},
                    new double[] {0, 10},
                    new double[] {10, 0}))
                .containsExactly(5.0, 5.0);
        }

        @Test
        void intersectSegmentsIsNullWhenOnlyTheLinesWouldCross() {
            // The distinguishing case against intersectLines, and the whole reason this
            // exists: two short spans far apart whose infinite lines meet at (4, 4).
            // Extended they cross; as drawn they come nowhere near each other.
            assertThat(Segments.intersectSegments(
                    new double[] {0, 0},
                    new double[] {1, 1},
                    new double[] {4, 0},
                    new double[] {4, 1}))
                .isNull();
        }

        @Test
        void intersectSegmentsMeetingEndToEndCountsAsCrossing() {
            // Touching at a shared endpoint is a crossing at the ends of both spans.
            // Pinned because a ring's consecutive edges always touch this way, so a
            // caller scanning a ring for folds has to exclude them itself rather than
            // rely on this to.
            assertThat(Segments.intersectSegments(
                    new double[] {0, 0},
                    new double[] {10, 0},
                    new double[] {10, 0},
                    new double[] {10, 10}))
                .containsExactly(10.0, 0.0);
        }

        @Test
        void intersectSegmentsIsNullForCollinearSpansThatOverlap() {
            // Not a miss: these two share the stretch from 5 to 10. Pinned because null
            // reads as "they do not touch" everywhere else this returns it, and a caller
            // that needs to tell an overlap apart from a miss has to look elsewhere.
            assertThat(Segments.intersectSegments(
                    new double[] {0, 0},
                    new double[] {10, 0},
                    new double[] {5, 0},
                    new double[] {15, 0}))
                .isNull();
        }

        @Test
        void intersectSegmentsIsNullForParallelSpans() {

            assertThat(Segments.intersectSegments(
                    new double[] {0, 0},
                    new double[] {10, 0},
                    new double[] {0, 5},
                    new double[] {10, 5}))
                .isNull();
        }
    }

    @Nested
    class ComputeCrossingPoint {
        @Test
        void computeCrossingPointLandsWhereTheSignedValueReachesZero() {
            // Signed +4 at (0,0) and -4 at (8,0) crosses zero at the midpoint.
            assertThat(Segments.computeCrossingPoint(new double[] {0, 0}, new double[] {8, 0}, 4, -4))
                .containsExactly(4.0, 0.0);
        }

        @Test
        void computeCrossingPointIsProportionalToTheSignedMagnitudes() {
            // +1 at start, -3 at end: the zero is a quarter of the way along.
            assertThat(Segments.computeCrossingPoint(new double[] {0, 0}, new double[] {8, 4}, 1, -3))
                .containsExactly(2.0, 1.0);
        }

        @Test
        void computeCrossingPointIsOrderIndependentForTheSameSegment() {
            // Walking the segment the other way with swapped signs finds the same
            // point on it.
            assertThat(Segments.computeCrossingPoint(new double[] {8, 0}, new double[] {0, 0}, -4, 4))
                .containsExactly(4.0, 0.0);
        }
    }

    @Nested
    class ComputeDistanceToPoint {

        @Test
        void computeDistanceToPointIsThePerpendicularWhereTheFootLandsOnTheSegment() {
            // (4,3) drops onto the segment at (4,0), three away.
            assertThat(Segments.computeDistanceToPoint(
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
            assertThat(Segments.computeDistanceToPoint(
                    new double[] {0, 0},
                    new double[] {8, 0},
                    new double[] {12, 0}))
                .isCloseTo(4.0, buildAssertionSlack());
        }

        @Test
        void computeDistanceToPointMeasuresToTheStartBeforeTheSegment() {
            assertThat(Segments.computeDistanceToPoint(
                    new double[] {0, 0},
                    new double[] {8, 0},
                    new double[] {-3, 4}))
                .isCloseTo(5.0, buildAssertionSlack());
        }

        @Test
        void computeDistanceToPointIsZeroOnTheSegment() {
            assertThat(Segments.computeDistanceToPoint(
                    new double[] {0, 0},
                    new double[] {8, 0},
                    new double[] {5, 0}))
                .isCloseTo(0.0, buildAssertionSlack());
        }

        @Test
        void computeDistanceToPointMeasuresToTheSpotASegmentTooShortToAimSitsAt() {
            // No direction to project onto, so the answer is the distance to the point
            // the degenerate segment occupies rather than a projection onto noise.
            assertThat(Segments.computeDistanceToPoint(
                    new double[] {3, 4},
                    new double[] {3, 4},
                    new double[] {0, 0}))
                .isCloseTo(5.0, buildAssertionSlack());
        }
    }
}
