package kmlib.testfixtures.starsector.ui.label;

import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A stand-in tooltip that gives every paragraph added to it a label of its own, so the highlights each
 * paragraph sets can be held to the game's rule afterwards.
 *
 * <p>The game routes a paragraph's highlights through the label {@code addPara} returns, one label per
 * paragraph. A subject draws into this tooltip as it would into the game's, and
 * {@link #findUnhighlightedRuns()} then reads every paragraph's text beside the runs set on its label.
 */
public final class HighlightedTooltipMock {

    private final List<AddedParagraph> addedParagraphs = new ArrayList<>();
    private final TooltipMakerAPI tooltipMock = mock(TooltipMakerAPI.class);

    private HighlightedTooltipMock() {

        when(tooltipMock.addPara(anyString(), any(Color.class), anyFloat()))
            .thenAnswer(invocation -> {

                var labelMock = mock(LabelAPI.class);

                addedParagraphs.add(new AddedParagraph(invocation.getArgument(0), labelMock));
                return labelMock;
            });
    }

    /**
     * Opens a tooltip recording nothing yet.
     *
     * @return the recording tooltip
     */
    public static HighlightedTooltipMock createTooltipMock() {
        return new HighlightedTooltipMock();
    }

    /**
     * The runs any paragraph added so far would leave plain once drawn.
     *
     * @return one sentence per such run, naming the run, its paragraph and what blocked it
     */
    public List<String> findUnhighlightedRuns() {

        var unhighlightedRuns = new ArrayList<String>();

        for (var addedParagraph : addedParagraphs) {

            var runTexts = ArgumentCaptor.forClass(String[].class);

            Mockito.verify(addedParagraph.labelMock(), Mockito.atMost(1))
                .setHighlight(runTexts.capture());

            if (runTexts.getAllValues().isEmpty()) {
                continue;
            }

            LabelHighlightRule
                .findUnhighlightedRuns(addedParagraph.paragraphText(), List.of(runTexts.getValue()))
                .forEach(run -> unhighlightedRuns.add("'" + run.runText() + "' in '" + addedParagraph.paragraphText()
                    + "' (" + run.blockingNeighbours() + ")"));
        }
        return unhighlightedRuns;
    }

    /**
     * The tooltip to hand the subject.
     *
     * @return the stand-in tooltip
     */
    public TooltipMakerAPI getTooltip() {
        return tooltipMock;
    }

    private record AddedParagraph(
        String paragraphText,
        LabelAPI labelMock) {
    }
}
