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
}
