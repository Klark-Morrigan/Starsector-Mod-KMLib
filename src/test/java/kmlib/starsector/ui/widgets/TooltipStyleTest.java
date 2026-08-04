package kmlib.starsector.ui.widgets;

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

    // Built without the live palette: these styles stand in for "a look" and are only ever compared by
    // identity, so resolving colours through the running game's palette would add a static stub for
    // nothing. The two differ in face so a mixed-up lookup cannot pass by coincidence.
    private static final TextStyle HEADER_STYLE = createStyleIn(StarsectorFont.VANILLA_ORBITRON_20AA);
    private static final TextStyle PARAGRAPH_STYLE = createStyleIn(StarsectorFont.VANILLA_INSIGNIA_15);

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
