package kmlib.starsector.ui.widgets.tooltip;

import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.text.TextAlignment;
import kmlib.starsector.ui.text.TextStyle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the lookup a tooltip's typography answers: every kind of line has a look of its own, and asking
 * for one kind never yields another's. That is the whole guarantee rows depend on when they carry a kind
 * instead of a face - a heading that resolved to the body look would be drawn as body text no matter
 * what the box was configured with. Also the parting it carries: the baseline a box gets by saying
 * nothing, and that stating one changes only that.
 */
class TooltipStyleTest {

    // The parting a box gets without asking - restated here rather than read off the class under test,
    // so a change to how far apart blocks stand has to be made deliberately in both places. It is
    // today's spacing exactly: the 4 line gap plus half a 15pt line.
    private static final float DEFAULT_SECTION_BREAK = 11.5f;
    private static final float WIDER_SECTION_BREAK = 24f;
    private static final float TOLERANCE = 0.001f;

    // What a box demotes a step by when it asks, and what it demotes by when it does not - restated here
    // rather than read off the class under test, so the baseline of "no shrink until asked for" has to be
    // changed deliberately in both places.
    private static final float LEVEL_SHRINK = 2f;
    private static final float NO_LEVEL_SHRINK = 0f;

    // How far under the box's own voice a line stands. The negative is not a level any row produces - a
    // row floors its own at zero - but the lookup is public and is handed whatever a caller holds.
    private static final int ABOVE_THE_BOXS_VOICE = -1;
    private static final int IN_THE_BOXS_VOICE = 0;
    private static final int ONE_STEP_UNDER = 1;
    private static final int TWO_STEPS_UNDER = 2;

    // Where those steps land off the body face's own 15, as literals rather than as that size less the
    // step, so the arithmetic under test is asserted here rather than repeated. A face carries its size as
    // a double, so these are compared at a tolerance of their own rather than at the float one above.
    private static final double ONE_STEP_UNDER_SIZE = 13d;
    private static final double TWO_STEPS_UNDER_SIZE = 11d;
    private static final double SIZE_TOLERANCE = 0.001d;

    // A stack deeper than the step can carry, and the size the shrink stops at: below it no atlas renders
    // legibly, and the step would go on to zero and past it.
    private static final int DEEPER_THAN_THE_FLOOR = 9;
    private static final double SMALLEST_SUBORDINATE_SIZE = 8d;

    // Built without the live palette: these styles stand in for "a look" and are only ever compared by
    // identity, so resolving colours through the running game's palette would add a static stub for
    // nothing. The two differ in face so a mixed-up lookup cannot pass by coincidence.
    private static final TextStyle HEADER_STYLE = createStyleIn(StarsectorFont.VANILLA_ORBITRON_20AA);
    private static final TextStyle PARAGRAPH_STYLE = createStyleIn(StarsectorFont.VANILLA_INSIGNIA_15);
    private static final TextStyle FOOTNOTE_STYLE =
        createStyleIn(StarsectorFont.VANILLA_ORBITRON_12_CONDENSED);

    private static TextStyle createStyleIn(StarsectorFont font) {
        return new TextStyle(
            new TextFace(font, font.getNativeSize()),
            Color.WHITE,
            TextAlignment.TOP_LEFT,
            false);
    }

    private static TooltipStyle buildTwoFacedStyle() {
        return TooltipStyle.createStyle(HEADER_STYLE, PARAGRAPH_STYLE);
    }

    // The same two looks under a box that asked for its levels to read as levels - the only state in which
    // a line's subordination changes anything, so every case about the shrink starts here.
    private static TooltipStyle buildShrinkingStyle() {
        return buildTwoFacedStyle().shrunkPerLevel(LEVEL_SHRINK);
    }

    // The size a body line lands on standing that many steps under the box's own voice, which is the one
    // lookup a renderer makes per row.
    private static double resolveParagraphSizeAt(int subordinationLevel) {
        return buildShrinkingStyle()
            .resolveStyleFor(TooltipLineStyle.PARAGRAPH, subordinationLevel)
            .face()
            .size();
    }

    @Nested
    class CreateStyle {

        @Test
        void createStyleCarriesTheTwoLooksAtTheStandardParting() {

            var style = buildTwoFacedStyle();

            assertThat(style.headerStyle())
                .isEqualTo(HEADER_STYLE);
            assertThat(style.paragraphStyle())
                .isEqualTo(PARAGRAPH_STYLE);
            assertThat(style.sectionBreak())
                .isCloseTo(DEFAULT_SECTION_BREAK, within(TOLERANCE));
        }

        @Test
        void createStyleSetsANoteInTheBodyLookUntilOneIsAskedFor() {
            // Most boxes note nothing and so never resolve the look; demanding a third face up front
            // would have every caller name one for a line it will not draw.
            assertThat(buildTwoFacedStyle().footnoteStyle())
                .isEqualTo(PARAGRAPH_STYLE);
        }
    }

    @Nested
    class FootnotedIn {

        @Test
        void footnotedInSetsTheLookANoteAtTheFootIsDrawnIn() {
            assertThat(buildTwoFacedStyle().footnotedIn(FOOTNOTE_STYLE).footnoteStyle())
                .isEqualTo(FOOTNOTE_STYLE);
        }

        @Test
        void footnotedInChangesNothingElse() {
            // A box setting its notes apart says nothing about how its headings or its body are drawn,
            // nor about how far apart its blocks stand.
            assertThat(buildTwoFacedStyle().footnotedIn(FOOTNOTE_STYLE))
                .usingRecursiveComparison()
                .ignoringFields("footnoteStyle")
                .isEqualTo(buildTwoFacedStyle());
        }
    }

    @Nested
    class ShrunkPerLevel {

        @Test
        void shrunkPerLevelSetsHowMuchSmallerEachStepDraws() {
            assertThat(buildTwoFacedStyle().shrunkPerLevel(LEVEL_SHRINK).levelShrink())
                .isCloseTo(LEVEL_SHRINK, within(TOLERANCE));
        }

        @Test
        void shrunkPerLevelChangesNothingElse() {
            // The step and the parting are both bare floats travelling side by side into the rebuild, so
            // a transposition between them compiles silently - this is what would catch it.
            assertThat(buildTwoFacedStyle().shrunkPerLevel(LEVEL_SHRINK))
                .usingRecursiveComparison()
                .ignoringFields("levelShrink")
                .isEqualTo(buildTwoFacedStyle());
        }

        @Test
        void shrunkPerLevelLeavesAStyleDrawingEveryLevelAtItsKindsOwnSize() {
            // A box that never asks demotes nothing, so a stack of rows reads at one size until one does.
            assertThat(buildTwoFacedStyle().levelShrink())
                .isCloseTo(NO_LEVEL_SHRINK, within(TOLERANCE));
        }
    }

    @Nested
    class PartedBy {

        @Test
        void partedBySetsHowFarApartTheBlocksStand() {
            assertThat(buildTwoFacedStyle().partedBy(WIDER_SECTION_BREAK).sectionBreak())
                .isCloseTo(WIDER_SECTION_BREAK, within(TOLERANCE));
        }

        @Test
        void partedByChangesNothingElse() {
            // A box widening its partings is saying nothing about how its lines are drawn, so the two
            // looks have to come through the refinement untouched.
            assertThat(buildTwoFacedStyle().partedBy(WIDER_SECTION_BREAK))
                .usingRecursiveComparison()
                .ignoringFields("sectionBreak")
                .isEqualTo(buildTwoFacedStyle());
        }
    }

    @Nested
    class ResolveStyleFor {
        
        @Test
        void resolveStyleForReturnsTheHeaderLookForAHeaderLine() {
            assertThat(buildTwoFacedStyle().resolveStyleFor(TooltipLineStyle.HEADER))
                .isEqualTo(HEADER_STYLE);
        }

        @Test
        void resolveStyleForReturnsTheParagraphLookForAParagraphLine() {
            assertThat(buildTwoFacedStyle().resolveStyleFor(TooltipLineStyle.PARAGRAPH))
                .isEqualTo(PARAGRAPH_STYLE);
        }

        @ParameterizedTest
        @EnumSource(TooltipLineStyle.class)
        void resolveStyleForAnswersEveryLineStyle(TooltipLineStyle lineStyle) {
            // Swept over the enum rather than asserted per value: a renderer resolves whatever kind the
            // row it is drawing carries, so a kind with no answer would fail at paint time on the one
            // box that happened to use it.
            assertThat(buildTwoFacedStyle().resolveStyleFor(lineStyle))
                .isNotNull();
        }

        @Test
        void resolveStyleForReturnsTheFootnoteLookForANoteAtTheFoot() {
            assertThat(buildTwoFacedStyle()
                    .footnotedIn(FOOTNOTE_STYLE)
                    .resolveStyleFor(TooltipLineStyle.FOOTNOTE))
                .isEqualTo(FOOTNOTE_STYLE);
        }

        @Test
        void resolveStyleForDrawsALineSpeakingInTheBoxsVoiceAtItsKindsOwnSize() {
            // The level a row carries when it is subordinate to nothing, which is most of them - the
            // lookup has to hand back the kind's own look untouched rather than a rebuilt copy of it.
            assertThat(buildShrinkingStyle()
                    .resolveStyleFor(TooltipLineStyle.PARAGRAPH, IN_THE_BOXS_VOICE))
                .isEqualTo(PARAGRAPH_STYLE);
        }

        @Test
        void resolveStyleForShrinksALineOncePerStepItStandsUnderTheBoxsVoice() {
            // The step compounds with depth rather than being one demotion applied to anything below the
            // box's voice, which is what makes a stack of levels read as a stack.
            assertThat(resolveParagraphSizeAt(ONE_STEP_UNDER))
                .isCloseTo(ONE_STEP_UNDER_SIZE, within(SIZE_TOLERANCE));
            assertThat(resolveParagraphSizeAt(TWO_STEPS_UNDER))
                .isCloseTo(TWO_STEPS_UNDER_SIZE, within(SIZE_TOLERANCE));
        }

        @Test
        void resolveStyleForKeepsTheKindsOwnFaceWhileShrinkingIt() {
            // Only the size is demoted: a subordinate heading is still set in the heading face, or the
            // step would quietly restyle a whole kind of line instead of quieting one.
            assertThat(buildShrinkingStyle()
                    .resolveStyleFor(TooltipLineStyle.HEADER, ONE_STEP_UNDER)
                    .face()
                    .font())
                .isEqualTo(StarsectorFont.VANILLA_ORBITRON_20AA);
        }

        @Test
        void resolveStyleForDrawsEveryLevelAtItsKindsOwnSizeWhenNoStepWasAskedFor() {
            // The default path, and the one every box that lists nothing deep takes: a level carried by a
            // row means nothing to a style that was never asked to demote one.
            assertThat(buildTwoFacedStyle()
                    .resolveStyleFor(TooltipLineStyle.PARAGRAPH, TWO_STEPS_UNDER))
                .isEqualTo(PARAGRAPH_STYLE);
        }

        @Test
        void resolveStyleForStopsShrinkingAtTheSmallestLegibleSize() {
            // A listing is as deep as its subject matter, so nothing bounds the level a row can carry.
            // Left to run, the step would resolve a size no atlas renders, then zero, then a negative.
            assertThat(resolveParagraphSizeAt(DEEPER_THAN_THE_FLOOR))
                .isCloseTo(SMALLEST_SUBORDINATE_SIZE, within(SIZE_TOLERANCE));
        }

        @Test
        void resolveStyleForTreatsALevelBelowTheBoxsVoiceAsSpeakingInIt() {
            // A row floors its own level at zero, but the lookup is public and is handed whatever a
            // caller has - a negative must not resolve a size LARGER than the box's own voice.
            assertThat(buildShrinkingStyle()
                    .resolveStyleFor(TooltipLineStyle.PARAGRAPH, ABOVE_THE_BOXS_VOICE))
                .isEqualTo(PARAGRAPH_STYLE);
        }

        @Test
        void resolveStyleForReturnsOneLookForEveryKindWhenBothAreTheSame() {
            // A box that wants its headings drawn like its body says so by passing one look twice, so
            // the lookup has to be free of any per-kind adjustment of its own.
            var flatStyle = TooltipStyle.createStyle(PARAGRAPH_STYLE, PARAGRAPH_STYLE);

            assertThat(flatStyle.resolveStyleFor(TooltipLineStyle.HEADER))
                .isEqualTo(PARAGRAPH_STYLE);
            assertThat(flatStyle.resolveStyleFor(TooltipLineStyle.PARAGRAPH))
                .isEqualTo(PARAGRAPH_STYLE);
        }
    }
}
