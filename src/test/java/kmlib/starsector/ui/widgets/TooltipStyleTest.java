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

/**
 * Pins the lookup a tooltip's typography answers: every kind of line has a look of its own, and asking
 * for one kind never yields another's. That is the whole guarantee rows depend on when they carry a kind
 * instead of a face - a heading that resolved to the body look would be drawn as body text no matter
 * what the box was configured with.
 */
class TooltipStyleTest {

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

    private static TooltipStyle twoFacedStyle() {
        return new TooltipStyle(HEADER_STYLE, PARAGRAPH_STYLE);
    }

    @Nested
    class ResolveStyleFor {
        @Test
        void resolveStyleForReturnsTheHeaderLookForAHeaderLine() {
            assertThat(twoFacedStyle().resolveStyleFor(TooltipLineStyle.HEADER)).isEqualTo(HEADER_STYLE);
        }

        @Test
        void resolveStyleForReturnsTheParagraphLookForAParagraphLine() {
            assertThat(twoFacedStyle().resolveStyleFor(TooltipLineStyle.PARAGRAPH))
                    .isEqualTo(PARAGRAPH_STYLE);
        }

        @ParameterizedTest
        @EnumSource(TooltipLineStyle.class)
        void resolveStyleForAnswersEveryLineStyle(TooltipLineStyle lineStyle) {
            // Swept over the enum rather than asserted per value: a renderer resolves whatever kind the
            // row it is drawing carries, so a kind with no answer would fail at paint time on the one
            // box that happened to use it.
            assertThat(twoFacedStyle().resolveStyleFor(lineStyle)).isNotNull();
        }

        @Test
        void resolveStyleForReturnsOneLookForEveryKindWhenBothAreTheSame() {
            // A box that wants its headings drawn like its body says so by passing one look twice, so
            // the lookup has to be free of any per-kind adjustment of its own.
            var flatStyle = new TooltipStyle(PARAGRAPH_STYLE, PARAGRAPH_STYLE);

            assertThat(flatStyle.resolveStyleFor(TooltipLineStyle.HEADER)).isEqualTo(PARAGRAPH_STYLE);
            assertThat(flatStyle.resolveStyleFor(TooltipLineStyle.PARAGRAPH)).isEqualTo(PARAGRAPH_STYLE);
        }
    }
}
