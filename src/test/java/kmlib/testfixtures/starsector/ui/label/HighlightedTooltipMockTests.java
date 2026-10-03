package kmlib.testfixtures.starsector.ui.label;

import kmlib.starsector.ui.highlight.Highlight;
import kmlib.starsector.ui.highlight.HighlightedParagraph;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins that every paragraph drawn into the stand-in tooltip is judged on its own label and its own text.
 */
final class HighlightedTooltipMockTests {

    // A Chinese full stop (U+3002), which the game refuses beside a highlighted run.
    private static final String CHINESE_FULL_STOP = "。";

    @Nested
    class FindUnhighlightedRuns {

        @Test
        void paragraphsWithoutHighlightsOrWithHighlightableRunsAreClean() {

            var tooltipMock = HighlightedTooltipMock.createTooltipMock();

            new HighlightedParagraph("Plain words", Color.WHITE)
                .addTo(tooltipMock.getTooltip());

            new HighlightedParagraph("Shows the Political Map.", Color.WHITE, new Highlight("Political Map", Color.RED))
                .addTo(tooltipMock.getTooltip());

            assertThat(tooltipMock.findUnhighlightedRuns())
                .isEmpty();
        }

        @Test
        void eachParagraphsRunsAreHeldToThatParagraphsText() {

            var tooltipMock = HighlightedTooltipMock.createTooltipMock();

            // The run is in the first paragraph and stands beside a full stop only in the second.
            new HighlightedParagraph("Map here", Color.WHITE, new Highlight("Map", Color.RED))
                .addTo(tooltipMock.getTooltip());

            new HighlightedParagraph("Map" + CHINESE_FULL_STOP, Color.WHITE, new Highlight("Map", Color.RED))
                .addTo(tooltipMock.getTooltip());

            assertThat(tooltipMock.findUnhighlightedRuns())
                .containsExactly("'Map' in 'Map" + CHINESE_FULL_STOP + "' (after it: " + CHINESE_FULL_STOP + ")");
        }
    }
}
