package kmlib.text;

import java.util.Locale;

/**
 * Generic number-to-string formatters shared across the KMLib jar.
 * Sibling to {@link KmlibStrings} (which is predicates on strings);
 * this class is purely numeric formatting and depends on nothing
 * from Starsector.
 */
public final class KmlibNumbers {

    // "%,d" inserts the locale's grouping separator every three
    // digits. The separator only appears once a value reaches four
    // figures, so a smaller value renders bare and grouping shows
    // only when it is warranted.
    private static final String GROUPED_INTEGER = "%,d";

    private KmlibNumbers() {
    }

    /**
     * Formats {@code value} in compact scientific notation, e.g.
     * {@code 1523.4 -> "1.5e3"}, {@code 0.05 -> "5.0e-2"}. The
     * mantissa carries one decimal; the exponent drops the
     * {@code "%e"} default's explicit {@code '+'} and zero padding
     * ({@code "1.5e+03"} becomes {@code "1.5e3"}) so the result reads
     * like a catalogued magnitude rather than a raw printf token.
     *
     * <p>{@link Locale#ROOT} is forced so the decimal separator is a
     * dot regardless of the JVM's default locale - the notation is a
     * fixed technical format, not locale-sensitive prose.
     */
    public static String formatScientific(double value) {
        // "%.1e" yields a mantissa and a signed, zero-padded exponent
        // (e.g. "1.5e+03"); re-parsing the exponent strips the sign and
        // padding so it prints as the bare integer "3".
        String formatted = String.format(Locale.ROOT, "%.1e", value);
        var exponentMarker = formatted.indexOf('e');
        var mantissa = formatted.substring(0, exponentMarker);
        var exponent = Integer.parseInt(formatted.substring(exponentMarker + 1));
        return mantissa + "e" + exponent;
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

    /**
     * Formats {@code value} as a whole number grouped by thousands,
     * e.g. {@code 1234567 -> "1,234,567"}, so a large tally reads at
     * a glance.
     *
     * <p>{@link Locale#ROOT} is forced so the separator is a comma
     * regardless of the JVM's default locale - the grouping is a
     * fixed presentation style matching the English copy the KM*
     * mods are written in, not locale-sensitive prose.
     */
    public static String formatGroupedInteger(int value) {
        return String.format(Locale.ROOT, GROUPED_INTEGER, value);
    }
}
