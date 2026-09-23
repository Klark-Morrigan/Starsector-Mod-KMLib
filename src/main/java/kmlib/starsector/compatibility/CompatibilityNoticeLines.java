package kmlib.starsector.compatibility;

import kmlib.starsector.compatibility.CompatibilityNoticeLine.Emphasis;
import kmlib.starsector.compatibility.CompatibilityNoticeLine.EmphasisedRun;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds a {@link CompatibilityNoticeLine} out of wording and the values filled into it.
 *
 * <p>Its own class rather than a second half of {@link CompatibilityFailure}, which is a record of
 * what broke: nothing here knows what a compatibility failure is. What it knows is how a line and
 * its emphasis are assembled - that runs go in reading order, that a run has to begin on a word, and
 * that a value coloured like the wording around it belongs to that wording rather than beside it.
 * Those rules are the part that can be subtly wrong, and kept here they are testable without a
 * failure to compose.
 *
 * <p>A phrase spliced into a line is carried in the same shape as a line, being exactly a fragment
 * of one: wording with the runs of it that stand out.
 *
 * <p>Final class with a private constructor: operations over a value type, no instances.
 */
final class CompatibilityNoticeLines {

    // The slot a template carries, which is also where a phrase is split into its runs.
    private static final String VALUE_SLOT = "%s";

    private CompatibilityNoticeLines() {
    }

    /**
     * One line and the runs of it that stand out.
     *
     * @param lineText the wording with its values already in place
     * @param runs     the runs that stand out, which must be given in the order they appear
     * @return the line
     */
    static CompatibilityNoticeLine buildLine(String lineText, EmphasisedRun... runs) {

        return new CompatibilityNoticeLine(lineText, List.of(runs));
    }

    /**
     * The same, where a phrase filled into the line brought runs of its own.
     *
     * <p>The phrase's runs follow the line's because the phrase sits at the end of every template
     * that takes one. Reading order is what the engine matches on, so a phrase spliced in ahead of
     * them would find nothing.
     *
     * @param lineText    the wording with its values already in place
     * @param leadingRuns the line's own runs, in the order they appear
     * @param phraseRuns  the runs the filled phrase brought, which follow them
     * @return the line
     */
    static CompatibilityNoticeLine buildSplicedLine(
            String lineText,
            List<EmphasisedRun> leadingRuns,
            List<EmphasisedRun> phraseRuns) {

        var runs = new ArrayList<>(leadingRuns);
        runs.addAll(phraseRuns);

        return new CompatibilityNoticeLine(lineText, runs);
    }

    /**
     * One templated phrase as its filled text and the runs it is made of.
     *
     * <p>Split on the slot the template already carries, so that a value can stand out differently
     * from the wording around it - a mod named inside a warning is brought forward there as it is
     * anywhere else, rather than disappearing into the colour of the instruction holding it.
     *
     * <p>A value marked the same as the wording is folded into it rather than kept apart, so what
     * reads as one colour arrives as one run. That is what keeps a two-letter join out of the run
     * list, a short run being the one thing this could hand the engine that might match inside a
     * longer word.
     *
     * @param template        the wording with a slot per value
     * @param wordingEmphasis what the words around the values stand out as
     * @param values          the values, in slot order
     * @return the filled phrase and its runs, in reading order
     */
    static CompatibilityNoticeLine buildPhrase(
            String template,
            Emphasis wordingEmphasis,
            EmphasisedRun... values) {

        var text = new StringBuilder();
        var runs = new ArrayList<EmphasisedRun>();
        var wording = new StringBuilder();
        var parts = template.split(Pattern.quote(VALUE_SLOT), -1);

        for (var i = 0; i < parts.length; i++) {
            text.append(parts[i]);
            wording.append(parts[i]);

            if (i >= values.length) {
                continue;
            }
            var value = values[i];
            text.append(value.runText());

            if (value.emphasis() == wordingEmphasis) {
                wording.append(value.runText());
                continue;
            }
            closeWordingRun(runs, wording, wordingEmphasis);
            runs.add(value);
        }
        closeWordingRun(runs, wording, wordingEmphasis);

        return new CompatibilityNoticeLine(text.toString(), runs);
    }

    /**
     * A run brought forward: a name, a version, or the place to look.
     *
     * @param runText the run itself, exactly as it appears in the line
     * @return the run
     */
    static EmphasisedRun bringForward(String runText) {

        return new EmphasisedRun(runText, Emphasis.HIGHLIGHT);
    }

    /**
     * A run warned with: what went wrong, what it costs, or what to do about it.
     *
     * @param runText the run itself, exactly as it appears in the line
     * @return the run
     */
    static EmphasisedRun warn(String runText) {

        return new EmphasisedRun(runText, Emphasis.WARNING);
    }

    // The wording gathered since the last value, as one run. Trimmed so a run begins and ends on a
    // word: the engine checks the character before a match, and a run opening on a space would be
    // asked to start in the middle of the word before it.
    private static void closeWordingRun(
            List<EmphasisedRun> runs,
            StringBuilder wording,
            Emphasis wordingEmphasis) {

        var run = wording.toString().trim();
        wording.setLength(0);

        if (!run.isEmpty()) {
            runs.add(new EmphasisedRun(run, wordingEmphasis));
        }
    }
}
