package kmlib.profiling.report;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.Profiler;
import kmlib.profiling.snapshot.DurationBuckets;
import kmlib.profiling.snapshot.ProfileCount;
import kmlib.profiling.snapshot.ProfileNode;
import kmlib.profiling.snapshot.ProfileOriginTree;
import kmlib.profiling.snapshot.ProfileTiming;
import kmlib.profiling.snapshot.WorstCall;
import kmlib.text.KmlibStrings;
import kmlib.time.Timings;

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
 * <p>Above each group of roots is the origin they were measured in, so a table
 * taken across two games reads as two captures side by side rather than as one
 * whose numbers cannot be traced to a save. The columns are shared across the
 * groups, which is what lets one game's row be read against the other's.
 *
 * <p>Every counter anything in the tree touched adds a group of columns: what
 * the row counted in all, the spread of one call's worth, and what one item
 * cost it. A duration is judged against the work it covered, so the two are
 * read on one line rather than in a table and a log.
 *
 * <p>The last column is where the row's calls fell in duration: one character
 * per band, the bands doubling from a microsecond up, a dot where no call
 * landed and otherwise how many digits that band's tally has. A row whose calls
 * sit in one band costs what it costs; a row with an outlier band stalled, and
 * the two want different fixes.
 *
 * <p>Under a row whose slowest call has something to say beyond its duration
 * goes a second line naming that call and what its counters stood at, since the
 * maximum column already carries the duration and nothing else could carry the
 * rest.
 *
 * <p>Under a row whose calls ran a loop goes a line for the loop: how many turns
 * it took, what one turn cost in each of the section's steps, and the slowest
 * turn. Per turn rather than in total, because the row's own columns already
 * report the whole loop and what is wanted beside them is what one item cost.
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

    // Names the scale rather than the contents: the bands run left to right
    // from a microsecond through milliseconds to a second and over, which is
    // the one thing a reader needs to place a mark in the column.
    private static final String SPREAD_HEADER = "us>ms>s";

    // What one of something cost, whether the something is an item a row counted
    // or a turn of its loop. One spelling, since a reader meets both in one
    // table and two would read as two different measures.
    private static final String PER_ITEM_UNIT = "us/ea";

    // Suffixed onto the counter's own name, so a reader can tell which group a
    // spread belongs to when several counters are in the table at once.
    private static final String COUNTER_MINIMUM_SUFFIX = " MIN";
    private static final String COUNTER_MAXIMUM_SUFFIX = " MAX";
    private static final String COUNTER_PER_ITEM_SUFFIX = " " + PER_ITEM_UNIT;

    // Two spaces per level of nesting: enough for the eye to follow a row to
    // its parent, narrow enough that a deep tree still fits a console line.
    private static final int INDENT_SPACES_PER_DEPTH = 2;

    private static final String COLUMN_GAP = "  ";

    // Floors, not fixed widths: a column is widened by anything that does not
    // fit, and these keep the timing columns from closing up around small
    // numbers, where a table that reflows between two captures cannot be
    // compared against the one before it by eye. The section and counter
    // columns take no floor - how long a section is named, what a counter is
    // called and how big it gets are the caller's, not this table's.
    private static final int NO_COLUMN_FLOOR = 0;
    private static final int CALL_COUNT_COLUMN_FLOOR = 8;
    private static final int DURATION_COLUMN_FLOOR = 10;
    private static final int TOTAL_COLUMN_FLOOR = 11;

    // The spread is one character per band whether or not a row filled any, so
    // its floor is the band count itself: every row's marks then sit under the
    // same bands, which is the only way one row's shape can be read against
    // another's.
    private static final int SPREAD_COLUMN_FLOOR = DurationBuckets.countBuckets();

    // A band no call landed in, and the digit a filled one is marked from: a
    // band's mark is how many digits its tally has, capped so the column keeps
    // its width whatever the capture holds.
    private static final char EMPTY_BAND_MARK = '.';
    private static final char FIRST_DIGIT_MARK = '0';
    private static final int WIDEST_MARKED_TALLY = 9;

    // The line above a group of roots, naming the game they were measured in.
    // Unquoted, so a label a caller composed reads as the heading it is rather
    // than as one more tag written across the columns.
    private static final String ORIGIN_PREFIX = "origin ";

    // The second line under a row: what its slowest call took, what it was
    // called, and what each counter stood at when it ended.
    private static final String WORST_CALL_PREFIX = "worst ";
    private static final String MILLIS_UNIT = "ms";
    private static final String TAG_QUOTE = "\"";
    private static final String COUNT_ASSIGNMENT = "=";

    // The loop line under a row: the turns, what one of them cost in each step,
    // and the slowest of them.
    private static final String ITERATION_COUNT_LABEL = "iterations";
    private static final String SLOWEST_ITERATION_LABEL = "slowest";

    // Blank where a row never touched a counter - see ProfileNode#getCounts for
    // why that is not a zero.
    private static final String ABSENT_CELL = "";

    private static final String MILLIS_FORMAT = "%.3f";
    private static final String PER_ITEM_FORMAT = "%.1f";

    private TimingReport() {
    }

    /**
     * Renders {@code originTrees} as a table with one row per section per
     * parent, children indented under it and each group of roots headed by the
     * origin it was measured in: call count and average / minimum / maximum /
     * self / total milliseconds, then a group of columns per counter touched.
     *
     * @param originTrees the capture to report, e.g. {@link Profiler#snapshot()}
     * @return the formatted table, or a short notice when nothing was recorded
     */
    public static String format(List<ProfileOriginTree> originTrees) {

        if (originTrees.isEmpty()) {
            return NO_TIMINGS_NOTICE;
        }

        var columns = buildColumns(originTrees);
        var table = new ArrayList<List<String>>();

        table.add(buildHeaderRow(columns));

        for (var originTree : originTrees) {

            table.add(List.of(ORIGIN_PREFIX + originTree.getOrigin().getLabel()));
            appendNodeRows(table, originTree.getRoots(), 0, columns);
        }
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
            appendWorstCallLine(table, node, depth);
            appendIterationsLine(table, node, depth);
            appendNodeRows(table, node.getChildren(), depth + 1, columns);
        }
    }

    // What the row's slowest call was doing, on a line of its own under it: the
    // counters it carries are that one call's values rather than the row's, so
    // they belong to no column, and a tag is free text of the caller's length.
    // Written only where it says something the maximum column does not, so the
    // rows whose calls are all alike stay one line each.
    private static void appendWorstCallLine(List<List<String>> table, ProfileNode node, int depth) {

        var worstCall = node.getWorstCall();

        if (!hasContextBeyondItsDuration(worstCall)) {
            return;
        }
        var line = new StringBuilder();

        line.append(indentSpanningLine(depth));
        line.append(WORST_CALL_PREFIX);
        line.append(formatMillis(worstCall.getDurationNanos()));
        line.append(MILLIS_UNIT);

        appendQuotedTag(line, worstCall.getTag());

        for (var count : worstCall.getCounts()) {
            line.append(COLUMN_GAP);
            line.append(count.getCounter().getName());
            line.append(COUNT_ASSIGNMENT);
            line.append(count.getAmount());
        }
        table.add(List.of(line.toString()));
    }

    // What the row's loop ran, on a line of its own under it. Its numbers are
    // per turn while every column of the row is per call - a bake and a cell are
    // different denominators - so they belong to no column and are written
    // beside their own labels. A row whose calls ran no loop writes nothing.
    private static void appendIterationsLine(
            List<List<String>> table,
            ProfileNode node,
            int depth) {

        var iterations = node.getIterations();

        if (!iterations.hasAnyIterations()) {
            return;
        }
        var line = new StringBuilder();

        line.append(indentSpanningLine(depth));

        line.append(ITERATION_COUNT_LABEL)
            .append(COUNT_ASSIGNMENT)
            .append(iterations.getCount());

        for (var phaseTotal : iterations.getPhaseTotals()) {

            line.append(COLUMN_GAP);
            line.append(phaseTotal.getPhase().getName());
            line.append(COUNT_ASSIGNMENT);
            line.append(formatMicrosPerIteration(phaseTotal.getTotalNanos(), iterations.getCount()));
        }
        line.append(COLUMN_GAP);

        line.append(SLOWEST_ITERATION_LABEL)
            .append(COUNT_ASSIGNMENT);

        line.append(formatMillis(iterations.getSlowestNanos()))
            .append(MILLIS_UNIT);

        appendQuotedTag(line, iterations.getSlowestTag());

        table.add(List.of(line.toString()));
    }

    // Quoted, because a tag is whatever the caller wrote - spaces included -
    // and its end has to be tellable from whatever follows it. Nothing at all
    // where the caller named nothing.
    private static void appendQuotedTag(StringBuilder line, String tag) {

        if (KmlibStrings.hasText(tag)) {

            line.append(COLUMN_GAP)
                .append(TAG_QUOTE)
                .append(tag)
                .append(TAG_QUOTE);
        }
    }

    // The columns after the section name. The timing ones are always there; the
    // counter ones are whatever the capture happened to count, so a tree that
    // counted nothing renders exactly the table it did before counters existed.
    // Taken over every origin at once, since the groups share one set of columns
    // and a column present for one game only would leave the other's rows
    // unreadable against it.
    private static List<ReportColumn> buildColumns(List<ProfileOriginTree> originTrees) {

        var columns = new ArrayList<ReportColumn>();

        columns.add(new ReportColumn(
            COUNT_HEADER,
            CALL_COUNT_COLUMN_FLOOR,
            node -> Long.toString(node.getTiming().getCount())));

        columns.add(buildTimingColumn(AVERAGE_HEADER, ProfileTiming::getAverageNanos));
        columns.add(buildTimingColumn(MINIMUM_HEADER, ProfileTiming::getMinNanos));
        columns.add(buildTimingColumn(MAXIMUM_HEADER, ProfileTiming::getMaxNanos));

        columns.add(new ReportColumn(
            SELF_HEADER,
            DURATION_COLUMN_FLOOR,
            node -> formatMillis(node.getSelfNanos())));

        columns.add(new ReportColumn(
            TOTAL_HEADER,
            TOTAL_COLUMN_FLOOR,
            node -> formatMillis(node.getTiming().getTotalNanos())));

        columns.add(new ReportColumn(
            SPREAD_HEADER,
            SPREAD_COLUMN_FLOOR,
            TimingReport::formatBands));

        for (var counter : collectCounters(originTrees)) {
            columns.addAll(buildCounterColumns(counter));
        }
        return columns;
    }

    private static List<ReportColumn> buildCounterColumns(ProfileCounter counter) {

        var name = counter.getName().toUpperCase(Locale.ROOT);

        return List.of(
            buildCounterColumn(
                name,
                counter,
                count -> count.getTotals().getTotal()),
            buildCounterColumn(
                name + COUNTER_MINIMUM_SUFFIX,
                counter,
                count -> count.getSpread().getMinPerCall()),
            buildCounterColumn(
                name + COUNTER_MAXIMUM_SUFFIX,
                counter,
                count -> count.getSpread().getMaxPerCall()),
            new ReportColumn(
                name + COUNTER_PER_ITEM_SUFFIX,
                NO_COLUMN_FLOOR,
                node -> formatSelfMicrosPerItem(node, counter)));
    }

    private static ReportColumn buildCounterColumn(
            String header,
            ProfileCounter counter,
            ToLongFunction<ProfileCount> readAmount) {

        return new ReportColumn(header, NO_COLUMN_FLOOR, node -> {

            var count = node.findCount(counter);

            return count == null
                ? ABSENT_CELL
                : Long.toString(readAmount.applyAsLong(count));
        });
    }

    private static ReportColumn buildTimingColumn(
            String header,
            ToLongFunction<ProfileTiming> readNanos) {

        return new ReportColumn(
            header,
            DURATION_COLUMN_FLOOR,
            node -> formatMillis(readNanos.applyAsLong(node.getTiming())));
    }

    private static List<String> buildHeaderRow(List<ReportColumn> columns) {

        var headers = new ArrayList<String>(columns.size() + 1);
        headers.add(SECTION_HEADER);

        for (var column : columns) {

            headers.add(column.getHeader());
        }
        return headers;
    }

    // In the order the capture first counted them, which is the order the rows
    // themselves are in, so a column group sits near the rows that fill it.
    private static Collection<ProfileCounter> collectCounters(
            List<ProfileOriginTree> originTrees) {

        var counters = new LinkedHashSet<ProfileCounter>();

        for (var originTree : originTrees) {
            collectCountersInto(originTree.getRoots(), counters);
        }
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

    // A band's mark is how many digits its tally has, so a band holding
    // thousands of calls reads taller than one holding three while the column
    // stays as wide as it was - a shape that can be compared between two
    // captures rather than one that reflows with them. Capped at the widest
    // digit for the same reason.
    private static char formatBandMark(long calls) {

        if (calls == 0) {
            return EMPTY_BAND_MARK;
        }
        var digits = Math.min(Long.toString(calls).length(), WIDEST_MARKED_TALLY);

        return (char) (FIRST_DIGIT_MARK + digits);
    }

    // Blank on a row no call has finished on: there is no shape to show, and a
    // line of dots would read as calls that were all too fast to matter.
    private static String formatBands(ProfileNode node) {

        var buckets = node.getTiming().getBuckets();

        if (!buckets.hasAnyCalls()) {
            return ABSENT_CELL;
        }
        var bands = new StringBuilder(DurationBuckets.countBuckets());

        for (var index = 0; index < DurationBuckets.countBuckets(); index++) {
            bands.append(formatBandMark(buckets.getCallsInBucket(index)));
        }
        return bands.toString();
    }

    // What one turn of the loop spent in one of its steps. Microseconds for the
    // reason the per-item column is in them: a step of a per-item loop that read
    // in milliseconds is a loop already too slow to be running.
    private static String formatMicrosPerIteration(long totalNanos, long iterations) {

        return String.format(
            Locale.ROOT,
            PER_ITEM_FORMAT,
            Timings.convertNanosToMicros(totalNanos) / iterations) + PER_ITEM_UNIT;
    }

    private static String formatMillis(long nanos) {
        return String.format(Locale.ROOT, MILLIS_FORMAT, Timings.convertNanosToMillis(nanos));
    }

    // Self time over what the row counted itself: what one system, market or
    // cell cost here, which is the number an optimisation is judged against.
    // Microseconds because a per-item cost that read in milliseconds would be a
    // row worth no further attention.
    private static String formatSelfMicrosPerItem(ProfileNode node, ProfileCounter counter) {

        var count = node.findCount(counter);

        // A row whose children did all the counting has no per-item cost of its
        // own, and dividing by their items would price its self time against
        // work it did not do.
        if (count == null || count.getTotals().getSelfTotal() == 0) {
            return ABSENT_CELL;
        }
        return String.format(
            Locale.ROOT,
            PER_ITEM_FORMAT,
            Timings.convertNanosToMicros(node.getSelfNanos()) / count.getTotals().getSelfTotal());
    }

    // Whether the record says anything the table does not already carry. Its
    // duration is the maximum column, so a call that was named nothing and
    // counted nothing would print a line repeating a number one column to the
    // left.
    private static boolean hasContextBeyondItsDuration(WorstCall worstCall) {
        return KmlibStrings.hasText(worstCall.getTag()) || !worstCall.getCounts().isEmpty();
    }

    private static String indentSectionName(ProfileNode node, int depth) {
        return " ".repeat(depth * INDENT_SPACES_PER_DEPTH) + node.getSection().getName();
    }

    // One level past the row it belongs to, so a line written across the columns
    // reads as something said about the row above it rather than as a row of its
    // own.
    private static String indentSpanningLine(int depth) {
        return " ".repeat((depth + 1) * INDENT_SPACES_PER_DEPTH);
    }

    // A row is either a cell per column or a single line written across all of
    // them, and the count tells the two apart: a table always has the section
    // column and the timing ones beside it, so nothing that is padded into
    // columns is ever one cell wide.
    private static boolean isSpanningLine(List<String> row) {
        return row.size() == 1;
    }

    // The widest cell in each column, floors included, so every row agrees on
    // where a column starts - a table whose rows disagree cannot be read down.
    private static int[] measureColumnWidths(List<List<String>> table, List<ReportColumn> columns) {

        var widths = new int[columns.size() + 1];

        widths[0] = NO_COLUMN_FLOOR;
        for (var index = 0; index < columns.size(); index++) {
            widths[index + 1] = columns.get(index).getFloorWidth();
        }
        for (var row : table) {

            // An origin heading, a worst-call line and a loop line are each one
            // cell spanning the whole width, so measuring one would widen the
            // section column by however long a caller's own text was and push
            // every number away from its header.
            if (isSpanningLine(row)) {
                continue;
            }
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
            if (isSpanningLine(row)) {
                // Written as it stands: it carries its own indent and belongs to
                // no column, so padding it would only trail spaces.
                report.append(row.get(0));
                continue;
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
