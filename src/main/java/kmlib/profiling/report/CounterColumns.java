package kmlib.profiling.report;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.snapshot.ProfileNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The columns a capture's counters add: what a row counted in all, and what one
 * item cost it.
 *
 * <p>Apart from the timing columns because these are not fixed - which of them
 * exist is whatever the rows being written happened to count, so a reading that
 * shows no counting row renders the table it did before counters existed and one
 * whose rows counted five things carries five groups. Deciding that is a job of
 * its own: it reads every row the reading keeps before a single column can be
 * named. The rows kept rather than the whole capture, because a group no shown
 * row fills is a column of blanks, and a reading narrowed to one namespace was
 * narrowed to be read.
 *
 * <p>Two columns a counter rather than one for each figure a row holds about it.
 * The least and the most one call counted are per-call facts, and the line under
 * a row already carries the one call worth reading them off - every counter of it
 * together, beside what it was called - where a column apiece put fourteen
 * near-constant cells on a row for the two that vary.
 *
 * <p>A duration is judged against the work it covered, which is why these sit in
 * the table beside the milliseconds rather than in a log line of their own.
 */
final class CounterColumns {

    // Suffixed onto the counter's own name, so a reader can tell which group a
    // cost belongs to when several counters are in the table at once.
    private static final String PER_ITEM_SUFFIX = " " + ReportFormats.PER_ITEM_UNIT;

    // How big a counter gets and how long it is named are the caller's, not this
    // table's, so these columns close up around whatever they hold.
    private static final int NO_COLUMN_FLOOR = 0;

    private CounterColumns() {
        // utility class, no instances.
    }

    /**
     * @param shownRows  every row the reading writes, across all of its origins:
     *                   the groups are shared across the origins, since a column
     *                   present for one game only would leave the other's rows
     *                   unreadable against it
     * @param isPerFrame whether the totals are being divided by a frame count,
     *                   which the total column's header says
     * @return a group of columns per counter any shown row touched, in the order
     *         the rows first count them - which is the order the rows themselves
     *         are in, so a group sits near the rows that fill it
     */
    static List<ReportColumn> buildColumnsForEveryCounter(
            List<ProfileReportRow> shownRows,
            boolean isPerFrame) {

        var columns = new ArrayList<ReportColumn>();

        for (var counter : collectCounters(shownRows)) {
            columns.addAll(buildColumnsFor(counter, isPerFrame));
        }
        return columns;
    }

    private static List<ReportColumn> buildColumnsFor(
            ProfileCounter counter,
            boolean isPerFrame) {

        var name = counter.getName().toUpperCase(Locale.ROOT);

        return List.of(
            ReportColumn.describeNumberColumn(
                ReportFormats.markPerFrame(name, isPerFrame),
                NO_COLUMN_FLOOR,
                (row, scale) -> formatTotal(row.getNode(), counter, scale)),
            ReportColumn.describeNumberColumn(
                name + PER_ITEM_SUFFIX,
                NO_COLUMN_FLOOR,
                (row, scale) -> formatSelfMicrosPerItem(row.getNode(), counter)));
    }

    private static Set<ProfileCounter> collectCounters(List<ProfileReportRow> shownRows) {

        var counters = new LinkedHashSet<ProfileCounter>();

        for (var row : shownRows) {
            for (var count : row.getNode().getCounts()) {
                counters.add(count.getCounter());
            }
        }
        return counters;
    }

    private static String formatTotal(
            ProfileNode node,
            ProfileCounter counter,
            ReportScale scale) {

        var count = node.findCount(counter);

        return count == null
            ? ReportFormats.ABSENT_CELL
            : scale.formatCount(count.getTotals().getTotal());
    }

    // Self time over what the row counted itself: what one system, market or cell
    // cost here, which is the number an optimisation is judged against. Per item
    // whatever the table is divided by, a frame count having nothing to say about
    // what one item cost.
    private static String formatSelfMicrosPerItem(ProfileNode node, ProfileCounter counter) {

        var count = node.findCount(counter);

        // A row whose children did all the counting has no per-item cost of its
        // own, and dividing by their items would price its self time against work
        // it did not do.
        if (count == null || count.getTotals().getSelfTotal() == 0) {
            return ReportFormats.ABSENT_CELL;
        }
        return ReportFormats.formatMicrosPerItem(
            node.getSelfNanos(), count.getTotals().getSelfTotal());
    }
}
