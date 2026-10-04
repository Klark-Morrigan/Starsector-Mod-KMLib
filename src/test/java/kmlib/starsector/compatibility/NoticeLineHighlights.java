package kmlib.starsector.compatibility;

import kmlib.starsector.ui.highlight.Highlight;
import kmlib.starsector.ui.highlight.HighlightedParagraph;
import kmlib.testfixtures.starsector.ui.label.LabelHighlightRule;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds each notice line into the paragraph the notice panel draws and holds its highlights to the game's
 * rule for which runs it can highlight.
 */
final class NoticeLineHighlights {

    private NoticeLineHighlights() {
    }

    // One sentence per run the engine would leave plain, naming the run, the line and what blocked it.
    static List<String> findUnhighlightedRuns(List<CompatibilityNoticeLine> lines) {

        var unhighlightedRuns = new ArrayList<String>();

        for (var line : lines) {

            var highlights = line
                .emphasisedRuns()
                .stream()
                .map(run -> new Highlight(run.runText(), Color.WHITE))
                .toArray(Highlight[]::new);

            var paragraph = new HighlightedParagraph(line.lineText(), Color.WHITE, highlights);

            LabelHighlightRule.findUnhighlightedRuns(paragraph)
                .forEach(run -> unhighlightedRuns.add(run.describeIn(line.lineText())));
        }
        return unhighlightedRuns;
    }
}
