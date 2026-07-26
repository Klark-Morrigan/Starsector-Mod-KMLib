package kmlib.starsector.ui.render.gl;

import java.awt.Color;

/**
 * A neutrally-named look bundle both a {@link PanelRenderer} and the generic {@link ControlRenderer}
 * read: the backdrop fill, the accent the chrome and controls stroke with, the brighter accent a
 * checkbox ticks with, the body-control font, the {@link TabStyle} a tabs control draws in, and the
 * {@link NotchStyle} a collapse handle draws in. It bundles the look so a consumer builds it once
 * (typically each frame from its live player colours and settings) rather than threading a dozen loose
 * arguments through the render call. The tab-specific and handle-specific looks are grouped into their
 * own records rather than spread across this one, so the generic look stays separate from the few
 * pieces of chrome that need a palette of their own. The per-frame values that are not "look" - the
 * border width, the opacity - stay render parameters.
 *
 * @param panelFill    the box backdrop, filled behind the chrome and controls
 * @param accent       the frame / wash / label-chrome colour every element strokes with
 * @param brightAccent the brighter colour a checkbox ticks with
 * @param bodyFont     the {@code graphics/fonts} basename the body-control labels draw in
 * @param tabStyle     the look a tabs control draws in (colours + face), read only when one is drawn
 * @param notchStyle   the chevron shades a collapse handle draws in, read only when one is drawn
 */
public record WidgetStyle(
        Color panelFill,
        Color accent,
        Color brightAccent,
        String bodyFont,
        TabStyle tabStyle,
        NotchStyle notchStyle) {
}
