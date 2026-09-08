package kmlib.profiling.report;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.snapshot.DurationBuckets;
import kmlib.profiling.snapshot.ProfileCount;
import kmlib.profiling.snapshot.ProfileNode;
import kmlib.profiling.snapshot.ProfileOriginTree;
import kmlib.profiling.snapshot.ProfileTiming;
import kmlib.text.TextTableColumn;
import kmlib.time.Timings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.ToLongFunction;

/**
 * Which columns a capture has, and what each of them says about a row.
 *
 * <p>Decided from the capture rather than fixed, because the counter columns are
 * whatever the capture happened to count: a tree that counted nothing renders
 * the table it did before counters existed, and one that counted five things
 * carries a group of columns per thing.
 *
 * <p>Taken over every origin at once, since the groups share one set of columns
 * and a column present for one game only would leave the other's rows unreadable
 * against it. What each origin's totals are divided by is still its own, so a
 * cell is asked for the scale of the group it is being written in.
 */
final class ReportColumns {

    private static final String SECTION_HEADER = "SECTION";
    private static final String COUNT_HEADER = "COUNT";
    private static final String AVERAGE_HEADER = "AVG ms";
    private static final String MINIMUM_HEADER = "MIN ms";
    private static final String MAXIMUM_HEADER = "MAX ms";
    private static final String SELF_HEADER = "SELF ms";
    private static final String TOTAL_HEADER = "TOTAL ms";

    // Names the scale rather than the contents: the bands run left to right from
    // a microsecond through milliseconds to a second and over, which is the one
    // thing a reader needs to place a mark in the column.
    private static final String SPREAD_HEADER = "us>ms>s";

    // Suffixed onto the counter's own name, so a reader can tell which group a
    // spread belongs to when several counters are in the table at once.
    private static final String COUNTER_MINIMUM_SUFFIX = " MIN";
    private static final String COUNTER_MAXIMUM_SUFFIX = " MAX";
    private static final String COUNTER_PER_ITEM_SUFFIX = " " + ReportFormats.PER_ITEM_UNIT;

    // Marks the columns a frame count was divided into, so a reader meeting a
    // total of 0.41 knows it is what a frame spent rather than what the session
    // did.
    private static final String PER_FRAME_SUFFIX = "/f";

    // Floors, not fixed widths: a column is widened by anything that does not
    // fit, and these keep the timing columns from closing up around small
    // numbers, where a table that reflows between two captures cannot be compared
    // against the one before it by eye. The section and counter columns take no
    // floor - how long a section is named, what a counter is called and how big
    // it gets are the caller's, not this table's.
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

    private final List<ReportColumn> columns;

    private ReportColumns(List<ReportColumn> columns) {
        this.columns = columns;
    }

    /**
     * @param originTrees the whole capture, so every group shares one set of
     *                    columns
     * @param isPerFrame  whether the totals are being divided by a frame count,
     *                    which the headers of the divided columns say
     * @return the columns the capture needs, left to right
     */
    static ReportColumns buildColumns(List<ProfileOriginTree> originTrees, boolean isPerFrame) {

        var columns = new ArrayList<ReportColumn>();

        columns.add(new ReportColumn(
            SECTION_HEADER,
            NO_COLUMN_FLOOR,
            true,
            (row, scale) -> ReportFormats.indentToDepth(row.getDepth()) + row.getName()));

        columns.add(new ReportColumn(
            scaledHeader(COUNT_HEADER, isPerFrame),
            CALL_COUNT_COLUMN_FLOOR,
            false,
            (row, scale) -> scale.formatCount(row.getNode().getTiming().getCount())));

        columns.add(buildCallDurationColumn(AVERAGE_HEADER, ProfileTiming::getAverageNanos));
        columns.add(buildCallDurationColumn(MINIMUM_HEADER, ProfileTiming::getMinNanos));
        columns.add(buildCallDurationColumn(MAXIMUM_HEADER, ProfileTiming::getMaxNanos));

        columns.add(new ReportColumn(
            scaledHeader(SELF_HEADER, isPerFrame),
            DURATION_COLUMN_FLOOR,
            false,
            (row, scale) -> scale.formatMillis(row.getNode().getSelfNanos())));

        columns.add(new ReportColumn(
            scaledHeader(TOTAL_HEADER, isPerFrame),
            TOTAL_COLUMN_FLOOR,
            false,
            (row, scale) -> scale.formatMillis(row.getNode().getTiming().getTotalNanos())));

        columns.add(new ReportColumn(
            SPREAD_HEADER,
            SPREAD_COLUMN_FLOOR,
            false,
            (row, scale) -> formatBands(row.getNode())));

        for (var counter : collectCounters(originTrees)) {
            columns.addAll(buildCounterColumns(counter, isPerFrame));
        }
        return new ReportColumns(columns);
    }

    List<TextTableColumn> describeTableColumns() {

        var described = new ArrayList<TextTableColumn>(columns.size());

        for (var column : columns) {
            described.add(column.describeTableColumn());
        }
        return described;
    }

    List<String> buildHeaderRow() {

        var headers = new ArrayList<String>(columns.size());

        for (var column : columns) {
            headers.add(column.getHeader());
        }
        return headers;
    }

    /**
     * @param row   the row being written
     * @param scale what its origin's totals divide by
     * @return one cell per column, in the columns' order
     */
    List<String> buildCells(ProfileReportRow row, ReportScale scale) {

        var cells = new ArrayList<String>(columns.size());

        for (var column : columns) {
            cells.add(column.renderCell(row, scale));
        }
        return cells;
    }

    private static List<ReportColumn> buildCounterColumns(
            ProfileCounter counter,
            boolean isPerFrame) {

        var name = counter.getName().toUpperCase(Locale.ROOT);

        return List.of(
            new ReportColumn(
                scaledHeader(name, isPerFrame),
                NO_COLUMN_FLOOR,
                false,
                (row, scale) -> formatCounterTotal(row.getNode(), counter, scale)),
            buildCounterSpreadColumn(
                name + COUNTER_MINIMUM_SUFFIX,
                counter,
                count -> count.getSpread().getMinPerCall()),
            buildCounterSpreadColumn(
                name + COUNTER_MAXIMUM_SUFFIX,
                counter,
                count -> count.getSpread().getMaxPerCall()),
            new ReportColumn(
                name + COUNTER_PER_ITEM_SUFFIX,
                NO_COLUMN_FLOOR,
                false,
                (row, scale) -> formatSelfMicrosPerItem(row.getNode(), counter)));
    }

    private static ReportColumn buildCounterSpreadColumn(
            String header,
            ProfileCounter counter,
            ToLongFunction<ProfileCount> readAmount) {

        return new ReportColumn(header, NO_COLUMN_FLOOR, false, (row, scale) -> {

            var count = row.getNode().findCount(counter);

            return count == null
                ? ReportFormats.ABSENT_CELL
                : Long.toString(readAmount.applyAsLong(count));
        });
    }

    // A minimum, a maximum and an average are per call already, so no frame count
    // divides them: a call is not a frame, and a beat that ran twice in one would
    // report half of what one call took.
    private static ReportColumn buildCallDurationColumn(
            String header,
            ToLongFunction<ProfileTiming> readNanos) {

        return new ReportColumn(header, DURATION_COLUMN_FLOOR, false, (row, scale) ->
            ReportFormats.formatMillis(
                Timings.convertNanosToMillis(readNanos.applyAsLong(row.getNode().getTiming()))));
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

    // A band's mark is how many digits its tally has, so a band holding thousands
    // of calls reads taller than one holding three while the column stays as wide
    // as it was - a shape that can be compared between two captures rather than
    // one that reflows with them. Capped at the widest digit for the same reason.
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
            return ReportFormats.ABSENT_CELL;
        }
        var bands = new StringBuilder(DurationBuckets.countBuckets());

        for (var index = 0; index < DurationBuckets.countBuckets(); index++) {
            bands.append(formatBandMark(buckets.getCallsInBucket(index)));
        }
        return bands.toString();
    }

    private static String formatCounterTotal(
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

    private static String scaledHeader(String header, boolean isPerFrame) {
        return isPerFrame ? header + PER_FRAME_SUFFIX : header;
    }

    /**
     * One column of the table: what it is headed, how narrow it may get, which
     * side its cells are padded on, and what it says about a row.
     *
     * <p>A column rather than a format string per row shape, because the counter
     * columns are not known until the capture is in hand and a row has to fill
     * whatever columns it turned out to need.
     */
    private static final class ReportColumn {

        private final String header;
        private final int floorWidth;
        private final boolean isTextColumn;
        private final BiFunction<ProfileReportRow, ReportScale, String> readCell;

        private ReportColumn(
                String header,
                int floorWidth,
                boolean isTextColumn,
                BiFunction<ProfileReportRow, ReportScale, String> readCell) {

            this.header = header;
            this.floorWidth = floorWidth;
            this.isTextColumn = isTextColumn;
            this.readCell = readCell;
        }

        private String getHeader() {
            return header;
        }

        private TextTableColumn describeTableColumn() {

            return isTextColumn
                ? TextTableColumn.alignCellsLeft(floorWidth)
                : TextTableColumn.alignCellsRight(floorWidth);
        }

        private String renderCell(ProfileReportRow row, ReportScale scale) {
            return readCell.apply(row, scale);
        }
    }
}
