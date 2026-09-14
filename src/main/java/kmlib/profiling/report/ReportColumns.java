package kmlib.profiling.report;

import kmlib.profiling.snapshot.DurationBuckets;
import kmlib.profiling.snapshot.ProfileNode;
import kmlib.profiling.snapshot.ProfileTiming;
import kmlib.text.TextTableColumn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Which columns a capture has, and what each of them says about a row.
 *
 * <p>The timing columns are always there and the counter groups
 * ({@link CounterColumns}) are whatever the capture happened to count, so the
 * set is decided once from the whole capture: the groups are shared across its
 * origins, and a column present for one game only would leave the other's rows
 * unreadable against it. What each origin's totals are divided by is still its
 * own, so a cell is asked for the scale of the group it is being written in.
 *
 * <p>The last column is where the row's calls fell in duration: one character
 * per band, the bands doubling from a microsecond up, a dot where no call landed
 * and otherwise how many digits that band's tally has. A row whose calls sit in
 * one band costs what it costs; a row with an outlier band stalled, and the two
 * want different fixes.
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

    // Floors, not fixed widths: a column is widened by anything that does not
    // fit, and these keep the timing columns from closing up around small
    // numbers, where a table that reflows between two captures cannot be compared
    // against the one before it by eye. The section column takes no floor - how
    // long a section is named is the caller's, not this table's.
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
     * @param shownRows  every row the reading writes, across all of its origins,
     *                   so every group shares one set of columns and no column is
     *                   raised for a counter none of them fills
     * @param isPerFrame whether the totals are being divided by a frame count,
     *                   which the headers of the divided columns say
     * @return the columns those rows need, left to right
     */
    static ReportColumns buildColumns(List<ProfileReportRow> shownRows, boolean isPerFrame) {

        var columns = new ArrayList<ReportColumn>();

        columns.add(ReportColumn.describeTextColumn(
            SECTION_HEADER,
            NO_COLUMN_FLOOR,
            (row, scale) -> ReportFormats.indentToDepth(row.getDepth()) + row.getName()));

        columns.add(ReportColumn.describeNumberColumn(
            ReportFormats.markPerFrame(COUNT_HEADER, isPerFrame),
            CALL_COUNT_COLUMN_FLOOR,
            (row, scale) -> scale.formatCount(row.getNode().getTiming().getCount())));

        columns.add(buildCallDurationColumn(AVERAGE_HEADER, ProfileTiming::getAverageNanos));
        columns.add(buildCallDurationColumn(MINIMUM_HEADER, ProfileTiming::getMinNanos));
        columns.add(buildCallDurationColumn(MAXIMUM_HEADER, ProfileTiming::getMaxNanos));

        columns.add(ReportColumn.describeNumberColumn(
            ReportFormats.markPerFrame(SELF_HEADER, isPerFrame),
            DURATION_COLUMN_FLOOR,
            (row, scale) -> scale.formatMillis(row.getNode().getSelfNanos())));

        columns.add(ReportColumn.describeNumberColumn(
            ReportFormats.markPerFrame(TOTAL_HEADER, isPerFrame),
            TOTAL_COLUMN_FLOOR,
            (row, scale) -> scale.formatMillis(row.getNode().getTiming().getTotalNanos())));

        columns.add(ReportColumn.describeNumberColumn(
            SPREAD_HEADER,
            SPREAD_COLUMN_FLOOR,
            (row, scale) -> formatBands(row.getNode())));

        columns.addAll(CounterColumns.buildColumnsForEveryCounter(shownRows, isPerFrame));
        return new ReportColumns(columns);
    }

    List<TextTableColumn> describeTableColumns() {

        var described = new ArrayList<TextTableColumn>(columns.size());

        for (var column : columns) {
            described.add(column.getTableColumn());
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

    // A minimum, a maximum and an average are per call already, so no frame count
    // divides them: a call is not a frame, and a beat that ran twice in one would
    // report half of what one call took.
    private static ReportColumn buildCallDurationColumn(
            String header,
            ToLongFunction<ProfileTiming> readNanos) {

        return ReportColumn.describeNumberColumn(
            header,
            DURATION_COLUMN_FLOOR,
            (row, scale) ->
                ReportFormats.formatNanosAsMillis(readNanos.applyAsLong(row.getNode().getTiming())));
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
}
