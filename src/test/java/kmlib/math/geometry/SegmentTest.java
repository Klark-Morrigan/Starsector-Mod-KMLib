package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}
