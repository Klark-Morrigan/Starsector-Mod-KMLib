package kmlib.starsector.ui.render.gl;

import java.awt.Color;

/**
 * How one line of body text looks: the face it draws in, the colour before its opacity fade, that
 * opacity, and the glyph size. Bundles the four "look" inputs {@link LabelRenderer} draws with into
 * one value, so a call passes its styling as a unit and its placement - the text, position, and
 * anchor - reads apart from it rather than four style arguments being threaded in among the four
 * placement ones.
 *
 * @param font     the body font's {@code graphics/fonts} basename
 * @param colour   the text colour before the opacity fade
 * @param opacity  overall alpha, 0..1
 * @param fontSize the glyph size to render at
 */
public record LabelStyle(String font, Color colour, float opacity, double fontSize) {
}
