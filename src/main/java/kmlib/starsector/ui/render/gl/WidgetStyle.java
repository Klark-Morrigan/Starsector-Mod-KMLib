package kmlib.starsector.ui.render.gl;

import java.awt.Color;

/**
 * A neutrally-named look bundle both a {@link PanelRenderer} and the generic {@link ControlRenderer}
 * read: the backdrop fill, the accent the chrome and controls stroke with, the brighter accent a
 * checkbox ticks with, the body-control font, and the {@link TabStyle} a tabs control draws in. It
 * bundles the look so a consumer builds it once (typically each frame from its live player colours
 * and settings) rather than threading a dozen loose arguments through the render call. The
 * tab-specific look is grouped into {@link TabStyle} rather than spread across this record, so the
 * generic look stays separate from the one control kind that needs its own palette and face. The
 * per-frame values that are not "look" - the border width, the opacity - stay render parameters.
 *
 * @param panelFill    the box backdrop, filled behind the chrome and controls
 * @param accent       the frame / wash / label-chrome colour every element strokes with
 * @param brightAccent the brighter colour a checkbox ticks with
 * @param bodyFont     the {@code graphics/fonts} basename the body-control labels draw in
 * @param tabStyle     the look a tabs control draws in (colours + font), read only when one is drawn
 */
public record WidgetStyle(
        Color panelFill,
        Color accent,
        Color brightAccent,
        String bodyFont,
        TabStyle tabStyle) {
}
