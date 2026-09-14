package kmlib.starsector.ui.colour;

import java.awt.Color;

/**
 * The accent a panel is drawn in, as the three steps the engine's own controls are built from: the
 * recessive shade a surface behind a control is filled and framed with, the shade every control washes
 * and labels with, and the brighter one reserved for the marks that have to read against it - a
 * checkbox's tick, a lit tab's label. Three steps of one accent rather than three unrelated colours,
 * which is why they travel together: a look that moved one without the others would leave a tick no
 * longer distinguishable from the chrome it sits on, or a frame no longer of a piece with what it
 * encloses.
 *
 * <p>Ordered darkest to brightest, which is what makes a slip of argument order visible: three bare
 * {@code Color}s in a row are three positions a caller can transpose with nothing to catch it, and a
 * step out of place is a step that reads wrong against the two beside it. That is also the whole reason
 * anything wanting all three takes this rather than the three loose - the set exists to make the
 * transposition unrepresentable, so handing its members over one at a time gives it up.
 *
 * <p>Substrate-independent, and here rather than with the panel's other shade groups for that reason:
 * the engine builds a control from this set, so both a panel's paint pass and the neutral value a tab
 * row is measured and painted from are composed out of it. A shade group that only a GL pass reads can
 * sit with the GL passes; this one is read on both sides of that line, and the layout tier cannot import
 * across it.
 *
 * <p>Apart from the box's own fill-and-frame pair rather than bundled with it, because the two dress
 * different things - a box has to match the chrome it abuts, a control what the player picked - and
 * because keeping them apart is what stops a frame's shade reaching a control by a slip of argument
 * order.
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
