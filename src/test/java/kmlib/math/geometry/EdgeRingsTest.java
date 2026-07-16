package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the contract of {@link EdgeRings#chainIntoRings}:
 *  - four segments of one square chain into a single four-corner ring in order,
 *  - endpoints that differ by less than the weld tolerance still chain,
 *  - two disjoint squares come back as two separate rings,
 *  - a strand that never closes is dropped rather than emitted open,
 *  - empty input yields no rings.
 *
 * <p>And of {@link EdgeRings#chainIntoRingsWithEdgeValues}: a per-segment value rides
 * onto the ring edge its segment became through the reordering, a value array not
 * parallel to the segments is rejected, and an unclosed strand drops with its values.
 */
final class EdgeRingsTest {

    private static final double WELD_TOLERANCE = 1e-3;

    // A directed segment the chainer consumes.
    private static Segment segment(double x1, double y1, double x2, double y2) {
        return new Segment(x1, y1, x2, y2);
    }

    @Nested
    class ChainIntoRings {
        @Test
        void four_segments_of_a_square_chain_into_one_ordered_ring() {
            // CCW unit square handed over out of order; the walk stitches it back
            // into one ring of its four corners.
            var segments = Arrays.asList(
                    segment(10, 10, 0, 10),
                    segment(0, 0, 10, 0),
                    segment(10, 0, 10, 10),
                    segment(0, 10, 0, 0));

            var rings = EdgeRings.chainIntoRings(segments, WELD_TOLERANCE);

            assertThat(rings).hasSize(1);
            assertThat(rings.get(0)).hasSize(4);
            // Every square corner appears exactly once, so the ring closes on the
            // full loop rather than a partial strand.
            assertThat(rings.get(0)).anySatisfy(v -> assertThat(v).containsExactly(0.0, 0.0));
            assertThat(rings.get(0)).anySatisfy(v -> assertThat(v).containsExactly(10.0, 0.0));
            assertThat(rings.get(0)).anySatisfy(v -> assertThat(v).containsExactly(10.0, 10.0));
            assertThat(rings.get(0)).anySatisfy(v -> assertThat(v).containsExactly(0.0, 10.0));
        }

        @Test
        void endpoints_within_the_weld_tolerance_still_chain() {
            // A corner reported by its two segments at coordinates a rounding
            // whisker apart (well below the tolerance) still welds into one corner,
            // so the ring closes rather than splitting at the seam.
            var drift = WELD_TOLERANCE / 10;
            var segments = Arrays.asList(
                    segment(0, 0, 10, 0),
                    segment(10 + drift, drift, 10, 10),
                    segment(10, 10, 0, 0));

            var rings = EdgeRings.chainIntoRings(segments, WELD_TOLERANCE);

            assertThat(rings).hasSize(1);
            assertThat(rings.get(0)).hasSize(3);
        }

        @Test
        void disjoint_squares_come_back_as_two_rings() {
            var segments = Arrays.asList(
                    segment(0, 0, 10, 0),
                    segment(10, 0, 10, 10),
                    segment(10, 10, 0, 10),
                    segment(0, 10, 0, 0),
                    segment(100, 100, 110, 100),
                    segment(110, 100, 110, 110),
                    segment(110, 110, 100, 110),
                    segment(100, 110, 100, 100));

            var rings = EdgeRings.chainIntoRings(segments, WELD_TOLERANCE);

            assertThat(rings).hasSize(2);
            assertThat(rings).allSatisfy(ring -> assertThat(ring).hasSize(4));
        }

        @Test
        void an_unclosed_strand_is_dropped() {
            // Three segments that march away without returning to the start: no
            // ring closes, so nothing is emitted (never a stray open loop).
            var segments = Arrays.asList(
                    segment(0, 0, 10, 0),
                    segment(10, 0, 20, 0),
                    segment(20, 0, 30, 0));

            assertThat(EdgeRings.chainIntoRings(segments, WELD_TOLERANCE)).isEmpty();
        }

        @Test
        void empty_input_yields_no_rings() {
            assertThat(EdgeRings.chainIntoRings(List.of(), WELD_TOLERANCE)).isEmpty();
        }
    }

    @Nested
    class ChainIntoRingsWithEdgeValues {
        @Test
        void values_follow_their_segments_through_the_reordering() {
            // The square handed over out of order, each segment tagged with its own
            // start x. The walk re-orders the segments into winding order, and each
            // edge's carried value still equals its corner's x - so every value rode
            // along with the segment it was attached to rather than staying by index.
            var segments = Arrays.asList(
                    segment(10, 10, 0, 10),
                    segment(0, 0, 10, 0),
                    segment(10, 0, 10, 10),
                    segment(0, 10, 0, 0));
            var values = new double[] {10.0, 0.0, 10.0, 0.0};

            var rings = EdgeRings.chainIntoRingsWithEdgeValues(segments, values, WELD_TOLERANCE);

            assertThat(rings).hasSize(1);
            var ring = rings.get(0);
            assertThat(ring.corners()).hasSize(4);
            assertThat(ring.edgeValues()).hasSize(4);
            for (var k = 0; k < ring.corners().size(); k++) {
                assertThat(ring.edgeValues()[k]).isEqualTo(ring.corners().get(k)[0]);
            }
        }

        @Test
        void a_value_array_not_parallel_to_the_segments_is_rejected() {
            var segments = Arrays.asList(
                    segment(0, 0, 10, 0),
                    segment(10, 0, 10, 10),
                    segment(10, 10, 0, 0));

            assertThatThrownBy(() -> EdgeRings.chainIntoRingsWithEdgeValues(
                    segments, new double[] {1.0, 2.0}, WELD_TOLERANCE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void an_unclosed_strand_drops_with_its_values() {
            var segments = Arrays.asList(
                    segment(0, 0, 10, 0),
                    segment(10, 0, 20, 0),
                    segment(20, 0, 30, 0));

            assertThat(EdgeRings.chainIntoRingsWithEdgeValues(
                    segments, new double[] {1.0, 2.0, 3.0}, WELD_TOLERANCE)).isEmpty();
        }
    }
}
