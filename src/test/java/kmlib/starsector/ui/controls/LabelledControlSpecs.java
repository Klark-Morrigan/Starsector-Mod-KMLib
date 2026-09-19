package kmlib.starsector.ui.controls;

import kmlib.starsector.ui.controls.specs.CheckboxSpec;
import kmlib.starsector.ui.controls.specs.ControlAction;
import kmlib.starsector.ui.controls.specs.LabelSpec;
import kmlib.starsector.ui.controls.specs.ToggleSpec;
import kmlib.starsector.ui.text.TextSpan;

import java.awt.Color;

/**
 * Test-only builders for the three controls that carry a label of their own - the checkbox, the toggle,
 * and the caption. A test states the words it is measuring, placing, or clicking and these wrap them in
 * the one fixture tone, so a fixture stays a plain string while the spec under test still holds runs.
 *
 * <p>The runs carry a literal colour rather than the engine's live text tone, since nothing about a
 * layout or a hit test turns on which colour a control draws in and resolving the live palette would put
 * the game's settings in the way of every one of these fixtures.
 */
public final class LabelledControlSpecs {

    /** The tone every fixture label carries - a literal, so no fixture needs the live palette. */
    public static final Color LABEL_TEXT_COLOUR = Color.WHITE;

    private LabelledControlSpecs() {
    }

    /**
     * Builds a checkbox labelled in the fixture tone.
     *
     * @param label  the checkbox's trailing label
     * @param isOn   whether the box is ticked
     * @param action what a click on the row does
     * @return the checkbox spec
     */
    public static CheckboxSpec buildCheckbox(
            String label,
            boolean isOn,
            ControlAction action) {

        return CheckboxSpec.lit(buildLabelSpan(label), isOn, action);
    }

    /**
     * Builds a caption labelled in the fixture tone.
     *
     * @param label the caption text
     * @return the caption spec
     */
    public static LabelSpec buildLabel(String label) {
        return LabelSpec.createLabel(buildLabelSpan(label));
    }

    /**
     * Builds one label run in the fixture tone, for a test composing its own runs.
     *
     * @param text the run's text
     * @return the run
     */
    public static TextSpan buildLabelSpan(String text) {
        return new TextSpan(text, LABEL_TEXT_COLOUR);
    }

    /**
     * Builds a toggle labelled in the fixture tone.
     *
     * @param label  the button's centred label
     * @param isOn   whether the button is lit
     * @param action what a click on the button does
     * @return the toggle spec
     */
    public static ToggleSpec buildToggle(
            String label,
            boolean isOn,
            ControlAction action) {

        return ToggleSpec.lit(buildLabelSpan(label), isOn, action);
    }
}
