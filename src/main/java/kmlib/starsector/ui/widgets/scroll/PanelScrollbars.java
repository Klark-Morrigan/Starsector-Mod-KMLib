package kmlib.starsector.ui.widgets.scroll;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.widgets.PanelPlacement;

/**
 * The scrollbar geometry for a {@link PanelPlacement}: a thin convenience over the region-scoped {@link
 * Scrollbar} that projects the panel to its {@link ScrollRegion} and computes the track, thumb, grab
 * column, and pointer-to-offset for it. It is the one place the panel-to-scrollbar bridge lives, so a
 * panel's renderer and its input controller read the same geometry rather than each re-projecting the
 * placement - what is drawn and what a drag grabs cannot drift. How thick the bar draws is taken off that
 * same placement, so neither pass names a width of its own.
 *
 * <p>{@link Scrollbar} stays scoped to a bare {@link ScrollRegion}; this only adapts a panel onto it.
 * Pure geometry in UI coordinates, computing rectangles and offsets and rendering nothing.
 */
public final class PanelScrollbars {
    private PanelScrollbars() {
    }

    /**
     * @param placement the laid-out panel
     * @return the scrollbar track in the panel body's right gutter, at the placement's own thickness
     */
    public static Rectangle computeTrack(PanelPlacement placement) {
        return computeTrackOver(placement, placement.toScrollRegion());
    }

    /**
     * @param placement the laid-out panel
     * @return the thumb sized and positioned within the track for how far the panel's list is scrolled
     */
    public static Rectangle computeThumb(PanelPlacement placement) {
        var region = placement.toScrollRegion();
        return Scrollbar.computeThumb(region, computeTrackOver(placement, region));
    }

    /**
     * @param placement the laid-out panel
     * @return the gutter column a drag grabs the scrollbar by, stated whether or not this placement draws
     *         a bar - {@link PanelPlacement#isScrollbarDrawn} is what says there is one to grab
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
        var track = computeTrackOver(placement, region);
        return Scrollbar.resolveOffsetForPointer(region, track, pointerY);
    }

    // The track for a placement over a region already projected from it - the one pairing of a region with
    // the thickness that sizes its bar. Every method here that needs a track goes through this, so a
    // thickness cannot come to be read on the path that draws the bar and missed on a path that measures
    // against it. Takes the region rather than re-projecting, the callers holding one already.
    private static Rectangle computeTrackOver(PanelPlacement placement, ScrollRegion region) {
        return Scrollbar.computeTrack(region, placement.scrollbarThickness());
    }
}
