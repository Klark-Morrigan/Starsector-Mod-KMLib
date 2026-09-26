package kmlib.starsector.spreadsheets;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reads the rows of a spreadsheet the game has merged, in the shape the
 * {@code getMergedSpreadsheetData} family on {@code SettingsAPI} hands them back.
 *
 * <p>The game's tables carry two kinds of row that are not data: a blank ID, which authors use as
 * a visual separator, and an ID starting with {@code #}, which comments the row out. The merger
 * hands both back like any other row, so a reader that does not recognise them either fails on the
 * first typed read of a row that was never data or files a comment under its own text as though
 * that were an ID. Reading through here leaves them out in one place.
 *
 * <p>Takes the rows rather than opening the file, because what a failed read costs differs by
 * caller: a mod whose definitions are the file cannot load without it, while one only consulting a
 * file goes on without it. The read and its failure stay with the caller.
 */
public final class SpreadsheetRows {

    // Marks a row the author wrote for the reader rather than for the game.
    private static final String COMMENT_PREFIX = "#";

    // The separator inside a list-valued cell. A cell holding one is quoted in the file, which is
    // the file format's concern: the merger hands the cell over as one string.
    private static final String LIST_SEPARATOR = ",";

    // Keeps the empty entries a trailing separator leaves, which String.split drops by default.
    private static final int KEEP_TRAILING_EMPTY_ENTRIES = -1;

    private SpreadsheetRows() {
    }

    /**
     * The rows of a merged spreadsheet that carry data, in the order the game handed them back.
     *
     * <p>Leaves out rows whose ID is blank or starts with {@code #}, and entries that are not rows
     * at all. The game never hands back the last kind, but an array assembled by hand can hold
     * anything, and one stray entry is not worth its neighbours.
     *
     * @param rows     the merged rows
     * @param idColumn the column the spreadsheet was merged on
     * @return the data rows, unmodifiable
     */
    public static List<JSONObject> readDataRows(JSONArray rows, String idColumn) {

        var dataRows = new ArrayList<JSONObject>(rows.length());

        for (var rowIndex = 0; rowIndex < rows.length(); rowIndex++) {

            var row = rows.optJSONObject(rowIndex);

            if (row != null && isDataRow(row, idColumn)) {
                dataRows.add(row);
            }
        }
        return List.copyOf(dataRows);
    }

    /**
     * The entries of a list-valued cell, split on commas and trimmed.
     *
     * <p>An empty entry between two separators, or after a trailing one, is kept rather than
     * dropped, so a caller pairing two list columns by position still sees the gap its author left.
     *
     * @param row    a row of the spreadsheet
     * @param column the list-valued column
     * @return the entries, empty where the cell is blank or the row has no such column; unmodifiable
     */
    public static List<String> splitListCell(JSONObject row, String column) {

        var cell = row
            .optString(column, "")
            .trim();

        if (cell.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(cell.split(LIST_SEPARATOR, KEEP_TRAILING_EMPTY_ENTRIES))
            .map(String::trim)
            .toList();
    }

    private static boolean isDataRow(JSONObject row, String idColumn) {

        var id = row
            .optString(idColumn, "")
            .trim();

        return !id.isEmpty()
            && !id.startsWith(COMMENT_PREFIX);
    }
}
