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
 * Pins what a block of a tooltip is: a run of lines that is never empty, whatever blocks nest inside it,
 * held as a value and read back flat in the order it draws. The floor matters because an empty block
 * would still be parted from its neighbours, leaving a gap on screen with no line in it and nothing to
 * say which caller left it there.
 *
 * <p>The line count is pinned as its own answer because the spacing rule reads it: whether a nested block
 * came to one line or to an account of its own is what decides whether the block after it stands clear.
 */
final class TooltipSectionTest {

    private static TooltipRow buildRow(String text) {
        return TooltipRow.createRow(new TextSpan(text, Color.WHITE));
    }

    @Nested
    class Constructor {
        @Test
        void constructorRejectsASectionWithNoLines() {
            assertThatThrownBy(() -> TooltipSection.createSection(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("openingRows");
        }

        @Test
        void constructorRejectsANullLineList() {
            assertThatThrownBy(() -> TooltipSection.createSection(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void constructorCopiesTheLinesItIsBuiltFrom() {
            // The block is a value, so a caller still holding the list it composed must not be able to
            // add a line to a block already handed to a layout - which would size a box for one set of
            // lines and then draw another.
            var rows = new ArrayList<TooltipRow>();
            rows.add(buildRow("The Hegemony"));

            var section = TooltipSection.createSection(rows);

            rows.add(buildRow("Tri-Tachyon"));

            assertThat(section.openingRows())
                .hasSize(1);
        }

        @Test
        void constructorCopiesTheMembersItIsBuiltFrom() {
            var members = new ArrayList<TooltipSection>();
            members.add(TooltipSection.createSection(List.of(buildRow("Jangala"))));

            var section = TooltipSection
                .createSection(List.of(buildRow("The Hegemony")))
                .nesting(members);

            members.add(TooltipSection.createSection(List.of(buildRow("Culann"))));

            assertThat(section.members())
                .hasSize(1);
        }
    }

    @Nested
    class CreateSection {

        @Test
        void createSectionNestsNothingInsideTheBlock() {
            // What most blocks are, and the state a caller gets without saying anything: a block that
            // groups something says so, rather than every other caller passing an empty list for it.
            assertThat(TooltipSection.createSection(List.of(buildRow("Claim:"))).members())
                .isEmpty();
        }
    }

    @Nested
    class Nesting {

        @Test
        void nestingHoldsThoseBlocksInsideThisOne() {

            var member = TooltipSection.createSection(List.of(buildRow("Jangala")));
            var section = TooltipSection
                .createSection(List.of(buildRow("The Hegemony")))
                .nesting(List.of(member));

            assertThat(section.members())
                .containsExactly(member);
        }

        @Test
        void nestingLeavesTheBlocksOwnLinesAsTheyWere() {

            var openingRow = buildRow("The Hegemony");
            var section = TooltipSection
                .createSection(List.of(openingRow))
                .nesting(List.of(TooltipSection.createSection(List.of(buildRow("Jangala")))));

            assertThat(section.openingRows())
                .containsExactly(openingRow);
        }
    }

    @Nested
    class CountLines {

        @Test
        void countLinesCountsABlocksOwnLines() {
            assertThat(TooltipSection
                    .createSection(List.of(buildRow("Claim:"), buildRow("The Hegemony")))
                    .countLines())
                .isEqualTo(2);
        }

        @Test
        void countLinesCountsEveryLineNestedBeneathIt() {
            // What the spacing rule reads, so it has to be the whole of what the block draws as: counted
            // one level down, a faction whose colonies each break down further would read as short and
            // the block after it would sit flush against its last term.
            var section = TooltipSection
                .createSection(List.of(buildRow("The Hegemony")))
                .nesting(List.of(TooltipSection
                    .createSection(List.of(buildRow("Jangala")))
                    .nesting(List.of(
                        TooltipSection.createSection(List.of(buildRow("Size"))),
                        TooltipSection.createSection(List.of(buildRow("Patrols")))))));

            assertThat(section.countLines())
                .isEqualTo(4);
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
                TooltipSection.createSection(List.of(firstRow, secondRow)),
                TooltipSection.createSection(List.of(thirdRow))));

            assertThat(rows)
                .containsExactly(firstRow, secondRow, thirdRow);
        }

        @Test
        void readRowsInOrderReadsANestedBlockUnderTheLinesThatOpenIt() {
            // Depth first, which is where a reader looks for a breakdown - and the order the layout
            // walked to space the lines, so an anchor and the row it belongs to cannot slip apart.
            var headingRow = buildRow("Dominated by:");
            var factionRow = buildRow("The Hegemony");
            var marketRow = buildRow("Jangala");
            var nextFactionRow = buildRow("Tri-Tachyon");

            var rows = TooltipSection.readRowsInOrder(List.of(TooltipSection
                .createSection(List.of(headingRow))
                .nesting(List.of(
                    TooltipSection
                        .createSection(List.of(factionRow))
                        .nesting(List.of(TooltipSection.createSection(List.of(marketRow)))),
                    TooltipSection.createSection(List.of(nextFactionRow))))));

            assertThat(rows)
                .containsExactly(headingRow, factionRow, marketRow, nextFactionRow);
        }

        @Test
        void readRowsInOrderReadsNoLinesFromNoBlocks() {
            assertThat(TooltipSection.readRowsInOrder(List.of()))
                .isEmpty();
        }
    }
}
