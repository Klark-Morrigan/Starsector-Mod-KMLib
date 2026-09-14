package kmlib.starsector.ui.render.gl;

import kmlib.starsector.ui.font.TextFace;

import java.awt.Color;

/**
 * How one line of body text looks: the face it draws in, the colour before its opacity fade, and that
 * opacity. Bundles the "look" inputs {@link LabelRenderer} draws with into one value, so a call passes
 * its styling as a unit and its placement - the text, position, and anchor - reads apart from it rather
 * than the style arguments being threaded in among the placement ones.
 *
 * @param face    the body font's atlas and glyph size
 * @param colour  the text colour before the opacity fade
 * @param opacity overall alpha, 0..1
 */
public record LabelStyle(
    TextFace face,
    Color colour,
    float opacity) {
}
