package kmlib.profiling.report;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.snapshot.ProfileCount;
import kmlib.profiling.snapshot.ProfileNode;
import kmlib.profiling.snapshot.ProfileOriginTree;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.ToLongFunction;

/**
 * The columns a capture's counters add: what a row counted in all, how far one
 * call's worth spread, and what one item cost it.
 *
 * <p>Apart from the timing columns because these are not fixed - which of them
 * exist is whatever the capture happened to count, so a tree that counted
 * nothing renders the table it did before counters existed and one that counted
 * five things carries five groups. Deciding that is a job of its own: it reads
 * the whole capture before a single column can be named.
 *
 * <p>A duration is judged against the work it covered, which is why these sit in
 * the table beside the milliseconds rather than in a log line of their own.
 */
final class CounterColumns {

    // Suffixed onto the counter's own name, so a reader can tell which group a
    // spread belongs to when several counters are in the table at once.
    private static final String MINIMUM_SUFFIX = " MIN";
    private static final String MAXIMUM_SUFFIX = " MAX";
    private static final String PER_ITEM_SUFFIX = " " + ReportFormats.PER_ITEM_UNIT;

    // How big a counter gets and how long it is named are the caller's, not this
    // table's, so these columns close up around whatever they hold.
    private static final int NO_COLUMN_FLOOR = 0;

    private CounterColumns() {
        // utility class, no instances.
    }

    /**
     * @param originTrees the whole capture, since the groups are shared across
     *                    its origins and a column present for one game only would
     *                    leave the other's rows unreadable against it
     * @param isPerFrame  whether the totals are being divided by a frame count,
     *                    which the total column's header says
     * @return a group of columns per counter anything in the capture touched, in
     *         the order the capture first counted them - which is the order the
     *         rows themselves are in, so a group sits near the rows that fill it
     */
    static List<ReportColumn> buildColumnsForEveryCounter(
            List<ProfileOriginTree> originTrees,
            boolean isPerFrame) {

        var columns = new ArrayList<ReportColumn>();

        for (var counter : collectCounters(originTrees)) {
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
            buildSpreadColumn(
                name + MINIMUM_SUFFIX,
                counter,
                count -> count.getSpread().getMinPerCall()),
            buildSpreadColumn(
                name + MAXIMUM_SUFFIX,
                counter,
                count -> count.getSpread().getMaxPerCall()),
            ReportColumn.describeNumberColumn(
                name + PER_ITEM_SUFFIX,
                NO_COLUMN_FLOOR,
                (row, scale) -> formatSelfMicrosPerItem(row.getNode(), counter)));
    }

    private static ReportColumn buildSpreadColumn(
            String header,
            ProfileCounter counter,
            ToLongFunction<ProfileCount> readAmount) {

        return ReportColumn.describeNumberColumn(header, NO_COLUMN_FLOOR, (row, scale) -> {

            var count = row.getNode().findCount(counter);

            return count == null
                ? ReportFormats.ABSENT_CELL
                : Long.toString(readAmount.applyAsLong(count));
        });
    }

    private static Set<ProfileCounter> collectCounters(List<ProfileOriginTree> originTrees) {

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
