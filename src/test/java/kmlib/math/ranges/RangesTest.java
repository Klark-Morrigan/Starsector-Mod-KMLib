package kmlib.math.ranges;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link Ranges#clampToUnit}: a fraction already within is unchanged,
 * one below or above is pulled to the nearer end, and both ends are themselves allowed.
 *
 * <p>And of {@link Ranges#clampInto}: a value within the bounds is unchanged, one outside is
 * pulled to the bound it passed, and an EMPTY range - a floor pushed past its ceiling by two
 * constraints that do not overlap - collapses to whichever end is nearer rather than handing
 * back the ceiling regardless, which would silently prefer one of the two constraints.
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
}
