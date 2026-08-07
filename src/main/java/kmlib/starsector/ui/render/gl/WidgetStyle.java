package kmlib.starsector.ui.render.gl;

import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.sound.UiSoundScheme;
import kmlib.starsector.ui.widgets.tabs.TabStyle;

import java.awt.Color;

/**
 * A neutrally-named look bundle both a {@link PanelRenderer} and the generic {@link ControlRenderer}
 * read: the backdrop fill, the colour the outer frame strokes in, the accent the controls wash and label
 * with, the brighter accent a checkbox ticks with, the body-control font, the {@link TabStyle} a tabs
 * control draws in, and the {@link NotchColours} a collapse handle draws in. It bundles the look so a
 * consumer builds it once (typically each frame from its live player colours and settings) rather than
 * threading a dozen loose arguments through the render call. The handle-specific look is grouped into its
 * own record rather than spread across this one, so the generic look stays separate from the chrome that
 * needs a palette of its own; the tab look already travels as one value the layout shares. The per-frame
 * values that are not "look" - the border width, the opacity - stay render parameters.
 *
 * <p>The frame colour is its own field rather than the accent reused, because a panel can sit in a host
 * whose surrounding chrome is drawn in a different colour from the accent its controls want: the frame
 * has to match what it abuts, the controls what the player picked. A host that wants neither to diverge
 * passes its accent for both.
 *
 * @param panelFill    the box backdrop, filled behind the chrome and controls
 * @param borderColour the colour the panel's outer frame strokes in - and with it the collapse handle's
 *                     outer edges, which continue that frame past the box - independent of the accent
 * @param accent       the wash / label-chrome colour every control strokes with
 * @param brightAccent the brighter colour a checkbox ticks with
 * @param bodyFont     the atlas the body-control labels draw in
 * @param tabStyle     the tab look a tabs control draws in; only its colours and face are read here,
 *                     its band height being the layout's side of the same value
 * @param notchColours the chevron shades a collapse handle draws in, read only when one is drawn
 * @param soundScheme   which interface sound each moment a control on this panel answers makes, read by
 *                      whatever detects those moments rather than by a painter
 */
public record WidgetStyle(
    Color panelFill,
    Color borderColour,
    Color accent,
    Color brightAccent,
    StarsectorFont bodyFont,
    TabStyle tabStyle,
    NotchColours notchColours,
    UiSoundScheme soundScheme) {
}
