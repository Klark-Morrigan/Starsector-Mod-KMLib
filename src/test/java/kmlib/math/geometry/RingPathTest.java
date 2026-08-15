package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static kmlib.math.geometry.GeometryTestSupport.assertThatPointsAre;
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
 *
 * <p>And of {@link RingPath#fuseStretchAcrossStart}: the pair reaching the path's two ends
 * comes back as the one stretch it is, closing past the perimeter, while stretches falling
 * short of either end and a lone stretch are left exactly as they arrived.
 *
 * <p>And of {@link RingPath#placeSpanNearestStart}: a span sits as near the path's start as
 * its stretch allows, which is on the start itself where there is room after it, backed up
 * where the stretch closes too soon, at the stretch's own opening where a shape covers the
 * start, and at whichever end of a far-off stretch puts the span's start nearer - ties
 * taking the stretch's start. A span outrunning its stretch opens where the stretch does,
 * and an empty path has no lap to place within.
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

    // A layout a sixth of the path long. Short enough against the stretches it is placed on
    // that where it sits is a decision rather than the only place it fits, which is the
    // question the placement exists to answer.
    private static final double SPAN_LENGTH = 4.0;

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
            assertThatPointsAre(
                traceReferenceSquare().collectPointsBetween(1, 2),
                List.of(new double[] {6, 8}, new double[] {7, 8}));
        }

        @Test
        void stretch_spanning_a_corner_keeps_the_corner() {
            // From 1.5 along the top edge to 1.5 down the right-hand one. The corner at 3
            // is where the stretch bends, so it has to survive into the polyline - the
            // straight line between the two ends would cut across it.
            assertThatPointsAre(
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
            assertThatPointsAre(
                traceReferenceSquare().collectPointsBetween(1.5, 3),
                List.of(new double[] {6.5, 8}, new double[] {8, 8}));
        }

        @Test
        void stretch_starting_on_a_corner_walks_forward_from_it() {
            // A start landing exactly on a corner belongs to the edge leaving it, so the
            // walk steps forward down the right-hand edge rather than back along the top.
            assertThatPointsAre(
                traceReferenceSquare().collectPointsBetween(3, 4),
                List.of(new double[] {8, 8}, new double[] {8, 7}));
        }

        @Test
        void stretch_of_no_length_is_the_single_point_it_sits_at() {
            assertThatPointsAre(
                traceReferenceSquare().collectPointsBetween(5, 5),
                List.of(new double[] {8, 6}));
        }

        @Test
        void stretch_wrapping_past_the_start_carries_on_round() {
            // From 1 before the end to 1 after it. The start point is a listed corner of
            // the path - it split the edge it sits on - so it appears on the way past.
            assertThatPointsAre(
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
            assertThatPointsAre(
                traceReferenceSquare().collectPointsBetween(5, 4),
                List.of(new double[] {8, 6}));
        }

        @Test
        void stretch_longer_than_the_perimeter_is_cut_to_one_lap() {
            // Going round twice would only retrace the same geometry, so the walk stops
            // where it began - both ends of the lap kept, since they are the two ends of
            // a polyline rather than a repeated corner.
            assertThatPointsAre(
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

    @Nested
    class FindClearArcs {

        @Test
        void whole_path_is_clear_when_no_shape_covers_it() {
            // The ordinary case, and the one every layout that asks for nothing to be kept
            // clear of lands in: one interval, the path from end to end.
            assertThatArcsAre(
                traceReferenceSquare().findClearArcs(List.of()),
                List.of(new double[] {0, 24}));
        }

        @Test
        void shape_over_the_path_leaves_the_stretches_either_side_of_it() {
            // A box over the top right corner, covering the top edge from x=6 and the right
            // edge down to y=7. The path reaches x=6 one along and leaves y=7 four along, so
            // what is left is the run up to the box and the run from it back round.
            assertThatArcsAre(
                traceReferenceSquare().findClearArcs(List.of(buildBox(6, 7, 9, 9))),
                List.of(
                    new double[] {0, 1},
                    new double[] {4, 24}));
        }

        @Test
        void cover_spanning_several_edges_comes_back_as_one_stretch() {
            // The same box states the point on its own: it covers the end of one edge and the
            // start of the next, and the corner between them is under it too. Reported as two
            // intervals meeting at the corner, a layout would read a gap where the shape is
            // continuous, and could lay a run in it.
            assertThat(traceReferenceSquare().findClearArcs(List.of(buildBox(6, 7, 9, 9))))
                .hasSize(2);
        }

        @Test
        void shape_over_the_start_leaves_the_stretch_between_its_two_sides() {
            // A box across the top centre covers the first stretch of the path and the last -
            // they meet at the start, but the intervals do not wrap, so the pieces before and
            // after the origin are stated separately and only the middle survives.
            assertThatArcsAre(
                traceReferenceSquare().findClearArcs(List.of(buildBox(4, 7, 6, 9))),
                List.of(new double[] {1, 23}));
        }

        @Test
        void several_shapes_over_the_path_each_take_their_own_stretch() {
            // Every shape is tested, not just the nearest: two names over one cell take two
            // bites out of its ring, which is the case a single-shape carve would half-answer.
            assertThatArcsAre(
                traceReferenceSquare().findClearArcs(List.of(
                    buildBox(6, 7, 9, 9),
                    buildBox(1, 3, 3, 5))),
                List.of(
                    new double[] {0, 1},
                    new double[] {4, 16},
                    new double[] {18, 24}));
        }

        @Test
        void shape_covering_the_whole_path_leaves_nothing_clear() {
            // A name across the whole shape. Nothing can be laid along what is left, and
            // saying so is what lets a caller answer "then draw none" rather than draw a
            // sliver somewhere.
            assertThat(traceReferenceSquare().findClearArcs(List.of(buildBox(0, 0, 10, 10))))
                .isEmpty();
        }

        @Test
        void shape_lying_elsewhere_covers_nothing() {
            // The whole map's shapes are handed over, so most of them are nowhere near any one
            // path - and a shape that misses must leave the path exactly as it found it.
            assertThatArcsAre(
                traceReferenceSquare().findClearArcs(List.of(buildBox(100, 100, 120, 120))),
                List.of(new double[] {0, 24}));
        }

        @Test
        void an_empty_path_has_no_stretches_to_offer() {
            // A ring that left nothing to trace has nothing to carve either, and answering
            // with a stretch of a path that does not exist would be worse than answering none.
            assertThat(RingPath.nothingLeftToTrace().findClearArcs(List.of(buildBox(0, 0, 1, 1))))
                .isEmpty();
        }
    }

    @Nested
    class FuseStretchAcrossStart {

        @Test
        void stretches_meeting_at_the_start_come_back_as_the_one_stretch_they_are() {
            // A shape on the far side of the path leaves one run, stated by the carve as the
            // piece before the origin and the piece after it. Fused, it is the 20-long run it
            // actually is, closing past the perimeter; read as carved, a caller comparing the
            // two would take the longer half and give up the rest.
            assertThatArcsAre(
                traceReferenceSquare().fuseStretchAcrossStart(List.of(
                    new RingStretch(0, 8),
                    new RingStretch(12, 24))),
                List.of(new double[] {12, 32}));
        }

        @Test
        void stretches_between_the_two_ends_are_left_where_they_are() {
            // Only the pair reaching the two ends is fused. A stretch in the middle of the path
            // neither moves nor changes order, so the fuse costs a caller nothing it did not
            // ask for.
            assertThatArcsAre(
                traceReferenceSquare().fuseStretchAcrossStart(List.of(
                    new RingStretch(0, 4),
                    new RingStretch(8, 12),
                    new RingStretch(16, 24))),
                List.of(
                    new double[] {8, 12},
                    new double[] {16, 28}));
        }

        @Test
        void stretch_falling_short_of_the_start_is_not_fused_with_the_one_opening_it() {
            // The two nearly meet, and nearly is not meeting: the path's start is covered, so
            // there is one stretch either side of it rather than one stretch through it. Fused
            // regardless, a layout would be laid straight over the shape at the origin.
            assertThatArcsAre(
                traceReferenceSquare().fuseStretchAcrossStart(List.of(
                    new RingStretch(0, 8),
                    new RingStretch(12, 23))),
                List.of(
                    new double[] {0, 8},
                    new double[] {12, 23}));
        }

        @Test
        void stretch_opening_past_the_start_is_not_fused_with_the_one_closing_the_path() {
            // The same rule read from the other end, and worth posing separately: the pair is
            // fused for reaching the start, so a stretch reaching only the perimeter is no more
            // fusable than one reaching only the origin.
            assertThatArcsAre(
                traceReferenceSquare().fuseStretchAcrossStart(List.of(
                    new RingStretch(1, 8),
                    new RingStretch(12, 24))),
                List.of(
                    new double[] {1, 8},
                    new double[] {12, 24}));
        }

        @Test
        void a_single_stretch_is_never_fused_with_itself() {
            // The whole path uncovered is one stretch reaching both ends, and it is already the
            // run it describes. Fused with itself it would come back twice as long as the path
            // it lies on.
            assertThatArcsAre(
                traceReferenceSquare().fuseStretchAcrossStart(List.of(new RingStretch(0, 24))),
                List.of(new double[] {0, 24}));
        }
    }

    @Nested
    class PlaceSpanNearestStart {

        @Test
        void span_starts_at_the_path_start_where_the_stretch_has_room_after_it() {
            // The ordinary case: the path's start lies on the stretch with room clockwise of it,
            // so the layout opens exactly on the landmark. The stretch is one fused across the
            // start, so the landmark within it is the perimeter rather than the zero it would
            // otherwise be measured back to.
            assertThat(traceReferenceSquare()
                    .placeSpanNearestStart(new RingStretch(20, 30), SPAN_LENGTH))
                .isEqualTo(24.0);
        }

        @Test
        void span_backs_up_where_the_stretch_closes_too_soon_after_the_path_start() {
            // The path's start is on the stretch, but the stretch closes one after it and the
            // span reaches four. The start backs up to the latest the stretch allows, so the
            // landmark still falls on the span and only which part of it lands there moves.
            assertThat(traceReferenceSquare()
                    .placeSpanNearestStart(new RingStretch(20, 25), SPAN_LENGTH))
                .isEqualTo(21.0);
        }

        @Test
        void span_opens_where_the_stretch_does_where_a_shape_covers_the_path_start() {
            // A shape over the start and one unit of path clockwise of it. The span begins as
            // near the landmark as the shape allows rather than being thrown to the stretch's
            // far end - which is what makes this a clamp rather than a preference with a
            // fallback.
            assertThat(traceReferenceSquare()
                    .placeSpanNearestStart(new RingStretch(1, 18), SPAN_LENGTH))
                .isEqualTo(1.0);
        }

        @Test
        void span_ends_near_the_path_start_where_the_stretch_closes_just_behind_it() {
            // The stretch closes one short of the landmark and opens six the other side of it,
            // so the span's start is nearer at the closing end: it sits at 19 and runs to 23.
            assertThat(traceReferenceSquare()
                    .placeSpanNearestStart(new RingStretch(6, 23), SPAN_LENGTH))
                .isEqualTo(19.0);
        }

        @Test
        void span_takes_the_one_position_an_exact_fit_stretch_allows() {
            // A stretch the span exactly fills has one position, and the clamp reaches it
            // however far off the landmark lies.
            assertThat(traceReferenceSquare()
                    .placeSpanNearestStart(new RingStretch(8, 12), SPAN_LENGTH))
                .isEqualTo(8.0);
        }

        @Test
        void span_takes_the_stretchs_own_start_where_both_its_ends_are_equally_far() {
            // A stretch lying opposite the landmark: six of path from it round to where the
            // stretch opens, and six from the latest start the stretch allows back to it. The
            // tie takes the stretch's start, so such a stretch places the same way every call.
            assertThat(traceReferenceSquare()
                    .placeSpanNearestStart(new RingStretch(6, 22), SPAN_LENGTH))
                .isEqualTo(6.0);
        }

        @Test
        void span_longer_than_its_stretch_opens_where_the_stretch_does() {
            // Sizing a layout to its stretch is the caller's, so a span that outruns the one it
            // was handed is a length question asked of a placement. It opens where the stretch
            // does and overruns the far end, rather than being refused an answer it cannot give.
            assertThat(traceReferenceSquare()
                    .placeSpanNearestStart(new RingStretch(8, 10), SPAN_LENGTH))
                .isEqualTo(8.0);
        }

        @Test
        void an_empty_path_has_no_lap_to_place_within() {
            // A path that was never traced has no perimeter, and nearness the short way round is
            // measured within one. Answering with a position on a path that does not exist would
            // be worse than answering none.
            assertThatThrownBy(() -> RingPath.nothingLeftToTrace()
                    .placeSpanNearestStart(new RingStretch(0, 1), SPAN_LENGTH))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    // The path around the reference square inset by 2, anchored at its centre: the square
    // from (2,2) to (8,8), traced clockwise from (5,8) - a 24-long path whose corners fall at
    // 3, 9, 15 and 21.
    private static RingPath traceReferenceSquare() {
        return RingPath.traceInsetRing(
            buildReferenceSquare(),
            INSET_DISTANCE,
            MITER_SPIKE_LIMIT,
            SQUARE_CENTRE);
    }

    // An axis-aligned keep-out box by its two opposite corners.
    private static List<double[]> buildBox(
            double fromX,
            double fromY,
            double toX,
            double toY) {

        return List.of(
            new double[] {fromX, fromY},
            new double[] {toX, fromY},
            new double[] {toX, toY},
            new double[] {fromX, toY});
    }

    // Asserts the stretches match the expected {start, end} intervals in order, at the
    // shared slack. The points helper cannot stand in: these are parameters along a path
    // rather than points, and reading them as coordinates would make a failure say the
    // wrong thing.
    private static void assertThatArcsAre(List<RingStretch> arcs, List<double[]> expected) {

        assertThat(arcs)
            .hasSameSizeAs(expected);

        for (var i = 0; i < expected.size(); i++) {

            assertThat(new double[] {
                    arcs.get(i).startArcLength(),
                    arcs.get(i).endArcLength()})
                .containsExactly(expected.get(i), buildAssertionSlack());
        }
    }
}
