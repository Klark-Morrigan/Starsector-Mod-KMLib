package kmlib.math.percentages;

/**
 * Converts between {@code [0, 1]} fractions and percents. Settings and computations carry a share as a fraction, so
 * the arithmetic reads naturally, while a reader and a settings slider count in percents.
 *
 * <p>One scale for every conversion, so no caller divides by a bound that happens to be a hundred: a slider ceiling
 * doing duty as the unit silently rescales its setting the day that ceiling is narrowed.
 */
public final class Percentages {

    // Float so the float overload multiplies in float; a double operand widens it exactly.
    private static final float PERCENT_PER_FRACTION = 100f;

    private Percentages() {
    }

    /**
     * Converts a {@code [0, 1]} fraction to a percent, keeping every digit: {@code 0.125} returns {@code 12.5}.
     *
     * @param fraction the fraction to convert, e.g. {@code 0.20} for 20%
     * @return the percent
     */
    public static double convertFractionToPercent(double fraction) {

        return fraction * PERCENT_PER_FRACTION;
    }

    /**
     * Converts a {@code [0, 1]} fraction to the nearest whole percent: {@code 0.20f} returns {@code 20}.
     *
     * <p>Rounds rather than truncates. A float rarely holds the decimal it was written as - {@code 0.53f} is
     * {@code 0.52999997} - and a fraction computed from other figures can land just under a whole percent, so
     * truncation would put both one percent low.
     *
     * <p>Multiplies in float rather than widening to the double overload. Widening exposes the float's error at a
     * half: {@code 0.015f} becomes just under {@code 1.5} and rounds to 1, where the float product is {@code 1.5}.
     *
     * @param fraction the fraction to convert, e.g. {@code 0.20f} for 20%
     * @return the nearest whole percent
     */
    public static int convertFractionToWholePercent(float fraction) {

        return Math.round(fraction * PERCENT_PER_FRACTION);
    }
}
