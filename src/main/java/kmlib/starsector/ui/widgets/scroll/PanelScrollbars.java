package kmlib.starsector.ui.widgets.scroll;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.widgets.PanelPlacement;

/**
 * The scrollbar geometry for a {@link PanelPlacement}: a thin convenience over the region-scoped {@link
 * Scrollbar} that projects the panel to its {@link ScrollRegion} and computes the track, thumb, grab
 * column, and pointer-to-offset for it. It is the one place the panel-to-scrollbar bridge lives, so a
 * panel's renderer and its input controller read the same geometry rather than each re-projecting the
 * placement - what is drawn and what a drag grabs cannot drift.
 *
 * <p>{@link Scrollbar} stays scoped to a bare {@link ScrollRegion}; this only adapts a panel onto it.
 * Pure geometry in UI coordinates, computing rectangles and offsets and rendering nothing.
 */
public final class PanelScrollbars {
    private PanelScrollbars() {
    }

    /**
     * @param placement the laid-out panel
     * @return the scrollbar track in the panel body's right gutter
     */
    public static Rectangle computeTrack(PanelPlacement placement) {
        return Scrollbar.computeTrack(placement.toScrollRegion());
    }

    /**
     * @param placement the laid-out panel
     * @return the thumb sized and positioned within the track for how far the panel's list is scrolled
     */
    public static Rectangle computeThumb(PanelPlacement placement) {
        var region = placement.toScrollRegion();
        return Scrollbar.computeThumb(region, Scrollbar.computeTrack(region));
    }

    /**
     * @param placement the laid-out panel
     * @return the gutter column a drag grabs the scrollbar by
     */
    public static Rectangle computeGrabColumn(PanelPlacement placement) {
        return Scrollbar.computeGrabColumn(placement.toScrollRegion());
    }

    /**
     * @param placement the laid-out panel
     * @param pointerY  the pointer's y, in UI coordinates
     * @return the scroll offset the pointer maps to along the track, 0..overflow
     */
    public static float resolveOffsetForPointer(PanelPlacement placement, float pointerY) {
        var region = placement.toScrollRegion();
        return Scrollbar.resolveOffsetForPointer(region, Scrollbar.computeTrack(region), pointerY);
    }
}
