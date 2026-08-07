package kmlib.starsector.ui.render.gl.controls;

import kmlib.starsector.ui.render.gl.style.WidgetStyle;

import java.awt.Color;

/**
 * The two-tone palette a radio group's chrome paints with: the frame stroke (the outer outline and the
 * dividers between segments) and the wash lighting the selected segment. Bundled so a radio renderer
 * reads one palette rather than a loose frame/selected colour pair, matching how
 * {@link kmlib.starsector.ui.widgets.tabs.style.TabPalette} groups a tab strip's shades - and both roles may
 * resolve to one accent (the common case), just as the tab strip's accent serves its wash and its
 * dividers alike. Opacity is not a colour, so it stays a render parameter (as {@link WidgetStyle} keeps
 * it); this record is only the two shades.
 *
 * @param frame        the outer outline and the inter-segment dividers
 * @param selectedWash the wash lighting the lit segment
 */
public record RadioColours(
    Color frame,
    Color selectedWash) {
}
