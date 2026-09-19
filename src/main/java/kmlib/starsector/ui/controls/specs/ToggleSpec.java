package kmlib.starsector.ui.controls.specs;

import kmlib.starsector.ui.text.LabelRun;
import kmlib.starsector.ui.text.LabelRuns;
import kmlib.starsector.ui.text.TextSpan;

import java.util.List;

/**
 * A single framed button that washes when on: a single-cell control lit at {@link #SINGLE_CELL}
 * when on and {@link #NO_SELECTION} when off, the button itself the hit target. It reads the same
 * on/off state as a {@link CheckboxSpec} but draws as a lit push-button with its label centred inside,
 * rather than a tick box with an adjacent label.
 *
 * @param labelRuns     the centred label's runs in reading order - text in the colour it draws in
 *                      before any opacity fade the host applies, or an image squared off the row;
 *                      never empty
 * @param selectedIndex {@link #SINGLE_CELL} when on, {@link #NO_SELECTION} when off
 * @param action        what a click on the button does
 */
public record ToggleSpec(
    List<LabelRun> labelRuns,
    int selectedIndex,
    ControlAction action) implements InteractiveSpec {

    /** Holds the label to the floor {@link LabelRuns} holds every label to. */
    public ToggleSpec {
        labelRuns = LabelRuns.copyRuns(labelRuns);
    }

    /**
     * Builds a toggle in its current lit state, mapping on/off to the single-cell {@code
     * selectedIndex} as {@link CheckboxSpec#lit} does for a tick box.
     *
     * @param labelRun the centred label's one run - ordinarily a {@link TextSpan}, the text and the
     *                 colour it draws in
     * @param isOn     whether the button is lit
     * @param action   what a click on the button does
     * @return the toggle spec
     */
    public static ToggleSpec lit(LabelRun labelRun, boolean isOn, ControlAction action) {

        return new ToggleSpec(
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
     * @return an otherwise-identical toggle whose label carries that run last
     */
    public ToggleSpec continuesWith(LabelRun labelRun) {

        return new ToggleSpec(
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
