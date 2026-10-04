package kmlib.testfixtures.starsector.settings;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins how a settings cell is read the way LunaLib reads it: brackets removed in pairs, in reading order,
 * each pair's text kept as a run.
 */
final class LunaSettingsHighlightsTests {

    @Nested
    class ParseHighlights {

        @Test
        void eachPairIsRemovedAndItsTextKeptAsARunInReadingOrder() {

            var highlights = LunaSettingsHighlights.parseHighlights("Draws [above] the fog.\n[Default: Below]");

            assertThat(highlights.drawnText())
                .isEqualTo("Draws above the fog.\nDefault: Below");
            assertThat(highlights.runTexts())
                .containsExactly("above", "Default: Below");
        }

        @Test
        void aCellWithNoBracketsIsDrawnAsWrittenWithNoRuns() {

            var highlights = LunaSettingsHighlights.parseHighlights("Plain words.");

            assertThat(highlights.drawnText())
                .isEqualTo("Plain words.");
            assertThat(highlights.runTexts())
                .isEmpty();
        }

        @Test
        void extractionStopsAfterAHundredPairs() {

            var highlights = LunaSettingsHighlights.parseHighlights("[x]".repeat(101));

            assertThat(highlights.runTexts())
                .hasSize(100);
            assertThat(highlights.drawnText())
                .isEqualTo("x".repeat(100) + "[x]");
        }

        @Test
        void anOpeningBracketWithNoClosingOneIsRefused() {

            assertThatThrownBy(() -> LunaSettingsHighlights
                .parseHighlights("Unclosed [run"))
                .isInstanceOf(AssertionError.class)
                .hasMessage("LunaLib cannot read a [ with no ] after it: Unclosed [run");
        }
    }
}
