package kmlib.starsector.ui.render.gl;

import java.awt.Color;

/**
 * A neutrally-named look bundle both a {@link PanelRenderer} and the generic {@link ControlRenderer}
 * read: the backdrop fill, the accent the chrome and controls stroke with, the brighter accent a
 * checkbox ticks with, the tab strip's colour scheme, and the tab and body fonts. It bundles the look
 * so a consumer builds it once (typically each frame from its live player colours and settings) rather
 * than threading a dozen loose arguments through the render call. The per-frame values that are not
 * "look" - the border width, the opacity - stay render parameters.
 *
 * @param panelFill    the box backdrop, filled behind the chrome and controls
 * @param accent       the frame / wash / label-chrome colour every element strokes with
 * @param brightAccent the brighter colour a checkbox ticks with
 * @param tabColors    the tab strip's selected / hovered / idle colour scheme
 * @param tabFont      the {@code graphics/fonts} basename the tab labels draw in
 * @param tabFontSize  the size the tab labels draw at
 * @param bodyFont     the {@code graphics/fonts} basename the body-control labels draw in
 */
public record WidgetStyle(Color panelFill, Color accent, Color brightAccent, VanillaTabColors tabColors,
        String tabFont, double tabFontSize, String bodyFont) {
}
