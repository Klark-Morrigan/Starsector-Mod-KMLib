package kmlib.starsector.ui.map;

import kmlib.math.geometry.Rectangle;

import java.util.List;

/**
 * Where a map tab actually shows its map: the surface the map is drawn on, less the chrome the tab
 * lays beside or over it. One value rather than a bare rectangle because neither half answers the
 * question on its own - the surface says where the map could be visible, and the chrome says where
 * something else is drawn in front of it.
 *
 * <p>The chrome half is what a complement test alone cannot cover. On the {@code M} map the surface
 * is inset and every chrome piece sits beside it, so "outside the surface" catches all of it; on the
 * intel screen's map visor the surface fills the tab exactly and the control bar is drawn over it,
 * where no complement of the surface can exclude the bar. Carrying the siblings makes the same rule
 * fit both, and costs the map tab nothing because its added clause never fires there.
 *
 * @param box                the surface's own box in UI coordinates, confined to its tab
 * @param siblingChromeBoxes the boxes of the tab's other drawn direct children, whether they sit
 *                           beside the surface or over it
 */
public record MapSurfaceArea(
    Rectangle box,
    List<Rectangle> siblingChromeBoxes) {

    /**
     * Copied on the way in, because a caller holds this across frames while the list it was built
     * from is a scratch collection the measure walked the tree to fill.
     */
    public MapSurfaceArea {
        siblingChromeBoxes = List.copyOf(siblingChromeBoxes);
    }

    /**
     * Whether a point falls on the visible map: inside the surface, and inside none of the chrome
     * drawn in the same tab.
     *
     * @param pointX the point's x in UI coordinates
     * @param pointY the point's y in UI coordinates
     * @return whether the map, rather than the tab's own furniture, is what is under that point
     */
    public boolean containsPoint(float pointX, float pointY) {
        if (!box.containsPoint(pointX, pointY)) {
            return false;
        }
        for (var chromeBox : siblingChromeBoxes) {
            if (chromeBox.containsPoint(pointX, pointY)) {
                return false;
            }
        }
        return true;
    }
}
