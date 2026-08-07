package kmlib.starsector.ui.render.gl.style;

import kmlib.starsector.ui.render.gl.controls.RadioColours;

import java.awt.Color;

/**
 * The accent pair a panel's controls are drawn in: the shade every control washes and labels with, and
 * the brighter one reserved for the marks that have to read against it - a checkbox's tick, a lit tab's
 * label. Two steps of one accent rather than two unrelated colours, which is why they travel together:
 * a look that brightened one without the other would leave a tick no longer distinguishable from the
 * chrome it sits on.
 *
 * <p>Apart from {@link BoxColours} rather than beside it in one palette, because the two dress different
 * things - the box has to match the chrome it abuts, the controls what the player picked - and because
 * keeping them apart is what stops a frame's shade being handed to a control by a slip of argument
 * order. {@link RadioColours} is the narrower sibling: one control's own two roles, where this is the
 * pair every control on the panel draws from.
 *
 * @param base   the wash / label-chrome colour every control strokes with
 * @param bright the brighter step, for a checkbox's tick and anything else that must read against the
 *               base
 */
public record AccentColours(
    Color base,
    Color bright) {
}
