package kmlib.testfixtures.starsector.ui.label;

import com.fs.starfarer.api.ui.LabelAPI;

import kmlib.starsector.ui.highlight.HighlightedParagraph;

import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Which runs a label's highlight call would leave plain, decided the way the game's renderer decides it.
 *
 * <p>The game finds each run with a plain search, then accepts the hit only when the character on each
 * side is whitespace or ASCII punctuation, or the run starts or ends the text. Any other neighbour skips
 * the hit without an error. A run beside a CJK character or full-width punctuation such as {@code 。} is
 * therefore never highlighted. Each run is searched for from where the previous run matched, and a line
 * break counts as a space.
 *
 * <p>Copied from the renderer rather than called, because the game's text classes do not load outside a
 * running game.
 */
public final class LabelHighlightRule {

    // The characters the game accepts before a run, besides whitespace.
    private static final String ACCEPTED_BEFORE_RUN = "/.,;:\"'[]+-=!@$%^&*(){}|\\?<>`~";

    // The characters the game accepts after a run, besides whitespace. The underscore is accepted here and
    // not before a run, as the game has it.
    private static final String ACCEPTED_AFTER_RUN = "-_.,;:\"'[]+=!@$%^&*(){}|\\?/<>`~";

    private LabelHighlightRule() {
    }

    /**
     * The runs a paragraph would leave plain once drawn: its highlights as it hands them to a label, held
     * to the game's rule over its text.
     *
     * @param paragraph the paragraph as a caller builds it
     * @return one entry per run left plain, in the order the paragraph hands them over
     */
    public static List<UnhighlightedRun> findUnhighlightedRuns(HighlightedParagraph paragraph) {

        if (paragraph.getHighlights().length == 0) {
            return List.of();
        }

        var labelMock = mock(LabelAPI.class);
        var runTexts = ArgumentCaptor.forClass(String[].class);

        paragraph.applyTo(labelMock);

        verify(labelMock)
            .setHighlight(runTexts.capture());

        return findUnhighlightedRuns(paragraph.getText(), List.of(runTexts.getValue()));
    }

    /**
     * The runs a highlight call over the text would leave plain, in the order they were given.
     *
     * @param text      the label's text
     * @param runTexts  the runs passed to the highlight call, in order; empty runs are skipped, as the
     *                  game skips them
     * @return one entry per run left plain, naming the neighbours that blocked its first occurrence
     */
    public static List<UnhighlightedRun> findUnhighlightedRuns(String text, List<String> runTexts) {

        Objects.requireNonNull(text, "text");

        var searchedText = text.replace('\n', ' ');
        var unhighlightedRuns = new ArrayList<UnhighlightedRun>();
        var searchStart = 0;

        for (var runText : runTexts) {

            if (runText == null || runText.isEmpty()) {
                continue;
            }

            var searchedRun = runText.replace('\n', ' ');
            var matchEnd = findAcceptedMatchEnd(text, searchedText, searchedRun, searchStart);

            if (matchEnd.isPresent()) {
                searchStart = matchEnd.get();

            } else {

                unhighlightedRuns.add(new UnhighlightedRun(
                    runText,
                    describeBlockingNeighbours(text, searchedText, searchedRun, searchStart)));
            }
        }
        return unhighlightedRuns;
    }

    // Names the refused neighbours of the first occurrence the game looked at, or why it looked at none.
    private static String describeBlockingNeighbours(
            String text,
            String searchedText,
            String searchedRun,
            int searchStart) {

        var matchStart = searchedText.indexOf(searchedRun, searchStart);
        if (matchStart == -1) {

            return searchedText.contains(searchedRun)
                ? "only before the previous run's match, where the search does not look"
                : "not in the text";
        }

        var matchEnd = matchStart + searchedRun.length();
        var blockingNeighbours = new ArrayList<String>();

        if (!isAcceptedBefore(text, matchStart)) {
            blockingNeighbours.add("before it: " + text.charAt(matchStart - 1));
        }

        if (!isAcceptedAfter(text, matchEnd)) {
            blockingNeighbours.add("after it: " + text.charAt(matchEnd));
        }

        return String.join(", ", blockingNeighbours);
    }

    // A refused hit does not end the search: a later occurrence of the same run may still be accepted.
    private static Optional<Integer> findAcceptedMatchEnd(
            String text,
            String searchedText,
            String searchedRun,
            int searchStart) {

        var position = searchStart;

        while (position < searchedText.length()) {

            var matchStart = searchedText.indexOf(searchedRun, position);
            if (matchStart == -1) {
                return Optional.empty();
            }

            var matchEnd = matchStart + searchedRun.length();

            if (isAcceptedBefore(text, matchStart) && isAcceptedAfter(text, matchEnd)) {
                return Optional.of(matchEnd);
            }

            position = matchEnd;
        }
        return Optional.empty();
    }

    private static boolean isAcceptedAfter(String text, int matchEnd) {

        if (matchEnd >= text.length()) {
            return true;
        }

        var nextCharacter = text.charAt(matchEnd);
        var isFollowedByDigit = matchEnd + 1 < text.length()
            && Character.isDigit(text.charAt(matchEnd + 1));

        if (!Character.isWhitespace(nextCharacter) && ACCEPTED_AFTER_RUN.indexOf(nextCharacter) == -1) {
            return false;
        }

        // A run ending before a decimal point or a thousands separator would split a number.
        return !((nextCharacter == ',' || nextCharacter == '.')
            && isFollowedByDigit);
    }

    private static boolean isAcceptedBefore(String text, int matchStart) {

        if (matchStart == 0) {
            return true;
        }

        var previousCharacter = text.charAt(matchStart - 1);

        return Character.isWhitespace(previousCharacter)
            || ACCEPTED_BEFORE_RUN.indexOf(previousCharacter) != -1;
    }

    /**
     * One run a highlight call would leave plain.
     *
     * @param runText            the run as passed to the highlight call
     * @param blockingNeighbours which neighbours of its first occurrence the game refuses, or that the
     *                           run is not in the text
     */
    public record UnhighlightedRun(
        String runText,
        String blockingNeighbours) {

        /**
         * Words this run as a finding: the run, the text it was looked for in, and what blocked it.
         *
         * @param text the text the run was looked for in
         * @return the finding
         */
        public String describeIn(String text) {
            return "'" + runText + "' in '" + text + "' (" + blockingNeighbours + ")";
        }
    }
}
