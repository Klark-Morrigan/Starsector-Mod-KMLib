package kmlib.starsector.ui.widgets.tooltip;

import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.text.TextAlignment;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.text.TextStyle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins how a box's blocks come to a run of lines: the order they flatten in, the look each line resolves
 * to at the depth it stands, and - the substance of it - how much room stands above each of them. Two
 * lines of one block sit a line gap apart, resolved from the tier of the line above the gap; two blocks
 * stand the style's break apart; and a block nested inside one takes the narrower group break only where
 * the nested block before it broke down into more than a line. The box's first line takes nothing at all.
 *
 * <p>Asserted on the run itself rather than on a laid-out box, since the room above a line is settled
 * before anything is placed and needs no cursor, no bound, and no way to measure text to ask about. That
 * the placement then steps down by exactly these amounts is {@code CursorTooltipTest}'s.
 *
 * <p>Every gap here is a number the styles below state outright, so a measured gap says which rule
 * resolved it rather than which arithmetic happened to agree.
 */
class TooltipRowStackTest {

    private static final StarsectorFont BODY_FONT = StarsectorFont.VANILLA_INSIGNIA_15;
    private static final StarsectorFont HEADING_FONT = StarsectorFont.VANILLA_ORBITRON_20AA;
    private static final double BODY_LINE_HEIGHT = 15d;
    private static final double HEADING_LINE_HEIGHT = 20d;

    private static final float TOLERANCE = 0.001f;

    // How far apart the styles below part two blocks, and two blocks nested inside one. Restated rather
    // than read off the style so each case names the number it is actually spending.
    private static final float SECTION_BREAK = 11.5f;
    private static final float GROUP_BREAK = 5.75f;

    // The gap two lines of one block sit at where the box holds no tier apart, restated for the same
    // reason.
    private static final float LINE_GAP = 4f;

    // What the box's first line takes above itself, its own padding already standing there.
    private static final float NO_LEADING_GAP = 0f;

    // A tier the tiered style below holds apart from the rest, and the gap it holds it at: a step no
    // other tier resolves, so a measured gap says which line's tier it was read from. The level above it
    // is named too, since a run is only tightened once the line introducing it has been passed.
    private static final int TIGHTENED_LEVEL = 2;
    private static final int LEVEL_ABOVE_THE_TIGHTENED_RUN = 1;
    private static final float TIGHTENED_GAP = 1f;

    // Where the room above a line is read from: the line it stands above, by position in the run. Named
    // per case rather than passed as bare indexes, since which line a gap belongs to is the whole claim.
    private static final int FIRST_LINE_OF_THE_BOX = 0;
    private static final int SECOND_LINE_OF_THE_BOX = 1;
    private static final int THIRD_LINE_OF_THE_BOX = 2;

    // A box whose headings and body lines look alike, and one where they differ in both face and size.
    // The uniform style is what every case not about per-row looks stacks through, so those cases read as
    // the plain spacing arithmetic they are testing rather than as typography.
    private static final TooltipStyle UNIFORM_STYLE = TooltipStyle.createStyle(
        createStyle(BODY_FONT, BODY_LINE_HEIGHT),
        createStyle(BODY_FONT, BODY_LINE_HEIGHT));

    private static final TooltipStyle TWO_FACE_STYLE = TooltipStyle.createStyle(
        createStyle(HEADING_FONT, HEADING_LINE_HEIGHT),
        createStyle(BODY_FONT, BODY_LINE_HEIGHT));

    // The uniform box with one tier of its lines closed up: everything else still resolves the plain gap,
    // which is what lets the cases below say whether a gap was resolved from the line above it or from
    // the line below it.
    private static final TooltipStyle TIERED_STYLE = UNIFORM_STYLE.stackedAt(
        TooltipLineGaps.createGaps(LINE_GAP).gappedAtLevel(TIGHTENED_LEVEL, TIGHTENED_GAP));

    // The uniform box demoting each step under its own voice by two units, and a line standing one such
    // step under it. A size no other style here resolves, so a measured height says the shrink was spent
    // on the line rather than the line being stacked at its kind's own size.
    private static final float LEVEL_SHRINK = 2f;
    private static final int ONE_STEP_UNDER = 1;
    private static final TooltipStyle SHRINKING_STYLE = UNIFORM_STYLE.shrunkPerLevel(LEVEL_SHRINK);

    // Three plain body lines, told apart by their labels alone: nothing a line carries beyond its kind
    // and its depth has any say in what stands above it.
    private static final TooltipRow.TableRow FIRST_LINE = createCrestlessRow("AA");
    private static final TooltipRow.TableRow SECOND_LINE = createCrestlessRow("BB");
    private static final TooltipRow.TableRow THIRD_LINE = createCrestlessRow("CC");

    private static TextStyle createStyle(StarsectorFont font, double size) {
        // Built by hand rather than through TextStyle's own factory: its baseline colour resolves from
        // the running game's palette, which a spacing test has no business standing up for a value it
        // never reads.
        return new TextStyle(
            new TextFace(font, size),
            Color.WHITE,
            TextAlignment.TOP_LEFT,
            false);
    }

    private static TooltipRow.TableRow createCrestlessRow(String text) {
        return TooltipRow.createRow(new TextSpan(text, Color.WHITE));
    }

    // A body line standing a stated number of steps under the box's voice - the fact a per-tier gap is
    // resolved from.
    private static TooltipRow.TableRow createSubordinateRow(String text, int subordinationLevel) {
        return createCrestlessRow(text).subordinatedAt(subordinationLevel);
    }

    // The lines of one block, which is how a caller says they belong together.
    private static TooltipRowStack stackOneBlock(List<TooltipRow> rows, TooltipStyle style) {
        return TooltipRowStack.stackRows(List.of(TooltipSection.createSection(rows)), style);
    }

    // Two lines put in blocks of their own, which is the only way to say they are parted.
    private static List<TooltipSection> partIntoSections(TooltipRow firstRow, TooltipRow secondRow) {
        return List.of(
            TooltipSection.createSection(List.of(firstRow)),
            TooltipSection.createSection(List.of(secondRow)));
    }

    // One block holding a heading over two groups: the first breaking down into an account of its own,
    // the second a line on its own. The shape most cases below are read off, since it holds all three
    // boundaries at once - a block's lines to its first group, a group to its own account, and one group
    // to the next.
    //
    // Its three lines are handed in rather than fixed, since a case about what a tier resolves needs the
    // same shape built out of tiered lines - stated twice, the two copies would drift and a gap read off
    // one of them would no longer mean what the other's does.
    private static TooltipSection buildGroupedSection(
            TooltipRow headingRow,
            TooltipRow groupRow,
            TooltipRow detailRow) {

        return TooltipSection
            .createSection(List.of(headingRow))
            .nesting(List.of(
                TooltipSection
                    .createSection(List.of(groupRow))
                    .nesting(List.of(TooltipSection.createSection(List.of(detailRow)))),
                TooltipSection.createSection(List.of(groupRow))));
    }

    // The room standing above one line of the run. Read through a helper so a case names the line it is
    // asking about rather than the two hops it takes to reach one.
    private static float readLeadingGap(TooltipRowStack rowStack, int lineIndex) {
        return rowStack.readStackedRows().get(lineIndex).leadingGap();
    }

    // The size one line of the run draws at, which is also the height it stacks at.
    private static double readLineSize(TooltipRowStack rowStack, int lineIndex) {
        return rowStack.readStackedRows().get(lineIndex).textStyle().face().size();
    }

    @Nested
    class StackRows {

        @Test
        void readsTheLinesOfEveryBlockInDrawOrder() {
            // Depth first: a block's own lines, then everything nested in it, before the next block. What
            // the room above each line is then resolved against, so an order settled here differently
            // from the one drawn would space every listing by the wrong neighbours.
            var rowStack = TooltipRowStack.stackRows(
                List.of(buildGroupedSection(FIRST_LINE, SECOND_LINE, THIRD_LINE)),
                UNIFORM_STYLE);

            assertThat(rowStack.readStackedRows())
                .extracting(TooltipRowStack.StackedRow::row)
                .containsExactly(FIRST_LINE, SECOND_LINE, THIRD_LINE, SECOND_LINE);
        }

        @Test
        void resolvesEachLinesLookFromItsKindAndTheDepthItStandsAt() {
            // The one resolution there is: a line measured at one size and painted at another overlaps
            // its own words, and resolving the look here is what leaves nothing downstream to resolve it
            // a second time from.
            var headedStack = stackOneBlock(
                List.of(createCrestlessRow("AA").readsAs(TooltipLineStyle.HEADER), SECOND_LINE),
                TWO_FACE_STYLE);

            var demotedStack = stackOneBlock(
                List.of(FIRST_LINE, createSubordinateRow("BB", ONE_STEP_UNDER)),
                SHRINKING_STYLE);

            assertThat(readLineSize(headedStack, FIRST_LINE_OF_THE_BOX))
                .isCloseTo(20d, within(0.001d));
            assertThat(readLineSize(headedStack, SECOND_LINE_OF_THE_BOX))
                .isCloseTo(15d, within(0.001d));
            assertThat(readLineSize(demotedStack, SECOND_LINE_OF_THE_BOX))
                .isCloseTo(13d, within(0.001d));
        }

        @Test
        void spendsNoRoomAboveTheBoxsFirstLine() {
            // Nothing to part from - the box's own padding already sits above it - so the break is
            // dropped rather than padding the top edge unevenly against every other side.
            var rowStack = TooltipRowStack.stackRows(
                partIntoSections(FIRST_LINE, SECOND_LINE),
                UNIFORM_STYLE);

            assertThat(readLeadingGap(rowStack, FIRST_LINE_OF_THE_BOX))
                .isCloseTo(NO_LEADING_GAP, within(TOLERANCE));
        }

        @Test
        void partsTwoLinesOfOneBlockByTheLineGapAndTwoBlocksByTheStylesBreak() {
            // The two partings side by side, which is the whole of what the grouping buys: lines that
            // belong together sit closer than lines that do not.
            var withinBlock = stackOneBlock(List.of(FIRST_LINE, SECOND_LINE), UNIFORM_STYLE);
            var acrossBlocks = TooltipRowStack.stackRows(
                partIntoSections(FIRST_LINE, SECOND_LINE),
                UNIFORM_STYLE);

            assertThat(readLeadingGap(withinBlock, SECOND_LINE_OF_THE_BOX))
                .isCloseTo(LINE_GAP, within(TOLERANCE));
            assertThat(readLeadingGap(acrossBlocks, SECOND_LINE_OF_THE_BOX))
                .isCloseTo(SECTION_BREAK, within(TOLERANCE));
        }

        @Test
        void partsTwoLinesOfABlockByTheLineGapHoweverManyBlocksStandAboveIt() {
            // The remaining branch of the rule, and the one a box actually spends most: a line that is
            // neither the box's first nor its own block's first takes the plain gap wherever it sits. A
            // break reaching it would part a block from itself, which no case above could catch - every
            // block past the first one they stack holds a single line.
            var rowStack = TooltipRowStack.stackRows(
                List.of(
                    TooltipSection.createSection(List.of(FIRST_LINE)),
                    TooltipSection.createSection(List.of(SECOND_LINE, THIRD_LINE))),
                UNIFORM_STYLE);

            // Into the second block by the style's break, then on to its own second line by the gap -
            // both partings asserted in one run, which is where getting them the same would show.
            assertThat(readLeadingGap(rowStack, SECOND_LINE_OF_THE_BOX))
                .isCloseTo(SECTION_BREAK, within(TOLERANCE));
            assertThat(readLeadingGap(rowStack, THIRD_LINE_OF_THE_BOX))
                .isCloseTo(LINE_GAP, within(TOLERANCE));
        }

        @Test
        void partsTwoNestedBlocksByTheStylesGroupBreakWhereTheFirstBrokeDownFurther() {
            // What a nested block is for: an entry that broke down into an account of its own is one
            // thing, and the entry after it stands clear of the whole of it rather than of its last line.
            // Narrower than the section break, so a run inside a block never reads as a block.
            var rowStack = TooltipRowStack.stackRows(
                List.of(buildGroupedSection(FIRST_LINE, SECOND_LINE, THIRD_LINE)),
                UNIFORM_STYLE);

            var secondGroup = 3;

            assertThat(readLeadingGap(rowStack, secondGroup))
                .isCloseTo(GROUP_BREAK, within(TOLERANCE));
        }

        @Test
        void partsABlocksFirstNestedBlockFromItsOwnLinesByTheLineGap() {
            // A block's own lines are its voice rather than a sibling of the groups beneath them, so the
            // first group hugs the lines that introduce it. A break spent here would put a gap under
            // every heading in the box that the heading's own block boundary already paid for.
            var rowStack = TooltipRowStack.stackRows(
                List.of(buildGroupedSection(FIRST_LINE, SECOND_LINE, THIRD_LINE)),
                UNIFORM_STYLE);

            assertThat(readLeadingGap(rowStack, SECOND_LINE_OF_THE_BOX))
                .isCloseTo(LINE_GAP, within(TOLERANCE));
        }

        @Test
        void partsANestedBlockFromItsOwnAccountByTheLineGap() {
            // The same rule one level down: what a group breaks into opens flush beneath it, so a parting
            // never lands between an entry and the first term explaining it.
            var rowStack = TooltipRowStack.stackRows(
                List.of(buildGroupedSection(FIRST_LINE, SECOND_LINE, THIRD_LINE)),
                UNIFORM_STYLE);

            assertThat(readLeadingGap(rowStack, THIRD_LINE_OF_THE_BOX))
                .isCloseTo(LINE_GAP, within(TOLERANCE));
        }

        @Test
        void partsTwoOneLineNestedBlocksByTheLineGap() {
            // A run of groups that each came to a line is a plain list, and a parting between every pair
            // of them would space a list of colonies as though each were a breakdown.
            var rowStack = TooltipRowStack.stackRows(
                List.of(TooltipSection
                    .createSection(List.of(FIRST_LINE))
                    .nesting(List.of(
                        TooltipSection.createSection(List.of(SECOND_LINE)),
                        TooltipSection.createSection(List.of(THIRD_LINE))))),
                UNIFORM_STYLE);

            assertThat(readLeadingGap(rowStack, THIRD_LINE_OF_THE_BOX))
                .isCloseTo(LINE_GAP, within(TOLERANCE));
        }

        @Test
        void spendsOneGroupBreakWhereSeveralNestedBlocksEndOnTheOneLine() {
            // The whole point of charging the parting to the block that follows: a faction's last colony
            // and the faction itself close on the same line, and the next faction must not be pushed off
            // by both partings at once.
            var rowStack = TooltipRowStack.stackRows(
                List.of(TooltipSection
                    .createSection(List.of(FIRST_LINE))
                    .nesting(List.of(
                        buildGroupedSection(SECOND_LINE, SECOND_LINE, THIRD_LINE),
                        TooltipSection.createSection(List.of(THIRD_LINE))))),
                UNIFORM_STYLE);

            var lineAfterTheFirstFaction = 5;

            assertThat(readLeadingGap(rowStack, lineAfterTheFirstFaction))
                .isCloseTo(GROUP_BREAK, within(TOLERANCE));
        }

        @Test
        void stacksARunOfLinesAtTheGapTheirOwnTierIsHeldAt() {
            // What the per-tier gaps are for: a run of lines at one depth closes up to its own gap, so a
            // box can tighten its deepest listing without touching the lines above it.
            var rowStack = stackOneBlock(
                List.of(
                    createSubordinateRow("AA", TIGHTENED_LEVEL),
                    createSubordinateRow("BB", TIGHTENED_LEVEL)),
                TIERED_STYLE);

            assertThat(readLeadingGap(rowStack, SECOND_LINE_OF_THE_BOX))
                .isCloseTo(TIGHTENED_GAP, within(TOLERANCE));
        }

        @Test
        void opensARunAtTheGapOfTheShallowerLineAboveIt() {
            // The gap belongs to the line just laid rather than to the line about to be. So the first
            // line of a tightened run follows the line introducing it at that line's own gap, and only
            // the lines within the run close up - resolved the other way, the whole run would be pulled
            // up against the line it stands under.
            var rowStack = stackOneBlock(
                List.of(
                    createSubordinateRow("AA", LEVEL_ABOVE_THE_TIGHTENED_RUN),
                    createSubordinateRow("BB", TIGHTENED_LEVEL),
                    createSubordinateRow("CC", TIGHTENED_LEVEL)),
                TIERED_STYLE);

            assertThat(readLeadingGap(rowStack, SECOND_LINE_OF_THE_BOX))
                .isCloseTo(LINE_GAP, within(TOLERANCE));
            assertThat(readLeadingGap(rowStack, THIRD_LINE_OF_THE_BOX))
                .isCloseTo(TIGHTENED_GAP, within(TOLERANCE));
        }

        @Test
        void opensANestedBlockAtTheGapOfTheLineItFollows() {
            // Where nearly every gap in a listing actually falls: a caller that gives each entry a block
            // of its own has no two lines sharing one block, so a tier gap that reached only the lines
            // within a block would never be spent at all.
            var rowStack = TooltipRowStack.stackRows(
                List.of(TooltipSection
                    .createSection(List.of(createSubordinateRow("AA", TIGHTENED_LEVEL)))
                    .nesting(List.of(TooltipSection.createSection(
                        List.of(createSubordinateRow("BB", TIGHTENED_LEVEL)))))),
                TIERED_STYLE);

            assertThat(readLeadingGap(rowStack, SECOND_LINE_OF_THE_BOX))
                .isCloseTo(TIGHTENED_GAP, within(TOLERANCE));
        }

        @Test
        void partsTwoBlocksOfATightenedTierByTheStylesBreak() {
            // A tier gap is what stands where no boundary falls, so it cannot close up the boundaries: a
            // block of tightened lines is parted from the next by the box's own break, exactly as any
            // other block is.
            var rowStack = TooltipRowStack.stackRows(
                partIntoSections(
                    createSubordinateRow("AA", TIGHTENED_LEVEL),
                    createSubordinateRow("BB", TIGHTENED_LEVEL)),
                TIERED_STYLE);

            assertThat(readLeadingGap(rowStack, SECOND_LINE_OF_THE_BOX))
                .isCloseTo(SECTION_BREAK, within(TOLERANCE));
        }

        @Test
        void partsTwoNestedBlocksOfATightenedTierByTheStylesGroupBreak() {
            // The same at the inner boundary: an entry that broke down into an account of its own is
            // still set clear of the entry after it, however tightly the tier's own lines stack.
            var rowStack = TooltipRowStack.stackRows(
                List.of(buildGroupedSection(
                    createSubordinateRow("AA", TIGHTENED_LEVEL),
                    createSubordinateRow("BB", TIGHTENED_LEVEL),
                    createSubordinateRow("CC", TIGHTENED_LEVEL))),
                TIERED_STYLE);

            var secondGroup = 3;

            assertThat(readLeadingGap(rowStack, secondGroup))
                .isCloseTo(GROUP_BREAK, within(TOLERANCE));
        }

        @Test
        void resolvesTheGapUnderACentredLineFromTheBoxsOwnVoice() {
            // A centred line has left the table and speaks for the box, so it stands at no tier within
            // it: the line under a centred title takes the box's plain gap however tightly the run it
            // introduces stacks, which keeps a tightened listing from closing up on its own title.
            var rowStack = stackOneBlock(
                List.of(
                    TooltipRow.createCentredRow(new TextSpan("AA", Color.WHITE)),
                    createSubordinateRow("BB", TIGHTENED_LEVEL)),
                TIERED_STYLE);

            assertThat(readLeadingGap(rowStack, SECOND_LINE_OF_THE_BOX))
                .isCloseTo(LINE_GAP, within(TOLERANCE));
        }

        @Test
        void partsTwoBlocksByTheSameBreakWhateverLineHeightsTheyOpenOn() {
            // The point of the parting living on the box rather than on the line that opens a block: a
            // block opened by a 20-tall heading stands exactly as far from the one above it as a block
            // opened by a 15-tall body line, so no box has partings of two different widths in it.
            var headedStack = TooltipRowStack.stackRows(
                partIntoSections(FIRST_LINE, createCrestlessRow("BB").readsAs(TooltipLineStyle.HEADER)),
                TWO_FACE_STYLE);

            var plainStack = TooltipRowStack.stackRows(
                partIntoSections(FIRST_LINE, SECOND_LINE),
                TWO_FACE_STYLE);

            assertThat(readLeadingGap(headedStack, SECOND_LINE_OF_THE_BOX))
                .isCloseTo(SECTION_BREAK, within(TOLERANCE));
            assertThat(readLeadingGap(plainStack, SECOND_LINE_OF_THE_BOX))
                .isCloseTo(SECTION_BREAK, within(TOLERANCE));
        }
    }

    @Nested
    class MeasureContentHeight {

        @Test
        void sumsEachLinesHeightAndTheRoomAboveIt() {

            // Two 15-tall lines + the 4 gap between them.
            assertThat(stackOneBlock(List.of(FIRST_LINE, SECOND_LINE), UNIFORM_STYLE)
                    .measureContentHeight())
                .isCloseTo(34d, within(0.001d));
        }

        @Test
        void growsByALineAndItsGapPerLine() {

            // Three 15-tall lines + two 4 gaps.
            assertThat(stackOneBlock(List.of(FIRST_LINE, SECOND_LINE, THIRD_LINE), UNIFORM_STYLE)
                    .measureContentHeight())
                .isCloseTo(53d, within(0.001d));
        }

        @Test
        void spendsTheStylesBreakBetweenTwoBlocks() {

            // The same two lines parted by the 11.5 break rather than the 4 gap: what a box spends on
            // nothing is as much of its height as its lines are.
            assertThat(TooltipRowStack.stackRows(partIntoSections(FIRST_LINE, SECOND_LINE), UNIFORM_STYLE)
                    .measureContentHeight())
                .isCloseTo(41.5d, within(0.001d));
        }

        @Test
        void countsAHeadingLineAtItsOwnHeight() {

            var headingRow = createCrestlessRow("AA").readsAs(TooltipLineStyle.HEADER);

            // The heading's own 20-tall line over a 15-tall body line, parted by the one break: a run
            // counted at the body's height throughout would come to 5 short of what is drawn.
            assertThat(TooltipRowStack.stackRows(
                    partIntoSections(headingRow, SECOND_LINE),
                    TWO_FACE_STYLE)
                    .measureContentHeight())
                .isCloseTo(46.5d, within(0.001d));
        }

        @Test
        void stacksARunOfATightenedTierAtItsOwnGap() {

            var tightenedRows = List.<TooltipRow>of(
                createSubordinateRow("AA", TIGHTENED_LEVEL),
                createSubordinateRow("BB", TIGHTENED_LEVEL));

            // Two 15-tall lines + the tightened tier's own 1 gap, where the plain 4 would have come to
            // 34.
            assertThat(stackOneBlock(tightenedRows, TIERED_STYLE).measureContentHeight())
                .isCloseTo(31d, within(0.001d));
        }

        @Test
        void shrinksASubordinateLineByTheStylesLevelShrink() {

            var demotedRows = List.<TooltipRow>of(
                FIRST_LINE,
                createSubordinateRow("BB", ONE_STEP_UNDER));

            // The box's own voice at 15 over a line demoted one step to 13, + the 4 gap: a height read
            // off the kinds alone would come to 34 and leave two units of the box unaccounted.
            assertThat(stackOneBlock(demotedRows, SHRINKING_STYLE).measureContentHeight())
                .isCloseTo(32d, within(0.001d));
        }

        @Test
        void isNothingForABoxOfNoBlocks() {

            // An empty stack stands no height at all: what a box of nothing still spends is its own
            // padding, which is the box's to add rather than the run's.
            assertThat(TooltipRowStack.stackRows(List.of(), UNIFORM_STYLE).measureContentHeight())
                .isCloseTo(0d, within(0.001d));
        }
    }
}
