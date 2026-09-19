package kmlib.starsector.ui.controls.specs;

import kmlib.starsector.ui.text.LabelRun;
import kmlib.starsector.ui.text.LabelRuns;
import kmlib.starsector.ui.text.TextSpan;

import java.util.List;

/**
 * A tick box with a trailing label: a single-cell control lit at {@link #SINGLE_CELL} when on and
 * {@link #NO_SELECTION} when off, its whole row one hit target. The renderer draws the tick box at
 * the row's left and the label beside it.
 *
 * @param labelRuns     the trailing label's runs in reading order - text in the colour it draws in
 *                      before any opacity fade the host applies, or an image squared off the row;
 *                      never empty
 * @param selectedIndex {@link #SINGLE_CELL} when ticked, {@link #NO_SELECTION} when off
 * @param action        what a click on the row does
 */
public record CheckboxSpec(
    List<LabelRun> labelRuns,
    int selectedIndex,
    ControlAction action) implements InteractiveSpec {

    /** Holds the label to the floor {@link LabelRuns} holds every label to. */
    public CheckboxSpec {
        labelRuns = LabelRuns.copyRuns(labelRuns);
    }

    /**
     * Builds a checkbox in its current lit state, mapping on/off to the single-cell {@code
     * selectedIndex} in one place so no host re-derives the "cell 0 lit or nothing" convention. Its
     * label is the one run given; a host calling part of it out layers a second on with
     * {@link #continuesWith}.
     *
     * @param labelRun the trailing label's one run - ordinarily a {@link TextSpan}, the text and the
     *                 colour it draws in
     * @param isOn     whether the box is ticked
     * @param action   what a click on the row does
     * @return the checkbox spec
     */
    public static CheckboxSpec lit(LabelRun labelRun, boolean isOn, ControlAction action) {

        return new CheckboxSpec(
            List.of(labelRun),
            isOn ? SINGLE_CELL : NO_SELECTION,
            action);
    }

    /**
     * Returns a copy whose label runs on into {@code labelRun} - the next stretch of the same
     * sentence, a stretch of text picked out in its own colour or an image set among the words,
     * while what came before it stays as it was.
     *
     * @param labelRun the run continuing the label
     * @return an otherwise-identical checkbox whose label carries that run last
     */
    public CheckboxSpec continuesWith(LabelRun labelRun) {

        return new CheckboxSpec(
            LabelRuns.appendRun(labelRuns, labelRun),
            selectedIndex,
            action);
    }

    @Override
    public List<String> labels() {
        return List.of(LabelRuns.resolveLineText(labelRuns));
    }

    @Override
    public boolean isSegmented() {
        return false;
    }

    @Override
    public ReselectBehaviour reselectBehaviour() {
        return ReselectBehaviour.INERT;
    }
}
