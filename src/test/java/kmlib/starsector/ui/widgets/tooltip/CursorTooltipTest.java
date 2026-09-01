package kmlib.starsector.ui.widgets.tooltip;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.font.TextSpanMeasurer;
import kmlib.starsector.ui.text.ImageSpan;
import kmlib.starsector.ui.text.TextAlignment;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.text.TextStyle;
import kmlib.starsector.ui.widgets.LabelledRow;
import kmlib.starsector.ui.widgets.RowSlot;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link CursorTooltip}'s row layout: the box sizes to the widest row across indent tiers and to
 * however tall the rows stack, each row's line and its three columns anchor within the placed box, a
 * crest-less row still reserves its column unless it steps out of it, a centred row lays as a
 * standalone span in the middle of the content region, and two blocks are parted where two lines of one
 * block are not. A row carrying both a label and a value also measures the stretch a rule is led along
 * between them - stood off each end by that row's own word space, and led only where what is left is at
 * least that long. Where no parting falls, the room a line takes is the style's answer for the tier of the
 * line above it, so a run at one depth stacks at its own gap while the boundaries still win over it. The flanking columns are reserved from what each row's slot reports, so what a row
 * leads or trails with is pinned by width rather than by kind. On top of that, each row is measured and stacked in the look its own kind of line
 * resolves to, which is the whole reason a box can hold a heading and a body line at once. The box
 * padding and screen clamp themselves are {@link kmlib.starsector.ui.layout.TooltipBoxLayout}'s and
 * covered there; this fixes the row model on top of it. The cursor sits clear of every edge so no clamp
 * perturbs the anchors under test.
 *
 * <p>The same stack answered on its own, without a layout, is pinned to the same arithmetic: a box asked
 * how tall it comes to counts every line at the size its tier resolves and every gap the blocks imply, so
 * a caller weighing content against the room it has is told what would actually be drawn.
 */
class CursorTooltipTest {

    private static final StarsectorFont BODY_FONT = StarsectorFont.VANILLA_INSIGNIA_15;
    private static final StarsectorFont HEADING_FONT = StarsectorFont.VANILLA_ORBITRON_20AA;
    private static final StarsectorFont SPRAWLING_FONT = StarsectorFont.VANILLA_INSIGNIA_42;
    private static final double BODY_LINE_HEIGHT = 15d;
    private static final double HEADING_LINE_HEIGHT = 20d;
    private static final double SPRAWLING_LINE_HEIGHT = 42d;

    // How the stand-in faces measure. The body face charges one unit per character, so a label's width
    // is its length and the arithmetic below reads off the strings directly; the heading face charges
    // triple, standing in for a genuinely wider atlas, so a measured width says which face measured it.
    // The sprawling face charges nine, wide enough that its own word space swallows the gap the box
    // reserves between a label and a value - the only shape in which a leader rule has nowhere to run.
    private static final double BODY_WIDTH_PER_CHARACTER = 1d;
    private static final double HEADING_WIDTH_PER_CHARACTER = 3d;
    private static final double SPRAWLING_WIDTH_PER_CHARACTER = 9d;

    // A capital I costs four units where a lower-case one costs one, so a width also says whether a span
    // was measured as its caller authored it or as its style will actually draw it. No other label here
    // holds an i, so this prices nothing but the casing case.
    private static final double SHOUTED_I_WIDTH = 4d;

    // The runs of a label, by position: the first is the label a row is authored with, and the ones past
    // it are what a caller appended to pick part of the line out in another colour.
    private static final int FIRST_RUN = 0;
    private static final int SECOND_RUN = 1;
    private static final int THIRD_RUN = 2;

    private static final Rectangle SCREEN = new Rectangle(0f, 0f, 1920f, 1080f);
    private static final float CURSOR_X = 200f;
    private static final float CURSOR_Y = 300f;
    private static final float TOLERANCE = 0.001f;

    // How far apart the styles below part two blocks. Restated rather than read off the style so the
    // arithmetic in each case names the number it is actually spending.
    private static final float SECTION_BREAK = 11.5f;

    // How far apart those same styles part two blocks nested inside one, restated for the same reason.
    private static final float GROUP_BREAK = 5.75f;

    // The gap two lines of one block sit at where the box holds no tier apart, restated for the same
    // reason.
    private static final float LINE_GAP = 4f;

    // A tier the tiered style below holds apart from the rest, and the gap it holds it at: a step no
    // other tier resolves, so a measured step says which line's tier the gap was read from. The level
    // above it is named too, since a run is only tightened once the line introducing it has been passed.
    private static final int TIGHTENED_LEVEL = 2;
    private static final int LEVEL_ABOVE_THE_TIGHTENED_RUN = 1;
    private static final float TIGHTENED_GAP = 1f;

    // The row a step is ordinarily measured from - the top of the box, where nothing above the pair
    // under test can shift what parts them.
    private static final int FIRST_ROW = 0;

    // A box whose headings and body lines look alike, and one where they differ in both face and size.
    // The uniform style is what every case not about per-row looks lays out through, so those cases read
    // as the plain row-model arithmetic they are testing rather than as typography.
    private static final TooltipStyle UNIFORM_STYLE = TooltipStyle.createStyle(
        createStyle(BODY_FONT, BODY_LINE_HEIGHT),
        createStyle(BODY_FONT, BODY_LINE_HEIGHT));

    private static final TooltipStyle TWO_FACE_STYLE = TooltipStyle.createStyle(
        createStyle(HEADING_FONT, HEADING_LINE_HEIGHT),
        createStyle(BODY_FONT, BODY_LINE_HEIGHT));

    // A box set throughout in the sprawling face, whose word space is wider than half the gap the box
    // reserves for its value column - so its rows close up to less than a space between label and value.
    private static final TooltipStyle SPRAWLING_STYLE = TooltipStyle.createStyle(
        createStyle(SPRAWLING_FONT, SPRAWLING_LINE_HEIGHT),
        createStyle(SPRAWLING_FONT, SPRAWLING_LINE_HEIGHT));

    // The uniform box with one tier of its lines closed up: everything else still resolves the plain gap,
    // which is what lets the cases below say whether a step was resolved from the line above the gap or
    // from the line below it.
    private static final TooltipStyle TIERED_STYLE = UNIFORM_STYLE.stackedAt(
        TooltipLineGaps.createGaps(LINE_GAP).gappedAtLevel(TIGHTENED_LEVEL, TIGHTENED_GAP));

    // The uniform box demoting each step under its own voice by two units, and a line standing one such
    // step under it. A size no other style here resolves, so a measured height says the shrink was spent
    // on the row rather than the row being stacked at its kind's own size.
    private static final float LEVEL_SHRINK = 2f;
    private static final int ONE_STEP_UNDER = 1;
    private static final TooltipStyle SHRINKING_STYLE = UNIFORM_STYLE.shrunkPerLevel(LEVEL_SHRINK);

    // A top-tier row at no indent (short label, no value) and a wider indented member (longer label, a
    // value): the member is the widest laid-out row, so it must drive the box width even though it is the
    // indented one - the point of measuring across tiers. Both are crest-aligned body lines, as a row
    // built without stating a placement or a kind of line is.
    private static final TooltipRow TOP_TIER = TooltipRow.createRow(new TextSpan("AA", Color.WHITE))
        .carriesCrest("crest_a");

    private static final TooltipRow MEMBER = TooltipRow.createRow(new TextSpan("BBBB", Color.LIGHT_GRAY))
        .carriesCrest("crest_b")
        .carriesValue(new TextSpan("9", Color.GRAY))
        .indentsBy(14f);

    private static TextStyle createStyle(StarsectorFont font, double size) {
        // Built by hand rather than through TextStyle's own factory: its baseline colour resolves from the
        // running game's palette, which a layout test has no business standing up for a value it never
        // reads.
        return new TextStyle(
            new TextFace(font, size),
            Color.WHITE,
            TextAlignment.TOP_LEFT,
            false);
    }

    private static double measureSpanWidth(TextFace face, String span) {

        var widthPerCharacter = resolveWidthPerCharacter(face.font());
        var characterCost = 0d;

        for (var character : span.toCharArray()) {
            characterCost += character == 'I' ? SHOUTED_I_WIDTH : 1d;
        }
        return characterCost * widthPerCharacter;
    }

    // What one glyph of a face costs. Switched over the faces rather than tested against one, so a third
    // stand-in face is priced here alone and no case below has to say which branch it fell down.
    private static double resolveWidthPerCharacter(StarsectorFont font) {

        if (font == HEADING_FONT) {
            return HEADING_WIDTH_PER_CHARACTER;
        }
        if (font == SPRAWLING_FONT) {
            return SPRAWLING_WIDTH_PER_CHARACTER;
        }
        return BODY_WIDTH_PER_CHARACTER;
    }

    // Lays the rows out as one block, which is what every case not about the parting is testing: lines
    // that belong together, so nothing is spent between them beyond the plain line gap.
    private static TooltipLayout layOut(List<TooltipRow> rows) {
        return layOut(rows, UNIFORM_STYLE);
    }

    private static TooltipLayout layOut(List<TooltipRow> rows, TooltipStyle style) {
        return layOutSections(List.of(TooltipSection.createSection(rows)), style);
    }

    private static TooltipLayout layOutSections(List<TooltipSection> sections, TooltipStyle style) {
        TextSpanMeasurer measurer = CursorTooltipTest::measureSpanWidth;
        return CursorTooltip.layOut(sections, style, measurer, CURSOR_X, CURSOR_Y, SCREEN);
    }

    // How tall the same one-block box comes to, asked without laying it out - the answer a caller weighs
    // before it draws, taken through the same shapes the layout cases above are read off so the two can
    // be compared line for line.
    private static float measureBoxHeight(List<TooltipRow> rows, TooltipStyle style) {
        return CursorTooltip.measureBoxHeight(List.of(TooltipSection.createSection(rows)), style);
    }

    // Two lines put in blocks of their own, which is the only way to say they are parted.
    private static List<TooltipSection> partIntoSections(TooltipRow firstRow, TooltipRow secondRow) {
        return List.of(
            TooltipSection.createSection(List.of(firstRow)),
            TooltipSection.createSection(List.of(secondRow)));
    }

    // The grouped block above, laid out on its own - the box every case about a nested parting reads its
    // steps off, so none of them restates the setup the assertion is not about.
    private static TooltipLayout layOutGroupedSection() {
        return layOutSections(
            List.of(buildGroupedSection(createCrestlessRow("AA"), TOP_TIER, MEMBER)),
            UNIFORM_STYLE);
    }

    // One block holding a heading over two groups: the first breaking down into an account of its own,
    // the second a line on its own. The shape every parting case below is read off, since it holds all
    // three boundaries at once - a block's lines to its first group, a group to its own account, and one
    // group to the next.
    //
    // Its three lines are handed in rather than fixed, since a case about what a tier resolves needs the
    // same shape built out of tiered lines - stated twice, the two copies would drift and a step read off
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

    private static TooltipRow.TableRow createCrestlessRow(String text) {
        return TooltipRow.createRow(new TextSpan(text, Color.WHITE));
    }

    // A body line standing a stated number of steps under the box's voice - the fact a per-tier gap is
    // resolved from. Crest-less, so nothing but the spacing differs between the lines of a tiered case.
    private static TooltipRow.TableRow createSubordinateRow(String text, int subordinationLevel) {
        return createCrestlessRow(text).subordinatedAt(subordinationLevel);
    }

    private static TooltipRow.CentredRow createCentredRow(String text) {
        return TooltipRow.createCentredRow(new TextSpan(text, Color.WHITE));
    }

    // A row leading with something that is not a crest, built through the content core because the
    // tooltip's own refinement offers only an image. What a row leads with is the slot's business, and
    // the column has to be reserved from what that slot reports whatever kind it turns out to be.
    private static TooltipRow.TableRow createRowLeadingWith(String text, RowSlot leadingRowSlot) {
        return new TooltipRow.TableRow(
            TooltipLineStyle.PARAGRAPH,
            TooltipLabelPlacement.ALIGNED_WITH_CRESTS,
            0f,
            0,
            LabelledRow.createRow(new TextSpan(text, Color.WHITE)).leadsWith(leadingRowSlot));
    }

    private static TooltipRow.TableRow createHeadingRow(String text) {
        return createCrestlessRow(text)
            .clearsCrestColumn()
            .readsAs(TooltipLineStyle.HEADER);
    }

    // The stretch a row leads its rule along. Read through a helper so a case names the row it is asking
    // about rather than the two hops it takes to reach one.
    private static TooltipLeaderLine readLeaderLine(TooltipLayout layout, int rowIndex) {
        return layout.rows().get(rowIndex).leaderLine();
    }

    // Where one of a row's label runs anchors. Read by position rather than by name, since the runs are
    // a sequence: the first is where the label starts, and each one past it is where the line carries on.
    private static float readRunX(TooltipLayout.TooltipRowLayout rowLayout, int runIndex) {
        return rowLayout.labelRunXs().get(runIndex);
    }

    // How far the second row sits below the first, which is what a section break widens - unlike the
    // rows' absolute anchors, which a taller box shifts wholesale as it grows up from its cursor.
    private static float measureRowStep(List<TooltipRow> rows, TooltipStyle style) {
        return measureStepBelow(layOut(rows, style), FIRST_ROW);
    }

    // The same step where the two lines sit in blocks of their own, so what is measured is the parting
    // rather than the gap inside a block.
    private static float measureSectionStep(List<TooltipSection> sections, TooltipStyle style) {
        return measureStepBelow(layOutSections(sections, style), FIRST_ROW);
    }

    // How far the row under {@code upperRowIndex} sits below it. Taken by position rather than always
    // off the top, since a box with blocks above the pair under test is the only way to ask what a row
    // deep in the stack is parted by.
    private static float measureStepBelow(TooltipLayout layout, int upperRowIndex) {
        var laidOut = layout.rows();
        return laidOut.get(upperRowIndex).rowTopY() - laidOut.get(upperRowIndex + 1).rowTopY();
    }

    @Nested
    class LayOut {

        @Test
        void sizesTheBoxToTheWidestRowAcrossIndentTiers() {

            var box = layOut(List.of(TOP_TIER, MEMBER)).box();

            // Member row: indent 14 + crest 15 + crest gap 6 + label 4 + value gap 16 + value 1 = 56,
            // wider than the top-tier row's 39; + 8 padding on each side = 72.
            assertThat(box.width())
                .isCloseTo(72f, within(TOLERANCE));
        }

        @Test
        void sizesTheBoxHeightForEachLinePlusTheGapAndPadding() {

            var box = layOut(List.of(TOP_TIER, MEMBER)).box();

            // Two 15-tall lines + one 4 line gap + 8 padding top and bottom = 50.
            assertThat(box.height())
                .isCloseTo(50f, within(TOLERANCE));
        }

        @Test
        void anchorsTheTopTierRowsColumnsOnOneLine() {

            var layout = layOut(List.of(TOP_TIER, MEMBER));
            var topTier = layout.rows().get(0);

            // Box at (218, 318): left content edge 226, right 282, top content edge 360. The row sits at
            // zero indent, so its leading column starts at the left content edge and its trailing column
            // is anchored to the right one - both on the row's one line, which is stated once.
            assertThat(topTier.rowTopY())
                .isCloseTo(360f, within(TOLERANCE));
            assertThat(topTier.lineHeight())
                .isCloseTo(15f, within(TOLERANCE));
            assertThat(topTier.leadingRowSlotX())
                .isCloseTo(226f, within(TOLERANCE));
            assertThat(readRunX(topTier, FIRST_RUN))
                .isCloseTo(247f, within(TOLERANCE));
            assertThat(topTier.trailingRowSlotX())
                .isCloseTo(282f, within(TOLERANCE));
        }

        @Test
        void indentsTheMemberRowAndStepsItDownOneLine() {

            var layout = layOut(List.of(TOP_TIER, MEMBER));
            var member = layout.rows().get(1);

            // One line height 15 + gap 4 below the row above's 360 -> 341; the leading column and the
            // label shift right by the 14 indent, while the trailing column stays pinned to the box's
            // right content edge.
            assertThat(member.rowTopY())
                .isCloseTo(341f, within(TOLERANCE));
            assertThat(member.leadingRowSlotX())
                .isCloseTo(240f, within(TOLERANCE));
            assertThat(readRunX(member, FIRST_RUN))
                .isCloseTo(261f, within(TOLERANCE));
            assertThat(member.trailingRowSlotX())
                .isCloseTo(282f, within(TOLERANCE));
        }

        @Test
        void leadsARuleFromTheLabelsEndToTheValuesStart() {
            // The aid itself: the value column is pinned to the box's right edge whatever the label
            // measures, so the two ends of a row can sit far apart and the rule is what carries the eye
            // across. Its ends stand one word space clear of the glyphs either side - the member's label
            // ends at 261 + 4, and its value starts at 282 - 1.
            var valuedRow = 1;
            var leaderLine = readLeaderLine(layOut(List.of(TOP_TIER, MEMBER)), valuedRow);

            assertThat(leaderLine.isRuled())
                .isTrue();
            assertThat(leaderLine.leftX())
                .isCloseTo(266f, within(TOLERANCE));
            assertThat(leaderLine.rightX())
                .isCloseTo(280f, within(TOLERANCE));
        }

        @Test
        void standsALeaderRuleOffBothEndsByTheRowsOwnWordSpace() {
            // The standoff is the row's own face's space, not a width chosen once for the box: the
            // heading face sets a 3-wide space, so its rule runs 226 + label 6 + 3 to 251 - value 3 - 3.
            // Measured on the body face it would run 233 to 247 and crowd the glyphs of a face that
            // spaces its own words three times as widely.
            var headed = createHeadingRow("AA").carriesValue(new TextSpan("9", Color.GRAY));
            var leaderLine = readLeaderLine(layOut(List.of(headed), TWO_FACE_STYLE), FIRST_ROW);

            assertThat(leaderLine.leftX())
                .isCloseTo(235f, within(TOLERANCE));
            assertThat(leaderLine.rightX())
                .isCloseTo(245f, within(TOLERANCE));
        }

        @Test
        void leadsNoRuleFromARowThatTrailsWithNothing() {
            // A rule leads to a value, so a row carrying none has nothing to lead to - and a rule run
            // from a label out to the empty right edge would point at the box rather than at anything
            // in it.
            var leaderLine = readLeaderLine(layOut(List.of(TOP_TIER, MEMBER)), FIRST_ROW);

            assertThat(leaderLine.isRuled())
                .isFalse();
        }

        @Test
        void leadsNoRuleFromALabelThatCameOutBlank() {
            // The other end has to be there too: a caller that assembled a label from parts and came up
            // blank has no name for the rule to lead back from, so the row shows its value alone rather
            // than a rule emerging from the content edge.
            var blankLabelled = createCrestlessRow("").carriesValue(new TextSpan("9", Color.GRAY));

            assertThat(readLeaderLine(layOut(List.of(blankLabelled)), FIRST_ROW).isRuled())
                .isFalse();
        }

        @Test
        void leadsNoRuleAcrossACentredRow() {
            // A centred line has left the table and holds no value column, so there are no two columns
            // for a rule to join - and one ruled to the content edge would cut across the very line the
            // box centred.
            var centred = createCentredRow("AA");
            var centredRow = 1;

            assertThat(readLeaderLine(layOut(List.of(TOP_TIER, centred)), centredRow).isRuled())
                .isFalse();
        }

        @Test
        void leadsNoRuleWhereTheColumnsCloseToWithinAWordSpace() {
            // The floor: what is left after both standoffs has to be at least a word space long, or the
            // rule is a smudge between two columns already close enough to read as one line. The
            // sprawling face's 9-wide space eats the whole 16 the box reserves - label ends at 226 + 9
            // and the value starts at 260 - 9, leaving 16 for two 9-wide standoffs.
            var valued = createCrestlessRow("A").carriesValue(new TextSpan("B", Color.GRAY));

            assertThat(readLeaderLine(layOut(List.of(valued), SPRAWLING_STYLE), FIRST_ROW).isRuled())
                .isFalse();
        }

        @Test
        void leadsARuleWhereTheColumnsStandAWordSpaceApartOrMore() {
            // The same row on the body face clears the floor comfortably - 226 + 1 + 1 to 244 - 1 - 1 -
            // so the case above is the standoffs closing up rather than a rule that never leads at all.
            var valued = createCrestlessRow("A").carriesValue(new TextSpan("B", Color.GRAY));
            var leaderLine = readLeaderLine(layOut(List.of(valued)), FIRST_ROW);

            assertThat(leaderLine.leftX())
                .isCloseTo(228f, within(TOLERANCE));
            assertThat(leaderLine.rightX())
                .isCloseTo(242f, within(TOLERANCE));
        }

        @Test
        void leadsARuleToTheLeftEdgeOfAValueDrawnInSeveralRuns() {
            // The rule stops where the value column starts drawing, not at the box's right edge: a value
            // picked out in two colours is measured as the one column it fills (2 + the face's 1-wide
            // space + 1 = 4), so the rule ends at 248 - 4 - 1 rather than running under its first run.
            var valued = createCrestlessRow("AA")
                .carriesValueRuns(List.of(
                    new TextSpan("BB", Color.GRAY),
                    new TextSpan("C", Color.WHITE)));

            var leaderLine = readLeaderLine(layOut(List.of(valued)), FIRST_ROW);

            assertThat(leaderLine.leftX())
                .isCloseTo(229f, within(TOLERANCE));
            assertThat(leaderLine.rightX())
                .isCloseTo(243f, within(TOLERANCE));
        }

        @Test
        void reservesTheCrestColumnForACrestLessRowWhenAnotherRowCarriesACrest() {

            var crestless = createCrestlessRow("AA");
            var crested = createCrestlessRow("BB").carriesCrest("crest");
            var crestlessRow = layOut(List.of(crestless, crested)).rows().get(0);

            // The box carries a crest, so the crest-less row still reserves the column - its label is
            // anchored past the reserved 15 + 6 gutter and lines up under the crested row.
            assertThat(readRunX(crestlessRow, FIRST_RUN))
                .isCloseTo(247f, within(TOLERANCE));
        }

        @Test
        void collapsesTheCrestColumnWhenNoRowCarriesACrest() {

            var only = layOut(List.of(createCrestlessRow("AA"))).rows().get(0);

            // No row carries a crest, so the gutter collapses and the label lays flush at the left
            // content edge (226) rather than past a phantom crest column - the empty-state box case.
            assertThat(readRunX(only, FIRST_RUN))
                .isCloseTo(226f, within(TOLERANCE));
        }

        @Test
        void reservesTheCrestColumnForALeadingSlotThatIsNoCrest() {
            // The column is reserved from what the slot reports, not from a row being asked whether it
            // has a crest: a tick squares off its line as a crest does, so it opens the same 15 + 6
            // gutter and the crest-less row beside it lines up under it.
            var ticked = createRowLeadingWith("BB", new RowSlot.Tick(true));
            var crestlessRow = layOut(List.of(createCrestlessRow("AA"), ticked)).rows().get(0);

            assertThat(readRunX(crestlessRow, FIRST_RUN))
                .isCloseTo(247f, within(TOLERANCE));
        }

        @Test
        void reservesNoCrestColumnForALeadingRunThatCameOutBlank() {
            // A slot charged nothing reserves nothing: a leading run assembled from parts and coming up
            // blank leaves the labels flush at the content edge rather than opening a gutter of the gap
            // alone in front of no glyphs.
            var blankLed = createRowLeadingWith(
                "AA",
                new RowSlot.Text(TextSpan.createBlank(Color.WHITE)));

            var blankLedRow = layOut(List.of(blankLed)).rows().get(0);

            assertThat(readRunX(blankLedRow, FIRST_RUN))
                .isCloseTo(226f, within(TOLERANCE));
        }

        @Test
        void sizesTheBoxForEveryRunOfAValueDrawnInSeveral() {
            // The value column is reserved from what the slot reports, and a value picked out in two
            // colours reports all of its runs and the gap between them - measured as one run, the box
            // would be too narrow by the rest and the value would be drawn past its own edge.
            var row = createCrestlessRow("AA")
                .carriesValueRuns(List.of(
                    new TextSpan("BB", Color.GRAY),
                    new TextSpan("C", Color.WHITE)));

            // No crest column; label 2 + value gap 16 + value (2 + the body face's 1-wide space + 1) =
            // 22; + 8 padding each side = 38.
            assertThat(layOut(List.of(row)).box().width())
                .isCloseTo(38f, within(TOLERANCE));
        }

        @Test
        void laysARowThatClearsTheCrestColumnFlushWithTheContentEdge() {
            // A title over a crested list: it names the box rather than sitting in the list, so it
            // ignores the gutter the rows below reserve and starts at the content edge (226).
            var title = createCrestlessRow("AA").clearsCrestColumn();
            var titleRow = layOut(List.of(title, MEMBER)).rows().get(0);

            assertThat(readRunX(titleRow, FIRST_RUN))
                .isCloseTo(226f, within(TOLERANCE));
        }

        @Test
        void sizesARowThatClearsTheCrestColumnWithoutTheGutter() {
            // The width measurement drops the gutter for that row too, so a long title does not push
            // the box wider by a column it never occupies.
            var title = createCrestlessRow("AAAAAAAAAA").clearsCrestColumn();
            var box = layOut(List.of(title, MEMBER)).box();

            // Title: label 10 + value gap 16 = 26, under the member's 56, so the member still sizes the
            // box at 56 + 16 padding = 72 - which it would not if the title were charged the gutter.
            assertThat(box.width())
                .isCloseTo(72f, within(TOLERANCE));
        }

        @Test
        void centresACentredRowInTheContentRegion() {
            // The status-line case: a wide title sizes the box (label 10 + value gap 16 = 26, box 42),
            // and the short centred line sits in the middle of that 26-wide region - 226 + (26 - 2) / 2.
            var title = createCrestlessRow("AAAAAAAAAA").clearsCrestColumn();
            var centred = createCentredRow("BB");
            var centredRow = layOut(List.of(title, centred)).rows().get(1);

            assertThat(readRunX(centredRow, FIRST_RUN))
                .isCloseTo(238f, within(TOLERANCE));
        }

        @Test
        void centresACentredRowClearOfTheCrestColumnTheRowsAroundItReserve() {
            // The crested row opens a 15 + 6 gutter and sizes the box (crest column 21 + label 2 + value
            // gap 16 = 39), and the centred line ignores it: it centres in the whole 39-wide content
            // region at 226 + (39 - 2) / 2, where a line laid in the columns would start at 226 + 21.
            var centredRow = layOut(List.of(TOP_TIER, createCentredRow("BB"))).rows().get(1);

            assertThat(readRunX(centredRow, FIRST_RUN))
                .isCloseTo(244.5f, within(TOLERANCE));
        }

        @Test
        void sizesACentredRowToItsLabelSpanAlone() {
            // Label 2 + 8 padding each side = 18, where a table row of the same label would measure 34
            // for the value column a centred line does not have - and a box widened for that column
            // would then centre the line against space nothing fills.
            var box = layOut(List.of(createCentredRow("AA"))).box();

            assertThat(box.width())
                .isCloseTo(18f, within(TOLERANCE));
        }

        @Test
        void centresACentredRowsLabelRunsAsOneSpan() {
            // Span: first run 2 + the face's 1-wide space + second run 3 = 6, centred in the title's
            // 26-wide region, so the label starts at 226 + (26 - 6) / 2 and the second run still follows
            // one space past it - the runs centre together because they are one sentence, not two columns.
            var title = createCrestlessRow("AAAAAAAAAA").clearsCrestColumn();
            var centred = createCentredRow("BB")
                .continuesWith(new TextSpan("MMM", Color.YELLOW));

            var centredRow = layOut(List.of(title, centred)).rows().get(1);

            assertThat(readRunX(centredRow, FIRST_RUN))
                .isCloseTo(236f, within(TOLERANCE));
            assertThat(readRunX(centredRow, SECOND_RUN))
                .isCloseTo(239f, within(TOLERANCE));
        }

        @Test
        void sizesACentredRowToItsImageRunAndWordsTogether() {
            // A crest set among the words is charged the line it squares off, so the span is label 2 +
            // the face's 1-wide space + image 15 = 18, and the box is that plus 8 padding each side.
            // Charged to the span rather than to a column is what keeps the line centred: a box widened
            // for a gutter would push the crest and its words off the middle.
            var box = layOut(List.of(createCentredRow("BB")
                .continuesWith(new ImageSpan("crest_a"))))
                .box();

            assertThat(box.width())
                .isCloseTo(34f, within(TOLERANCE));
        }

        @Test
        void centresACentredRowsImageRunWithItsWords() {
            // The crest leads the sentence rather than a column: the 18-wide span (15 + the face's
            // 1-wide space + 2) centres in the title's 26-wide region at 226 + (26 - 18) / 2, and the
            // words follow one space past the image's own square at 230 + 15 + 1.
            var title = createCrestlessRow("AAAAAAAAAA").clearsCrestColumn();
            var centred = TooltipRow
                .createCentredRow(new ImageSpan("crest_a"))
                .continuesWith(new TextSpan("BB", Color.WHITE));

            var centredRow = layOut(List.of(title, centred)).rows().get(1);

            assertThat(readRunX(centredRow, FIRST_RUN))
                .isCloseTo(230f, within(TOLERANCE));
            assertThat(readRunX(centredRow, SECOND_RUN))
                .isCloseTo(246f, within(TOLERANCE));
        }

        @Test
        void partsTwoBlocksByTheStylesBreakAndTwoLinesOfOneBlockByTheLineGap() {
            // Asserted as the step between the two rows, not their absolute anchors: the box is pinned
            // at its lower-left corner and grows upward, so a taller box lifts every row's y together
            // while the parting between them is what the grouping actually changes.
            var withinBlockStep = measureRowStep(List.of(TOP_TIER, MEMBER), UNIFORM_STYLE);
            var acrossBlocksStep =
                measureSectionStep(partIntoSections(TOP_TIER, MEMBER), UNIFORM_STYLE);

            // A line 15 + the 4 line gap inside a block; the style's 11.5 break in place of that gap
            // between two of them.
            assertThat(withinBlockStep)
                .isCloseTo(19f, within(TOLERANCE));
            assertThat(acrossBlocksStep)
                .isCloseTo(15f + SECTION_BREAK, within(TOLERANCE));
        }

        @Test
        void partsTwoRowsOfABlockByTheLineGapHoweverManyBlocksStandAboveIt() {
            // The remaining branch of the rule, and the one a box actually spends most: a row that is
            // neither the box's first nor its own block's first takes the plain gap wherever it sits.
            // A break reaching it would part a block from itself, which no test above could catch -
            // every block past the first one they lay out holds a single line.
            var layout = layOutSections(
                List.of(
                    TooltipSection.createSection(List.of(TOP_TIER)),
                    TooltipSection.createSection(List.of(TOP_TIER, MEMBER))),
                UNIFORM_STYLE);

            var secondBlockOpener = 1;

            // Into the second block by the style's break, then on to its own second line by the gap -
            // both partings asserted in one box, which is where getting them the same would show.
            assertThat(measureStepBelow(layout, FIRST_ROW))
                .isCloseTo(26.5f, within(TOLERANCE));
            assertThat(measureStepBelow(layout, secondBlockOpener))
                .isCloseTo(19f, within(TOLERANCE));
        }

        @Test
        void partsTwoNestedBlocksByTheStylesGroupBreakWhereTheFirstBrokeDownFurther() {
            // What a nested block is for: an entry that broke down into an account of its own is one
            // thing, and the entry after it stands clear of the whole of it rather than of its last
            // line. Narrower than the section break, so a run inside a block never reads as a block.
            var layout = layOutGroupedSection();

            var lastLineOfTheFirstGroup = 2;

            assertThat(measureStepBelow(layout, lastLineOfTheFirstGroup))
                .isCloseTo(15f + GROUP_BREAK, within(TOLERANCE));
        }

        @Test
        void partsABlocksFirstNestedBlockFromItsOwnLinesByTheLineGap() {
            // A block's own lines are its voice rather than a sibling of the groups beneath them, so the
            // first group hugs the lines that introduce it. A break spent here would put a gap under
            // every heading in the box that the heading's own block boundary already paid for.
            var layout = layOutGroupedSection();

            assertThat(measureStepBelow(layout, FIRST_ROW))
                .isCloseTo(19f, within(TOLERANCE));
        }

        @Test
        void partsANestedBlockFromItsOwnAccountByTheLineGap() {
            // The same rule one level down: what a group breaks into opens flush beneath it, so a parting
            // never lands between an entry and the first term explaining it.
            var layout = layOutGroupedSection();

            var firstGroupOpener = 1;

            assertThat(measureStepBelow(layout, firstGroupOpener))
                .isCloseTo(19f, within(TOLERANCE));
        }

        @Test
        void partsTwoOneLineNestedBlocksByTheLineGap() {
            // A run of groups that each came to a line is a plain list, and a parting between every pair
            // of them would space a list of colonies as though each were a breakdown.
            var layout = layOutSections(
                List.of(TooltipSection
                    .createSection(List.of(createCrestlessRow("AA")))
                    .nesting(List.of(
                        TooltipSection.createSection(List.of(TOP_TIER)),
                        TooltipSection.createSection(List.of(TOP_TIER))))),
                UNIFORM_STYLE);

            var firstGroup = 1;

            assertThat(measureStepBelow(layout, firstGroup))
                .isCloseTo(19f, within(TOLERANCE));
        }

        @Test
        void spendsOneGroupBreakWhereSeveralNestedBlocksEndOnTheOneLine() {
            // The whole point of charging the parting to the block that follows: a faction's last colony
            // and the faction itself close on the same line, and the next faction must not be pushed off
            // by both partings at once.
            var layout = layOutSections(
                List.of(TooltipSection
                    .createSection(List.of(createCrestlessRow("AA")))
                    .nesting(List.of(
                        buildGroupedSection(TOP_TIER, TOP_TIER, MEMBER),
                        TooltipSection.createSection(List.of(TOP_TIER))))),
                UNIFORM_STYLE);

            var lastLineOfTheFirstFaction = 4;

            assertThat(measureStepBelow(layout, lastLineOfTheFirstFaction))
                .isCloseTo(15f + GROUP_BREAK, within(TOLERANCE));
        }

        @Test
        void stacksARunOfLinesAtTheGapTheirOwnTierIsHeldAt() {
            // What the per-tier gaps are for: a run of lines at one depth closes up to its own gap, so a
            // box can tighten its deepest listing without touching the lines above it.
            var step = measureRowStep(
                List.of(
                    createSubordinateRow("AA", TIGHTENED_LEVEL),
                    createSubordinateRow("BB", TIGHTENED_LEVEL)),
                TIERED_STYLE);

            assertThat(step)
                .isCloseTo(15f + TIGHTENED_GAP, within(TOLERANCE));
        }

        @Test
        void opensARunAtTheGapOfTheShallowerLineAboveIt() {
            // The gap belongs to the line just drawn rather than to the line about to be. So the first
            // line of a tightened run follows the line introducing it at that line's own gap, and only
            // the lines within the run close up - resolved the other way, the whole run would be pulled
            // up against the line it stands under.
            var layout = layOut(
                List.of(
                    createSubordinateRow("AA", LEVEL_ABOVE_THE_TIGHTENED_RUN),
                    createSubordinateRow("BB", TIGHTENED_LEVEL),
                    createSubordinateRow("CC", TIGHTENED_LEVEL)),
                TIERED_STYLE);

            var firstLineOfTheRun = 1;

            assertThat(measureStepBelow(layout, FIRST_ROW))
                .isCloseTo(15f + LINE_GAP, within(TOLERANCE));
            assertThat(measureStepBelow(layout, firstLineOfTheRun))
                .isCloseTo(15f + TIGHTENED_GAP, within(TOLERANCE));
        }

        @Test
        void opensANestedBlockAtTheGapOfTheLineItFollows() {
            // Where nearly every gap in a listing actually falls: a caller that gives each entry a block
            // of its own has no two lines sharing one block, so a tier gap that reached only the lines
            // within a block would never be spent at all.
            var layout = layOutSections(
                List.of(TooltipSection
                    .createSection(List.of(createSubordinateRow("AA", TIGHTENED_LEVEL)))
                    .nesting(List.of(TooltipSection.createSection(
                        List.of(createSubordinateRow("BB", TIGHTENED_LEVEL)))))),
                TIERED_STYLE);

            assertThat(measureStepBelow(layout, FIRST_ROW))
                .isCloseTo(15f + TIGHTENED_GAP, within(TOLERANCE));
        }

        @Test
        void partsTwoBlocksOfATightenedTierByTheStylesBreak() {
            // A tier gap is what stands where no boundary falls, so it cannot close up the boundaries: a
            // block of tightened lines is parted from the next by the box's own break, exactly as any
            // other block is.
            var step = measureSectionStep(
                partIntoSections(
                    createSubordinateRow("AA", TIGHTENED_LEVEL),
                    createSubordinateRow("BB", TIGHTENED_LEVEL)),
                TIERED_STYLE);

            assertThat(step)
                .isCloseTo(15f + SECTION_BREAK, within(TOLERANCE));
        }

        @Test
        void partsTwoNestedBlocksOfATightenedTierByTheStylesGroupBreak() {
            // The same at the inner boundary: an entry that broke down into an account of its own is
            // still set clear of the entry after it, however tightly the tier's own lines stack.
            var layout = layOutSections(
                List.of(buildGroupedSection(
                    createSubordinateRow("AA", TIGHTENED_LEVEL),
                    createSubordinateRow("BB", TIGHTENED_LEVEL),
                    createSubordinateRow("CC", TIGHTENED_LEVEL))),
                TIERED_STYLE);

            var lastLineOfTheFirstGroup = 2;

            assertThat(measureStepBelow(layout, lastLineOfTheFirstGroup))
                .isCloseTo(15f + GROUP_BREAK, within(TOLERANCE));
        }

        @Test
        void resolvesTheGapUnderACentredLineFromTheBoxsOwnVoice() {
            // A centred line has left the table and speaks for the box, so it stands at no tier within
            // it: the line under a centred title takes the box's plain gap however tightly the run it
            // introduces stacks, which is what keeps a tightened listing from closing up on its own title.
            var step = measureRowStep(
                List.of(
                    createCentredRow("AA"),
                    createSubordinateRow("BB", TIGHTENED_LEVEL)),
                TIERED_STYLE);

            assertThat(step)
                .isCloseTo(15f + LINE_GAP, within(TOLERANCE));
        }

        @Test
        void partsTwoBlocksByTheSameBreakWhateverLineHeightsTheyOpenOn() {
            // The point of the parting living on the box rather than on the line that opens a block: a
            // block opened by a 20-tall heading stands exactly as far from the one above it as a block
            // opened by a 15-tall body line, so no box has partings of two different widths in it.
            var headedStep = measureSectionStep(
                partIntoSections(MEMBER, createHeadingRow("AA")),
                TWO_FACE_STYLE);

            var plainStep = measureSectionStep(
                partIntoSections(MEMBER, createCrestlessRow("AA")),
                TWO_FACE_STYLE);

            assertThat(headedStep)
                .isCloseTo(15f + SECTION_BREAK, within(TOLERANCE));
            assertThat(plainStep)
                .isCloseTo(15f + SECTION_BREAK, within(TOLERANCE));
        }

        @Test
        void partsATitleBlockFromTheBodyByTheSameBreakAsTwoBodyBlocks() {
            // The question the box's shape turns on: a title over a body is two blocks like any other
            // two, so the gap under the title cannot come out different from the gaps below it - which
            // is exactly what a break scaled off each opening line used to do.
            var underTheTitleStep = measureSectionStep(
                partIntoSections(createHeadingRow("AA"), MEMBER),
                TWO_FACE_STYLE);

            var betweenBodyBlocksStep = measureSectionStep(
                partIntoSections(MEMBER, MEMBER),
                TWO_FACE_STYLE);

            // The title's own 20-tall line against the body's 15, then the one break in both cases.
            assertThat(underTheTitleStep)
                .isCloseTo(20f + SECTION_BREAK, within(TOLERANCE));
            assertThat(betweenBodyBlocksStep)
                .isCloseTo(15f + SECTION_BREAK, within(TOLERANCE));
        }

        @Test
        void sizesTheBoxForTheGapsItsBlocksImply() {

            var box = layOutSections(partIntoSections(TOP_TIER, MEMBER), UNIFORM_STYLE).box();

            // Two 15-tall lines + the 11.5 break between the blocks + 16 padding = 57.5, where the same
            // two lines in one block come to 50.
            assertThat(box.height())
                .isCloseTo(57.5f, within(TOLERANCE));
        }

        @Test
        void spendsNoBreakAboveTheBoxsFirstBlock() {
            // Nothing to part from - the box's own padding already sits above it - so the break is
            // dropped rather than padding the top edge unevenly.
            var box = layOutSections(
                List.of(TooltipSection.createSection(List.of(TOP_TIER, MEMBER))),
                UNIFORM_STYLE)
                .box();

            assertThat(box.height())
                .isCloseTo(50f, within(TOLERANCE));
        }

        @Test
        void clampsTheBoxInsideTheBoundItIsHanded() {
            // The bound the widget is handed is the one the box is placed against, rather than a screen
            // size it reads for itself: a cursor near the far corner of a small bound pulls the box back
            // inside it (300 - 34 wide, 300 - 31 tall).
            TextSpanMeasurer measurer = CursorTooltipTest::measureSpanWidth;

            var box = CursorTooltip.layOut(
                List.of(TooltipSection.createSection(List.of(createCrestlessRow("AA")))),
                UNIFORM_STYLE,
                measurer,
                290f,
                290f,
                new Rectangle(0f, 0f, 300f, 300f))
                .box();

            assertThat(box.x())
                .isCloseTo(266f, within(TOLERANCE));
            assertThat(box.y())
                .isCloseTo(269f, within(TOLERANCE));
        }

        @Test
        void anchorsASecondRunOneGapPastTheFirst() {

            var continued = createCrestlessRow("AA").continuesWith(new TextSpan("MMM", Color.YELLOW));
            var only = layOut(List.of(continued)).rows().get(0);

            // The run carries on from where the one before it ended rather than sitting in a shared
            // column: label left edge 226 + its measured 2 + the body face's 1-wide space = 229.
            assertThat(readRunX(only, FIRST_RUN))
                .isCloseTo(226f, within(TOLERANCE));
            assertThat(readRunX(only, SECOND_RUN))
                .isCloseTo(229f, within(TOLERANCE));
        }

        @Test
        void anchorsAThirdRunPastTheSecond() {
            // A third colour on one line costs the layout nothing beyond another space: 226 + 2 + 1 =
            // 229 for the second run, + its 3 + 1 = 233 for the third.
            var continued = createCrestlessRow("AA")
                .continuesWith(new TextSpan("MMM", Color.YELLOW))
                .continuesWith(new TextSpan("XX", Color.CYAN));

            var only = layOut(List.of(continued)).rows().get(0);

            assertThat(readRunX(only, THIRD_RUN))
                .isCloseTo(233f, within(TOLERANCE));
        }

        @Test
        void sizesTheBoxForASecondRunsGapAndWidth() {

            var continued = createCrestlessRow("AA").continuesWith(new TextSpan("MMM", Color.YELLOW));
            var box = layOut(List.of(continued)).box();

            // Label 2 + the face's 1-wide space + second run 3 + value gap 16 = 22; + 8 padding each
            // side = 38.
            assertThat(box.width())
                .isCloseTo(38f, within(TOLERANCE));
        }

        @Test
        void anchorsASecondRunPastAReservedCrestColumn() {
            // A run rides off where the label itself starts, so it clears the crest gutter the box
            // reserved rather than being measured from the row's left edge: 226 + crest 15 + gap 6 +
            // label 2 + the face's 1-wide space = 250.
            var continued = createCrestlessRow("AA").continuesWith(new TextSpan("MMM", Color.YELLOW));
            var continuedRow = layOut(List.of(continued, TOP_TIER)).rows().get(0);

            assertThat(readRunX(continuedRow, SECOND_RUN))
                .isCloseTo(250f, within(TOLERANCE));
        }

        @Test
        void chargesNothingForALabelOfOneRun() {
            // The run gap rides with the run it precedes, so a plain one-run label measures exactly what
            // its own text costs (label 2 + value gap 16 + padding 16 = 34) and anchors the one run it
            // has, rather than paying for a column no row fills.
            var layout = layOut(List.of(createCrestlessRow("AA")));

            assertThat(layout.box().width())
                .isCloseTo(34f, within(TOLERANCE));
            assertThat(layout.rows().get(0).labelRunXs())
                .hasSize(1);
        }

        @Test
        void chargesNothingForARunWithNothingToDraw() {
            // A caller that assembles a run from parts and comes up with whitespace gets the line it
            // would have had without it: the box measures as the one-run box does (34), and the blank
            // run anchors where the run before it ended (226 + 2) with no gap opened in front of it.
            var continued = createCrestlessRow("AA").continuesWith(new TextSpan(" ", Color.YELLOW));
            var layout = layOut(List.of(continued));

            assertThat(layout.box().width())
                .isCloseTo(34f, within(TOLERANCE));
            assertThat(readRunX(layout.rows().get(0), SECOND_RUN))
                .isCloseTo(228f, within(TOLERANCE));
        }

        @Test
        void chargesNothingForAValueThatCameOutBlank() {
            // The value column is charged what its slot draws, not what its text would have measured:
            // a caller that assembles a value from parts and comes up with whitespace gets the box it
            // would have had without one (34), where charging the space would have widened it to 35.
            var blankValued = createCrestlessRow("AA").carriesValue(new TextSpan(" ", Color.GRAY));

            assertThat(layOut(List.of(blankValued)).box().width())
                .isCloseTo(34f, within(TOLERANCE));
        }

        @Test
        void opensNoGapInFrontOfTheFirstRunThatDraws() {
            // The gap sits between two runs that draw, not in front of the first one that does, so a
            // label whose opening run came out blank starts flush at the content edge (226) and is
            // measured as though the blank were never written (3 + value gap 16 + padding 16 = 35).
            var continued = createCrestlessRow("").continuesWith(new TextSpan("MMM", Color.YELLOW));
            var layout = layOut(List.of(continued));

            assertThat(layout.box().width())
                .isCloseTo(35f, within(TOLERANCE));
            assertThat(readRunX(layout.rows().get(0), SECOND_RUN))
                .isCloseTo(226f, within(TOLERANCE));
        }

        @Test
        void stacksAHeadingRowAtItsOwnLineHeight() {

            var box = layOut(List.of(createHeadingRow("AA"), MEMBER), TWO_FACE_STYLE).box();

            // The heading's own 20-tall line + the member's 15 + one 4 line gap + 16 padding = 55,
            // rather than the 50 a box that stacked every row at the body's height would come to.
            assertThat(box.height())
                .isCloseTo(55f, within(TOLERANCE));
        }

        @Test
        void stepsTheRowBelowAHeadingDownTheHeadingsOwnLineHeight() {

            var step = measureRowStep(List.of(createHeadingRow("AA"), MEMBER), TWO_FACE_STYLE);

            // The heading's 20 + the member's 4 line gap: a row follows the line above it by that line's
            // height, so a taller heading does not overlap the row under it.
            assertThat(step)
                .isCloseTo(24f, within(TOLERANCE));
        }

        @Test
        void measuresAHeadingRowOnItsOwnFace() {

            var box = layOut(List.of(createHeadingRow("AA")), TWO_FACE_STYLE).box();

            // Label 2 characters at the heading face's 3 units each = 6, + value gap 16 + 16 padding =
            // 38. Measured on the body face it would come to 34, and the wider glyphs it paints in would
            // then overflow the box that sized itself for them.
            assertThat(box.width())
                .isCloseTo(38f, within(TOLERANCE));
        }

        @Test
        void sizesTheBoxToTheWidestRowMeasuredOnItsOwnFace() {

            var rows = List.<TooltipRow>of(createHeadingRow("AAA"), createCrestlessRow("AAAAA"));
            var box = layOut(rows, TWO_FACE_STYLE).box();

            // Heading: 3 characters at 3 = 9 + value gap 16 = 25, against the longer body row's 5 + 16 =
            // 21. The shorter heading is the wider row, so it sizes the box: 25 + 16 padding = 41.
            assertThat(box.width())
                .isCloseTo(41f, within(TOLERANCE));
        }

        @Test
        void laysARowWithNoKindStatedAsABodyLine() {

            var box = layOut(List.of(createCrestlessRow("AA")), TWO_FACE_STYLE).box();

            // A row authored without naming a kind takes the body look, so it measures on the body face
            // (2 + value gap 16 + 16 padding = 34) and stacks at the body's height (15 + 16 = 31) even in
            // a box whose headings look nothing like that.
            assertThat(box.width())
                .isCloseTo(34f, within(TOLERANCE));
            assertThat(box.height())
                .isCloseTo(31f, within(TOLERANCE));
        }

        @Test
        void reservesTheCrestColumnForTheTallestCrestInTheBox() {

            var heading = createCrestlessRow("AA")
                .carriesCrest("crest_heading")
                .readsAs(TooltipLineStyle.HEADER);

            var layout = layOut(List.of(heading, MEMBER), TWO_FACE_STYLE);

            // The heading's crest squares off its own 20-tall line, so the column is 20 + its 6 gap wide
            // and both labels clear it - the heading's at 226 + 26, the member's at 226 + 14 + 26. One
            // column width, or the labels the column exists to line up would each sit at their own
            // offset.
            assertThat(layout.rows().get(0).lineHeight())
                .isCloseTo(20f, within(TOLERANCE));
            assertThat(readRunX(layout.rows().get(0), FIRST_RUN))
                .isCloseTo(252f, within(TOLERANCE));
            assertThat(readRunX(layout.rows().get(1), FIRST_RUN))
                .isCloseTo(266f, within(TOLERANCE));
        }

        @Test
        void collapsesTheCrestColumnWhenOnlyARowOutsideItCarriesACrest() {
            // A crest on a flush row sits at the content edge, not in the column, so it opens no column
            // for the rows that do sit in one - which would otherwise indent them past a gutter holding
            // nothing.
            var flushCrested = createCrestlessRow("AA")
                .carriesCrest("crest")
                .clearsCrestColumn();

            var columnRow = layOut(List.of(flushCrested, createCrestlessRow("BB"))).rows().get(1);

            assertThat(readRunX(columnRow, FIRST_RUN))
                .isCloseTo(226f, within(TOLERANCE));
        }

        @Test
        void anchorsAHeadingRowsSecondRunOnTheHeadingFace() {
            // A run rides off the measured width of what came before it, so a heading's second run has to
            // clear the first as the heading face measures it - and the space parting them is that face's
            // own, three units where the body's is one: 226 + label 2 * 3 + space 3 = 235, where the body
            // face's measurement would have set it at 229 and let the wider label run under it.
            var continued = createHeadingRow("AA").continuesWith(new TextSpan("MMM", Color.YELLOW));
            var continuedRow = layOut(List.of(continued), TWO_FACE_STYLE).rows().get(0);

            assertThat(readRunX(continuedRow, SECOND_RUN))
                .isCloseTo(235f, within(TOLERANCE));
        }

        @Test
        void measuresARowAsItsStyleWillDrawIt() {

            var shoutingStyle = TooltipStyle.createStyle(
                createStyle(BODY_FONT, BODY_LINE_HEIGHT),
                createStyle(BODY_FONT, BODY_LINE_HEIGHT).inUpperCase());

            var box = layOut(List.of(createCrestlessRow("iiii")), shoutingStyle).box();

            // Shouted, the label's four narrow i glyphs become four wide ones: 4 * 4 = 16 + value gap 16
            // + 16 padding = 48, where measuring the text as authored would size the box at 36 and clip
            // the wider line it then painted.
            assertThat(box.width())
                .isCloseTo(48f, within(TOLERANCE));
        }
    }

    @Nested
    class MeasureBoxHeight {
        
        @Test
        void measuresEachLinePlusTheGapAndPadding() {

            // The 50 the laid-out box comes to: two 15-tall lines + one 4 line gap + 8 padding top and
            // bottom. Weighed as anything else, a box that fits would be cut and one that does not left
            // running off the screen.
            assertThat(measureBoxHeight(List.of(TOP_TIER, MEMBER), UNIFORM_STYLE))
                .isCloseTo(50f, within(TOLERANCE));
        }

        @Test
        void growsByALineAndItsGapPerRow() {

            // Three 15-tall lines + two 4 line gaps + 16 padding.
            assertThat(measureBoxHeight(List.of(TOP_TIER, MEMBER, MEMBER), UNIFORM_STYLE))
                .isCloseTo(69f, within(TOLERANCE));
        }

        @Test
        void spendsTheStylesBreakBetweenTwoBlocks() {

            // Two 15-tall lines parted by the 11.5 break rather than the 4 gap, + 16 padding: what a box
            // spends on nothing is as much of its height as its lines are.
            assertThat(CursorTooltip.measureBoxHeight(
                    partIntoSections(TOP_TIER, MEMBER),
                    UNIFORM_STYLE))
                .isCloseTo(57.5f, within(TOLERANCE));
        }

        @Test
        void stacksARunOfATightenedTierAtItsOwnGap() {

            var tightenedRows = List.<TooltipRow>of(
                createSubordinateRow("AA", TIGHTENED_LEVEL),
                createSubordinateRow("BB", TIGHTENED_LEVEL));

            // Two 15-tall lines + the tightened tier's own 1 gap + 16 padding, where the plain 4 would
            // have measured 50.
            assertThat(measureBoxHeight(tightenedRows, TIERED_STYLE))
                .isCloseTo(47f, within(TOLERANCE));
        }

        @Test
        void shrinksASubordinateLineByTheStylesLevelShrink() {

            var demotedRows = List.<TooltipRow>of(
                createCrestlessRow("AA"),
                createSubordinateRow("BB", ONE_STEP_UNDER));

            // The box's own voice at 15 over a line demoted one step to 13 + the 4 gap + 16 padding: a
            // height read off the kinds alone would measure 50 and leave two units of the box unaccounted.
            assertThat(measureBoxHeight(demotedRows, SHRINKING_STYLE))
                .isCloseTo(48f, within(TOLERANCE));
        }

        @Test
        void measuresThePaddingAloneForABoxOfNoBlocks() {

            // A box with nothing in it is still its own chrome.
            assertThat(CursorTooltip.measureBoxHeight(List.of(), UNIFORM_STYLE))
                .isCloseTo(16f, within(TOLERANCE));
        }
    }
}
