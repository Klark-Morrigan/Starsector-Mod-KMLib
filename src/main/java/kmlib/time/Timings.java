package kmlib.time;

import java.util.Locale;

/**
 * Duration conversion and formatting.
 *
 * <p>The single home for the nanosecond divisors and the "{@code 1.234ms}"
 * format, for anything reading {@link System#nanoTime()}: a measured span, a
 * diagnostic trace, an animation phased off the clock. All three convert the same
 * way, so the divisors are written once.
 *
 * <p>Its own package rather than a profiler's, because reading a clock is not
 * profiling - the profiler is one caller of this and not its owner. Pure maths
 * and text, with no state and no Starsector dependency.
 */
public final class Timings {

    private static final double NANOS_PER_MICROSECOND = 1_000.0;
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    /**
     * How a duration in milliseconds is written wherever one is: the decimals
     * alone, so a caller writing a column of them adds no unit and one writing a
     * sentence adds {@link #MILLIS_UNIT}.
     *
     * <p>Published because the precision is the fact worth sharing rather than the
     * whole format: two surfaces writing one measurement to different numbers of
     * decimals read as two measurements, and the log and the report are exactly
     * two surfaces onto the same call.
     */
    public static final String MILLIS_FORMAT = "%.3f";

    /** What a duration in milliseconds is called, where a caller writes the unit. */
    public static final String MILLIS_UNIT = "ms";

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
     * millisecond reading uses - so a span stated in the unit a short duration is
     * reported in reaches a clock counting in nanoseconds without a second spelling of
     * the conversion.
     *
     * <p>Truncated rather than rounded, since the result is a count of nanoseconds and
     * the fraction of one that is dropped is below what any clock here resolves.
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
     * Formats a nanosecond duration as milliseconds with three decimals and a
     * trailing unit, e.g. {@code "1.234ms"}. {@link Locale#ROOT} is forced so the
     * decimal separator is a dot regardless of the JVM's default locale - a fixed
     * technical format, not locale-sensitive prose.
     *
     * <p>Three rather than two, because the durations written through here are
     * read beside the ones a profiling report writes in its columns, and the same
     * measurement at two precisions reads as two: a call is logged as it closes
     * and reported afterwards, and a bound stated here is printed on the very line
     * the report gives the call that broke it. Three decimals is also what keeps a
     * span under ten microseconds from reading as zero.
     */
    public static String formatMillis(long nanos) {
        return String.format(Locale.ROOT, MILLIS_FORMAT + MILLIS_UNIT, convertNanosToMillis(nanos));
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
