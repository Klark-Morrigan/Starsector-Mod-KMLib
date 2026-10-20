package kmlib.testfixtures.starsector.ui.label;

import com.fs.starfarer.api.campaign.TextPanelAPI;

import org.mockito.Answers;
import org.mockito.invocation.InvocationOnMock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * A stand-in dialog text panel that keeps every highlighted paragraph added to it, so the highlights can be held to
 * the game's rule afterwards.
 *
 * <p>A text panel takes a paragraph's highlights in the {@code addPara} call itself, as the trailing runs, rather than
 * through the label it returns as a tooltip does. A subject draws into this panel as it would into the game's, and
 * {@link #findUnhighlightedRuns()} then reads every paragraph's text beside the runs it was added with.
 */
public final class HighlightedTextPanelMock {

    private final List<AddedParagraph> addedParagraphs = new ArrayList<>();
    private final TextPanelAPI textPanelMock = mock(
        TextPanelAPI.class,
        withSettings().defaultAnswer(this::recordHighlightedParagraph));

    private HighlightedTextPanelMock() {
    }

    /**
     * Opens a text panel recording nothing yet.
     *
     * @return the recording text panel
     */
    public static HighlightedTextPanelMock createTextPanelMock() {
        return new HighlightedTextPanelMock();
    }

    /**
     * The runs any highlighted paragraph added so far would leave plain once drawn.
     *
     * @return one sentence per such run, naming the run, its paragraph and what blocked it
     */
    public List<String> findUnhighlightedRuns() {

        var unhighlightedRuns = new ArrayList<String>();

        for (var addedParagraph : addedParagraphs) {

            LabelHighlightRule
                .findUnhighlightedRuns(addedParagraph.paragraphText(), addedParagraph.runTexts())
                .forEach(run -> unhighlightedRuns.add(run.describeIn(addedParagraph.paragraphText())));
        }
        return unhighlightedRuns;
    }

    /**
     * The text panel to hand the subject.
     *
     * @return the stand-in text panel
     */
    public TextPanelAPI getTextPanel() {
        return textPanelMock;
    }

    /**
     * The text of every paragraph added with highlights, in the order they were added.
     *
     * @return the paragraph texts
     */
    public List<String> listHighlightedParagraphTexts() {

        return addedParagraphs.stream()
            .map(AddedParagraph::paragraphText)
            .toList();
    }

    // Both highlighting addPara overloads end in the runs, so a call whose last raw argument is a run array is one.
    private Object recordHighlightedParagraph(InvocationOnMock invocation) throws Throwable {

        var arguments = invocation.getRawArguments();

        if ("addPara".equals(invocation.getMethod().getName())
                && arguments.length > 2
                && arguments[arguments.length - 1] instanceof String[] runTexts) {

            // Arrays.asList keeps a null run, which the rule skips as the game does.
            addedParagraphs.add(new AddedParagraph((String) arguments[0], Arrays.asList(runTexts)));
        }

        return Answers.RETURNS_DEFAULTS.answer(invocation);
    }

    private record AddedParagraph(
        String paragraphText,
        List<String> runTexts) {
    }
}
