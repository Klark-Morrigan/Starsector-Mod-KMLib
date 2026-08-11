package kmlib.starsector.ui.render.gl.controls;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.render.gl.tabs.VanillaTabStripRenderer;
import kmlib.starsector.ui.widgets.segments.HorizontalSegments;

import java.awt.Color;
import java.util.List;

/**
 * Raw-GL paint for the chrome every horizontal segmented control shares - the selected-segment wash and
 * the dividers ruled on the seams between segments. It is the paint-side companion to the {@link
 * HorizontalSegments} geometry widget: that lays the segments and computes the seams, this fills the
 * highlight and rules the dividers over them, so a radio row and a tab strip draw their common chrome
 * one way and never drift on the highlight strength or the seam positions.
 *
 * <p>Only the shared kernel lives here. What differs by control kind stays with each renderer - a radio
 * frames the whole row ({@link RadioRowRenderer}), a tab strip lays a per-state fill, a hover wash, a
 * baseline, and multi-colour text ({@link VanillaTabStripRenderer}) - because the sizing is shared but
 * the look is not. GL passthrough (over {@link UiFill}), exercised in-engine like the other
 * draw helpers; the caller wraps it in the GL-state save its panel already holds.
 */
public final class HorizontalSegmentsRenderer {
    // A lit segment reads as a wash over its backdrop rather than a second opaque block; the seam
    // dividers are fainter still so they separate the segments without competing. Shared so a radio row
    // and a tab strip cannot drift on either strength. The seam rule is one pixel wide, matching the
    // thickness HorizontalSegments.computeDividers is asked for.
    public static final float SELECTED_WASH_ALPHA_MULT = 0.30f;
    public static final float DIVIDER_ALPHA_MULT = 0.40f;
    public static final float DIVIDER_THICKNESS = 1f;

    private HorizontalSegmentsRenderer() {
    }

    /**
     * Washes one segment as a highlight: a fill of {@code colour} faded to the shared selected-wash alpha
     * over {@code opacity}. The lit cell of a radio row or the selected tab of a strip, drawn as a wash
     * so the segment's backdrop still reads through it.
     *
     * @param segment the segment to wash, in UI coordinates
     * @param colour  the wash colour (the control's accent)
     * @param opacity overall alpha, 0..1
     */
    public static void renderSelectedWash(Rectangle segment, Color colour, float opacity) {

        var selectedPaint = new UiElementPaint(colour, opacity * SELECTED_WASH_ALPHA_MULT);
        UiFill.renderQuad(segment, selectedPaint);
    }

    /**
     * Rules a divider on each interior seam of the laid {@code segments} - a one-pixel line at every
     * non-first segment's left edge, spanning that segment's height, faded to the shared divider alpha.
     * Reads the seams straight off {@link HorizontalSegments#computeDividers}, so the ruling lands on the
     * real boundaries whether the segments are even (uniform) or ragged (snapped). A row of one segment
     * or none has no interior seam and draws nothing.
     *
     * @param segments the laid-out segment rects, in row order left to right
     * @param colour   the divider colour (the control's accent)
     * @param opacity  overall alpha, 0..1
     */
    public static void renderSeamDividers(List<Rectangle> segments, Color colour, float opacity) {
        for (var divider : HorizontalSegments.computeDividers(segments, DIVIDER_THICKNESS)) {

            var dividerPaint = new UiElementPaint(colour, opacity * DIVIDER_ALPHA_MULT);
            UiFill.renderQuad(divider, dividerPaint);
        }
    }

    /**
     * Fills each channel of a parted row - the empty strip between two neighbouring segments - with {@code
     * colour}. The counterpart to {@link #renderSeamDividers} for the other kind of row: an abutting row is
     * marked where its segments meet, and a parted one has the surface its segments stand on showing
     * between them instead, which a control floating over a screen of its own has to paint or the channel
     * shows that screen through.
     *
     * <p>Filled at the caller's opacity rather than at a fainter one: this is a surface and not a rule, so
     * it fades with the row it belongs to and no further. A row whose segments abut has no channel and
     * draws nothing, so either kind of row may call it.
     *
     * @param segments the laid-out segment rects, in row order left to right
     * @param colour   the surface the row stands over
     * @param opacity  overall alpha, 0..1
     */
    public static void renderChannelFills(List<Rectangle> segments, Color colour, float opacity) {
        for (var channel : HorizontalSegments.computeChannels(segments)) {
            UiFill.renderQuad(channel, new UiElementPaint(colour, opacity));
        }
    }
}
