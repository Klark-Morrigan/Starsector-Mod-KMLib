package kmlib.testfixtures.starsector.spreadsheets;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The CSVs a mod ships, read the way the engine reads them, for the suites that pin a row against the
 * code naming it.
 *
 * <p>Here rather than in each mod's suite because the read is the same read every time - every KM mod
 * ships several tables, the engine's own loader runs inside the game classloader and cannot be reached
 * from a suite, and what a suite needs back is one of three shapes. The part that can be subtly wrong
 * is the parse: a shipped table quotes the fields carrying commas, and a reading that splits on the
 * comma instead lands one column left of what it meant to read, on exactly the rows that quote. It
 * keeps passing, against the wrong cell.
 *
 * <p>Parsed rather than split by hand, so the quoting rules the shipped files actually use - quoted
 * commas, doubled {@code ""} escapes, a newline inside a quoted field - are the parser's business and
 * not each caller's. Cells are trimmed, matching what the hand-rolled readings did and what the
 * authored files assume.
 *
 * <p>Blank rows are furniture. The engine's tables use a row with no ID as a visual separator, so the
 * keyed readings drop them; {@link #readCells} keeps them, because a caller reading by column position
 * is reading the file as it lies and judges its own rows.
 *
 * <p>Not every shipped table can be read by header name. LunaLib's settings table leaves several of
 * its column names blank, and a parser asked to key on that header refuses the file rather than
 * guessing - so that one is read positionally, which is why {@link #readCells} exists alongside the
 * keyed readings rather than beneath them.
 *
 * <p>Deliberately no JSON reading here. A mod whose own table parser takes the engine's row-object
 * shape builds it from {@link #readRowsByHeader} in its own suite, where its own org.json is: the game
 * ships one whose {@code JSONException} is checked, and a mod on a current org.json has one that is
 * not, so a reading spelled here would compile against the wrong one for somebody.
 */
public final class ShippedSpreadsheet {

    /** The column the engine's own tables key their rows by, and what the keyed readings assume. */
    public static final String ID_COLUMN = "id";

    // A shipped file is named as a Path rather than a File, matching the sibling fixtures that
    // already read one - ShippedStrings and LunaSettingsTable. One spelling, so nothing has to
    // convert at the boundary: the read below wants a Path either way.

    private ShippedSpreadsheet() {
        // fixture of static readers, no instances.
    }

    /**
     * Every value in one column, in file order, skipping the rows leaving it blank.
     *
     * @param spreadsheet the shipped CSV to read
     * @param column      the header name of the column to read
     * @return that column's values, one per row that fills it
     */
    public static List<String> listColumnValues(Path spreadsheet, String column) {

        var values = new ArrayList<String>();

        for (var cells : readRowsByHeader(spreadsheet, column)) {
            values.add(cells.get(column));
        }
        return values;
    }

    /**
     * Every line as its cells, in file order and in column order, with nothing dropped and the header
     * line among them - for a caller reading the file by column position rather than by header name.
     *
     * <p>The header is not read as one here, which is the point of the reading as much as the column
     * order is: LunaLib's settings table leaves several of its column names blank, and a parser asked
     * to key on that header refuses the file outright.
     *
     * @param spreadsheet the shipped CSV to read
     * @return one cell list per line
     */
    public static List<List<String>> readCells(Path spreadsheet) {

        var rows = new ArrayList<List<String>>();

        for (var record : readRecords(spreadsheet, CSVFormat.DEFAULT.builder().setTrim(true))) {
            rows.add(record.toList());
        }
        return rows;
    }

    /**
     * The data rows as cells by header name, in file order, with the separator rows dropped.
     *
     * @param spreadsheet the shipped CSV to read
     * @return one cell map per data row
     */
    public static List<Map<String, String>> readRowsByHeader(Path spreadsheet) {

        return readRowsByHeader(spreadsheet, ID_COLUMN);
    }

    /**
     * As above, judging a separator row by a named column rather than by {@code id}.
     *
     * @param spreadsheet the shipped CSV to read
     * @param keyColumn   the header name whose blankness marks a row as furniture
     * @return one cell map per data row
     */
    public static List<Map<String, String>> readRowsByHeader(Path spreadsheet, String keyColumn) {

        var rows = new ArrayList<Map<String, String>>();

        var format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true);

        for (var record : readRecords(spreadsheet, format)) {

            var cells = record.toMap();
            var key = cells.get(keyColumn);

            if (key != null && !key.isBlank()) {
                rows.add(cells);
            }
        }
        return rows;
    }

    /**
     * The data rows keyed by their ID column, so a suite can assert on one row directly rather than
     * searching the file for it. A later row under an ID already read replaces the earlier one, which
     * is what the engine's own loader does with a duplicated ID.
     *
     * @param spreadsheet the shipped CSV to read
     * @return each data row's cells by header name, keyed by the row's ID, in file order
     */
    public static Map<String, Map<String, String>> readRowsById(Path spreadsheet) {

        return readRowsById(spreadsheet, ID_COLUMN);
    }

    /**
     * As above, keyed by a named column, for the tables the engine keys by something other than
     * {@code id} - a command table by its command name, an override table by the type it overrides.
     *
     * @param spreadsheet the shipped CSV to read
     * @param keyColumn   the header name of the column identifying a row
     * @return each data row's cells by header name, keyed by that column, in file order
     */
    public static Map<String, Map<String, String>> readRowsById(Path spreadsheet, String keyColumn) {

        var rowsById = new LinkedHashMap<String, Map<String, String>>();

        for (var cells : readRowsByHeader(spreadsheet, keyColumn)) {
            rowsById.put(cells.get(keyColumn), cells);
        }
        return rowsById;
    }

    // Opens the file once and reads every record out of it, so the character set and the failure
    // wording are stated here rather than at each reading. The dialect comes from the caller, because
    // the two readings want different things of the header line. Records outlive the reader, so the
    // file is closed before any caller walks them.
    private static List<CSVRecord> readRecords(Path spreadsheet, CSVFormat.Builder dialect) {

        try (Reader reader = Files.newBufferedReader(spreadsheet, StandardCharsets.UTF_8)) {

            return dialect.build().parse(reader).getRecords();
        } catch (IOException unreadable) {

            // Surfaced rather than swallowed: the file is shipped data, so a read failure means the
            // reading is looking in the wrong place, not that the table is fine.
            throw new UncheckedIOException(
                "Could not read " + spreadsheet.toAbsolutePath(),
                unreadable);
        }
    }
}
