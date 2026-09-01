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
 * Pins what a box does about standing taller than the room it has: it gives up size rather than lines,
 * and it gives it up from the top. The line the reader asked for keeps the size it was asked at, the
 * tiers above it come down to meet it, and the room between the lines comes down with them - so a box
 * compressed to fit still says everything the same box said at full size.
 *
 * <p>Every case reads the fit back as a typography rather than as a drawn box, since that is what it
 * answers: the sizes and the spacing the box would then be stacked by, which the stack and the layout
 * spend without knowing a fit happened.
 *
 * <p>The heights are the numbers the fixture actually comes to - four 15-tall lines, the three 4-wide
 * gaps between them, and the box's padding either side - stated as literals so a case says which box it
 * is weighing rather than repeating the arithmetic under test.
 */
class TooltipHeightFitTest {

    private static final StarsectorFont BODY_FONT = StarsectorFont.VANILLA_INSIGNIA_15;
    private static final double BODY_SIZE = 15d;

    // The tiers the fixture's lines stand at: the box's own voice down to the deepest line it draws,
    // which is the one a compression anchors on.
    private static final int IN_THE_BOXS_VOICE = 0;
    private static final int ONE_STEP_UNDER = 1;
    private static final int TWO_STEPS_UNDER = 2;
    private static final int DEEPEST_LEVEL = 3;

    // The smallest size the style resolves, which is where a compression stops buying height.
    private static final double SMALLEST_SUBORDINATE_SIZE = 7d;

    // What the fixture stands at drawn as authored: four 15-tall lines, three 4-wide gaps, and 8 of
    // padding either side. Every budget below is stated against it.
    private static final float UNFITTED_BOX_HEIGHT = 88f;

    // Room to spare, so the box is drawn as it stands.
    private static final float ROOMY_BUDGET = 120f;

    // Two budgets the box meets only compressed, the second the tighter of them - a pair, since how far
    // the solve went is readable only against another solve.
    private static final float TIGHT_BUDGET = 80f;
    private static final float TIGHTER_BUDGET = 70f;

    // Less room than the fixture can be compressed into: every line above the deepest sits at the floor
    // and the box still overflows, which is where content has to be given up instead.
    private static final float UNREACHABLE_BUDGET = 40f;

    private static final float TOLERANCE = 0.001f;
    private static final double SIZE_TOLERANCE = 0.001d;

    // The plainest box there is: one look for both kinds and the standard room between its lines, so a
    // measured height is the fixture's own lines rather than a typography the case never stated.
    private static final TooltipStyle PLAIN_STYLE = TooltipStyle.createStyle(
        createBodyStyle(),
        createBodyStyle());

    private static TextStyle createBodyStyle() {
        // Built by hand rather than through TextStyle's own factory: its baseline colour resolves from
        // the running game's palette, which a height test has no business standing up for a value it
        // never reads.
        return new TextStyle(
            new TextFace(BODY_FONT, BODY_SIZE),
            Color.WHITE,
            TextAlignment.TOP_LEFT,
            false);
    }

    private static TooltipRow createRowAt(String text, int subordinationLevel) {
        return TooltipRow
            .createRow(new TextSpan(text, Color.WHITE))
            .subordinatedAt(subordinationLevel);
    }

    // One block of four lines, each a step deeper than the last: the shape a listing takes, and the one
    // a compression has something to act on - the three tiers above the deepest line are what it spends.
    private static List<TooltipSection> buildTieredBox() {
        return List.of(TooltipSection.createSection(List.<TooltipRow>of(
            createRowAt("AA", IN_THE_BOXS_VOICE),
            createRowAt("BB", ONE_STEP_UNDER),
            createRowAt("CC", TWO_STEPS_UNDER),
            createRowAt("DD", DEEPEST_LEVEL))));
    }

    // The same four lines all speaking in the box's voice: a box with nothing standing above its deepest
    // line, which no ramp can compress.
    private static List<TooltipSection> buildFlatBox() {
        return List.of(TooltipSection.createSection(List.<TooltipRow>of(
            createRowAt("AA", IN_THE_BOXS_VOICE),
            createRowAt("BB", IN_THE_BOXS_VOICE),
            createRowAt("CC", IN_THE_BOXS_VOICE),
            createRowAt("DD", IN_THE_BOXS_VOICE))));
    }

    private static TooltipStyle fitTieredBoxTo(float heightBudget) {
        return TooltipHeightFit.fitToHeight(buildTieredBox(), PLAIN_STYLE, heightBudget);
    }

    private static float measureTieredBoxIn(TooltipStyle style) {
        return CursorTooltip.measureBoxHeight(buildTieredBox(), style);
    }

    // The size a body line lands on at one tier of a fitted box, which is the lookup the stack makes per
    // line.
    private static double resolveSizeAt(TooltipStyle style, int subordinationLevel) {
        return style
            .resolveStyleFor(TooltipLineStyle.PARAGRAPH, subordinationLevel)
            .face()
            .size();
    }

    @Nested
    class FitToHeight {

        @Test
        void fitToHeightLeavesABoxThatFitsExactlyAsItStands() {
            // Nearly every box, and the reason the compression is an overflow response rather than a
            // look: it inverts how a listing reads, so a box that was never the problem must not pay for
            // it. The very typography handed in comes back, not a rebuilt copy of it.
            assertThat(measureTieredBoxIn(PLAIN_STYLE))
                .isCloseTo(UNFITTED_BOX_HEIGHT, within(TOLERANCE));

            assertThat(TooltipHeightFit.fitToHeight(buildTieredBox(), PLAIN_STYLE, ROOMY_BUDGET))
                .isSameAs(PLAIN_STYLE);
        }

        @Test
        void fitToHeightBringsAnOverBudgetBoxWithinItsBudget() {
            // The whole of what the pass is for: the box stands 88 tall as authored and is asked to fit
            // 70, and what comes back is a typography the same blocks stack inside that.
            assertThat(measureTieredBoxIn(fitTieredBoxTo(TIGHTER_BUDGET)))
                .isLessThanOrEqualTo(TIGHTER_BUDGET);
        }

        @Test
        void fitToHeightHoldsTheDeepestShownLineAtItsKindsOwnSize() {
            // What the compression is anchored on. The deepest line is what a reader who asked for that
            // depth is there to read, so it is the one thing the box does not give up - shrunk with the
            // rest, the fit would answer the request by quieting exactly what was asked for.
            assertThat(resolveSizeAt(fitTieredBoxTo(TIGHTER_BUDGET), DEEPEST_LEVEL))
                .isCloseTo(BODY_SIZE, within(SIZE_TOLERANCE));
        }

        @Test
        void fitToHeightShrinksTheTiersAboveTheDeepestLine() {
            // The other half of the anchoring, and the inversion it costs: the account a listing stands
            // under comes down to meet its deepest lines rather than the other way about, so a
            // compressed box reads quietest at the top.
            var fittedStyle = fitTieredBoxTo(TIGHTER_BUDGET);

            assertThat(resolveSizeAt(fittedStyle, IN_THE_BOXS_VOICE))
                .isLessThan(resolveSizeAt(fittedStyle, DEEPEST_LEVEL));
        }

        @Test
        void fitToHeightTightensTheRoomBetweenLinesWithTheGlyphs() {
            // Leading is most of what a row costs, so a compression spending only glyphs would give up
            // legibility for a fraction of the height it needs. The room comes down with the lines.
            assertThat(fitTieredBoxTo(TIGHTER_BUDGET).resolveLineGapAfter(IN_THE_BOXS_VOICE))
                .isLessThan(PLAIN_STYLE.resolveLineGapAfter(IN_THE_BOXS_VOICE));
        }

        @Test
        void fitToHeightCompressesNoFurtherThanTheBudgetAsks() {
            // The solve settles on the gentlest ramp that fits rather than on any that does: the faces
            // are atlases crisp at one size, so a box scaled further off its own than it had to be is
            // softer for nothing. The looser budget therefore leaves its tiers larger.
            assertThat(resolveSizeAt(fitTieredBoxTo(TIGHT_BUDGET), IN_THE_BOXS_VOICE))
                .isGreaterThan(resolveSizeAt(fitTieredBoxTo(TIGHTER_BUDGET), IN_THE_BOXS_VOICE));
        }

        @Test
        void fitToHeightStopsAtTheSmallestLegibleSizeWhereNoRampFits() {
            // The terminal case, and the reason the solve is bounded at all: past the ramp that floors
            // the tiers above the anchor, no larger one changes anything. The box comes back as short as
            // this can make it and still too tall - what is left is content to be given up.
            var fittedStyle = fitTieredBoxTo(UNREACHABLE_BUDGET);

            assertThat(resolveSizeAt(fittedStyle, IN_THE_BOXS_VOICE))
                .isCloseTo(SMALLEST_SUBORDINATE_SIZE, within(SIZE_TOLERANCE));
            assertThat(measureTieredBoxIn(fittedStyle))
                .isGreaterThan(UNREACHABLE_BUDGET);
        }

        @Test
        void fitToHeightLeavesABoxOfOneTierAsItStands() {
            // Nothing stands above the deepest line, so the ramp reaches nothing however far it is
            // pushed. Handed back untouched rather than solved for pointlessly - the box overflows, and
            // only giving up content answers it.
            assertThat(TooltipHeightFit.fitToHeight(buildFlatBox(), PLAIN_STYLE, TIGHTER_BUDGET))
                .isSameAs(PLAIN_STYLE);
        }
    }
}
