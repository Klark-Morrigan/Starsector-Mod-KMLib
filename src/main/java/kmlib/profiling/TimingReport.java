package kmlib.profiling;

import java.util.List;
import java.util.Locale;

/**
 * Formats a {@link Profiler} snapshot into an aligned, human-readable table.
 *
 * <p>Pure text transform, no profiling state of its own: it takes the tree of
 * {@link ProfileNode} and returns a string, so it is reusable by any output
 * sink (a console command, a log line). Durations are shown in milliseconds
 * (via {@link Timings#convertNanosToMillis}), the useful scale for frame-time
 * work.
 *
 * <p>Rows are indented under the section they ran inside, so what a row is made
 * of sits under it. Beside the inclusive total is self time, which is what the
 * section spent outside its children - the two columns together are what say
 * whether a slow row is slow itself or slow because of something below it.
 */
public final class TimingReport {

    // The section-column header, also the minimum width that column is sized to.
    private static final String SECTION_HEADER = "SECTION";

    // Two spaces per level of nesting: enough for the eye to follow a row to
    // its parent, narrow enough that a deep tree still fits a console line.
    private static final int INDENT_SPACES_PER_DEPTH = 2;

    private TimingReport() {
    }

    /**
     * Renders {@code roots} as a table with one row per section per parent,
     * children indented under it: call count and average / minimum / maximum /
     * self / total milliseconds.
     *
     * @param roots the section tree to report, e.g. {@link Profiler#snapshot()}
     * @return the formatted table, or a short notice when nothing was recorded
     */
    public static String format(List<ProfileNode> roots) {

        if (roots.isEmpty()) {
            return "No timings recorded.";
        }

        var sectionWidth = Math.max(SECTION_HEADER.length(), measureSectionWidth(roots, 0));
        var report = new StringBuilder();
        report.append(String.format(
            Locale.ROOT,
            "%-" + sectionWidth + "s  %8s  %10s  %10s  %10s  %10s  %11s",
            SECTION_HEADER,
            "COUNT",
            "AVG ms",
            "MIN ms",
            "MAX ms",
            "SELF ms",
            "TOTAL ms"));
        appendRows(report, roots, 0, sectionWidth);
        return report.toString();
    }

    // The widest name once indentation is counted, so the numeric columns of a
    // deep row line up with those of a shallow one.
    private static int measureSectionWidth(List<ProfileNode> nodes, int depth) {

        var width = 0;

        for (var node : nodes) {

            width = Math.max(width, indentSectionName(node, depth).length());
            width = Math.max(width, measureSectionWidth(node.getChildren(), depth + 1));
        }
        return width;
    }

    private static void appendRows(
            StringBuilder report,
            List<ProfileNode> nodes,
            int depth,
            int sectionWidth) {

        for (var node : nodes) {

            report.append('\n');
            report.append(String.format(
                Locale.ROOT,
                "%-" + sectionWidth + "s  %8d  %10.3f  %10.3f  %10.3f  %10.3f  %11.3f",
                indentSectionName(node, depth),
                node.getCount(),
                Timings.convertNanosToMillis(node.getAverageNanos()),
                Timings.convertNanosToMillis(node.getMinNanos()),
                Timings.convertNanosToMillis(node.getMaxNanos()),
                Timings.convertNanosToMillis(node.getSelfNanos()),
                Timings.convertNanosToMillis(node.getTotalNanos())));
            // Depth-first, so a child is printed under the row it ran inside
            // rather than after everything at its own level.
            appendRows(report, node.getChildren(), depth + 1, sectionWidth);
        }
    }

    private static String indentSectionName(ProfileNode node, int depth) {
        return " ".repeat(depth * INDENT_SPACES_PER_DEPTH) + node.getSection().getName();
    }
}
