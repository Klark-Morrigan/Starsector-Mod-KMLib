package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static kmlib.math.geometry.GeometryTestSupport.buildAssertionSlack;
import static kmlib.math.geometry.GeometryTestSupport.buildReferenceSquare;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the contract of {@link RingPath#traceInsetRing}: the path starts at the top
 * centre above the anchor, runs clockwise whatever winding the ring arrived in, and
 * insets inward from a clockwise ring rather than outward; it falls back to the ring's
 * own top corner when the anchor sits beside the shape rather than within its span; and
 * it leaves nothing to trace when the inset outruns the ring on every side at once, when
 * it outruns a ring thin in one direction only, or when the ring encloses no area.
 *
 * <p>And of {@link RingPath#getPerimeter}: the traced inset ring's own length, and zero
 * where there is no path.
 *
 * <p>And of {@link RingPath#computePointAt}: zero is the start, a distance within an edge
 * interpolates along it, distances past the perimeter and before the start wrap round,
 * and an empty path has nothing to measure.
 *
 * <p>And of {@link RingPath#collectPointsBetween}: a stretch within one edge is its two
 * ends, a stretch spanning a corner keeps that corner, a stretch landing on a corner does
 * not repeat it, a stretch of no length is the single point it sits at, a stretch wrapping
 * past the start carries on round, one ending behind its start has no length, and one
 * longer than the perimeter is cut to a lap.
 */
final class RingPathTest {

    // A generous miter spike limit, as the inset suites use: high enough that the square
    // fixtures keep crisp mitred corners, so a test asserting exact corner coordinates is
    // asserting the inset rather than a bevel.
    private static final double MITER_SPIKE_LIMIT = 4.0;

    // The reference square's own centre. The anchor sits inside the traced ring, which is
    // the ordinary case: the shape is traced around a point within it.
    private static final double[] SQUARE_CENTRE = new double[] {5, 5};

    // Inset of the side-10 reference square, leaving the square from (2,2) to (8,8): a
    // 24-long ring whose corners and arc lengths are whole numbers, so every position
    // assertion below is a literal rather than a computation.
    private static final double INSET_DISTANCE = 2.0;

    @Nested
    class TraceInsetRing {

        @Test
        void path_starts_at_the_top_centre_above_the_anchor() {
            // The vertical line through (5,5) crosses the inset ring at y = 2 and y = 8;
            // the path starts at the higher one, which is the top centre.
            assertThat(traceReferenceSquare().getPoints().get(0))
                .containsExactly(new double[] {5, 8}, buildAssertionSlack());
        }

        @Test
        void path_runs_clockwise_from_its_start() {
            // Leaving the top centre toward (8,8) is rightward along the top edge, which
            // continues down the right-hand side - clockwise where y points up. A path
            // running the other way would carry every layout on it backward.
            var points = traceReferenceSquare().getPoints();

            assertThat(points)
                .hasSize(5);
            assertThat(points.get(1))
                .containsExactly(new double[] {8, 8}, buildAssertionSlack());
            assertThat(points.get(2))
                .containsExactly(new double[] {8, 2}, buildAssertionSlack());
            assertThat(points.get(3))
                .containsExactly(new double[] {2, 2}, buildAssertionSlack());
            assertThat(points.get(4))
                .containsExactly(new double[] {2, 8}, buildAssertionSlack());
        }

        @Test
        void path_of_a_clockwise_ring_is_the_path_of_the_same_ring_wound_the_other_way() {
            // The winding is normalised before the offset, so a clockwise ring insets
            // inward like any other. Un-normalised it would grow instead, putting the
            // path outside the shape it is meant to run within - the corners here would
            // come back at 12 and -2 rather than at 8 and 2.
            var clockwise = buildReferenceSquare();

            Collections.reverse(clockwise);

            var traced = RingPath.traceInsetRing(
                clockwise,
                INSET_DISTANCE,
                MITER_SPIKE_LIMIT,
                SQUARE_CENTRE);

            var expected = traceReferenceSquare().getPoints();

            assertThat(traced.getPoints())
                .hasSameSizeAs(expected);

            for (var i = 0; i < expected.size(); i++) {
                assertThat(traced.getPoints().get(i))
                    .containsExactly(expected.get(i), buildAssertionSlack());
            }
        }

        @Test
        void path_starts_at_the_ring_s_top_corner_when_the_anchor_sits_beside_it() {
            // No vertical line through x = 100 meets the ring at all, so there is no top
            // centre to find. The topmost corner keeps the path starting somewhere along
            // the ring's top rather than dropping it over an anchor that only says where
            // to look.
            var traced = RingPath.traceInsetRing(
                buildReferenceSquare(),
                INSET_DISTANCE,
                MITER_SPIKE_LIMIT,
                new double[] {100, 5});

            assertThat(traced.getPoints().get(0))
                .containsExactly(new double[] {2, 8}, buildAssertionSlack());
        }

        @Test
        void nothing_is_left_to_trace_when_the_inset_outruns_the_ring() {
            // A side-10 square cannot hold an inset of 6: the four offset edges cross
            // past one another and the ring folds through itself. The fold is a shape a
            // caller would otherwise walk and draw as a spur.
            var traced = RingPath.traceInsetRing(
                buildReferenceSquare(),
                6.0,
                MITER_SPIKE_LIMIT,
                SQUARE_CENTRE);

            assertThat(traced.isEmpty())
                .isTrue();
        }

        @Test
        void nothing_is_left_to_trace_when_the_ring_is_too_thin_in_one_direction_only() {
            // A 20-by-4 ring has length to spare and no width: inset by 3, its long sides
            // cross while its ends do not, and what comes back is a tidy 14-by-2 rectangle
            // whose corners stand 1 from the ring rather than 3. A ring thin in one
            // direction is the shape a cell is most likely to be when it has no room, and
            // it loses the whole path, not the thin part of it.
            var thin = Arrays.asList(
                new double[] {0, 0},
                new double[] {20, 0},
                new double[] {20, 4},
                new double[] {0, 4});

            var traced = RingPath.traceInsetRing(
                thin,
                3.0,
                MITER_SPIKE_LIMIT,
                new double[] {10, 2});

            assertThat(traced.isEmpty())
                .isTrue();
        }

        @Test
        void nothing_is_left_to_trace_when_the_ring_encloses_no_area() {
            // Two vertices bound nothing, so there is no interior to inset into.
            var traced = RingPath.traceInsetRing(
                Arrays.asList(new double[] {0, 0}, new double[] {10, 0}),
                1.0,
                MITER_SPIKE_LIMIT,
                new double[] {5, 0});

            assertThat(traced.isEmpty())
                .isTrue();
        }
    }

    @Nested
    class GetPerimeter {

        @Test
        void perimeter_is_the_length_of_the_inset_ring() {
            // The inset square runs from (2,2) to (8,8): four sides of 6.
            assertThat(traceReferenceSquare().getPerimeter())
                .isCloseTo(24.0, buildAssertionSlack());
        }

        @Test
        void perimeter_is_zero_where_there_is_no_path() {
            assertThat(RingPath.nothingLeftToTrace().getPerimeter())
                .isCloseTo(0.0, buildAssertionSlack());
        }
    }

    @Nested
    class ComputePointAt {

        @Test
        void point_at_zero_is_the_start() {
            assertThat(traceReferenceSquare().computePointAt(0))
                .containsExactly(new double[] {5, 8}, buildAssertionSlack());
        }

        @Test
        void point_within_an_edge_is_interpolated_along_it() {
            // 1.5 along the top edge from (5,8), and 6 - three past the corner at 3 -
            // partway down the right-hand edge.
            var path = traceReferenceSquare();

            assertThat(path.computePointAt(1.5))
                .containsExactly(new double[] {6.5, 8}, buildAssertionSlack());
            assertThat(path.computePointAt(6))
                .containsExactly(new double[] {8, 5}, buildAssertionSlack());
        }

        @Test
        void point_past_the_perimeter_wraps_round_to_the_start() {
            // A layout running off the end of the path continues round it rather than
            // having to be split by whoever laid it out.
            var path = traceReferenceSquare();

            assertThat(path.computePointAt(24))
                .containsExactly(new double[] {5, 8}, buildAssertionSlack());
            assertThat(path.computePointAt(25.5))
                .containsExactly(new double[] {6.5, 8}, buildAssertionSlack());
        }

        @Test
        void point_before_the_start_measures_back_from_the_end() {
            // Three back from the top centre is the corner at (2,8), three before the
            // path's end at 24.
            assertThat(traceReferenceSquare().computePointAt(-3))
                .containsExactly(new double[] {2, 8}, buildAssertionSlack());
        }

        @Test
        void an_empty_path_has_nothing_to_measure_between() {
            assertThatThrownBy(() -> RingPath.nothingLeftToTrace().computePointAt(0))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class CollectPointsBetween {

        @Test
        void stretch_within_one_edge_is_its_two_ends() {
            assertThatStretchIs(
                traceReferenceSquare().collectPointsBetween(1, 2),
                List.of(new double[] {6, 8}, new double[] {7, 8}));
        }

        @Test
        void stretch_spanning_a_corner_keeps_the_corner() {
            // From 1.5 along the top edge to 1.5 down the right-hand one. The corner at 3
            // is where the stretch bends, so it has to survive into the polyline - the
            // straight line between the two ends would cut across it.
            assertThatStretchIs(
                traceReferenceSquare().collectPointsBetween(1.5, 4.5),
                List.of(
                    new double[] {6.5, 8},
                    new double[] {8, 8},
                    new double[] {8, 6.5}));
        }

        @Test
        void stretch_ending_on_a_corner_does_not_repeat_it() {
            // The corner is both the last turn and the end point; emitting it twice would
            // leave a zero-length step for whatever gives the stretch girth.
            assertThatStretchIs(
                traceReferenceSquare().collectPointsBetween(1.5, 3),
                List.of(new double[] {6.5, 8}, new double[] {8, 8}));
        }

        @Test
        void stretch_starting_on_a_corner_walks_forward_from_it() {
            // A start landing exactly on a corner belongs to the edge leaving it, so the
            // walk steps forward down the right-hand edge rather than back along the top.
            assertThatStretchIs(
                traceReferenceSquare().collectPointsBetween(3, 4),
                List.of(new double[] {8, 8}, new double[] {8, 7}));
        }

        @Test
        void stretch_of_no_length_is_the_single_point_it_sits_at() {
            assertThatStretchIs(
                traceReferenceSquare().collectPointsBetween(5, 5),
                List.of(new double[] {8, 6}));
        }

        @Test
        void stretch_wrapping_past_the_start_carries_on_round() {
            // From 1 before the end to 1 after it. The start point is a listed corner of
            // the path - it split the edge it sits on - so it appears on the way past.
            assertThatStretchIs(
                traceReferenceSquare().collectPointsBetween(23, 25),
                List.of(
                    new double[] {4, 8},
                    new double[] {5, 8},
                    new double[] {6, 8}));
        }

        @Test
        void stretch_ending_behind_its_start_is_of_no_length() {
            // The walk only runs forward, and an end behind its start is a caller's
            // arithmetic having gone wrong. Reading it as "almost all the way round" would
            // turn that slip into a nearly complete lap; a point is the safer reading.
            assertThatStretchIs(
                traceReferenceSquare().collectPointsBetween(5, 4),
                List.of(new double[] {8, 6}));
        }

        @Test
        void stretch_longer_than_the_perimeter_is_cut_to_one_lap() {
            // Going round twice would only retrace the same geometry, so the walk stops
            // where it began - both ends of the lap kept, since they are the two ends of
            // a polyline rather than a repeated corner.
            assertThatStretchIs(
                traceReferenceSquare().collectPointsBetween(0, 30),
                List.of(
                    new double[] {5, 8},
                    new double[] {8, 8},
                    new double[] {8, 2},
                    new double[] {2, 2},
                    new double[] {2, 8},
                    new double[] {5, 8}));
        }

        @Test
        void an_empty_path_has_nothing_to_walk_between() {
            assertThatThrownBy(() -> RingPath.nothingLeftToTrace().collectPointsBetween(0, 1))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    // The path around the reference square inset by 2, anchored at its centre: the square
    // from (2,2) to (8,8), traced clockwise from (5,8).
    private static RingPath traceReferenceSquare() {
        return RingPath.traceInsetRing(
            buildReferenceSquare(),
            INSET_DISTANCE,
            MITER_SPIKE_LIMIT,
            SQUARE_CENTRE);
    }

    private static void assertThatStretchIs(List<double[]> stretch, List<double[]> expected) {

        assertThat(stretch)
            .hasSameSizeAs(expected);

        for (var i = 0; i < expected.size(); i++) {
            assertThat(stretch.get(i))
                .containsExactly(expected.get(i), buildAssertionSlack());
        }
    }
}
