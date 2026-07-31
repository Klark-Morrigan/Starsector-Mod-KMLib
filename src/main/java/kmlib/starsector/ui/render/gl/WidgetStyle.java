package kmlib.starsector.ui.render.gl;

import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.widgets.tabs.TabStyle;

import java.awt.Color;

/**
 * A neutrally-named look bundle both a {@link PanelRenderer} and the generic {@link ControlRenderer}
 * read: the backdrop fill, the accent the chrome and controls stroke with, the brighter accent a
 * checkbox ticks with, the body-control font, the {@link TabStyle} a tabs control draws in, and the
 * {@link NotchColors} a collapse handle draws in. It bundles the look so a consumer builds it once
 * (typically each frame from its live player colours and settings) rather than threading a dozen loose
 * arguments through the render call. The handle-specific look is grouped into its own record rather than
 * spread across this one, so the generic look stays separate from the chrome that needs a palette of its
 * own; the tab look already travels as one value the layout shares. The per-frame values that are not
 * "look" - the border width, the opacity - stay render parameters.
 *
 * @param panelFill    the box backdrop, filled behind the chrome and controls
 * @param accent       the frame / wash / label-chrome colour every element strokes with
 * @param brightAccent the brighter colour a checkbox ticks with
 * @param bodyFont     the atlas the body-control labels draw in
 * @param tabStyle     the tab look a tabs control draws in; only its colours and face are read here,
 *                     its band height being the layout's side of the same value
 * @param notchColors  the chevron shades a collapse handle draws in, read only when one is drawn
 */
public record WidgetStyle(
    Color panelFill,
    Color accent,
    Color brightAccent,
    StarsectorFont bodyFont,
    TabStyle tabStyle,
    NotchColors notchColors) {
}
