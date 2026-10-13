package kmlib.math.percentages;

/**
 * Converts between {@code [0, 1]} fractions and percents. Settings and computations carry a share as a fraction, so
 * the arithmetic reads naturally, while a reader counts in whole percents.
 */
public final class Percentages {

    private static final float PERCENT_PER_FRACTION = 100f;

    private Percentages() {
    }

    /**
     * Converts a {@code [0, 1]} fraction to the nearest whole percent: {@code 0.20f} returns {@code 20}.
     *
     * <p>Rounds rather than truncates. A float rarely holds the decimal it was written as - {@code 0.53f} is
     * {@code 0.52999997} - and a fraction computed from other figures can land just under a whole percent, so
     * truncation would put both one percent low.
     *
     * @param fraction the fraction to convert, e.g. {@code 0.20f} for 20%
     * @return the nearest whole percent
     */
    public static int computeWholePercent(float fraction) {

        return Math.round(fraction * PERCENT_PER_FRACTION);
    }
}
