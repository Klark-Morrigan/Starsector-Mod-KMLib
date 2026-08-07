package kmlib.starsector.ui.render.gl.style;

import kmlib.starsector.ui.render.gl.controls.RadioColours;

import java.awt.Color;

/**
 * The accent a panel's controls are drawn in, as the three steps the engine's own controls are built
 * from: the recessive shade a surface behind a control is filled and framed with, the shade every
 * control washes and labels with, and the brighter one reserved for the marks that have to read against
 * it - a checkbox's tick, a lit tab's label. Three steps of one accent rather than three unrelated
 * colours, which is why they travel together: a look that moved one without the others would leave a
 * tick no longer distinguishable from the chrome it sits on, or a frame no longer of a piece with what
 * it encloses.
 *
 * <p>Ordered darkest to brightest, which is what makes a slip of argument order visible: three bare
 * {@code Color}s in a row are three positions a caller can transpose with nothing to catch it, and a
 * step out of place is a step that reads wrong against the two beside it.
 *
 * <p>Apart from {@link BoxColours} rather than beside it in one palette, because the two dress different
 * things - the box has to match the chrome it abuts, the controls what the player picked - and because
 * keeping them apart is what stops a frame's shade being handed to a control by a slip of argument
 * order. {@link RadioColours} is the narrower sibling: one control's own two roles, where this is the
 * set every control on the panel draws from.
 *
 * @param dark   the recessive step, for the surface a control stands on and the frame around it - what
 *               a shade is wanted for that must recede behind the base rather than carry a mark
 * @param base   the wash / label-chrome colour every control strokes with
 * @param bright the brighter step, for a checkbox's tick and anything else that must read against the
 *               base
 */
public record AccentColours(
    Color dark,
    Color base,
    Color bright) {
}
