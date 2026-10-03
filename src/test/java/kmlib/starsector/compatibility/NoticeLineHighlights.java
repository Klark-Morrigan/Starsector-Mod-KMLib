package kmlib.starsector.compatibility;

import kmlib.starsector.ui.highlight.Highlight;
import kmlib.starsector.ui.highlight.HighlightedParagraph;
import kmlib.testfixtures.localisation.LocalisationDirectory;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;
import kmlib.testfixtures.starsector.ui.label.LabelHighlightRule;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds each notice line into the paragraph the notice panel draws, in every locale KMLib ships, and holds
 * its highlights to the game's rule for which runs it can highlight.
 */
final class NoticeLineHighlights {

    private NoticeLineHighlights() {
    }

    // Fed to a parameterised case by its qualified name.
    static List<String> listLocaleTags() {

        return List.copyOf(new LocalisationDirectory(LocalisationDirectory.LOCALISATION_DIRECTORY)
            .readManifest()
            .declaredLocalesByTag()
            .keySet());
    }

    // Stands one locale's wording up as the game's strings, read from that locale's bundle.
    static void installLocaleStrings(String localeTag) {

        var directory = new LocalisationDirectory(LocalisationDirectory.LOCALISATION_DIRECTORY);
        var locale = directory
            .readManifest()
            .declaredLocalesByTag()
            .get(localeTag);

        StarsectorSettingsFake.installSettings(directory.openBundle(locale).readStringSource());
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
                .forEach(run -> unhighlightedRuns.add(
                    "'" + run.runText() + "' in '" + line.lineText() + "' (" + run.blockingNeighbours() + ")"));
        }
        return unhighlightedRuns;
    }
}
