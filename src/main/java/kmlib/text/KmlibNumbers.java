package kmlib.text;

/**
 * Generic number-to-string formatters shared across the KMLib jar.
 * Sibling to {@link KmlibStrings} (which is predicates on strings);
 * this class is purely numeric formatting and depends on nothing
 * from Starsector.
 */
public final class KmlibNumbers {

    private KmlibNumbers() {
    }

    /**
     * Formats {@code value} as a signed delta with an explicit
     * leading sign: {@code "+N"} when non-negative, {@code "-N"}
     * (which {@link Integer#toString(int)} already produces) when
     * negative. Useful for delta columns and bump magnitudes where
     * the player wants the direction read at a glance instead of
     * inferring it from absence of a minus.
     */
    public static String formatDelta(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    /**
     * Float convenience overload that truncates {@code value} to
     * {@code int} before formatting. The truncation (not rounding)
     * matches caller intent for whole-step counts that happen to be
     * carried as {@code float} - a magnitude of {@code 1.999f}
     * really means "one step" rather than "two", because the only
     * way to accumulate fractional steps is via float arithmetic
     * drift, not a deliberate half-step bump.
     */
    public static String formatDelta(float value) {
        return formatDelta((int) value);
    }
}
