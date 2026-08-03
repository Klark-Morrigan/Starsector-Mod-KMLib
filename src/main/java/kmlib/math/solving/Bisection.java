package kmlib.math.solving;

import java.util.function.DoublePredicate;

/**
 * Bisection search over a monotone predicate on a real interval: the largest value
 * at which a condition that holds low and fails high still holds.
 *
 * <p>The numeric complement to a closed-form solve for the "grow a value until it
 * hits a wall" shape - fit the widest label band a region holds, the largest font a
 * box takes, the longest arc that clears an obstacle - where the feasibility test is
 * available but its crossing point is not. Working on a {@link DoublePredicate} keeps
 * it blind to what the value means, so any monotone "does this still fit?" question
 * reuses the same halving.
 *
 * <p>The search is budgeted in steps, but a caller usually knows a precision rather
 * than a count: {@link #countStepsForTolerance} converts the one into the other, so how
 * fine a search runs can be stated where it means something and the step count follows
 * from it.
 */
public final class Bisection {

    // Natural log of 2, the divisor that turns a natural log into a base-2 one. Named
    // because a bare Math.log(2.0) in the step arithmetic reads as a magic operand.
    private static final double LOG_OF_TWO = Math.log(2.0);

    private Bisection() {
    }

    /**
     * How many halvings pin the crossing in {@code [low, high]} to within
     * {@code tolerance} - the step count {@link #findLargestPassing} needs to reach a
     * precision stated in the searched value's own units.
     *
     * <p>Each halving doubles the precision, so what is left after {@code n} steps is
     * {@code (high - low) / 2^n} and the count is the smallest {@code n} that brings
     * that under the tolerance. Stating a search by the precision it must reach rather
     * than by a fixed step count is what keeps its cost honest as the interval moves: a
     * fixed count over a narrower interval spends the same steps to buy resolution
     * nobody asked for, and over a wider one silently stops short.
     *
     * @param low       the low end of the search
     * @param high      the high end of the search
     * @param tolerance how far from the true crossing the result may land, in the same
     *                  units as the interval; must be positive
     * @return the halving count, zero when the interval is already within tolerance
     * @throws IllegalArgumentException when {@code tolerance} is not positive - a
     *                                  tolerance of zero or less names no reachable
     *                                  precision, since halving never closes an interval
     */
    public static int countStepsForTolerance(double low, double high, double tolerance) {

        if (!(tolerance > 0.0)) {
            throw new IllegalArgumentException(
                "Cannot bisect to a tolerance of " + tolerance);
        }
        var range = high - low;
        if (range <= tolerance) {
            return 0;
        }
        return (int) Math.ceil(Math.log(range / tolerance) / LOG_OF_TWO);
    }

    /**
     * The largest value in {@code [low, high]} at which {@code holds} is still true,
     * to within {@code steps} halvings of the interval.
     *
     * <p>Assumes {@code holds} is monotone across the interval - true from {@code low}
     * up to some crossing, false beyond it - which is what lets a single crossing be
     * found by halving. Returns {@code high} directly when the condition holds all the
     * way there; otherwise it narrows the gap between the largest known-true point and
     * the smallest known-false point, returning the known-true end. The caller must
     * have established that {@code holds} is true at {@code low}: this never verifies
     * it, so a predicate already false at {@code low} yields {@code low} unchanged,
     * which the caller is expected to treat as "nothing fits".
     *
     * @param low   the low end of the search, where the predicate is assumed to hold
     * @param high  the high end of the search
     * @param steps how many times to halve the interval - each step doubles the
     *              precision, so the result lands within {@code (high - low) / 2^steps}
     *              of the true crossing
     * @param holds the monotone feasibility test
     * @return the largest value found at which {@code holds} is true
     */
    public static double findLargestPassing(
            double low,
            double high,
            int steps,
            DoublePredicate holds) {

        if (holds.test(high)) {
            return high;
        }
        for (var step = 0; step < steps; step++) {
            var mid = (low + high) / 2.0;
            if (holds.test(mid)) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return low;
    }
}
