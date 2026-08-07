package kmlib.starsector.ui.render.gl;

import java.awt.Color;

/**
 * The two shades the box itself is drawn in: the backdrop filled behind everything, and the outer frame
 * stroked around it. Bundled for the reason {@link RadioColours} and {@link NotchColours} beside it are -
 * a renderer reads one palette rather than a loose pair - and for a second reason those two do not have:
 * these sit in {@link WidgetStyle} beside the accents, and four loose colours in one record are four
 * positions a caller can transpose without the compiler noticing. Split by what each dresses, a shade
 * meant for the frame can no longer be handed to the controls.
 *
 * <p>The frame is its own shade rather than the accent reused, because a panel can sit in a host whose
 * surrounding chrome is drawn in a different colour from the accent its controls want: the frame has to
 * match what it abuts, the controls what the player picked. A host that wants neither to diverge passes
 * one colour to both.
 *
 * <p>Opacity is not a colour, so it stays a render parameter - the same line {@link RadioColours} draws.
 *
 * @param fill   the box backdrop, filled behind the chrome and controls
 * @param border the outer frame's stroke - and with it the collapse handle's outer edges, which continue
 *               that frame past the box
 */
public record BoxColours(
    Color fill,
    Color border) {
}
