package kmlib.math.angles;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link Angles#normalise}: a direction already in the first turn is
 * unchanged, one a turn either side lands on the same place, the open end wraps to zero
 * rather than to a full turn, and a direction many turns out still lands in range.
 *
 * <p>And of {@link Angles#measureGap}: two directions a hair apart are a hair apart whichever
 * order they are given in, a pair either side of zero measure across it rather than the long
 * way round, and nothing exceeds a half turn.
 *
 * <p>And of {@link Angles#placeAfter}: a direction at the origin stays there, one just before
 * it moves a whole turn on rather than staying behind, and the result is always within one
 * turn of the origin - which is what makes it comparable to an interval that runs past a turn.
 *
 * <p>And of {@link Angles#measureSignedTurn}: a small turn keeps its sign either way, the
 * long way round becomes the short way with the sign flipped, and a half turn resolves
 * positive so that the range is closed at one end and open at the other.
 *
 * <p>And of {@link Angles#foldToHalfTurn}: a line already within a quarter turn of level is
 * untouched at both ends of that range, one past it folds by a half turn rather than a whole
 * one, and a direction and its opposite fold to the same line.
 *
 * <p>And of {@link Angles#measureUndirectedGap}: two lines a hair either side of vertical are
 * a hair apart rather than nearly a half turn, a line is no distance from itself or from its
 * own opposite, and nothing exceeds a quarter turn.
 *
 * <p>And of {@link Angles#intersectSpans}: two overlapping spans leave what they share, spans
 * built a turn apart still meet, one long span can meet another at both its ends, and spans
 * that only touch or that miss leave nothing.
 *
 * <p>And of {@link Angles#mergeSpans}: overlapping spans become one run, a span wholly inside
 * another leaves the outer one untouched, spans that only touch still join, disjoint ones
 * stay apart, and a run gathered around the far side of the window is not split across its
 * edge.
 */
final class AnglesTest {

    // Half a degree, comfortably wider than any rounding these do and far narrower than any
    // distinction they are asked to draw.
    private static final double SLACK = 0.0087;

    @Nested
    class Normalise {

        @Test
        void a_direction_in_the_first_turn_is_left_where_it_is() {
            assertThat(Angles.normalise(1.0))
                .isCloseTo(1.0, within());
        }

        @Test
        void a_direction_a_turn_past_the_first_lands_back_in_it() {
            // 1 + 2pi is the same direction as 1.
            assertThat(Angles.normalise(1.0 + 2 * Math.PI))
                .isCloseTo(1.0, within());
        }

        @Test
        void a_negative_direction_comes_back_round_the_top() {
            // A quarter turn short of zero is three quarters of a turn past it.
            assertThat(Angles.normalise(-Math.PI / 2))
                .isCloseTo(4.712388, within());
        }

        @Test
        void a_whole_turn_is_zero_rather_than_a_turn() {
            // The range is closed at zero and open at a turn, so a caller comparing against
            // its own zero never sees two spellings of the same direction.
            assertThat(Angles.normalise(2 * Math.PI))
                .isCloseTo(0.0, within());
        }

        @Test
        void a_direction_many_turns_out_still_lands_in_range() {
            assertThat(Angles.normalise(1.0 - 10 * Math.PI))
                .isCloseTo(1.0, within());
        }
    }

    @Nested
    class MeasureGap {

        @Test
        void two_directions_a_hair_apart_are_a_hair_apart() {
            assertThat(Angles.measureGap(1.0, 1.25))
                .isCloseTo(0.25, within());
        }

        @Test
        void the_gap_reads_the_same_whichever_order_the_two_are_given_in() {
            assertThat(Angles.measureGap(1.25, 1.0))
                .isCloseTo(0.25, within());
        }

        @Test
        void a_pair_either_side_of_zero_measure_across_it_rather_than_round() {
            // A tenth before a turn and a tenth after zero are a fifth apart, not a turn less.
            assertThat(Angles.measureGap(2 * Math.PI - 0.1, 0.1))
                .isCloseTo(0.2, within());
        }

        @Test
        void opposite_directions_are_a_half_turn_apart_which_is_the_most_there_is() {
            assertThat(Angles.measureGap(0.0, Math.PI))
                .isCloseTo(3.141593, within());
        }

        @Test
        void more_than_a_half_turn_round_is_reported_as_the_shorter_way_back() {
            // Three quarters of a turn forward is a quarter of a turn back.
            assertThat(Angles.measureGap(0.0, 3 * Math.PI / 2))
                .isCloseTo(1.570796, within());
        }
    }

    @Nested
    class PlaceAfter {

        @Test
        void a_direction_at_the_origin_stays_at_the_origin() {
            assertThat(Angles.placeAfter(1.0, 1.0))
                .isCloseTo(1.0, within());
        }

        @Test
        void a_direction_just_past_the_origin_stays_just_past_it() {
            assertThat(Angles.placeAfter(1.5, 1.0))
                .isCloseTo(1.5, within());
        }

        @Test
        void a_direction_just_before_the_origin_moves_a_whole_turn_on() {
            // 0.5 sits before an origin of 1, so it is placed at 0.5 + 2pi - which is what
            // lets an interval running from 1 to 1.2 past a turn be tested against it.
            assertThat(Angles.placeAfter(0.5, 1.0))
                .isCloseTo(6.783185, within());
        }

        @Test
        void a_direction_a_turn_out_is_placed_as_though_it_never_was() {
            assertThat(Angles.placeAfter(1.5 + 2 * Math.PI, 1.0))
                .isCloseTo(1.5, within());
        }

        @Test
        void an_origin_past_a_turn_keeps_the_result_beside_it_rather_than_in_the_first_turn() {
            // The point of the operation: the answer is in the origin's turn, not in turn one.
            assertThat(Angles.placeAfter(0.5, 7.0))
                .isCloseTo(13.066371, within());
        }
    }

    @Nested
    class MeasureSignedTurn {

        @Test
        void a_small_turn_forward_keeps_its_sign() {
            assertThat(Angles.measureSignedTurn(0.5))
                .isCloseTo(0.5, within());
        }

        @Test
        void a_small_turn_back_keeps_its_sign() {
            assertThat(Angles.measureSignedTurn(-0.5))
                .isCloseTo(-0.5, within());
        }

        @Test
        void the_long_way_forward_becomes_the_short_way_back() {
            // Three quarters of a turn anticlockwise is a quarter turn clockwise.
            assertThat(Angles.measureSignedTurn(3 * Math.PI / 2))
                .isCloseTo(-1.570796, within());
        }

        @Test
        void a_half_turn_resolves_forward_so_the_range_is_closed_at_one_end() {
            assertThat(Angles.measureSignedTurn(Math.PI))
                .isCloseTo(3.141593, within());
        }

        @Test
        void a_half_turn_back_resolves_forward_onto_the_same_answer() {
            assertThat(Angles.measureSignedTurn(-Math.PI))
                .isCloseTo(3.141593, within());
        }
    }

    @Nested
    class FoldToHalfTurn {

        @Test
        void a_line_near_level_is_left_where_it_is() {
            assertThat(Angles.foldToHalfTurn(0.3))
                .isCloseTo(0.3, within());
        }

        @Test
        void a_line_at_a_quarter_turn_is_left_there_rather_than_folded_to_its_negative() {
            // Both ends of the range are directions in their own right, so a caller that
            // asks with one does not get the other back.
            assertThat(Angles.foldToHalfTurn(Math.PI / 2))
                .isCloseTo(1.570796, within());
        }

        @Test
        void a_line_at_minus_a_quarter_turn_is_left_there_too() {
            assertThat(Angles.foldToHalfTurn(-Math.PI / 2))
                .isCloseTo(-1.570796, within());
        }

        @Test
        void a_line_past_the_range_folds_by_a_half_turn_rather_than_a_whole_one() {
            // Two radians is past a quarter turn, so it folds to 2 - pi.
            assertThat(Angles.foldToHalfTurn(2.0))
                .isCloseTo(-1.141593, within());
        }

        @Test
        void a_direction_and_its_opposite_fold_to_the_same_line() {
            assertThat(Angles.foldToHalfTurn(0.3 + Math.PI))
                .isCloseTo(0.3, within());
        }

        @Test
        void a_line_several_half_turns_out_still_folds_into_range() {
            assertThat(Angles.foldToHalfTurn(0.3 - 3 * Math.PI))
                .isCloseTo(0.3, within());
        }
    }

    @Nested
    class MeasureUndirectedGap {

        @Test
        void a_line_is_no_distance_from_itself() {
            assertThat(Angles.measureUndirectedGap(0.4, 0.4))
                .isCloseTo(0.0, within());
        }

        @Test
        void a_line_is_no_distance_from_its_own_opposite() {
            assertThat(Angles.measureUndirectedGap(0.4, 0.4 + Math.PI))
                .isCloseTo(0.0, within());
        }

        @Test
        void two_lines_a_hair_apart_are_a_hair_apart() {
            assertThat(Angles.measureUndirectedGap(0.4, 0.6))
                .isCloseTo(0.2, within());
        }

        @Test
        void two_lines_either_side_of_vertical_are_near_parallel_rather_than_opposite() {
            // A tenth short of a quarter turn and a tenth past it are a fifth apart as
            // lines, though as directions they are nearly a half turn apart.
            assertThat(Angles.measureUndirectedGap(Math.PI / 2 - 0.1, Math.PI / 2 + 0.1))
                .isCloseTo(0.2, within());
        }

        @Test
        void perpendicular_lines_are_a_quarter_turn_apart_which_is_the_most_there_is() {
            assertThat(Angles.measureUndirectedGap(0.0, Math.PI / 2))
                .isCloseTo(1.570796, within());
        }
    }

    @Nested
    class IntersectSpans {

        @Test
        void two_overlapping_spans_leave_the_stretch_they_share() {
            // [1, 3] against [2, 5] shares [2, 3].
            var shared = Angles.intersectSpans(
                List.of(new double[] {1.0, 2.0}),
                List.of(new double[] {2.0, 3.0}));

            assertThat(shared)
                .hasSize(1);

            assertThat(shared.get(0)[0])
                .isCloseTo(2.0, within());
            assertThat(shared.get(0)[1])
                .isCloseTo(1.0, within());
        }

        @Test
        void spans_built_a_turn_apart_still_meet() {
            // The same two spans as above, the second built a turn further round. Compared
            // as raw numbers they miss entirely.
            var shared = Angles.intersectSpans(
                List.of(new double[] {1.0, 2.0}),
                List.of(new double[] {2.0 + 2 * Math.PI, 3.0}));

            assertThat(shared)
                .hasSize(1);

            assertThat(shared.get(0)[0])
                .isCloseTo(2.0, within());
            assertThat(shared.get(0)[1])
                .isCloseTo(1.0, within());
        }

        @Test
        void the_answer_is_given_in_the_turn_the_first_set_was_built_in() {

            var shared = Angles.intersectSpans(
                List.of(new double[] {1.0 + 2 * Math.PI, 2.0}),
                List.of(new double[] {2.0, 3.0}));

            assertThat(shared)
                .hasSize(1);
            assertThat(shared.get(0)[0])
                .isCloseTo(8.283185, within());
        }

        @Test
        void a_span_most_of_a_turn_long_can_meet_another_at_both_of_its_ends() {
            // A span of 6 radians starting at 0 wraps nearly all the way round, so a short
            // span at 5.8 meets it both before it wraps and after.
            var shared = Angles.intersectSpans(
                List.of(new double[] {0.0, 6.0}),
                List.of(new double[] {5.8, 0.6}));

            assertThat(shared)
                .hasSize(2);
        }

        @Test
        void spans_that_only_touch_share_nothing() {

            assertThat(Angles.intersectSpans(
                    List.of(new double[] {1.0, 1.0}),
                    List.of(new double[] {2.0, 1.0})))
                .isEmpty();
        }

        @Test
        void spans_that_miss_share_nothing() {

            assertThat(Angles.intersectSpans(
                    List.of(new double[] {1.0, 0.5}),
                    List.of(new double[] {3.0, 0.5})))
                .isEmpty();
        }

        @Test
        void nothing_shares_nothing() {
            assertThat(Angles.intersectSpans(List.of(), List.of(new double[] {1.0, 1.0})))
                .isEmpty();
        }
    }

    @Nested
    class MergeSpans {

        @Test
        void two_overlapping_spans_become_one_run() {
            // [1, 3] and [2, 5] make [1, 5].
            var merged = Angles.mergeSpans(
                List.of(new double[] {1.0, 2.0}, new double[] {2.0, 3.0}),
                2.0);

            assertThat(merged)
                .hasSize(1);
            assertThat(merged.get(0)[1])
                .isCloseTo(4.0, within());
        }

        @Test
        void a_span_wholly_inside_another_leaves_the_outer_one_as_it_was() {

            var merged = Angles.mergeSpans(
                List.of(new double[] {1.0, 4.0}, new double[] {2.0, 1.0}),
                2.0);

            assertThat(merged)
                .hasSize(1);
            assertThat(merged.get(0)[1])
                .isCloseTo(4.0, within());
        }

        @Test
        void spans_that_only_touch_still_join() {
            // Ends meeting exactly is one run, not two - a wall handing on to the next
            // leaves no circle between them.
            var merged = Angles.mergeSpans(
                List.of(new double[] {1.0, 1.0}, new double[] {2.0, 1.0}),
                2.0);

            assertThat(merged)
                .hasSize(1);
            assertThat(merged.get(0)[1])
                .isCloseTo(2.0, within());
        }

        @Test
        void disjoint_spans_stay_apart() {

            var merged = Angles.mergeSpans(
                List.of(new double[] {1.0, 0.5}, new double[] {3.0, 0.5}),
                2.0);

            assertThat(merged)
                .hasSize(2);
        }

        @Test
        void the_runs_come_back_in_ascending_order_whatever_order_they_went_in() {

            var merged = Angles.mergeSpans(
                List.of(new double[] {3.0, 0.5}, new double[] {1.0, 0.5}),
                2.0);

            assertThat(merged)
                .hasSize(2);

            assertThat(merged.get(0)[0])
                .isCloseTo(1.0, within());
            assertThat(merged.get(1)[0])
                .isCloseTo(3.0, within());
        }

        @Test
        void spans_built_a_turn_apart_are_gathered_into_one_run() {
            // The second is the first's neighbour, written a turn further round. Sorted as
            // raw numbers they are a turn apart and would never be joined.
            var merged = Angles.mergeSpans(
                List.of(new double[] {1.0, 1.0}, new double[] {2.0 + 2 * Math.PI, 1.0}),
                2.0);

            assertThat(merged)
                .hasSize(1);
            assertThat(merged.get(0)[1])
                .isCloseTo(2.0, within());
        }

        @Test
        void a_run_gathered_around_the_window_edge_is_not_split_across_it() {
            // Two spans either side of zero, gathered about zero: read in the first turn
            // they sit at opposite ends and come back as two, but the window is centred on
            // what they gather around, so they are one.
            var merged = Angles.mergeSpans(
                List.of(new double[] {2 * Math.PI - 0.5, 0.5}, new double[] {0.0, 0.5}),
                0.0);

            assertThat(merged)
                .hasSize(1);
            assertThat(merged.get(0)[1])
                .isCloseTo(1.0, within());
        }

        @Test
        void nothing_merges_to_nothing() {
            assertThat(Angles.mergeSpans(List.of(), 0.0))
                .isEmpty();
        }
    }

    private static org.assertj.core.data.Offset<Double> within() {
        return org.assertj.core.data.Offset.offset(SLACK);
    }
}
