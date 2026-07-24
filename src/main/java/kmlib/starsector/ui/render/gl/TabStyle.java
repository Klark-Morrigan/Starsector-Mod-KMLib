package kmlib.starsector.ui.render.gl;

import kmlib.starsector.ui.font.TextFace;

/**
 * The look of a {@link kmlib.starsector.ui.controls.ControlSpec.Tabs} control: the tab strip's colour
 * scheme and the face its labels draw in. Grouped as one value so a {@link WidgetStyle} carries the
 * tab-specific look as a single nested bundle rather than spreading the tab colours and face across
 * its own fields - the generic look (fill, accent, body font) stays separate from the one control kind
 * that needs a distinct palette and face. Read only when a tabs control is drawn (through {@link
 * ControlRenderer}); a host that never draws tabs still supplies one, unused.
 *
 * @param colors the tab strip's selected / hovered / idle colour scheme
 * @param face   the font basename and size the tab labels draw in
 */
public record TabStyle(VanillaTabColors colors, TextFace face) {
}
