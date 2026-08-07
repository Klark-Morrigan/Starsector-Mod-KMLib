package kmlib.starsector.ui.render.gl.style;

import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.render.gl.controls.ControlRenderer;
import kmlib.starsector.ui.render.gl.panel.PanelRenderer;
import kmlib.starsector.ui.sound.UiSoundScheme;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;

/**
 * A neutrally-named look bundle both a {@link PanelRenderer} and the generic {@link ControlRenderer}
 * read: the {@link BoxColours} the box is drawn in, the {@link AccentColours} its controls draw from,
 * the body-control font, the {@link TabStyle} a tabs control draws in, the {@link NotchColours} a
 * collapse handle draws in, and the {@link UiSoundScheme} every control on the panel answers by. It
 * bundles the look so a consumer builds it once (typically each frame from its live player colours and
 * settings) rather than threading a dozen loose arguments through the render call. The per-frame values
 * that are not "look" - the border width, the opacity - stay render parameters.
 *
 * <p>Every field here is a grouped value rather than a loose one, and no two of them share a type. That
 * is deliberate: this record is built positionally from six things a host resolves separately, and four
 * bare {@code Color}s in a row would be four positions a caller could transpose with nothing to catch
 * it - a frame stroked in the tick colour compiles and paints. Grouped by what each dresses, the same
 * slip is a compile error, and each group is small enough that what remains transposable inside one is
 * two shades named a line apart. The handle and the tabs were already grouped this way, for their own
 * reason: each is chrome a consumer may want pitched apart from the panel's accents, so its shades are
 * swapped wholesale.
 *
 * <p>Sound sits in the look bundle rather than beside it because how a panel answers a press is part of
 * how it presents itself, exactly as its fills are: a host that describes its panel describes all of it
 * in one value, and a control added to that panel inherits its sound the way it inherits its accent. It
 * is the one field here no painter reads, sound being a look with no pixels; it travels with the look so
 * a host has one place to state its presentation rather than one for the seen half and one for the heard.
 *
 * @param boxColours    the backdrop and frame shades the box itself is drawn in
 * @param accentColours the accent pair every control on the panel washes, labels, and ticks with
 * @param bodyFont      the atlas the body-control labels draw in
 * @param tabStyle      the tab look a tabs control draws in; only its colours and face are read here,
 *                      its band height being the layout's side of the same value
 * @param notchColours  the chevron shades a collapse handle draws in, read only when one is drawn
 * @param soundScheme   which interface sound each moment a control on this panel answers makes, read by
 *                      whatever detects those moments rather than by a painter
 */
public record WidgetStyle(
    BoxColours boxColours,
    AccentColours accentColours,
    StarsectorFont bodyFont,
    TabStyle tabStyle,
    NotchColours notchColours,
    UiSoundScheme soundScheme) {
}
