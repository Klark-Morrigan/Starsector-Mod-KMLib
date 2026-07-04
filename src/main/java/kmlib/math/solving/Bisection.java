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
 */
public final class Bisection {

    private Bisection() {
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
    public static double findLargestPassing(double low, double high, int steps,
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
