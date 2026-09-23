package kmlib.testfixtures.starsector.spreadsheets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the reading every shipped-table suite in the family is built on.
 *
 * <p>Worth its own cases because a reading that drops too much passes everywhere. Each suite using it
 * walks what it is handed, so a separator rule that swallowed real rows, or a parse that shifted a
 * column on the quoted rows only, would leave every one of those suites green over less of the file
 * than it claims to cover - and the file is shipped data, so nothing else would notice.
 *
 * <p>The quoted-comma case is the one that matters most: it is exactly what the hand-rolled readings
 * this fixture replaced got wrong, and the shipped command table quotes two of its columns.
 */
final class ShippedSpreadsheetTest {

    private static final String ID_COLUMN = "id";

    private static Path writeSpreadsheet(Path directory, String contents) throws IOException {

        var spreadsheet = directory.resolve("table.csv");

        Files.writeString(spreadsheet, contents, StandardCharsets.UTF_8);

        return spreadsheet;
    }

    @Nested
    class ReadRowsByHeader {

        @Test
        void separatorRowsAreDropped(@TempDir Path directory) throws IOException {

            // The engine's tables space their sections with a row carrying no ID. It is furniture, not
            // data, and a suite counting rows would otherwise count the spacing.
            var spreadsheet = writeSpreadsheet(directory, """
                id,name
                first,One
                ,
                second,Two
                """);

            assertThat(ShippedSpreadsheet.readRowsByHeader(spreadsheet))
                .extracting(cells -> cells.get(ID_COLUMN))
                .containsExactly("first", "second");
        }

        @Test
        void aQuotedCommaStaysInsideItsCell(@TempDir Path directory) throws IOException {

            // The case a split on the comma gets wrong: every column after the quoted one shifts left,
            // and the reading keeps passing against the wrong cell.
            var spreadsheet = writeSpreadsheet(directory, """
                id,tags,help
                only,"a,b,c",Text
                """);

            assertThat(ShippedSpreadsheet.readRowsByHeader(spreadsheet))
                .singleElement()
                .satisfies(cells -> {
                    assertThat(cells.get("tags")).isEqualTo("a,b,c");
                    assertThat(cells.get("help")).isEqualTo("Text");
                });
        }

        @Test
        void aDoubledQuoteReadsAsOne(@TempDir Path directory) throws IOException {

            // Spelled as a concatenation rather than a text block: the cell ends on three quotes, which
            // inside a text block would close it.
            var spreadsheet = writeSpreadsheet(
                directory,
                "id,help\n"
                    + "only,\"He said \"\"no\"\"\"\n");

            assertThat(ShippedSpreadsheet.readRowsByHeader(spreadsheet))
                .singleElement()
                .satisfies(cells -> assertThat(cells.get("help")).isEqualTo("He said \"no\""));
        }

        @Test
        void aNewlineInsideAQuotedCellKeepsTheRowWhole(@TempDir Path directory) throws IOException {

            // A line-at-a-time reading would split this row in two and read the remainder as a row of
            // its own, which then fails the separator check and vanishes.
            var spreadsheet = writeSpreadsheet(directory, """
                id,help
                only,"first
                second"
                """);

            assertThat(ShippedSpreadsheet.readRowsByHeader(spreadsheet))
                .singleElement()
                .satisfies(cells -> assertThat(cells.get("help")).isEqualTo("first\nsecond"));
        }

        @Test
        void aNamedKeyColumnJudgesTheSeparator(@TempDir Path directory) throws IOException {

            // Not every shipped table keys on `id`; the command table keys on the command name, and
            // judging its rows by an `id` column it does not have would drop all of them.
            var spreadsheet = writeSpreadsheet(directory, """
                command,class
                kmlib_first,First
                ,
                kmlib_second,Second
                """);

            assertThat(ShippedSpreadsheet.readRowsByHeader(spreadsheet, "command"))
                .extracting(cells -> cells.get("command"))
                .containsExactly("kmlib_first", "kmlib_second");
        }
    }

    @Nested
    class ReadRowsById {

        @Test
        void rowsComeBackKeyedAndInFileOrder(@TempDir Path directory) throws IOException {

            var spreadsheet = writeSpreadsheet(directory, """
                id,name
                second,Two
                first,One
                """);

            assertThat(ShippedSpreadsheet.readRowsById(spreadsheet))
                .containsOnlyKeys("second", "first")
                .extractingByKey("first")
                .satisfies(cells -> assertThat(cells.get("name")).isEqualTo("One"));
        }

        @Test
        void aRepeatedIdIsWonByTheLaterRow(@TempDir Path directory) throws IOException {

            // What the engine's own loader does with a duplicated ID. Pinned so the reading is known to
            // match it, rather than the behaviour being whatever the map happened to do.
            var spreadsheet = writeSpreadsheet(directory, """
                id,name
                only,First
                only,Second
                """);

            assertThat(ShippedSpreadsheet.readRowsById(spreadsheet))
                .extractingByKey("only")
                .satisfies(cells -> assertThat(cells.get("name")).isEqualTo("Second"));
        }
    }

    @Nested
    class ListColumnValues {

        @Test
        void onlyTheRowsFillingTheColumnAreListed(@TempDir Path directory) throws IOException {

            var spreadsheet = writeSpreadsheet(directory, """
                group,note
                blueprints_low,first
                ,orphan
                weapons2,second
                """);

            assertThat(ShippedSpreadsheet.listColumnValues(spreadsheet, "group"))
                .containsExactly("blueprints_low", "weapons2");
        }
    }

    @Nested
    class ReadCells {

        @Test
        void everyLineComesBackInColumnOrder(@TempDir Path directory) throws IOException {

            // The positional reading judges no row: a caller reading by index owns that decision, so
            // the header line and a blank row both survive here where the keyed readings drop them.
            var spreadsheet = writeSpreadsheet(directory, """
                id,description
                first,"one, two"
                ,
                """);

            assertThat(ShippedSpreadsheet.readCells(spreadsheet))
                .containsExactly(
                    List.of("id", "description"),
                    List.of("first", "one, two"),
                    List.of("", ""));
        }

        @Test
        void aTableWithUnnamedColumnsStillReads(@TempDir Path directory) throws IOException {

            // LunaLib's settings table leaves column names blank, and a parser asked to key on that
            // header refuses the file. Reading it positionally is the whole reason this reading exists,
            // so the case that proves it belongs here rather than in the suite that discovered it.
            var spreadsheet = writeSpreadsheet(directory, """
                fieldID,,fieldName
                kmlib_level,,Level
                """);

            assertThat(ShippedSpreadsheet.readCells(spreadsheet))
                .containsExactly(
                    List.of("fieldID", "", "fieldName"),
                    List.of("kmlib_level", "", "Level"));
        }
    }

    @Nested
    class MissingFile {

        @Test
        void anUnreadableFileNamesItself(@TempDir Path directory) {

            // The file is shipped data, so a failed read means the reading is looking in the wrong
            // place. Naming the path is what turns that into a one-line fix.
            var missing = directory.resolve("absent.csv");

            assertThatThrownBy(() -> ShippedSpreadsheet.readRowsByHeader(missing))
                .hasMessageContaining("absent.csv");
        }
    }
}
