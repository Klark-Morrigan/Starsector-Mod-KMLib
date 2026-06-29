package kmlib.profiling;

import java.util.List;
import java.util.Locale;

/**
 * Formats a {@link Profiler} snapshot into an aligned, human-readable table.
 *
 * <p>Pure text transform, no profiling state of its own: it takes the list of
 * {@link SectionTiming} and returns a string, so it can be unit tested directly
 * and reused by any output sink (a console command, a log line). Durations are
 * shown in milliseconds, the useful scale for frame-time work.
 */
public final class TimingReport {
    private static final double NANOS_PER_MILLI = 1_000_000.0;
    // The section-column header, also the minimum width that column is sized to.
    private static final String SECTION_HEADER = "SECTION";

    private TimingReport() {
    }

    /**
     * Renders {@code timings} as a table with one row per section: call count
     * and average / minimum / maximum / total milliseconds.
     *
     * @param timings the sections to report, e.g. {@link Profiler#snapshot()}
     * @return the formatted table, or a short notice when nothing was recorded
     */
    public static String format(List<SectionTiming> timings) {
        if (timings.isEmpty()) {
            return "No timings recorded.";
        }

        var sectionWidth = SECTION_HEADER.length();
        for (var timing : timings) {
            sectionWidth = Math.max(sectionWidth, timing.getSection().length());
        }

        var report = new StringBuilder();
        report.append(String.format(Locale.ROOT,
                "%-" + sectionWidth + "s  %8s  %10s  %10s  %10s  %11s",
                SECTION_HEADER, "COUNT", "AVG ms", "MIN ms", "MAX ms", "TOTAL ms"));
        for (var timing : timings) {
            report.append('\n');
            report.append(String.format(Locale.ROOT,
                    "%-" + sectionWidth + "s  %8d  %10.3f  %10.3f  %10.3f  %11.3f",
                    timing.getSection(), timing.getCount(),
                    toMillis(timing.getAverageNanos()), toMillis(timing.getMinNanos()),
                    toMillis(timing.getMaxNanos()), toMillis(timing.getTotalNanos())));
        }
        return report.toString();
    }

    private static double toMillis(long nanos) {
        return nanos / NANOS_PER_MILLI;
    }
}
