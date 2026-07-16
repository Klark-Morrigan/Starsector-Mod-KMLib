package kmlib.profiling;

import java.util.Locale;

/**
 * Duration conversion and formatting for timing output.
 *
 * <p>The single home for the nanosecond-to-millisecond divisor and the
 * "{@code 1.23ms}" format, shared by {@link TimingReport} (the profiler's table)
 * and any diagnostic trace that times a span with {@link System#nanoTime()}. Kept
 * beside {@link Profiler} because it is a timing concern, not general number
 * formatting; pure math and text, with no profiling state and no Starsector
 * dependency.
 */
public final class Timings {
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;

    private Timings() {
    }

    /**
     * Converts a nanosecond duration to milliseconds - the one place the
     * nanosecond-to-millisecond divisor lives.
     */
    public static double convertNanosToMillis(long nanos) {
        return nanos / NANOS_PER_MILLISECOND;
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
}
