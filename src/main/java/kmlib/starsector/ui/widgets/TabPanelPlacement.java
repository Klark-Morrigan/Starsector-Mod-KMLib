package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

import java.util.List;

/**
 * The laid-out rectangles of one {@link TabPanel}: the outer {@code box} spanning the whole
 * footprint (border and all), one {@link VanillaTab} per tab in row order, and the {@code body}
 * rectangle the active tab fills. All are in UI coordinates, so the same placement the panel draws
 * is the one a consumer hit-tests and lays its body controls into, with no conversion.
 *
 * <p>{@code body} is a zero-size rectangle when the active tab opens no body (see
 * {@link TabPanelBodySize#NONE}), so a bodyless tab reserves no framed region beneath the tab row.
 *
 * @param box  the panel's full footprint, border included
 * @param tabs the laid-out tabs, in row order left to right
 * @param body the framed body rectangle beneath the tabs, zero-size when the tab has no body
 */
public record TabPanelPlacement(Rectangle box, List<VanillaTab> tabs, Rectangle body) {
}
