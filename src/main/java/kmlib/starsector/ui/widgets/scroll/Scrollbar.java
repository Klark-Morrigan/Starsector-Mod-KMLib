package kmlib.starsector.ui.widgets.scroll;

import kmlib.math.geometry.Rectangle;
import kmlib.math.ranges.Ranges;

/**
 * The geometry of a vertical scrollbar over a {@link ScrollRegion}: the track in the region's container
 * gutter, the thumb sized and positioned within it for how far the content is scrolled, the grab column a
 * drag reads, and the scroll offset a pointer on the track maps to. Substrate-independent - it computes
 * rectangles and offsets, rendering nothing - so a GL or a UI-API renderer paints against it and an input
 * listener hit-tests it. The raw-GL paint lives in {@link kmlib.starsector.ui.render.gl.panel.ScrollbarRenderer}.
 *
 * <p>The scrollbar is scoped to the scrollable region, not to any host: everything it needs rides on the
 * {@link ScrollRegion} (the container for the track's gutter, the viewport for its extent, the
 * offset/overflow for the thumb). The thumb's height reflects how much of the content fits - a short thumb
 * for a long list - and its position reflects the scroll offset, with the thumb at the track top when
 * scrolled to the first row and at the bottom when scrolled to the last. UI coordinates throughout (origin
 * bottom-left, y grows up), so a taller offset (scrolled further down the content) sits the thumb lower.
 */
public final class Scrollbar {

    /** The track's width, and the gap holding it off the container's right edge so it clears a border. */
    public static final float DEFAULT_TRACK_WIDTH = 3f;
    public static final float DEFAULT_RIGHT_MARGIN = 3f;

    // A floor on the thumb height so a very long list still leaves a grabbable thumb rather than a
    // sliver; a track shorter than this collapses the thumb to the whole track.
    static final float MIN_THUMB_HEIGHT = 12f;

    private Scrollbar() {
    }

    /**
     * The scrollbar track: a thin bar in the right-hand gutter of the region's container, spanning the
     * region's viewport. It pins to the container's right edge (not the viewport's), because the list
     * column the viewport covers may be narrower than the container, so the track sits in the container's
     * gutter clear of the content. Pairs with {@link #computeThumb}, which sizes the thumb within it.
     *
     * @param region the scrollable region
     * @return the track rectangle, in UI coordinates
     */
    public static Rectangle computeTrack(ScrollRegion region) {
        var container = region.container();
        var viewport = region.viewport();
        var trackX = container.x() + container.width() - DEFAULT_TRACK_WIDTH - DEFAULT_RIGHT_MARGIN;
        return new Rectangle(trackX, viewport.y(), DEFAULT_TRACK_WIDTH, viewport.height());
    }

    /**
     * The thumb rectangle within {@code track}: as tall as the track scaled by the fraction of the
     * content that fits (floored to a grabbable minimum), and positioned by the region's scroll offset -
     * flush with the track top at offset 0 and flush with the bottom at the full overflow. A content that
     * fits its viewport (no overflow) fills the track, since there is nothing to scroll.
     *
     * @param region the scrollable region
     * @param track  the track from {@link #computeTrack}
     * @return the thumb rectangle within the track
     */
    public static Rectangle computeThumb(ScrollRegion region, Rectangle track) {

        var thumbHeight = resolveThumbHeight(
            track,
            region.computeContentHeight(),
            region.viewport().height());

        var travel = track.height() - thumbHeight;
        var overflow = region.overflow();

        // The thumb hangs from the track top at offset 0 and drops through the travel as the content
        // scrolls, so its fraction of the travel matches the offset's fraction of the overflow.
        var fraction = overflow <= 0f ? 0f : Ranges.clampToUnit(region.offset() / overflow);
        var thumbY = track.y() + travel * (1f - fraction);

        return new Rectangle(
            track.x(),
            thumbY,
            track.width(),
            thumbHeight);
    }

    /**
     * The gutter column a drag grabs the scrollbar by: the strip right of the content, from the viewport's
     * right edge to the container's, at the viewport's height. It is wider than the thin track so a drag
     * need not hit the track exactly, and it sits right of the content so a press here grabs the scrollbar
     * rather than acting on the content (which lies to its left).
     *
     * @param region the scrollable region
     * @return the grab column, in UI coordinates
     */
    public static Rectangle computeGrabColumn(ScrollRegion region) {

        var viewport = region.viewport();
        var container = region.container();
        var listRight = viewport.x() + viewport.width();
        var containerRight = container.x() + container.width();

        return new Rectangle(
            listRight,
            viewport.y(),
            containerRight - listRight,
            viewport.height());
    }

    /**
     * The scroll offset a pointer at {@code pointerY} maps to, treating the pointer as the thumb's centre:
     * the track top yields 0 and the track bottom the full overflow, clamped between. Used by a click or
     * drag on the track to move the content to where the pointer sits. A content that fits (no overflow)
     * always resolves to 0, since there is nowhere to scroll.
     *
     * @param region   the scrollable region
     * @param track    the track from {@link #computeTrack}
     * @param pointerY the pointer's y, in UI coordinates
     * @return the scroll offset, 0..overflow
     */
    public static float resolveOffsetForPointer(ScrollRegion region, Rectangle track, float pointerY) {

        var overflow = region.overflow();
        var thumbHeight = resolveThumbHeight(
            track,
            region.computeContentHeight(),
            region.viewport().height());

        var travel = track.height() - thumbHeight;
        if (overflow <= 0f || travel <= 0f) {
            return 0f;
        }
        // The thumb centre travels between half a thumb below the top and half a thumb above the bottom,
        // so map the pointer across that centre range: at the top the content is scrolled to 0, at the
        // bottom to the full overflow.
        var centreTop = track.y() + track.height() - thumbHeight / 2f;
        var fraction = Ranges.clampToUnit((centreTop - pointerY) / travel);

        return fraction * overflow;
    }

    // The thumb's height: the track scaled by the fraction of the content that is visible, floored to a
    // grabbable minimum and capped at the track height (a content that fits fills the track).
    private static float resolveThumbHeight(
            Rectangle track,
            float contentHeight,
            float viewportHeight) {

        if (contentHeight <= 0f || viewportHeight >= contentHeight) {
            return track.height();
        }
        var proportional = track.height() * viewportHeight / contentHeight;
        return Math.min(track.height(), Math.max(MIN_THUMB_HEIGHT, proportional));
    }
}
