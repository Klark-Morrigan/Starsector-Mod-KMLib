package kmlib.testfixtures.starsector.ui.label;

import kmlib.starsector.ui.highlight.Highlight;
import kmlib.starsector.ui.highlight.HighlightedParagraph;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins that every highlighted paragraph added to the stand-in text panel is judged on its own text, and that a
 * paragraph without highlights is not recorded.
 */
final class HighlightedTextPanelMockTests {

    // A Chinese full stop (U+3002), which the game refuses beside a highlighted run.
    private static final String CHINESE_FULL_STOP = "。";

    @Nested
    class FindUnhighlightedRuns {

        @Test
        void paragraphsWithoutHighlightsOrWithHighlightableRunsAreClean() {

            var textPanelMock = HighlightedTextPanelMock.createTextPanelMock();

            new HighlightedParagraph("Plain words", Color.WHITE)
                .addTo(textPanelMock.getTextPanel());

            new HighlightedParagraph("Shows the Political Map.", Color.WHITE, new Highlight("Political Map", Color.RED))
                .addTo(textPanelMock.getTextPanel());

            assertThat(textPanelMock.findUnhighlightedRuns())
                .isEmpty();
        }

        @Test
        void eachParagraphsRunsAreHeldToThatParagraphsText() {

            var textPanelMock = HighlightedTextPanelMock.createTextPanelMock();

            // The run is in the first paragraph and stands beside a full stop only in the second.
            new HighlightedParagraph("Map here", Color.WHITE, new Highlight("Map", Color.RED))
                .addTo(textPanelMock.getTextPanel());

            new HighlightedParagraph("Map" + CHINESE_FULL_STOP, Color.WHITE, new Highlight("Map", Color.RED))
                .addTo(textPanelMock.getTextPanel());

            assertThat(textPanelMock.findUnhighlightedRuns())
                .containsExactly("'Map' in 'Map" + CHINESE_FULL_STOP + "' (after it: " + CHINESE_FULL_STOP + ")");
        }
    }

    @Nested
    class ListHighlightedParagraphTexts {

        @Test
        void listsOnlyTheParagraphsAddedWithHighlights() {

            var textPanelMock = HighlightedTextPanelMock.createTextPanelMock();

            new HighlightedParagraph("Plain words", Color.WHITE)
                .addTo(textPanelMock.getTextPanel());

            new HighlightedParagraph("Map here", Color.WHITE, new Highlight("Map", Color.RED))
                .addTo(textPanelMock.getTextPanel());

            assertThat(textPanelMock.listHighlightedParagraphTexts())
                .containsExactly("Map here");
        }
    }
}
