package kmlib.starsector.ui.controls.specs;

import kmlib.starsector.ui.text.LabelRun;
import kmlib.starsector.ui.text.LabelRuns;
import kmlib.starsector.ui.text.TextSpan;

import java.util.List;

/**
 * A text-only caption row, drawn but never clicked - it heads a run of controls with a title. It
 * is not {@link InteractiveSpec}: a caption carries no lit cell and no action.
 *
 * <p>Its one component is named for what it holds, as the two controls above name theirs: a caption
 * is a label like any other control's, and calling it {@code text} here and {@code label} there
 * would make one concept read as two.
 *
 * @param labelRuns the caption's runs in reading order - text in the colour it draws in before any
 *                  opacity fade the host applies, or an image squared off the row; never empty
 */
public record LabelSpec(
    List<LabelRun> labelRuns) implements ControlSpec {

    /** Holds the label to the floor {@link LabelRuns} holds every label to. */
    public LabelSpec {
        labelRuns = LabelRuns.copyRuns(labelRuns);
    }

    /**
     * Builds a caption of one run - what a caption that reads in a single colour is.
     *
     * @param labelRun the caption's one run - ordinarily a {@link TextSpan}, the text and the colour
     *                 it draws in
     * @return the caption spec
     */
    public static LabelSpec createLabel(LabelRun labelRun) {
        return new LabelSpec(List.of(labelRun));
    }

    /**
     * Returns a copy whose caption runs on into {@code labelRun} - the next stretch of the same
     * sentence, a stretch of text picked out in its own colour or an image set among the words,
     * while what came before it stays as it was.
     *
     * @param labelRun the run continuing the caption
     * @return an otherwise-identical caption carrying that run last
     */
    public LabelSpec continuesWith(LabelRun labelRun) {
        return new LabelSpec(LabelRuns.appendRun(labelRuns, labelRun));
    }

    @Override
    public List<String> labels() {
        return List.of(LabelRuns.resolveLineText(labelRuns));
    }
}
