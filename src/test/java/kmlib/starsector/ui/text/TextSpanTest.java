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

            assertThat(span.text()).isEqualTo("Contested by");
            assertThat(span.colour()).isEqualTo(SPAN_COLOUR);
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
    }

    @Nested
    class CreateBlank {
        @Test
        void createBlankCarriesNoText() {
            var span = TextSpan.createBlank(SPAN_COLOUR);

            assertThat(span.text()).isEmpty();
            assertThat(span.hasText()).isFalse();
        }

        @Test
        void createBlankKeepsTheColourItWouldHaveDrawnIn() {
            // The colour survives the absence precisely so nothing downstream has to branch on empty
            // before it can ask a span how it draws.
            assertThat(TextSpan.createBlank(SPAN_COLOUR).colour()).isEqualTo(SPAN_COLOUR);
        }
    }

    @Nested
    class HasText {
        @Test
        void hasTextIsTrueForARunWithGlyphs() {
            assertThat(new TextSpan("Contested by", SPAN_COLOUR).hasText()).isTrue();
        }

        @Test
        void hasTextIsFalseForAnEmptyRun() {
            assertThat(new TextSpan("", SPAN_COLOUR).hasText()).isFalse();
        }

        @Test
        void hasTextIsFalseForAWhitespaceOnlyRun() {
            // A run assembled from parts that all came up empty is still nothing to draw, however many
            // separators were joined between them.
            assertThat(new TextSpan("   ", SPAN_COLOUR).hasText()).isFalse();
        }
    }
}
