package kmlib.profiling.report;

import kmlib.profiling.Profiler;
import kmlib.profiling.snapshot.ProfileNode;
import kmlib.profiling.snapshot.ProfileOriginTree;
import kmlib.profiling.snapshot.WorstCall;
import kmlib.text.KmlibStrings;
import kmlib.text.TextTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Formats a {@link Profiler} snapshot into an aligned, human-readable table.
 *
 * <p>Pure text transform, no profiling state of its own: it takes the trees of
 * {@link ProfileNode} and a {@link ProfileReportRequest} and returns a string, so
 * it is reusable by any output sink - a console command, the game log.
 *
 * <p>What is written is the request's: which reading of the capture, over which
 * rows, how many of them, and whether the totals are what a frame spent or what
 * the session came to. What every reading has in common is here - the groups and
 * the lines written under a row - so two readings of one capture differ in what
 * they say and never in how it is laid out. Which columns they are laid out in
 * is {@link ReportColumns}.
 *
 * <p>Above each group of rows is the origin they were measured in, so a table
 * taken across two games reads as two captures side by side rather than as one
 * whose numbers cannot be traced to a save. The columns are shared across the
 * groups, which is what lets one game's row be read against the other's.
 *
 * <p>Under a row whose worst call has something to say beyond its duration goes
 * a second line naming that call and what its counters stood at, since the
 * maximum column already carries the duration and nothing else could carry the
 * rest. A row that went over what its section allows always gets that line,
 * carrying what it broke: a budget is what turns the table into findings, and a
 * finding printed apart from the call that produced it is a claim without its
 * evidence.
 *
 * <p>Under a row whose calls ran a loop goes a line for the loop: how many turns
 * it took, what one turn cost in each of the section's steps, and the slowest
 * turn. Per turn rather than in total, because the row's own columns already
 * report the whole loop and what is wanted beside them is what one item cost.
 */
public final class TimingReport {

    private static final String NO_TIMINGS_NOTICE = "No timings recorded.";
    private static final String NO_MATCHING_ROWS_NOTICE = "No rows matched.";

    // Between the parts of a line written across the columns. The table's own gap
    // between columns is its business; this is what keeps two things said about
    // one call from reading as one.
    private static final String PART_GAP = "  ";

    // The line above a group of rows, naming the game they were measured in.
    // Unquoted, so a label a caller composed reads as the heading it is rather
    // than as one more tag written across the columns.
    private static final String ORIGIN_PREFIX = "origin ";

    // What the group's totals were divided by, said where they were: a column of
    // fractions is unreadable without the count behind it, and a beat that never
    // ran here has to say so rather than let a total read as a frame's.
    private static final String PER_FRAME_PREFIX = "per frame, over ";
    private static final String PER_FRAME_OF = " of ";
    private static final String NO_FRAMES_SUFFIX = " never ran here; totals as captured";

    // The second line under a row: what the call it names took, what it was
    // called, and what each counter stood at when it ended.
    //
    // Which call that is changes once the row has broken its bound, and the line
    // says which rather than leaving the two to be told apart by reading the
    // accumulator: an unbreached row keeps the slowest call it has seen, while a
    // breached one keeps the latest call that broke the bound however fast that
    // call was. Both would otherwise read as "worst" beside a maximum column that
    // means the first of the two, and a breached row's number moving down over a
    // session reads as a fault in the report rather than as the newer finding it
    // is.
    private static final String WORST_CALL_PREFIX = "worst ";
    private static final String LATEST_BREACH_PREFIX = "latest breach ";
    private static final String TAG_QUOTE = "\"";
    private static final String COUNT_ASSIGNMENT = "=";

    // What the row broke, written on that same line: the call it names is the
    // breaching one, and the two apart would be a finding and its evidence on two
    // lines.
    private static final String OVER_BUDGET_PREFIX = "over budget: ";

    // The loop line under a row: the turns, what one of them cost in each step,
    // and the slowest of them.
    private static final String ITERATION_COUNT_LABEL = "iterations";
    private static final String SLOWEST_ITERATION_LABEL = "slowest";

    private TimingReport() {
    }

    /**
     * Renders the reading {@code request} asks for over {@code originTrees}, each
     * group of rows headed by the origin it was measured in.
     *
     * @param originTrees the capture to report, e.g. {@link Profiler#snapshot()}
     * @param request     which reading of it, over which rows, and against what
     * @return the formatted table, a short notice where nothing was recorded, or
     *         one where the request kept nothing - which is a different answer
     *         from an empty capture and is said as one
     */
    public static String format(
            List<ProfileOriginTree> originTrees,
            ProfileReportRequest request) {

        if (originTrees.isEmpty()) {
            return NO_TIMINGS_NOTICE;
        }
        // The rows are chosen before a column is named, because which counter
        // columns the table carries is decided by what those rows counted: a
        // group raised for the whole capture would stand empty on a reading
        // narrowed to rows that never fill it.
        var groups = new ArrayList<OriginGroup>();

        for (var originTree : originTrees) {

            var rows = selectRows(originTree, request);

            if (!rows.isEmpty()) {
                groups.add(new OriginGroup(
                    originTree,
                    ReportScale.resolveScale(originTree, request),
                    rows));
            }
        }
        if (groups.isEmpty()) {
            return NO_MATCHING_ROWS_NOTICE;
        }
        var columns = ReportColumns.buildColumns(collectRowsOf(groups), request.hasFrameBeat());
        var table = new TextTable(columns.describeTableColumns());

        table.addRow(columns.buildHeaderRow());

        for (var group : groups) {
            table.addSpanningLine(describeOrigin(group.originTree(), group.scale(), request));
            appendRows(table, columns, group.rows(), group.scale());
        }
        return table.renderAligned();
    }

    /**
     * One origin's part of the reading: the rows the request kept of it, and
     * the scale they are written at. Gathered before the table is built, since
     * the columns are chosen from every group's rows together.
     */
    private record OriginGroup(
        ProfileOriginTree originTree,
        ReportScale scale,
        List<ProfileReportRow> rows) {
    }

    // Every row the reading writes, in the order it writes them, which is the
    // order their counters' groups are raised in.
    private static List<ProfileReportRow> collectRowsOf(List<OriginGroup> groups) {

        var rows = new ArrayList<ProfileReportRow>();

        for (var group : groups) {
            rows.addAll(group.rows());
        }
        return rows;
    }

    private static List<ProfileReportRow> selectRows(
            ProfileOriginTree originTree,
            ProfileReportRequest request) {

        var roots = originTree.getRoots();

        return request.getView().presentRows(roots, ShownNodes.selectShownNodes(roots, request));
    }

    private static void appendRows(
            TextTable table,
            ReportColumns columns,
            List<ProfileReportRow> rows,
            ReportScale scale) {

        for (var row : rows) {

            table.addRow(columns.buildCells(row, scale));
            appendWorstCallLine(table, row);
            appendIterationsLine(table, row);
        }
    }

    // What the row's kept call was doing, on a line of its own under it: the
    // counters it carries are that one call's values rather than the row's, so
    // they belong to no column, and a tag is free text of the caller's length.
    // Written only where it says something the maximum column does not, so the
    // rows whose calls are all alike stay one line each - and always on a row that
    // went over budget, where the finding is the thing worth saying and the call
    // beside it is what broke the bound. Which call is kept is the row's breach to
    // decide, so the prefix is taken from it rather than fixed.
    private static void appendWorstCallLine(TextTable table, ProfileReportRow row) {

        var worstCall = row.getNode().getWorstCall();
        var breach = row.getNode().getBudgetBreach();

        if (!breach.hasBreached() && !hasContextBeyondItsDuration(worstCall)) {
            return;
        }
        var line = new StringBuilder();

        line.append(indentSpanningLine(row));
        line.append(breach.hasBreached() ? LATEST_BREACH_PREFIX : WORST_CALL_PREFIX);
        line.append(formatCallMillis(worstCall.getDurationNanos()));

        // Beside the duration they qualify: a maximum that was the row's first
        // call under a compiling JVM is a different finding from one that was
        // not, and the two have to be told apart where the number is.
        var conditions = worstCall.getWarmth().describeWarmth();

        if (!conditions.isEmpty()) {
            line.append(PART_GAP).append(conditions);
        }
        if (breach.hasBreached()) {

            line.append(PART_GAP)
                .append(OVER_BUDGET_PREFIX)
                .append(breach.describeBreach());
        }
        // The counts before the name, and the name last, in the order the same
        // call's closing line writes them: a reader who found a call in the log
        // and then looked it up here is reading one fact twice, and two orders
        // would make them compare it word by word.
        for (var count : worstCall.getCounts()) {
            line.append(PART_GAP);
            line.append(count.getCounter().getName());
            line.append(COUNT_ASSIGNMENT);
            line.append(count.getAmount());
        }
        appendQuotedTag(line, worstCall.getTag());

        table.addSpanningLine(line.toString());
    }

    // What the row's loop ran, on a line of its own under it. Its numbers are per
    // turn while every column of the row is per call - a bake and a cell are
    // different denominators - so they belong to no column and are written beside
    // their own labels. A row whose calls ran no loop writes nothing.
    private static void appendIterationsLine(TextTable table, ProfileReportRow row) {

        var iterations = row.getNode().getIterations();

        if (!iterations.hasAnyIterations()) {
            return;
        }
        var line = new StringBuilder();

        line.append(indentSpanningLine(row));

        line.append(ITERATION_COUNT_LABEL)
            .append(COUNT_ASSIGNMENT)
            .append(iterations.getCount());

        for (var phaseTotal : iterations.getPhaseTotals()) {

            line.append(PART_GAP);
            line.append(phaseTotal.getPhase().getName());
            line.append(COUNT_ASSIGNMENT);

            line.append(ReportFormats.formatMicrosPerItem(
                phaseTotal.getTotalNanos(), iterations.getCount()));

            line.append(ReportFormats.PER_ITEM_UNIT);
        }
        line.append(PART_GAP);

        line.append(SLOWEST_ITERATION_LABEL)
            .append(COUNT_ASSIGNMENT)
            .append(formatCallMillis(iterations.getSlowestNanos()));

        appendQuotedTag(line, iterations.getSlowestTag());

        table.addSpanningLine(line.toString());
    }

    // Quoted, because a tag is whatever the caller wrote - spaces included - and
    // its end has to be tellable from whatever follows it. Nothing at all where
    // the caller named nothing.
    private static void appendQuotedTag(StringBuilder line, String tag) {

        if (KmlibStrings.hasText(tag)) {

            line.append(PART_GAP)
                .append(TAG_QUOTE)
                .append(tag)
                .append(TAG_QUOTE);
        }
    }

    private static String describeOrigin(
            ProfileOriginTree originTree,
            ReportScale scale,
            ProfileReportRequest request) {

        var heading = new StringBuilder(ORIGIN_PREFIX + originTree.getOrigin().getLabel());

        if (!request.hasFrameBeat()) {
            return heading.toString();
        }
        var beatName = request.getFrameBeat().getName();

        heading.append(PART_GAP);

        if (scale.isPerFrame()) {
            heading.append(PER_FRAME_PREFIX).append(scale.getFrames())
                .append(PER_FRAME_OF).append(beatName);
        } else {
            heading.append(beatName).append(NO_FRAMES_SUFFIX);
        }
        return heading.toString();
    }

    // One call, whatever the table's totals are divided by: a worst call and a
    // slowest turn each happened once, and dividing either by a frame count would
    // report a duration nothing ever took.
    private static String formatCallMillis(long nanos) {
        return ReportFormats.formatNanosAsMillis(nanos) + ReportFormats.MILLIS_UNIT;
    }

    // Whether the record says anything the table does not already carry. Its
    // duration is the maximum column, so a call that was named nothing, counted
    // nothing and ran under nothing worth reporting would print a line repeating a
    // number one column to the left. The conditions count among what it says: a
    // maximum reached on a row's first call under a compiling JVM is a reading to
    // discount, and a row whose calls are alike in every other way is exactly where
    // that would otherwise go unsaid.
    private static boolean hasContextBeyondItsDuration(WorstCall worstCall) {

        return KmlibStrings.hasText(worstCall.getTag())
            || !worstCall.getCounts().isEmpty()
            || worstCall.getWarmth().hasAnythingToSay();
    }

    // One level past the row it belongs to, so a line written across the columns
    // reads as something said about the row above it rather than as a row of its
    // own.
    private static String indentSpanningLine(ProfileReportRow row) {
        return ReportFormats.indentToDepth(row.getDepth() + 1);
    }
}
