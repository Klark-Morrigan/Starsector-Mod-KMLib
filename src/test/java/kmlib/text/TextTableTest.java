package kmlib.text;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link TextTable}: a cell is padded into its column on the side the column states, a column
 * is never narrower than its floor and always wide enough for its widest cell, a line written
 * across the whole table is left exactly as it stands and widens no column, and a table nobody
 * wrote a row into renders nothing.
 */
final class TextTableTest {

    // A name column with no floor beside a number column that will not close below six, which is
    // the shape every table this serves is built from.
    private static final int NO_FLOOR = 0;
    private static final int NUMBER_COLUMN_FLOOR = 6;

    // Wider than that floor, so a case can tell a floor from a fixed width.
    private static final String OVERLONG_NUMBER = "1234567";

    @Nested
    class RenderAligned {

        @Test
        void padsTextOnTheRightAndNumbersOnTheLeft() {
            // What tells a name from a number at a glance: text reads from the left edge of its
            // column, while digits of the same magnitude only line up when the padding goes first.
            var table = new TextTable(List.of(
                TextTableColumn.alignCellsLeft(NO_FLOOR),
                TextTableColumn.alignCellsRight(NUMBER_COLUMN_FLOOR)));

            table.addRow(List.of("name", "1"));

            assertThat(table.renderAligned()).isEqualTo("name       1");
        }

        @Test
        void widensAColumnPastItsFloorForACellThatDoesNotFit() {
            // A floor is how narrow a column may get, not how wide it may be: a number cut to fit
            // its column would be a different number.
            var table = new TextTable(List.of(TextTableColumn.alignCellsRight(NUMBER_COLUMN_FLOOR)));

            table.addRow(List.of("1"));
            table.addRow(List.of(OVERLONG_NUMBER));

            assertThat(table.renderAligned().lines())
                .containsExactly("      1", OVERLONG_NUMBER);
        }

        @Test
        void keepsAColumnAtItsFloorWhereEveryCellIsNarrower() {
            // What the floor is for: a capture of small numbers renders its columns where the
            // capture before it put them, so the two can be read against each other by eye.
            var table = new TextTable(List.of(TextTableColumn.alignCellsRight(NUMBER_COLUMN_FLOOR)));

            table.addRow(List.of("1"));

            assertThat(table.renderAligned()).isEqualTo("     1");
        }

        @Test
        void writesASpanningLineExactlyAsItStands() {
            // It carries its own indent and belongs to no column, so padding it would only trail
            // spaces onto what a caller wrote.
            var table = new TextTable(List.of(
                TextTableColumn.alignCellsLeft(NO_FLOOR),
                TextTableColumn.alignCellsRight(NO_FLOOR)));

            table.addRow(List.of("a", "1"));
            table.addSpanningLine("  a note about the row above");

            assertThat(table.renderAligned().lines())
                .containsExactly("a  1", "  a note about the row above");
        }

        @Test
        void measuresNoColumnForASpanningLine() {
            // A line of a caller's own text would otherwise widen the first column by however long
            // that text was, and push every number away from the header it sits under.
            var table = new TextTable(List.of(
                TextTableColumn.alignCellsLeft(NO_FLOOR),
                TextTableColumn.alignCellsRight(NO_FLOOR)));

            table.addSpanningLine("a note far wider than any row of this table");
            table.addRow(List.of("a", "1"));

            assertThat(table.renderAligned().lines())
                .contains("a  1");
        }

        @Test
        void rendersNothingForATableNobodyWroteARowInto() {
            var table = new TextTable(List.of(TextTableColumn.alignCellsLeft(NO_FLOOR)));

            assertThat(table.renderAligned()).isEmpty();
        }
    }
}
