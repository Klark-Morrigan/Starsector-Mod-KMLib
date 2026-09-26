package kmlib.starsector.spreadsheets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins which merged rows count as data - blank-ID and {@code #}-comment rows out, and anything the
 * array holds that is not a row - and how a list-valued cell splits: on commas, trimmed, blank as
 * no entries, and with an empty entry kept where its author left a gap.
 */
final class SpreadsheetRowsTest {

    private static final String ID_COLUMN = "id";

    private static final String LIST_COLUMN = "conditions";

    @Nested
    class ReadDataRows {

        @Test
        void keepsDataRowsInTheOrderTheyWereHandedBack() throws JSONException {

            var rows = buildRows(buildRow("b"), buildRow("a"), buildRow("c"));

            assertThat(SpreadsheetRows.readDataRows(rows, ID_COLUMN))
                .extracting(row -> row.optString(ID_COLUMN))
                .containsExactly("b", "a", "c");
        }

        @Test
        void skipsARowWithABlankId() throws JSONException {

            var rows = buildRows(buildRow(""), buildRow("   "), buildRow("kept"));

            assertThat(SpreadsheetRows.readDataRows(rows, ID_COLUMN))
                .extracting(row -> row.optString(ID_COLUMN))
                .containsExactly("kept");
        }

        @Test
        void skipsARowWithoutTheIdColumn() throws JSONException {

            var rows = buildRows(new JSONObject(), buildRow("kept"));

            assertThat(SpreadsheetRows.readDataRows(rows, ID_COLUMN))
                .extracting(row -> row.optString(ID_COLUMN))
                .containsExactly("kept");
        }

        @Test
        void skipsACommentRow() throws JSONException {

            var rows = buildRows(buildRow("#commented_out"), buildRow("  # indented"), buildRow("kept"));

            assertThat(SpreadsheetRows.readDataRows(rows, ID_COLUMN))
                .extracting(row -> row.optString(ID_COLUMN))
                .containsExactly("kept");
        }

        @Test
        void keepsARowWhoseIdHoldsAHashPastItsFirstCharacter() throws JSONException {

            var rows = buildRows(buildRow("tier#2"));

            assertThat(SpreadsheetRows.readDataRows(rows, ID_COLUMN))
                .extracting(row -> row.optString(ID_COLUMN))
                .containsExactly("tier#2");
        }

        @Test
        void skipsAnEntryThatIsNotARow() throws JSONException {

            var rows = new JSONArray();
            rows.put("not a row");
            rows.put(buildRow("kept"));

            assertThat(SpreadsheetRows.readDataRows(rows, ID_COLUMN))
                .extracting(row -> row.optString(ID_COLUMN))
                .containsExactly("kept");
        }

        @Test
        void readsTheIdOutOfTheColumnItIsGiven() throws JSONException {

            var row = new JSONObject();
            row.put("faction", "data/world/factions/hegemony.faction");

            assertThat(SpreadsheetRows.readDataRows(buildRows(row), "faction"))
                .containsExactly(row);
        }
    }

    @Nested
    class SplitListCell {

        @Test
        void splitsOnCommasAndTrimsEachEntry() throws JSONException {

            var row = buildListRow(" ore_sparse , ore_moderate,ore_abundant ");

            assertThat(SpreadsheetRows.splitListCell(row, LIST_COLUMN))
                .containsExactly("ore_sparse", "ore_moderate", "ore_abundant");
        }

        @Test
        void readsASingleValueAsOneEntry() throws JSONException {

            var row = buildListRow("ore_sparse");

            assertThat(SpreadsheetRows.splitListCell(row, LIST_COLUMN))
                .containsExactly("ore_sparse");
        }

        @Test
        void readsABlankCellAsNoEntries() throws JSONException {

            var row = buildListRow("   ");

            assertThat(SpreadsheetRows.splitListCell(row, LIST_COLUMN))
                .isEmpty();
        }

        @Test
        void readsAnAbsentColumnAsNoEntries() {

            assertThat(SpreadsheetRows.splitListCell(new JSONObject(), LIST_COLUMN))
                .isEmpty();
        }

        @Test
        void keepsAnEmptyEntryWhereItsAuthorLeftAGap() throws JSONException {

            var row = buildListRow("1,,3,");

            assertThat(SpreadsheetRows.splitListCell(row, LIST_COLUMN))
                .containsExactly("1", "", "3", "");
        }
    }

    // A row as the merger hands it over: every cell a string.
    private static JSONObject buildRow(String id) throws JSONException {

        var row = new JSONObject();
        row.put(ID_COLUMN, id);
        return row;
    }

    private static JSONObject buildListRow(String cell) throws JSONException {

        var row = buildRow("listed");
        row.put(LIST_COLUMN, cell);
        return row;
    }

    private static JSONArray buildRows(JSONObject... rows) {

        var array = new JSONArray();

        for (var row : rows) {
            array.put(row);
        }
        return array;
    }
}
