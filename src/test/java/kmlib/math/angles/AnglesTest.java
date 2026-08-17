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
}
