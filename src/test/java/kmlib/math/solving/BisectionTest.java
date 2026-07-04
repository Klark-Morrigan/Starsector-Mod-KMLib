package kmlib.math.solving;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link Bisection#findLargestPassing}: it lands on the crossing of a monotone
 * predicate to within the interval halved by the step count, returns the high end
 * when the predicate holds all the way there, degrades to the low end when zero steps
 * are allowed, and returns the low end unchanged when the predicate is already false
 * there (the caller's "nothing fits" signal).
 */
final class BisectionTest {

    @Nested
    class FindLargestPassing {
        @Test
        void findLargestPassingLandsOnTheCrossingWithinTheHalvedInterval() {
            // Holds for x <= 42 across [0, 100]; 30 halvings pin the crossing to well
            // under a thousandth of the interval.
            var crossing = Bisection.findLargestPassing(0.0, 100.0, 30, x -> x <= 42.0);

            assertThat(crossing).isCloseTo(42.0, within(1e-3));
        }

        @Test
        void findLargestPassingReturnsTheHighEndWhenItHoldsThere() {
            // The predicate holds across the whole interval, so the widest value wins
            // outright with no halving.
            var largest = Bisection.findLargestPassing(0.0, 100.0, 30, x -> x <= 200.0);

            assertThat(largest).isEqualTo(100.0);
        }

        @Test
        void findLargestPassingReturnsTheLowEndWhenNoStepsAreAllowed() {
            // With the high end failing and no halving budget, the largest known-true
            // point is the low end the caller vouched for.
            var largest = Bisection.findLargestPassing(0.0, 100.0, 0, x -> x <= 42.0);

            assertThat(largest).isEqualTo(0.0);
        }

        @Test
        void findLargestPassingLeavesTheLowEndWhenThePredicateFailsThere() {
            // A predicate false even at the low end never advances, so the low end comes
            // back - the caller reads that as nothing in the interval fits.
            var largest = Bisection.findLargestPassing(10.0, 100.0, 30, x -> x <= 5.0);

            assertThat(largest).isEqualTo(10.0);
        }
    }
}
