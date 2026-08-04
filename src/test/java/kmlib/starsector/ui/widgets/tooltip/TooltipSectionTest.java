package kmlib.starsector.ui.widgets.tooltip;

import kmlib.starsector.ui.text.TextSpan;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins what a block of a tooltip is: a run of lines that is never empty, held as a value, and read back
 * flat in the order it draws. The floor matters because an empty block would still be parted from its
 * neighbours, leaving a gap on screen with no line in it and nothing to say which caller left it there.
 */
final class TooltipSectionTest {

    private static TooltipRow buildRow(String text) {
        return TooltipRow.createRow(new TextSpan(text, Color.WHITE));
    }

    @Nested
    class Constructor {
        @Test
        void constructorRejectsASectionWithNoLines() {
            assertThatThrownBy(() -> new TooltipSection(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rows");
        }

        @Test
        void constructorRejectsANullLineList() {
            assertThatThrownBy(() -> new TooltipSection(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void constructorCopiesTheLinesItIsBuiltFrom() {
            // The block is a value, so a caller still holding the list it composed must not be able to
            // add a line to a block already handed to a layout - which would size a box for one set of
            // lines and then draw another.
            var rows = new ArrayList<TooltipRow>();
            rows.add(buildRow("The Hegemony"));

            var section = new TooltipSection(rows);

            rows.add(buildRow("Tri-Tachyon"));

            assertThat(section.rows())
                .hasSize(1);
        }
    }

    @Nested
    class ReadRowsInOrder {
        @Test
        void readRowsInOrderFlattensTheBlocksIntoOneRunOfLines() {
            // The order a renderer paints in: blocks top to bottom, each block's own lines in the order
            // it holds them, with nothing of the grouping left in the result.
            var firstRow = buildRow("Claim:");
            var secondRow = buildRow("The Hegemony");
            var thirdRow = buildRow("Contested by:");

            var rows = TooltipSection.readRowsInOrder(List.of(
                new TooltipSection(List.of(firstRow, secondRow)),
                new TooltipSection(List.of(thirdRow))));

            assertThat(rows)
                .containsExactly(firstRow, secondRow, thirdRow);
        }

        @Test
        void readRowsInOrderReadsNoLinesFromNoBlocks() {
            assertThat(TooltipSection.readRowsInOrder(List.of()))
                .isEmpty();
        }
    }
}
