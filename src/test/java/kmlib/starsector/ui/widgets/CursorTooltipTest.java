package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.LineWidthMeasurer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link CursorTooltip}'s row layout: the box sizes to the widest row across indent tiers and to
 * however tall the rows stack, each row's crest, label, marker, and value anchor within the placed box,
 * a crest-less row still reserves its column unless it steps out of it, and a row opening a section is
 * parted from the one above it. The box padding and screen clamp themselves are {@link
 * kmlib.starsector.ui.layout.TooltipBoxLayout}'s and covered there; this fixes the row model on top of
 * it. The cursor sits clear of every edge so no clamp perturbs the anchors under test.
 */
class CursorTooltipTest {
    // Measures each line as one unit per character, ignoring the size, so a label's width is its length
    // and the arithmetic below reads off the strings directly.
    private static final LineWidthMeasurer MEASURER = (line, fontSize) -> line.length();
    private static final double LINE_HEIGHT = 15d;
    private static final float SCREEN_WIDTH = 1920f;
    private static final float SCREEN_HEIGHT = 1080f;
    private static final float CURSOR_X = 200f;
    private static final float CURSOR_Y = 300f;
    private static final float TOLERANCE = 0.001f;

    // A flush top-tier header (short label, no value) and a wider indented member (longer label, a
    // value): the member is the widest laid-out row, so it must drive the box width even though it is
    // the indented one - the point of measuring across tiers.
    private static final TooltipRow HEADER = TooltipRow.createRow("AA", Color.WHITE)
            .carriesCrest("crest_a");
    private static final TooltipRow MEMBER = TooltipRow.createRow("BBBB", Color.LIGHT_GRAY)
            .carriesCrest("crest_b")
            .carriesValue("9", Color.GRAY)
            .indentsBy(14f);

    private static TooltipLayout layOut(List<TooltipRow> rows) {
        return CursorTooltip.layOut(rows, LINE_HEIGHT, MEASURER, CURSOR_X, CURSOR_Y, SCREEN_WIDTH,
                SCREEN_HEIGHT);
    }

    private static TooltipRow crestlessRow(String text) {
        return TooltipRow.createRow(text, Color.WHITE);
    }

    // How far the second row sits below the first, which is what a section break widens - unlike the
    // rows' absolute anchors, which a taller box shifts wholesale as it grows up from its cursor.
    private static float measureRowStep(List<TooltipRow> rows) {
        var laidOut = layOut(rows).rows();
        return laidOut.get(0).textY() - laidOut.get(1).textY();
    }

    @Nested
    class LayOut {
        @Test
        void sizesTheBoxToTheWidestRowAcrossIndentTiers() {
            var box = layOut(List.of(HEADER, MEMBER)).box();

            // Member row: indent 14 + crest 15 + crest gap 6 + label 4 + value gap 16 + value 1 = 56,
            // wider than the header's 39; + 8 padding on each side = 72.
            assertThat(box.width()).isCloseTo(72f, within(TOLERANCE));
        }

        @Test
        void sizesTheBoxHeightForEachLinePlusTheGapAndPadding() {
            var box = layOut(List.of(HEADER, MEMBER)).box();

            // Two 15-tall lines + one 4 line gap + 8 padding top and bottom = 50.
            assertThat(box.height()).isCloseTo(50f, within(TOLERANCE));
        }

        @Test
        void anchorsTheHeaderRowsCrestLabelAndValue() {
            var layout = layOut(List.of(HEADER, MEMBER));
            var header = layout.rows().get(0);

            // Box at (218, 318): left content edge 226, right 282, top content edge 360. The header
            // sits at zero indent, so its crest hangs from the top edge at the left content edge.
            assertThat(header.crestBox()).isEqualTo(new Rectangle(226f, 345f, 15f, 15f));
            assertThat(header.textX()).isCloseTo(247f, within(TOLERANCE));
            assertThat(header.textY()).isCloseTo(360f, within(TOLERANCE));
            assertThat(header.valueX()).isCloseTo(282f, within(TOLERANCE));
            assertThat(header.valueY()).isCloseTo(360f, within(TOLERANCE));
        }

        @Test
        void indentsTheMemberRowAndStepsItDownOneLine() {
            var layout = layOut(List.of(HEADER, MEMBER));
            var member = layout.rows().get(1);

            // One line height 15 + gap 4 below the header's 360 -> 341; the crest and label shift right
            // by the 14 indent, while the value stays pinned to the box's right content edge.
            assertThat(member.textY()).isCloseTo(341f, within(TOLERANCE));
            assertThat(member.crestBox()).isEqualTo(new Rectangle(240f, 326f, 15f, 15f));
            assertThat(member.textX()).isCloseTo(261f, within(TOLERANCE));
            assertThat(member.valueX()).isCloseTo(282f, within(TOLERANCE));
        }

        @Test
        void reservesTheCrestColumnForACrestLessRowWhenAnotherRowCarriesACrest() {
            var crestless = crestlessRow("AA");
            var crested = crestlessRow("BB").carriesCrest("crest");

            var crestlessRow = layOut(List.of(crestless, crested)).rows().get(0);

            // The box carries a crest, so the crest-less row still reserves the column (null crest box,
            // label anchored past the reserved 15 + 6 gutter) and lines up under the crested row.
            assertThat(crestlessRow.crestBox()).isNull();
            assertThat(crestlessRow.textX()).isCloseTo(247f, within(TOLERANCE));
        }

        @Test
        void collapsesTheCrestColumnWhenNoRowCarriesACrest() {
            var only = layOut(List.of(crestlessRow("AA"))).rows().get(0);

            // No row carries a crest, so the gutter collapses and the label lays flush at the left
            // content edge (226) rather than past a phantom crest column - the empty-state box case.
            assertThat(only.crestBox()).isNull();
            assertThat(only.textX()).isCloseTo(226f, within(TOLERANCE));
        }

        @Test
        void laysARowThatClearsTheCrestColumnFlushWithTheContentEdge() {
            // A title over a crested list: it names the box rather than sitting in the list, so it
            // ignores the gutter the rows below reserve and starts at the content edge (226).
            var title = crestlessRow("AA").clearsCrestColumn();

            var titleRow = layOut(List.of(title, MEMBER)).rows().get(0);

            assertThat(titleRow.textX()).isCloseTo(226f, within(TOLERANCE));
        }

        @Test
        void sizesARowThatClearsTheCrestColumnWithoutTheGutter() {
            // The width measurement drops the gutter for that row too, so a long title does not push
            // the box wider by a column it never occupies.
            var title = crestlessRow("AAAAAAAAAA").clearsCrestColumn();

            var box = layOut(List.of(title, MEMBER)).box();

            // Title: label 10 + value gap 16 = 26, under the member's 56, so the member still sizes the
            // box at 56 + 16 padding = 72 - which it would not if the title were charged the gutter.
            assertThat(box.width()).isCloseTo(72f, within(TOLERANCE));
        }

        @Test
        void partsARowThatOpensASectionFromTheOneAboveIt() {
            // Asserted as the step between the two rows, not their absolute anchors: the box is pinned
            // at its lower-left corner and grows upward, so a taller box lifts every row's y together
            // while the parting between them is what the break actually changes.
            var plainStep = measureRowStep(List.of(HEADER, MEMBER));

            var brokenStep = measureRowStep(List.of(HEADER, MEMBER.opensSection()));

            // A line 15 + the 4 line gap normally; the break adds half a line (7.5) on top.
            assertThat(plainStep).isCloseTo(19f, within(TOLERANCE));
            assertThat(brokenStep).isCloseTo(26.5f, within(TOLERANCE));
        }

        @Test
        void sizesTheBoxForASectionBreak() {
            var box = layOut(List.of(HEADER, MEMBER.opensSection())).box();

            // The two-row box (50) plus the half-line break it now holds = 57.5.
            assertThat(box.height()).isCloseTo(57.5f, within(TOLERANCE));
        }

        @Test
        void ignoresASectionBreakOnTheFirstRow() {
            // Nothing to part from - the box's own padding already sits above it - so the break is
            // dropped rather than padding the top edge unevenly.
            var box = layOut(List.of(HEADER.opensSection(), MEMBER)).box();

            assertThat(box.height()).isCloseTo(50f, within(TOLERANCE));
        }

        @Test
        void anchorsAMarkerOneGapPastItsOwnLabel() {
            var marked = crestlessRow("AA").carriesMarker("MMM", Color.YELLOW);

            var only = layOut(List.of(marked)).rows().get(0);

            // The marker trails the label it qualifies rather than a shared column: label left edge
            // 226 + its measured 2 + the 6 marker gap = 234.
            assertThat(only.textX()).isCloseTo(226f, within(TOLERANCE));
            assertThat(only.markerX()).isCloseTo(234f, within(TOLERANCE));
        }

        @Test
        void sizesTheBoxForAMarkersGapAndWidth() {
            var marked = crestlessRow("AA").carriesMarker("MMM", Color.YELLOW);

            var box = layOut(List.of(marked)).box();

            // Label 2 + marker gap 6 + marker 3 + value gap 16 = 27; + 8 padding each side = 43.
            assertThat(box.width()).isCloseTo(43f, within(TOLERANCE));
        }

        @Test
        void anchorsAMarkerPastAReservedCrestColumn() {
            // The marker rides off the label's own anchor, so it clears the crest gutter the box
            // reserved rather than being measured from the row's left edge: 226 + crest 15 + gap 6 +
            // label 2 + marker gap 6 = 255.
            var marked = crestlessRow("AA").carriesMarker("MMM", Color.YELLOW);

            var markedRow = layOut(List.of(marked, HEADER)).rows().get(0);

            assertThat(markedRow.markerX()).isCloseTo(255f, within(TOLERANCE));
        }

        @Test
        void chargesNothingForAnUnmarkedRow() {
            // The marker gap rides with the marker, so an unmarked row measures exactly as it did
            // before markers existed (label 2 + value gap 16 + padding 16 = 34) and anchors its unused
            // marker at its own label, rather than paying for a column no row fills.
            var layout = layOut(List.of(crestlessRow("AA")));

            assertThat(layout.box().width()).isCloseTo(34f, within(TOLERANCE));
            assertThat(layout.rows().get(0).markerX())
                    .isCloseTo(layout.rows().get(0).textX(), within(TOLERANCE));
        }
    }
}
