package kmlib.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A grid of text cells rendered into aligned, monospaced lines: rows of cells
 * padded into columns, and lines written across the whole width for what belongs
 * to no column.
 *
 * <p>Alignment is a job of its own, apart from what any one table is about: a
 * caller decides which columns there are and what each cell says, and this
 * decides where a column starts once every row is in hand. Two callers laying
 * the same figures out differently is what a shared table exists to stop.
 *
 * <p>A spanning line is how something said about the row above it is written -
 * free text, of whatever length the caller wrote, belonging to no column. It is
 * left out of the measuring on purpose: a column widened by a caller's own prose
 * would push every number away from the header it sits under.
 */
public final class TextTable {

    // Wide enough that two columns cannot be read as one number, narrow enough
    // that a table with a column group per counter still fits a console line.
    private static final String COLUMN_GAP = "  ";

    private static final String LINE_BREAK = "\n";

    private final List<TextTableColumn> columns;
    private final List<TableLine> lines = new ArrayList<>();

    /**
     * @param columns what the table's cells are padded into, left to right
     */
    public TextTable(List<TextTableColumn> columns) {
        this.columns = List.copyOf(columns);
    }

    /**
     * Adds a row of cells, one per column in the order the columns were given.
     *
     * @param cells what the row says in each column
     */
    public void addRow(List<String> cells) {
        lines.add(new TableLine(List.copyOf(cells), false));
    }

    /**
     * Adds a line written across the whole table rather than into its columns -
     * a heading, or something said about the row above it.
     *
     * <p>Written exactly as it stands, its own indent included, and measured for
     * no column.
     *
     * @param line what the line says
     */
    public void addSpanningLine(String line) {
        lines.add(new TableLine(List.of(line), true));
    }

    /**
     * @return the table as text, one line per row, each cell padded into its
     *         column
     */
    public String renderAligned() {

        var widths = measureColumnWidths();
        var rendered = new StringBuilder();

        for (var line : lines) {

            if (rendered.length() > 0) {
                rendered.append(LINE_BREAK);
            }
            if (line.isSpanning()) {
                rendered.append(line.getCells().get(0));
                continue;
            }
            appendPaddedCells(rendered, line.getCells(), widths);
        }
        return rendered.toString();
    }

    private void appendPaddedCells(StringBuilder rendered, List<String> cells, int[] widths) {

        for (var index = 0; index < cells.size(); index++) {

            if (index > 0) {
                rendered.append(COLUMN_GAP);
            }
            rendered.append(padCell(cells.get(index), widths[index], index));
        }
    }

    // The widest cell in each column, floors included, so every row agrees on
    // where a column starts - a table whose rows disagree cannot be read down.
    private int[] measureColumnWidths() {

        var widths = new int[columns.size()];

        for (var index = 0; index < columns.size(); index++) {
            widths[index] = columns.get(index).getFloorWidth();
        }
        for (var line : lines) {

            if (line.isSpanning()) {
                continue;
            }
            var cells = line.getCells();

            for (var index = 0; index < cells.size() && index < widths.length; index++) {
                widths[index] = Math.max(widths[index], cells.get(index).length());
            }
        }
        return widths;
    }

    // A cell past the declared columns is written as it stands rather than
    // dropped: a row carrying more than it was told to is the caller's mistake,
    // and losing what it said would hide it.
    private String padCell(String cell, int width, int index) {

        if (index >= columns.size()) {
            return cell;
        }
        var alignment = columns.get(index).isPaddedOnTheLeft() ? "%" : "%-";

        return String.format(Locale.ROOT, alignment + width + "s", cell);
    }

    /**
     * One line of the table: either a cell per column, or one piece of text
     * written across all of them.
     *
     * <p>Which of the two it is, said rather than inferred from how many cells it
     * holds - a table of one column would otherwise have every row read as
     * spanning.
     */
    private static final class TableLine {

        private final List<String> cells;
        private final boolean isSpanning;

        private TableLine(List<String> cells, boolean isSpanning) {
            this.cells = cells;
            this.isSpanning = isSpanning;
        }

        private List<String> getCells() {
            return cells;
        }

        private boolean isSpanning() {
            return isSpanning;
        }
    }
}
