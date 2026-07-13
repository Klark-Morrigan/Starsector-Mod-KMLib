package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

/**
 * The geometry of a vertical scrollbar's thumb: given the track the host laid out and how far a list's
 * content overruns its viewport, it sizes and positions the thumb, and maps a pointer back to a scroll
 * offset for a click or drag on the track. Substrate-independent - it computes rectangles and offsets,
 * rendering nothing - so a GL or a UI-API renderer paints against it and an input listener hit-tests it.
 * The raw-GL paint lives in {@link kmlib.starsector.ui.render.gl.ScrollbarRenderer}.
 *
 * <p>The track is the host's to place (a thin bar in a body's right inset, spanning the scroll region),
 * so this owns only the thumb-within-track math, keeping it reusable across whatever frames the track.
 * The thumb's height reflects how much of the content fits - a short thumb for a long list - and its
 * position reflects the scroll offset, with the thumb at the track top when scrolled to the first row
 * and at the bottom when scrolled to the last. UI coordinates throughout (origin bottom-left, y grows
 * up), so a taller offset (scrolled further down the content) sits the thumb lower.
 */
public final class Scrollbar {
    // A floor on the thumb height so a very long list still leaves a grabbable thumb rather than a
    // sliver; a track shorter than this collapses the thumb to the whole track.
    static final float MIN_THUMB_HEIGHT = 12f;

    private Scrollbar() {
    }

    /**
     * The thumb rectangle within {@code track}: as tall as the track scaled by the fraction of the
     * content that fits (floored to a grabbable minimum), and positioned by the scroll offset - flush
     * with the track top when {@code scrollOffset} is 0 and flush with the bottom when it is the full
     * overflow. A content that fits its viewport (no overflow) fills the track, since there is nothing
     * to scroll.
     *
     * @param track          the track's footprint, in UI coordinates
     * @param contentHeight  the full height of the scrolled content
     * @param viewportHeight the visible height the content scrolls within
     * @param scrollOffset   how far the content is scrolled, 0..overflow
     * @return the thumb rectangle within the track
     */
    public static Rectangle computeThumb(Rectangle track, float contentHeight, float viewportHeight,
            float scrollOffset) {
        var thumbHeight = resolveThumbHeight(track, contentHeight, viewportHeight);
        var travel = track.height() - thumbHeight;
        var overflow = resolveOverflow(contentHeight, viewportHeight);
        // The thumb hangs from the track top at offset 0 and drops through the travel as the content
        // scrolls, so its fraction of the travel matches the offset's fraction of the overflow.
        var fraction = overflow <= 0f ? 0f : clampFraction(scrollOffset / overflow);
        var thumbY = track.y() + travel * (1f - fraction);
        return new Rectangle(track.x(), thumbY, track.width(), thumbHeight);
    }

    /**
     * The scroll offset a pointer at {@code pointerY} maps to, treating the pointer as the thumb's
     * centre: the track top yields 0 and the track bottom the full overflow, clamped between. Used by a
     * click or drag on the track to move the list to where the pointer sits. A content that fits (no
     * overflow) always resolves to 0, since there is nowhere to scroll.
     *
     * @param track          the track's footprint, in UI coordinates
     * @param contentHeight  the full height of the scrolled content
     * @param viewportHeight the visible height the content scrolls within
     * @param pointerY       the pointer's y, in UI coordinates
     * @return the scroll offset, 0..overflow
     */
    public static float resolveOffsetForPointer(Rectangle track, float contentHeight,
            float viewportHeight, float pointerY) {
        var overflow = resolveOverflow(contentHeight, viewportHeight);
        var thumbHeight = resolveThumbHeight(track, contentHeight, viewportHeight);
        var travel = track.height() - thumbHeight;
        if (overflow <= 0f || travel <= 0f) {
            return 0f;
        }
        // The thumb centre travels between half a thumb below the top and half a thumb above the bottom,
        // so map the pointer across that centre range: at the top the content is scrolled to 0, at the
        // bottom to the full overflow.
        var centreTop = track.y() + track.height() - thumbHeight / 2f;
        var fraction = clampFraction((centreTop - pointerY) / travel);
        return fraction * overflow;
    }

    // The thumb's height: the track scaled by the fraction of the content that is visible, floored to a
    // grabbable minimum and capped at the track height (a content that fits fills the track).
    private static float resolveThumbHeight(Rectangle track, float contentHeight,
            float viewportHeight) {
        if (contentHeight <= 0f || viewportHeight >= contentHeight) {
            return track.height();
        }
        var proportional = track.height() * viewportHeight / contentHeight;
        return Math.min(track.height(), Math.max(MIN_THUMB_HEIGHT, proportional));
    }

    // How far the content overruns its viewport, or 0 when it fits.
    private static float resolveOverflow(float contentHeight, float viewportHeight) {
        return Math.max(0f, contentHeight - viewportHeight);
    }

    // Confines a 0..1 fraction to that range, so a thumb position or a pointer mapping never runs past
    // the track ends.
    private static float clampFraction(float fraction) {
        return Math.max(0f, Math.min(1f, fraction));
    }
}
