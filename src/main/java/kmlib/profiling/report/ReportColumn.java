package kmlib.profiling.report;

import kmlib.text.TextTableColumn;

import java.util.function.BiFunction;

/**
 * One column of a report: what it is headed, how it is laid out, and what it
 * says about a row.
 *
 * <p>A column rather than a format string per row shape, because the counter
 * columns are not known until the capture is in hand and a row has to fill
 * whatever columns it turned out to need.
 *
 * <p>Named by what it holds - text or numbers - rather than built with a flag,
 * so a caller reads what a column is at the point it is declared rather than
 * counting arguments to find out.
 */
final class ReportColumn {

    private final String header;
    private final TextTableColumn tableColumn;
    private final BiFunction<ProfileReportRow, ReportScale, String> readCell;

    private ReportColumn(
            String header,
            TextTableColumn tableColumn,
            BiFunction<ProfileReportRow, ReportScale, String> readCell) {

        this.header = header;
        this.tableColumn = tableColumn;
        this.readCell = readCell;
    }

    /**
     * A column of text, which reads from its left edge.
     *
     * @param header     what the column is called
     * @param floorWidth how narrow it may get, whatever its cells hold
     * @param readCell   what it says about a row, at that origin's scale
     * @return the column so described
     */
    static ReportColumn describeTextColumn(
            String header,
            int floorWidth,
            BiFunction<ProfileReportRow, ReportScale, String> readCell) {

        return new ReportColumn(header, TextTableColumn.alignCellsLeft(floorWidth), readCell);
    }

    /**
     * A column of numbers, which end at its right edge so digits of the same
     * magnitude line up.
     *
     * @param header     what the column is called
     * @param floorWidth how narrow it may get, whatever its cells hold
     * @param readCell   what it says about a row, at that origin's scale
     * @return the column so described
     */
    static ReportColumn describeNumberColumn(
            String header,
            int floorWidth,
            BiFunction<ProfileReportRow, ReportScale, String> readCell) {

        return new ReportColumn(header, TextTableColumn.alignCellsRight(floorWidth), readCell);
    }

    String getHeader() {
        return header;
    }

    TextTableColumn getTableColumn() {
        return tableColumn;
    }

    String renderCell(ProfileReportRow row, ReportScale scale) {
        return readCell.apply(row, scale);
    }
}
