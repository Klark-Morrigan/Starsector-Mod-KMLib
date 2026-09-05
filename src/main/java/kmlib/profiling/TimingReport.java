package kmlib.profiling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToLongFunction;

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
 *
 * <p>Every counter anything in the tree touched adds a group of columns: what
 * the row counted in all, the spread of one call's worth, and what one item
 * cost it. A duration is judged against the work it covered, so the two are
 * read on one line rather than in a table and a log.
 */
public final class TimingReport {

    private static final String NO_TIMINGS_NOTICE = "No timings recorded.";

    private static final String SECTION_HEADER = "SECTION";
    private static final String COUNT_HEADER = "COUNT";
    private static final String AVERAGE_HEADER = "AVG ms";
    private static final String MINIMUM_HEADER = "MIN ms";
    private static final String MAXIMUM_HEADER = "MAX ms";
    private static final String SELF_HEADER = "SELF ms";
    private static final String TOTAL_HEADER = "TOTAL ms";

    // Suffixed onto the counter's own name, so a reader can tell which group a
    // spread belongs to when several counters are in the table at once.
    private static final String COUNTER_MINIMUM_SUFFIX = " MIN";
    private static final String COUNTER_MAXIMUM_SUFFIX = " MAX";
    private static final String COUNTER_PER_ITEM_SUFFIX = " us/ea";

    // Two spaces per level of nesting: enough for the eye to follow a row to
    // its parent, narrow enough that a deep tree still fits a console line.
    private static final int INDENT_SPACES_PER_DEPTH = 2;

    private static final String COLUMN_GAP = "  ";

    // Floors, not fixed widths: a column is widened by anything that does not
    // fit, and these keep the timing columns from closing up around small
    // numbers, where a table that reflows between two captures cannot be
    // compared against the one before it by eye.
    private static final int SECTION_COLUMN_FLOOR = 0;
    private static final int CALL_COUNT_COLUMN_FLOOR = 8;
    private static final int DURATION_COLUMN_FLOOR = 10;
    private static final int TOTAL_COLUMN_FLOOR = 11;

    // A counter column is as wide as its header and its widest number: what a
    // counter is called and how big it gets are the caller's, not this table's.
    private static final int COUNTER_COLUMN_FLOOR = 0;

    // Left blank where a subtree never touched a counter. A zero would say the
    // row counted none of it, which is a different fact and the one worth
    // seeing; a table of zeroes hides the rows that count.
    private static final String ABSENT_CELL = "";

    private static final String MILLIS_FORMAT = "%.3f";
    private static final String PER_ITEM_FORMAT = "%.1f";

    private TimingReport() {
    }

    /**
     * Renders {@code roots} as a table with one row per section per parent,
     * children indented under it: call count and average / minimum / maximum /
     * self / total milliseconds, then a group of columns per counter touched.
     *
     * @param roots the section tree to report, e.g. {@link Profiler#snapshot()}
     * @return the formatted table, or a short notice when nothing was recorded
     */
    public static String format(List<ProfileNode> roots) {

        if (roots.isEmpty()) {
            return NO_TIMINGS_NOTICE;
        }

        var columns = buildColumns(roots);
        var table = new ArrayList<List<String>>();

        table.add(buildHeaderRow(columns));
        appendNodeRows(table, roots, 0, columns);
        return renderTable(table, measureColumnWidths(table, columns));
    }

    // Depth-first, so a child is printed under the row it ran inside rather than
    // after everything at its own level.
    private static void appendNodeRows(
            List<List<String>> table,
            List<ProfileNode> nodes,
            int depth,
            List<ReportColumn> columns) {

        for (var node : nodes) {

            var cells = new ArrayList<String>(columns.size() + 1);

            cells.add(indentSectionName(node, depth));
            for (var column : columns) {
                cells.add(column.renderCell(node));
            }
            table.add(cells);
            appendNodeRows(table, node.getChildren(), depth + 1, columns);
        }
    }

    // The columns after the section name. The timing ones are always there; the
    // counter ones are whatever the capture happened to count, so a tree that
    // counted nothing renders exactly the table it did before counters existed.
    private static List<ReportColumn> buildColumns(List<ProfileNode> roots) {

        var columns = new ArrayList<ReportColumn>();

        columns.add(new ReportColumn(
            COUNT_HEADER, CALL_COUNT_COLUMN_FLOOR, node -> Long.toString(node.getCount())));
        columns.add(buildDurationColumn(AVERAGE_HEADER, ProfileNode::getAverageNanos));
        columns.add(buildDurationColumn(MINIMUM_HEADER, ProfileNode::getMinNanos));
        columns.add(buildDurationColumn(MAXIMUM_HEADER, ProfileNode::getMaxNanos));
        columns.add(buildDurationColumn(SELF_HEADER, ProfileNode::getSelfNanos));
        columns.add(new ReportColumn(
            TOTAL_HEADER,
            TOTAL_COLUMN_FLOOR,
            node -> formatMillis(node.getTotalNanos())));

        for (var counter : collectCounters(roots)) {
            columns.addAll(buildCounterColumns(counter));
        }
        return columns;
    }

    private static List<ReportColumn> buildCounterColumns(ProfileCounter counter) {

        var name = counter.getName().toUpperCase(Locale.ROOT);

        return List.of(
            buildCounterColumn(name, counter, ProfileCount::getTotal),
            buildCounterColumn(name + COUNTER_MINIMUM_SUFFIX, counter, ProfileCount::getMinPerCall),
            buildCounterColumn(name + COUNTER_MAXIMUM_SUFFIX, counter, ProfileCount::getMaxPerCall),
            new ReportColumn(
                name + COUNTER_PER_ITEM_SUFFIX,
                COUNTER_COLUMN_FLOOR,
                node -> formatSelfMicrosPerItem(node, counter)));
    }

    private static ReportColumn buildCounterColumn(
            String header,
            ProfileCounter counter,
            ToLongFunction<ProfileCount> readAmount) {

        return new ReportColumn(header, COUNTER_COLUMN_FLOOR, node -> {
            var count = findCount(node, counter);
            return count == null ? ABSENT_CELL : Long.toString(readAmount.applyAsLong(count));
        });
    }

    private static ReportColumn buildDurationColumn(
            String header,
            ToLongFunction<ProfileNode> readNanos) {

        return new ReportColumn(
            header,
            DURATION_COLUMN_FLOOR,
            node -> formatMillis(readNanos.applyAsLong(node)));
    }

    private static List<String> buildHeaderRow(List<ReportColumn> columns) {

        var headers = new ArrayList<String>(columns.size() + 1);

        headers.add(SECTION_HEADER);
        for (var column : columns) {
            headers.add(column.getHeader());
        }
        return headers;
    }

    // In the order the tree first counted them, which is the order the rows
    // themselves are in, so a column group sits near the rows that fill it.
    private static Collection<ProfileCounter> collectCounters(List<ProfileNode> roots) {
        var counters = new LinkedHashSet<ProfileCounter>();
        collectCountersInto(roots, counters);
        return counters;
    }

    private static void collectCountersInto(
            List<ProfileNode> nodes,
            Set<ProfileCounter> counters) {

        for (var node : nodes) {
            for (var count : node.getCounts()) {
                counters.add(count.getCounter());
            }
            collectCountersInto(node.getChildren(), counters);
        }
    }

    private static ProfileCount findCount(ProfileNode node, ProfileCounter counter) {
        for (var count : node.getCounts()) {
            if (count.getCounter() == counter) {
                return count;
            }
        }
        return null;
    }

    private static String formatMillis(long nanos) {
        return String.format(Locale.ROOT, MILLIS_FORMAT, Timings.convertNanosToMillis(nanos));
    }

    // Self time over what the row counted itself: what one system, market or
    // cell cost here, which is the number an optimisation is judged against.
    // Microseconds because a per-item cost that read in milliseconds would be a
    // row worth no further attention.
    private static String formatSelfMicrosPerItem(ProfileNode node, ProfileCounter counter) {

        var count = findCount(node, counter);

        // A row whose children did all the counting has no per-item cost of its
        // own, and dividing by their items would price its self time against
        // work it did not do.
        if (count == null || count.getSelfTotal() == 0) {
            return ABSENT_CELL;
        }
        return String.format(
            Locale.ROOT,
            PER_ITEM_FORMAT,
            Timings.convertNanosToMicros(node.getSelfNanos()) / count.getSelfTotal());
    }

    private static String indentSectionName(ProfileNode node, int depth) {
        return " ".repeat(depth * INDENT_SPACES_PER_DEPTH) + node.getSection().getName();
    }

    // The widest cell in each column, floors included, so every row agrees on
    // where a column starts - a table whose rows disagree cannot be read down.
    private static int[] measureColumnWidths(List<List<String>> table, List<ReportColumn> columns) {

        var widths = new int[columns.size() + 1];

        widths[0] = SECTION_COLUMN_FLOOR;
        for (var index = 0; index < columns.size(); index++) {
            widths[index + 1] = columns.get(index).getFloorWidth();
        }
        for (var row : table) {
            for (var index = 0; index < row.size(); index++) {
                widths[index] = Math.max(widths[index], row.get(index).length());
            }
        }
        return widths;
    }

    // The section name reads left to right and is padded on the right; every
    // number is padded on the left, so digits of the same magnitude line up.
    private static String renderTable(List<List<String>> table, int[] widths) {

        var report = new StringBuilder();

        for (var row : table) {

            if (report.length() > 0) {
                report.append('\n');
            }
            report.append(String.format(Locale.ROOT, "%-" + widths[0] + "s", row.get(0)));
            for (var index = 1; index < row.size(); index++) {
                report.append(COLUMN_GAP);
                report.append(String.format(Locale.ROOT, "%" + widths[index] + "s", row.get(index)));
            }
        }
        return report.toString();
    }

    /**
     * One column of the table: what it is headed, how narrow it may get, and
     * what it says about a row.
     *
     * <p>A column rather than a format string per row shape, because the
     * counter columns are not known until the tree is in hand and a row has to
     * fill whatever columns the capture turned out to need.
     */
    private static final class ReportColumn {

        private final String header;
        private final int floorWidth;
        private final Function<ProfileNode, String> readCell;

        private ReportColumn(
                String header,
                int floorWidth,
                Function<ProfileNode, String> readCell) {

            this.header = header;
            this.floorWidth = floorWidth;
            this.readCell = readCell;
        }

        private String getHeader() {
            return header;
        }

        private int getFloorWidth() {
            return floorWidth;
        }

        private String renderCell(ProfileNode node) {
            return readCell.apply(node);
        }
    }
}
