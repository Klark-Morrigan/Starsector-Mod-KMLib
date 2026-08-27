package kmlib.starsector.ui.text;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins {@link TextSpan}'s two contracts: a span always holds both halves of the pair, and blank text is
 * the one spelling of nothing-to-draw. Both are worth fixing because everything downstream measures and
 * paints a span without checking it - a null that reaches that point reports as a failure in a draw call
 * rather than at whoever built the span, and a second reading of "empty" would have some places reserve
 * room for a run that paints nothing.
 */
class TextSpanTest {

    private static final Color SPAN_COLOUR = new Color(200, 150, 50);

    @Nested
    class Constructor {

        @Test
        void constructorKeepsTheTextAndColourItWasGiven() {
            
            var span = new TextSpan("Contested by", SPAN_COLOUR);

            assertThat(span.text())
                .isEqualTo("Contested by");
            assertThat(span.colour())
                .isEqualTo(SPAN_COLOUR);
        }

        @Test
        void constructorRejectsNullText() {

            assertThatThrownBy(() -> new TextSpan(null, SPAN_COLOUR))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("text");
        }

        @Test
        void constructorRejectsNullColour() {

            assertThatThrownBy(() -> new TextSpan("Contested by", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("colour");
        }

        @Test
        void constructorBuildsAWordOfItsOwn() {
            // The plain reading, and the one an author never has to ask for: a run built from words and
            // a colour stands a word space clear of whatever precedes it.
            assertThat(new TextSpan("Contested by", SPAN_COLOUR).isJoinedToPreviousRun())
                .isFalse();
        }
    }

    @Nested
    class JoinsPreviousRun {

        @Test
        void joinsPreviousRunButtsTheSpanAgainstTheOneBeforeIt() {

            assertThat(new TextSpan("Station", SPAN_COLOUR).joinsPreviousRun().isJoinedToPreviousRun())
                .isTrue();
        }

        @Test
        void joinsPreviousRunKeepsTheWordsAndTheColour() {
            // The refinement says how the run sits beside its neighbour and nothing about the run, so
            // what it draws and the colour it draws in survive it untouched.
            var span = new TextSpan("Station", SPAN_COLOUR)
                .joinsPreviousRun();

            assertThat(span.text())
                .isEqualTo("Station");
            assertThat(span.colour())
                .isEqualTo(SPAN_COLOUR);
        }

        @Test
        void joinsPreviousRunLeavesTheSpanItWasBuiltFromAWordOfItsOwn() {
            // A refinement returns a new value, so a caller joining one run of a composed label cannot
            // reach into the span another caller is still holding.
            var separateSpan = new TextSpan("Station", SPAN_COLOUR);
            separateSpan.joinsPreviousRun();

            assertThat(separateSpan.isJoinedToPreviousRun())
                .isFalse();
        }
    }

    @Nested
    class CreateBlank {

        @Test
        void createBlankCarriesNoText() {

            var span = TextSpan.createBlank(SPAN_COLOUR);

            assertThat(span.text())
                .isEmpty();
            assertThat(span.hasContent())
                .isFalse();
        }

        @Test
        void createBlankKeepsTheColourItWouldHaveDrawnIn() {
            // The colour survives the absence precisely so nothing downstream has to branch on empty
            // before it can ask a span how it draws.
            assertThat(TextSpan.createBlank(SPAN_COLOUR).colour())
                .isEqualTo(SPAN_COLOUR);
        }
    }

    @Nested
    class HasContent {

        @Test
        void hasContentIsTrueForARunWithGlyphs() {

            assertThat(new TextSpan("Contested by", SPAN_COLOUR).hasContent())
                .isTrue();
        }

        @Test
        void hasContentIsFalseForAnEmptyRun() {

            assertThat(new TextSpan("", SPAN_COLOUR).hasContent())
                .isFalse();
        }

        @Test
        void hasContentIsFalseForAWhitespaceOnlyRun() {
            // A run assembled from parts that all came up empty is still nothing to draw, however many
            // separators were joined between them.
            assertThat(new TextSpan("   ", SPAN_COLOUR).hasContent())
                .isFalse();
        }
    }

    @Nested
    class ComputeWidth {

        // Each glyph one unit wide, so an expected width is the character count written as a literal.
        private static final StyledSpanMeasurer ONE_UNIT_PER_CHARACTER =
            textSpan -> textSpan.text().length();

        // Deliberately unlike any character count below, so a width taken from the line rather than the
        // glyphs would be visible in the assertion.
        private static final float LINE_HEIGHT = 20f;

        @Test
        void computeWidthChargesTheRunsGlyphs() {
            
            assertThat(new TextSpan("Hegemony", SPAN_COLOUR)
                    .computeWidth(LINE_HEIGHT, ONE_UNIT_PER_CHARACTER))
                .isEqualTo(8f);
        }

        @Test
        void computeWidthChargesABlankRunNothing() {
            // The measurement is skipped rather than returning whatever a blank string measures, so a
            // run that came out empty holds no room open in the line it sits on.
            assertThat(TextSpan.createBlank(SPAN_COLOUR)
                    .computeWidth(LINE_HEIGHT, ONE_UNIT_PER_CHARACTER))
                .isEqualTo(0f);
        }
    }
}
