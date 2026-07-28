package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.font.TextSpanMeasurer;
import kmlib.starsector.ui.text.TextAlignment;
import kmlib.starsector.ui.text.TextStyle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link CursorTooltip}'s row layout: the box sizes to the widest row across indent tiers and to
 * however tall the rows stack, each row's crest, label runs, and value anchor within the placed box, a
 * crest-less row still reserves its column unless it steps out of it, a centred row lays as a
 * standalone span in the middle of the content region, and a row opening a section is parted from the
 * one above it. On top of that, each row is measured and stacked in the look its own kind of line
 * resolves to, which is the whole reason a box can hold a heading and a body line at once. The box
 * padding and screen clamp themselves are {@link kmlib.starsector.ui.layout.TooltipBoxLayout}'s and
 * covered there; this fixes the row model on top of it. The cursor sits clear of every edge so no clamp
 * perturbs the anchors under test.
 */
class CursorTooltipTest {
    private static final StarsectorFont BODY_FONT = StarsectorFont.VANILLA_INSIGNIA_15;
    private static final StarsectorFont HEADING_FONT = StarsectorFont.VANILLA_ORBITRON_20AA;
    private static final double BODY_LINE_HEIGHT = 15d;
    private static final double HEADING_LINE_HEIGHT = 20d;

    // How the stand-in faces measure. The body face charges one unit per character, so a label's width
    // is its length and the arithmetic below reads off the strings directly; the heading face charges
    // triple, standing in for a genuinely wider atlas, so a measured width says which face measured it.
    private static final double BODY_WIDTH_PER_CHARACTER = 1d;
    private static final double HEADING_WIDTH_PER_CHARACTER = 3d;

    // A capital I costs four units where a lower-case one costs one, so a width also says whether a span
    // was measured as its caller authored it or as its style will actually draw it. No other label here
    // holds an i, so this prices nothing but the casing case.
    private static final double SHOUTED_I_WIDTH = 4d;

    // The runs of a label, by position: the first is the label a row is authored with, and the ones past
    // it are what a caller appended to pick part of the line out in another colour.
    private static final int FIRST_RUN = 0;
    private static final int SECOND_RUN = 1;
    private static final int THIRD_RUN = 2;

    private static final float SCREEN_WIDTH = 1920f;
    private static final float SCREEN_HEIGHT = 1080f;
    private static final float CURSOR_X = 200f;
    private static final float CURSOR_Y = 300f;
    private static final float TOLERANCE = 0.001f;

    // A box whose headings and body lines look alike, and one where they differ in both face and size.
    // The uniform style is what every case not about per-row looks lays out through, so those cases read
    // as the plain row-model arithmetic they are testing rather than as typography.
    private static final TooltipStyle UNIFORM_STYLE = new TooltipStyle(
            createStyle(BODY_FONT, BODY_LINE_HEIGHT),
            createStyle(BODY_FONT, BODY_LINE_HEIGHT));
    private static final TooltipStyle TWO_FACE_STYLE = new TooltipStyle(
            createStyle(HEADING_FONT, HEADING_LINE_HEIGHT),
            createStyle(BODY_FONT, BODY_LINE_HEIGHT));

    // A top-tier row at no indent (short label, no value) and a wider indented member (longer label, a
    // value): the member is the widest laid-out row, so it must drive the box width even though it is the
    // indented one - the point of measuring across tiers. Both are crest-aligned body lines, as a row
    // built without stating a placement or a kind of line is.
    private static final TooltipRow TOP_TIER = TooltipRow.createRow("AA", Color.WHITE)
            .carriesCrest("crest_a");
    private static final TooltipRow MEMBER = TooltipRow.createRow("BBBB", Color.LIGHT_GRAY)
            .carriesCrest("crest_b")
            .carriesValue("9", Color.GRAY)
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
        var widthPerCharacter = face.font() == HEADING_FONT
                ? HEADING_WIDTH_PER_CHARACTER
                : BODY_WIDTH_PER_CHARACTER;

        var characterCost = 0d;
        for (var character : span.toCharArray()) {
            characterCost += character == 'I' ? SHOUTED_I_WIDTH : 1d;
        }
        return characterCost * widthPerCharacter;
    }

    private static TooltipLayout layOut(List<TooltipRow> rows) {
        return layOut(rows, UNIFORM_STYLE);
    }

    private static TooltipLayout layOut(List<TooltipRow> rows, TooltipStyle style) {
        TextSpanMeasurer measurer = CursorTooltipTest::measureSpanWidth;
        return CursorTooltip.layOut(rows, style, measurer, CURSOR_X, CURSOR_Y, SCREEN_WIDTH,
                SCREEN_HEIGHT);
    }

    private static TooltipRow createCrestlessRow(String text) {
        return TooltipRow.createRow(text, Color.WHITE);
    }

    private static TooltipRow createHeadingRow(String text) {
        return createCrestlessRow(text)
                .clearsCrestColumn()
                .readsAs(TooltipLineStyle.HEADER);
    }

    // Where one of a row's label runs anchors. Read by position rather than by name, since the runs are
    // a sequence: the first is where the label starts, and each one past it is where the line carries on.
    private static float readRunX(TooltipLayout.TooltipRowLayout rowLayout, int runIndex) {
        return rowLayout.labelRunXs().get(runIndex);
    }

    // How far the second row sits below the first, which is what a section break widens - unlike the
    // rows' absolute anchors, which a taller box shifts wholesale as it grows up from its cursor.
    private static float measureRowStep(List<TooltipRow> rows, TooltipStyle style) {
        var laidOut = layOut(rows, style).rows();
        return laidOut.get(0).labelY() - laidOut.get(1).labelY();
    }

    @Nested
    class LayOut {
        @Test
        void sizesTheBoxToTheWidestRowAcrossIndentTiers() {
            var box = layOut(List.of(TOP_TIER, MEMBER)).box();

            // Member row: indent 14 + crest 15 + crest gap 6 + label 4 + value gap 16 + value 1 = 56,
            // wider than the top-tier row's 39; + 8 padding on each side = 72.
            assertThat(box.width()).isCloseTo(72f, within(TOLERANCE));
        }

        @Test
        void sizesTheBoxHeightForEachLinePlusTheGapAndPadding() {
            var box = layOut(List.of(TOP_TIER, MEMBER)).box();

            // Two 15-tall lines + one 4 line gap + 8 padding top and bottom = 50.
            assertThat(box.height()).isCloseTo(50f, within(TOLERANCE));
        }

        @Test
        void anchorsTheTopTierRowsCrestLabelAndValue() {
            var layout = layOut(List.of(TOP_TIER, MEMBER));
            var topTier = layout.rows().get(0);

            // Box at (218, 318): left content edge 226, right 282, top content edge 360. The row sits at
            // zero indent, so its crest hangs from the top edge at the left content edge.
            assertThat(topTier.crestBox()).isEqualTo(new Rectangle(226f, 345f, 15f, 15f));
            assertThat(readRunX(topTier, FIRST_RUN)).isCloseTo(247f, within(TOLERANCE));
            assertThat(topTier.labelY()).isCloseTo(360f, within(TOLERANCE));
            assertThat(topTier.valueX()).isCloseTo(282f, within(TOLERANCE));
            assertThat(topTier.valueY()).isCloseTo(360f, within(TOLERANCE));
        }

        @Test
        void indentsTheMemberRowAndStepsItDownOneLine() {
            var layout = layOut(List.of(TOP_TIER, MEMBER));
            var member = layout.rows().get(1);

            // One line height 15 + gap 4 below the row above's 360 -> 341; the crest and label shift
            // right by the 14 indent, while the value stays pinned to the box's right content edge.
            assertThat(member.labelY()).isCloseTo(341f, within(TOLERANCE));
            assertThat(member.crestBox()).isEqualTo(new Rectangle(240f, 326f, 15f, 15f));
            assertThat(readRunX(member, FIRST_RUN)).isCloseTo(261f, within(TOLERANCE));
            assertThat(member.valueX()).isCloseTo(282f, within(TOLERANCE));
        }

        @Test
        void reservesTheCrestColumnForACrestLessRowWhenAnotherRowCarriesACrest() {
            var crestless = createCrestlessRow("AA");
            var crested = createCrestlessRow("BB").carriesCrest("crest");

            var crestlessRow = layOut(List.of(crestless, crested)).rows().get(0);

            // The box carries a crest, so the crest-less row still reserves the column (null crest box,
            // label anchored past the reserved 15 + 6 gutter) and lines up under the crested row.
            assertThat(crestlessRow.crestBox()).isNull();
            assertThat(readRunX(crestlessRow, FIRST_RUN)).isCloseTo(247f, within(TOLERANCE));
        }

        @Test
        void collapsesTheCrestColumnWhenNoRowCarriesACrest() {
            var only = layOut(List.of(createCrestlessRow("AA"))).rows().get(0);

            // No row carries a crest, so the gutter collapses and the label lays flush at the left
            // content edge (226) rather than past a phantom crest column - the empty-state box case.
            assertThat(only.crestBox()).isNull();
            assertThat(readRunX(only, FIRST_RUN)).isCloseTo(226f, within(TOLERANCE));
        }

        @Test
        void laysARowThatClearsTheCrestColumnFlushWithTheContentEdge() {
            // A title over a crested list: it names the box rather than sitting in the list, so it
            // ignores the gutter the rows below reserve and starts at the content edge (226).
            var title = createCrestlessRow("AA").clearsCrestColumn();

            var titleRow = layOut(List.of(title, MEMBER)).rows().get(0);

            assertThat(readRunX(titleRow, FIRST_RUN)).isCloseTo(226f, within(TOLERANCE));
        }

        @Test
        void sizesARowThatClearsTheCrestColumnWithoutTheGutter() {
            // The width measurement drops the gutter for that row too, so a long title does not push
            // the box wider by a column it never occupies.
            var title = createCrestlessRow("AAAAAAAAAA").clearsCrestColumn();

            var box = layOut(List.of(title, MEMBER)).box();

            // Title: label 10 + value gap 16 = 26, under the member's 56, so the member still sizes the
            // box at 56 + 16 padding = 72 - which it would not if the title were charged the gutter.
            assertThat(box.width()).isCloseTo(72f, within(TOLERANCE));
        }

        @Test
        void centresACentredRowInTheContentRegion() {
            // The status-line case: a wide title sizes the box (label 10 + value gap 16 = 26, box 42),
            // and the short centred line sits in the middle of that 26-wide region - 226 + (26 - 2) / 2.
            var title = createCrestlessRow("AAAAAAAAAA").clearsCrestColumn();
            var centred = createCrestlessRow("BB").centred();

            var centredRow = layOut(List.of(title, centred)).rows().get(1);

            assertThat(readRunX(centredRow, FIRST_RUN)).isCloseTo(238f, within(TOLERANCE));
        }

        @Test
        void ignoresAnIndentOnACentredRow() {
            // A row built as a nested entry and then centred is no longer an entry: the indent that
            // would have set it under the line above is dropped rather than shifting it off the middle.
            var title = createCrestlessRow("AAAAAAAAAA").clearsCrestColumn();
            var centred = createCrestlessRow("BB").indentsBy(14f).centred();

            var centredRow = layOut(List.of(title, centred)).rows().get(1);

            assertThat(readRunX(centredRow, FIRST_RUN)).isCloseTo(238f, within(TOLERANCE));
        }

        @Test
        void sizesACentredRowToItsLabelSpanAlone() {
            // Label 2 + 8 padding each side = 18, where the same row uncentred would measure 34 for the
            // value column it never occupies - and a box widened for that column would then centre the
            // line against space nothing fills.
            var box = layOut(List.of(createCrestlessRow("AA").centred())).box();

            assertThat(box.width()).isCloseTo(18f, within(TOLERANCE));
        }

        @Test
        void centresACentredRowsLabelRunsAsOneSpan() {
            // Span: first run 2 + run gap 6 + second run 3 = 11, centred in the title's 26-wide region,
            // so the label starts at 226 + (26 - 11) / 2 and the second run still follows one gap past
            // it - the runs centre together because they are one sentence, not two columns.
            var title = createCrestlessRow("AAAAAAAAAA").clearsCrestColumn();
            var centred = createCrestlessRow("BB")
                    .continuesWith("MMM", Color.YELLOW)
                    .centred();

            var centredRow = layOut(List.of(title, centred)).rows().get(1);

            assertThat(readRunX(centredRow, FIRST_RUN)).isCloseTo(233.5f, within(TOLERANCE));
            assertThat(readRunX(centredRow, SECOND_RUN)).isCloseTo(241.5f, within(TOLERANCE));
        }

        @Test
        void partsARowThatOpensASectionFromTheOneAboveIt() {
            // Asserted as the step between the two rows, not their absolute anchors: the box is pinned
            // at its lower-left corner and grows upward, so a taller box lifts every row's y together
            // while the parting between them is what the break actually changes.
            var plainStep = measureRowStep(List.of(TOP_TIER, MEMBER), UNIFORM_STYLE);

            var brokenStep = measureRowStep(List.of(TOP_TIER, MEMBER.opensSection()), UNIFORM_STYLE);

            // A line 15 + the 4 line gap normally; the break adds half a line (7.5) on top.
            assertThat(plainStep).isCloseTo(19f, within(TOLERANCE));
            assertThat(brokenStep).isCloseTo(26.5f, within(TOLERANCE));
        }

        @Test
        void sizesTheBoxForASectionBreak() {
            var box = layOut(List.of(TOP_TIER, MEMBER.opensSection())).box();

            // The two-row box (50) plus the half-line break it now holds = 57.5.
            assertThat(box.height()).isCloseTo(57.5f, within(TOLERANCE));
        }

        @Test
        void ignoresASectionBreakOnTheFirstRow() {
            // Nothing to part from - the box's own padding already sits above it - so the break is
            // dropped rather than padding the top edge unevenly.
            var box = layOut(List.of(TOP_TIER.opensSection(), MEMBER)).box();

            assertThat(box.height()).isCloseTo(50f, within(TOLERANCE));
        }

        @Test
        void anchorsASecondRunOneGapPastTheFirst() {
            var continued = createCrestlessRow("AA").continuesWith("MMM", Color.YELLOW);

            var only = layOut(List.of(continued)).rows().get(0);

            // The run carries on from where the one before it ended rather than sitting in a shared
            // column: label left edge 226 + its measured 2 + the 6 run gap = 234.
            assertThat(readRunX(only, FIRST_RUN)).isCloseTo(226f, within(TOLERANCE));
            assertThat(readRunX(only, SECOND_RUN)).isCloseTo(234f, within(TOLERANCE));
        }

        @Test
        void anchorsAThirdRunPastTheSecond() {
            // A third colour on one line costs the layout nothing beyond another gap: 226 + 2 + 6 = 234
            // for the second run, + its 3 + 6 = 243 for the third.
            var continued = createCrestlessRow("AA")
                    .continuesWith("MMM", Color.YELLOW)
                    .continuesWith("XX", Color.CYAN);

            var only = layOut(List.of(continued)).rows().get(0);

            assertThat(readRunX(only, THIRD_RUN)).isCloseTo(243f, within(TOLERANCE));
        }

        @Test
        void sizesTheBoxForASecondRunsGapAndWidth() {
            var continued = createCrestlessRow("AA").continuesWith("MMM", Color.YELLOW);

            var box = layOut(List.of(continued)).box();

            // Label 2 + run gap 6 + second run 3 + value gap 16 = 27; + 8 padding each side = 43.
            assertThat(box.width()).isCloseTo(43f, within(TOLERANCE));
        }

        @Test
        void anchorsASecondRunPastAReservedCrestColumn() {
            // A run rides off where the label itself starts, so it clears the crest gutter the box
            // reserved rather than being measured from the row's left edge: 226 + crest 15 + gap 6 +
            // label 2 + run gap 6 = 255.
            var continued = createCrestlessRow("AA").continuesWith("MMM", Color.YELLOW);

            var continuedRow = layOut(List.of(continued, TOP_TIER)).rows().get(0);

            assertThat(readRunX(continuedRow, SECOND_RUN)).isCloseTo(255f, within(TOLERANCE));
        }

        @Test
        void chargesNothingForALabelOfOneRun() {
            // The run gap rides with the run it precedes, so a plain one-run label measures exactly what
            // its own text costs (label 2 + value gap 16 + padding 16 = 34) and anchors the one run it
            // has, rather than paying for a column no row fills.
            var layout = layOut(List.of(createCrestlessRow("AA")));

            assertThat(layout.box().width()).isCloseTo(34f, within(TOLERANCE));
            assertThat(layout.rows().get(0).labelRunXs()).hasSize(1);
        }

        @Test
        void chargesNothingForARunWithNothingToDraw() {
            // A caller that assembles a run from parts and comes up with whitespace gets the line it
            // would have had without it: the box measures as the one-run box does (34), and the blank
            // run anchors where the run before it ended (226 + 2) with no gap opened in front of it.
            var continued = createCrestlessRow("AA").continuesWith(" ", Color.YELLOW);

            var layout = layOut(List.of(continued));

            assertThat(layout.box().width()).isCloseTo(34f, within(TOLERANCE));
            assertThat(readRunX(layout.rows().get(0), SECOND_RUN)).isCloseTo(228f, within(TOLERANCE));
        }

        @Test
        void chargesNothingForAValueThatCameOutBlank() {
            // The value column is charged what its slot draws, not what its text would have measured:
            // a caller that assembles a value from parts and comes up with whitespace gets the box it
            // would have had without one (34), where charging the space would have widened it to 35.
            var blankValued = createCrestlessRow("AA").carriesValue(" ", Color.GRAY);

            assertThat(layOut(List.of(blankValued)).box().width()).isCloseTo(34f, within(TOLERANCE));
        }

        @Test
        void opensNoGapInFrontOfTheFirstRunThatDraws() {
            // The gap sits between two runs that draw, not in front of the first one that does, so a
            // label whose opening run came out blank starts flush at the content edge (226) and is
            // measured as though the blank were never written (3 + value gap 16 + padding 16 = 35).
            var continued = createCrestlessRow("").continuesWith("MMM", Color.YELLOW);

            var layout = layOut(List.of(continued));

            assertThat(layout.box().width()).isCloseTo(35f, within(TOLERANCE));
            assertThat(readRunX(layout.rows().get(0), SECOND_RUN)).isCloseTo(226f, within(TOLERANCE));
        }

        @Test
        void stacksAHeadingRowAtItsOwnLineHeight() {
            var box = layOut(List.of(createHeadingRow("AA"), MEMBER), TWO_FACE_STYLE).box();

            // The heading's own 20-tall line + the member's 15 + one 4 line gap + 16 padding = 55,
            // rather than the 50 a box that stacked every row at the body's height would come to.
            assertThat(box.height()).isCloseTo(55f, within(TOLERANCE));
        }

        @Test
        void stepsTheRowBelowAHeadingDownTheHeadingsOwnLineHeight() {
            var step = measureRowStep(List.of(createHeadingRow("AA"), MEMBER), TWO_FACE_STYLE);

            // The heading's 20 + the member's 4 line gap: a row follows the line above it by that line's
            // height, so a taller heading does not overlap the row under it.
            assertThat(step).isCloseTo(24f, within(TOLERANCE));
        }

        @Test
        void measuresAHeadingRowOnItsOwnFace() {
            var box = layOut(List.of(createHeadingRow("AA")), TWO_FACE_STYLE).box();

            // Label 2 characters at the heading face's 3 units each = 6, + value gap 16 + 16 padding =
            // 38. Measured on the body face it would come to 34, and the wider glyphs it paints in would
            // then overflow the box that sized itself for them.
            assertThat(box.width()).isCloseTo(38f, within(TOLERANCE));
        }

        @Test
        void sizesTheBoxToTheWidestRowMeasuredOnItsOwnFace() {
            var rows = List.of(createHeadingRow("AAA"), createCrestlessRow("AAAAA"));

            var box = layOut(rows, TWO_FACE_STYLE).box();

            // Heading: 3 characters at 3 = 9 + value gap 16 = 25, against the longer body row's 5 + 16 =
            // 21. The shorter heading is the wider row, so it sizes the box: 25 + 16 padding = 41.
            assertThat(box.width()).isCloseTo(41f, within(TOLERANCE));
        }

        @Test
        void laysARowWithNoKindStatedAsABodyLine() {
            var box = layOut(List.of(createCrestlessRow("AA")), TWO_FACE_STYLE).box();

            // A row authored without naming a kind takes the body look, so it measures on the body face
            // (2 + value gap 16 + 16 padding = 34) and stacks at the body's height (15 + 16 = 31) even in
            // a box whose headings look nothing like that.
            assertThat(box.width()).isCloseTo(34f, within(TOLERANCE));
            assertThat(box.height()).isCloseTo(31f, within(TOLERANCE));
        }

        @Test
        void partsASectionByTheOpeningRowsOwnLineHeight() {
            var step = measureRowStep(
                    List.of(MEMBER, createHeadingRow("AA").opensSection()),
                    TWO_FACE_STYLE);

            // The member's 15 + its 4 line gap + half of the heading's own 20-tall line = 29, so a
            // heading opens a section with the breathing room its own size asks for.
            assertThat(step).isCloseTo(29f, within(TOLERANCE));
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
            assertThat(layout.rows().get(0).crestBox().height()).isCloseTo(20f, within(TOLERANCE));
            assertThat(readRunX(layout.rows().get(0), FIRST_RUN)).isCloseTo(252f, within(TOLERANCE));
            assertThat(readRunX(layout.rows().get(1), FIRST_RUN)).isCloseTo(266f, within(TOLERANCE));
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

            assertThat(readRunX(columnRow, FIRST_RUN)).isCloseTo(226f, within(TOLERANCE));
        }

        @Test
        void anchorsAHeadingRowsSecondRunOnTheHeadingFace() {
            // A run rides off the measured width of what came before it, so a heading's second run has to
            // clear the first as the heading face measures it: 226 + label 2 * 3 + run gap 6 = 238, where
            // the body face's measurement would have set it at 234 and let the wider label run under it.
            var continued = createHeadingRow("AA").continuesWith("MMM", Color.YELLOW);

            var continuedRow = layOut(List.of(continued), TWO_FACE_STYLE).rows().get(0);

            assertThat(readRunX(continuedRow, SECOND_RUN)).isCloseTo(238f, within(TOLERANCE));
        }

        @Test
        void measuresARowAsItsStyleWillDrawIt() {
            var shoutingStyle = new TooltipStyle(
                    createStyle(BODY_FONT, BODY_LINE_HEIGHT),
                    createStyle(BODY_FONT, BODY_LINE_HEIGHT).inUpperCase());

            var box = layOut(List.of(createCrestlessRow("iiii")), shoutingStyle).box();

            // Shouted, the label's four narrow i glyphs become four wide ones: 4 * 4 = 16 + value gap 16
            // + 16 padding = 48, where measuring the text as authored would size the box at 36 and clip
            // the wider line it then painted.
            assertThat(box.width()).isCloseTo(48f, within(TOLERANCE));
        }
    }
}
