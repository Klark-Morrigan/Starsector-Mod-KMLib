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
 * Pins {@link CursorTooltip}'s row layout: the box sizes to the widest row across indent tiers, each
 * row's crest, label, and value anchor within the placed box, and a crest-less row still reserves its
 * column. The box sizing and screen clamp themselves are {@link
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
    private static final TooltipRow HEADER = new TooltipRow(0f, "crest_a", "AA", Color.WHITE, "",
            Color.GRAY);
    private static final TooltipRow MEMBER = new TooltipRow(14f, "crest_b", "BBBB", Color.LIGHT_GRAY,
            "9", Color.GRAY);

    private static TooltipLayout layOut(List<TooltipRow> rows) {
        return CursorTooltip.layOut(rows, LINE_HEIGHT, MEASURER, CURSOR_X, CURSOR_Y, SCREEN_WIDTH,
                SCREEN_HEIGHT);
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
            var crestless = new TooltipRow(0f, null, "AA", Color.WHITE, "", Color.GRAY);
            var crested = new TooltipRow(0f, "crest", "BB", Color.WHITE, "", Color.GRAY);

            var crestlessRow = layOut(List.of(crestless, crested)).rows().get(0);

            // The box carries a crest, so the crest-less row still reserves the column (null crest box,
            // label anchored past the reserved 15 + 6 gutter) and lines up under the crested row.
            assertThat(crestlessRow.crestBox()).isNull();
            assertThat(crestlessRow.textX()).isCloseTo(247f, within(TOLERANCE));
        }

        @Test
        void collapsesTheCrestColumnWhenNoRowCarriesACrest() {
            var crestless = new TooltipRow(0f, null, "AA", Color.WHITE, "", Color.GRAY);

            var only = layOut(List.of(crestless)).rows().get(0);

            // No row carries a crest, so the gutter collapses and the label lays flush at the left
            // content edge (226) rather than past a phantom crest column - the empty-state box case.
            assertThat(only.crestBox()).isNull();
            assertThat(only.textX()).isCloseTo(226f, within(TOLERANCE));
        }
    }
}
