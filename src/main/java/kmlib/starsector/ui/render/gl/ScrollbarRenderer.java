package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;

import java.awt.Color;

/**
 * Raw-GL paint for a vertical scrollbar: a dim track channel with a brighter thumb over it, both faded
 * by one opacity, in the panel's accent colour. It draws the two rectangles the host lays out - the
 * track it placed in a body's right inset and the thumb {@link kmlib.starsector.ui.widgets.Scrollbar}
 * sized within it - so the geometry stays with the widget and this only fills. The GL passthrough (over
 * {@link UiFill}), exercised in-engine like the other draw helpers.
 *
 * <p>The track reads quieter than the thumb so the channel shows where the list can scroll without
 * competing with the grabbable thumb, in the same accent every body control draws with.
 */
public final class ScrollbarRenderer {
    // The track sits well below a lit control's weight so it reads as a quiet channel; the thumb draws
    // brighter so the grabbable part stands out against it.
    private static final float TRACK_ALPHA_MULT = 0.2f;
    private static final float THUMB_ALPHA_MULT = 0.6f;

    private ScrollbarRenderer() {
    }

    /**
     * Fills the track and the thumb in {@code accent}, the track dim and the thumb bright, both scaled
     * by {@code opacity}. Must run with a current GL context, like any immediate-mode GL call.
     *
     * @param track   the track's footprint, in UI coordinates
     * @param thumb   the thumb's footprint within the track, from {@link
     *                kmlib.starsector.ui.widgets.Scrollbar#computeThumb}
     * @param accent  the scrollbar colour, the panel's accent
     * @param opacity overall alpha, 0..1
     */
    public static void render(Rectangle track, Rectangle thumb, Color accent, float opacity) {
        UiFill.renderQuad(track.x(), track.y(), track.width(), track.height(), accent,
                opacity * TRACK_ALPHA_MULT);
        UiFill.renderQuad(thumb.x(), thumb.y(), thumb.width(), thumb.height(), accent,
                opacity * THUMB_ALPHA_MULT);
    }
}
