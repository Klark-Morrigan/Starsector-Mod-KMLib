package kmlib.math.ranges;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link Ranges#clampToUnit}: a fraction already within is unchanged,
 * one below or above is pulled to the nearer end, and both ends are themselves allowed.
 *
 * <p>And of {@link Ranges#clampInto(double, double, double)}: a value within the bounds is
 * unchanged, one outside is pulled to the bound it passed, and an EMPTY range - a floor pushed past
 * its ceiling by two constraints that do not overlap - collapses to whichever end is nearer rather
 * than handing back the ceiling regardless, which would silently prefer one of the two constraints.
 *
 * <p>The integer form is held to the same answers rather than to a sample of them, since what a
 * caller reaching for it is trusting is that it decides nothing on its own. The bounds a settings
 * accessor clamps to are integers, so its own suite would otherwise be the only thing standing
 * behind the narrowing.
 */
final class RangesTest {

    @Nested
    class ClampToUnit {

        @Test
        void aFractionWithinTheUnitRangeIsLeftWhereItIs() {

            assertThat(Ranges.clampToUnit(0.4f))
                .isEqualTo(0.4f);
        }

        @Test
        void aFractionBelowTheRangeIsPulledUpToItsFloor() {

            assertThat(Ranges.clampToUnit(-0.5f))
                .isEqualTo(0f);
        }

        @Test
        void aFractionAboveTheRangeIsPulledDownToItsCeiling() {

            assertThat(Ranges.clampToUnit(1.5f))
                .isEqualTo(1f);
        }

        @Test
        void theFloorItselfIsAllowed() {

            assertThat(Ranges.clampToUnit(0f))
                .isEqualTo(0f);
        }

        @Test
        void theCeilingItselfIsAllowed() {

            assertThat(Ranges.clampToUnit(1f))
                .isEqualTo(1f);
        }
    }

    @Nested
    class ClampInto {

        @Test
        void aValueBetweenTheBoundsIsLeftWhereItIs() {

            assertThat(Ranges.clampInto(5.0, 1.0, 10.0))
                .isEqualTo(5.0);
        }

        @Test
        void aValueBelowTheFloorIsPulledUpToIt() {

            assertThat(Ranges.clampInto(-3.0, 1.0, 10.0))
                .isEqualTo(1.0);
        }

        @Test
        void aValueAboveTheCeilingIsPulledDownToIt() {

            assertThat(Ranges.clampInto(30.0, 1.0, 10.0))
                .isEqualTo(10.0);
        }

        @Test
        void eitherBoundIsItselfAllowed() {

            assertThat(Ranges.clampInto(1.0, 1.0, 10.0))
                .isEqualTo(1.0);
            assertThat(Ranges.clampInto(10.0, 1.0, 10.0))
                .isEqualTo(10.0);
        }

        @Test
        void aRangeOfNoWidthLeavesOnlyTheOneValueItAllows() {

            assertThat(Ranges.clampInto(5.0, 7.0, 7.0))
                .isEqualTo(7.0);
        }

        @Test
        void anEmptyRangeGivesTheFloorWhenTheValueIsNearerToIt() {
            // The floor has been pushed past the ceiling: 10 is wanted at least, 4 at most.
            // A value of 9 is one away from the floor and five from the ceiling.
            assertThat(Ranges.clampInto(9.0, 10.0, 4.0))
                .isEqualTo(10.0);
        }

        @Test
        void anEmptyRangeGivesTheCeilingWhenTheValueIsNearerToThat() {
            // The same impossible range, asked with a value down at the ceiling's end.
            assertThat(Ranges.clampInto(5.0, 10.0, 4.0))
                .isEqualTo(4.0);
        }

        @Test
        void anEmptyRangePrefersTheFloorWhenTheValueSitsExactlyBetween() {
            // Seven is three from each end. The tie goes to the floor, so an equidistant
            // value is answered the same way every time rather than by rounding.
            assertThat(Ranges.clampInto(7.0, 10.0, 4.0))
                .isEqualTo(10.0);
        }
    }

    @Nested
    class ClampIntoIntegers {

        @Test
        void aValueBetweenTheBoundsIsLeftWhereItIs() {

            assertThat(Ranges.clampInto(5, 1, 10))
                .isEqualTo(5);
        }

        @Test
        void aValueBelowTheFloorIsPulledUpToIt() {

            assertThat(Ranges.clampInto(-3, 1, 10))
                .isEqualTo(1);
        }

        @Test
        void aValueAboveTheCeilingIsPulledDownToIt() {

            assertThat(Ranges.clampInto(30, 1, 10))
                .isEqualTo(10);
        }

        @Test
        void eitherBoundIsItselfAllowed() {

            assertThat(Ranges.clampInto(1, 1, 10))
                .isEqualTo(1);
            assertThat(Ranges.clampInto(10, 1, 10))
                .isEqualTo(10);
        }

        @Test
        void aRangeOfNoWidthLeavesOnlyTheOneValueItAllows() {

            assertThat(Ranges.clampInto(5, 7, 7))
                .isEqualTo(7);
        }

        @Test
        void anEmptyRangeGivesTheNearerOfItsTwoEnds() {

            assertThat(Ranges.clampInto(9, 10, 4))
                .isEqualTo(10);
            assertThat(Ranges.clampInto(5, 10, 4))
                .isEqualTo(4);
        }

        @Test
        void anEmptyRangePrefersTheFloorWhenTheValueSitsExactlyBetween() {

            assertThat(Ranges.clampInto(7, 10, 4))
                .isEqualTo(10);
        }

        @Test
        void theWidestBoundsAnIntegerHasAreAnsweredWithoutOverflowing() {
            // The distances an empty range compares are what would overflow if the rule were
            // re-copied in integer arithmetic; answered through the double form, they cannot.
            assertThat(Ranges.clampInto(0, Integer.MIN_VALUE, Integer.MAX_VALUE))
                .isEqualTo(0);
            assertThat(Ranges.clampInto(0, Integer.MAX_VALUE, Integer.MIN_VALUE))
                .isEqualTo(Integer.MAX_VALUE);
        }
    }
}
