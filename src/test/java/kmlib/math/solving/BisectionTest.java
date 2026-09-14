package kmlib.math.solving;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link Bisection#findLargestPassing}: it lands on the crossing of a monotone
 * predicate to within the interval halved by the step count, returns the high end
 * when the predicate holds all the way there, degrades to the low end when zero steps
 * are allowed, and returns the low end unchanged when the predicate is already false
 * there (the caller's "nothing fits" signal). {@link Bisection#countStepsForTolerance}
 * is pinned alongside it, since a search budgeted by precision is only as correct as the
 * count that precision converts into.
 */
final class BisectionTest {

    @Nested
    class CountStepsForTolerance {
        @Test
        void countStepsForToleranceCountsTheHalvingsThatCloseTheIntervalToTheTolerance() {
            // A 1000-wide interval halves to under 1 after 10 steps (1000/2^10 is 0.98)
            // and not after 9 (1.95), so the count is the first halving that gets there.
            var steps = Bisection.countStepsForTolerance(0.0, 1000.0, 1.0);

            assertThat(steps)
                .isEqualTo(10);
        }

        @Test
        void countStepsForToleranceCountsFewerHalvingsForACoarserTolerance() {
            // The same interval to within 100 needs only 4 halvings (1000/2^4 is 62.5),
            // which is the saving a caller buys by asking for a precision it can use
            // rather than the finest the interval allows.
            var steps = Bisection.countStepsForTolerance(0.0, 1000.0, 100.0);

            assertThat(steps)
                .isEqualTo(4);
        }

        @Test
        void countStepsForToleranceCountsNoHalvingsWhenTheIntervalIsAlreadyWithinTolerance() {
            // Any point of a 10-wide interval is already within 50 of the crossing, so
            // the search has nothing to narrow and the caller pays for no measurements.
            var steps = Bisection.countStepsForTolerance(0.0, 10.0, 50.0);

            assertThat(steps)
                .isEqualTo(0);
        }

        @Test
        void countStepsForToleranceRejectsANonPositiveTolerance() {
            // Halving closes an interval towards zero without ever reaching it, so a
            // tolerance of zero names a precision no step count delivers - caught here
            // rather than as a search that never terminates for a reason it cannot state.
            assertThatThrownBy(() -> Bisection.countStepsForTolerance(0.0, 1000.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void countStepsForToleranceRejectsAToleranceThatIsNotANumber() {
            // A tolerance that is not a number compares false against every bound, so a
            // count derived from it would come back as whatever ceil() makes of NaN rather
            // than as a precision. Rejected by the same guard, and named here because it
            // is the input a settings read can produce that a negative one cannot.
            assertThatThrownBy(() -> Bisection.countStepsForTolerance(0.0, 1000.0, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class FindLargestPassing {
        @Test
        void findLargestPassingLandsOnTheCrossingWithinTheHalvedInterval() {
            // Holds for x <= 42 across [0, 100]; 30 halvings pin the crossing to well
            // under a thousandth of the interval.
            var crossing = Bisection.findLargestPassing(0.0, 100.0, 30, x -> x <= 42.0);

            assertThat(crossing)
                .isCloseTo(42.0, within(1e-3));
        }

        @Test
        void findLargestPassingReturnsTheHighEndWhenItHoldsThere() {
            // The predicate holds across the whole interval, so the widest value wins
            // outright with no halving.
            var largest = Bisection.findLargestPassing(0.0, 100.0, 30, x -> x <= 200.0);

            assertThat(largest)
                .isEqualTo(100.0);
        }

        @Test
        void findLargestPassingReturnsTheLowEndWhenNoStepsAreAllowed() {
            // With the high end failing and no halving budget, the largest known-true
            // point is the low end the caller vouched for.
            var largest = Bisection.findLargestPassing(0.0, 100.0, 0, x -> x <= 42.0);

            assertThat(largest)
                .isEqualTo(0.0);
        }

        @Test
        void findLargestPassingLeavesTheLowEndWhenThePredicateFailsThere() {
            // A predicate false even at the low end never advances, so the low end comes
            // back - the caller reads that as nothing in the interval fits.
            var largest = Bisection.findLargestPassing(10.0, 100.0, 30, x -> x <= 5.0);

            assertThat(largest)
                .isEqualTo(10.0);
        }
    }
}
