package kmlib.time;

import java.util.Locale;

/**
 * Duration conversion and formatting.
 *
 * <p>The single home for the nanosecond divisors and the "{@code 1.23ms}" format,
 * for anything reading {@link System#nanoTime()}: a measured span, a diagnostic
 * trace, an animation phased off the clock. All three convert the same way, so
 * the divisors are written once.
 *
 * <p>Its own package rather than a profiler's, because reading a clock is not
 * profiling - the profiler is one caller of this and not its owner. Pure maths
 * and text, with no state and no Starsector dependency.
 */
public final class Timings {

    private static final double NANOS_PER_MICROSECOND = 1_000.0;
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private Timings() {
    }

    /**
     * Converts a nanosecond duration to microseconds - the one place the
     * nanosecond-to-microsecond divisor lives.
     */
    public static double convertNanosToMicros(long nanos) {
        return nanos / NANOS_PER_MICROSECOND;
    }

    /**
     * Converts a nanosecond duration to milliseconds - the one place the
     * nanosecond-to-millisecond divisor lives.
     */
    public static double convertNanosToMillis(long nanos) {
        return nanos / NANOS_PER_MILLISECOND;
    }

    /**
     * Converts a nanosecond duration to seconds - the one place the
     * nanosecond-to-second divisor lives.
     *
     * <p>Milliseconds suit a duration being reported; seconds suit one being fed to
     * maths, where a per-second rate or period is the natural unit to state a knob in.
     */
    public static double convertNanosToSeconds(long nanos) {
        return nanos / NANOS_PER_SECOND;
    }

    /**
     * Converts a duration in milliseconds to nanoseconds, against the same divisor the
     * reading above uses - so a bound stated in the unit frame budgets are talked about
     * in reaches a clock counting in nanoseconds without a second spelling of the
     * conversion.
     *
     * <p>Truncated for the reason the seconds conversion below is.
     */
    public static long convertMillisToNanos(double millis) {
        return (long) (millis * NANOS_PER_MILLISECOND);
    }

    /**
     * Converts a duration in seconds to nanoseconds, against the same divisor the
     * reading above uses - so a span stated in the unit a rate or a period is written
     * in reaches a clock counting in nanoseconds without a second spelling of the
     * conversion.
     *
     * <p>Truncated rather than rounded, since the result is a count of nanoseconds and
     * the fraction of one that is dropped is below what any clock here resolves.
     */
    public static long convertSecondsToNanos(double seconds) {
        return (long) (seconds * NANOS_PER_SECOND);
    }

    /**
     * Formats a nanosecond duration as milliseconds with two decimals and a
     * trailing unit, e.g. {@code "1.23ms"}. {@link Locale#ROOT} is forced so the
     * decimal separator is a dot regardless of the JVM's default locale - a fixed
     * technical format, not locale-sensitive prose.
     */
    public static String formatMillis(long nanos) {
        return String.format(Locale.ROOT, "%.2fms", convertNanosToMillis(nanos));
    }

    /**
     * Formats a nanosecond duration as microseconds with one decimal and a trailing
     * unit, e.g. {@code "123.4us"}.
     *
     * <p>For a span short enough that milliseconds would round it away: at two decimals
     * anything under five microseconds reports as {@code "0.00ms"}, which compares
     * against nothing. Reported in plain {@code us} rather than the SI symbol, since a
     * log line carries no encoding with it and a mangled prefix reads as a different
     * unit rather than as a mangled one.
     */
    public static String formatMicros(long nanos) {
        return String.format(Locale.ROOT, "%.1fus", convertNanosToMicros(nanos));
    }
}
