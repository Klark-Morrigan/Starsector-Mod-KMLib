package kmlib.starsector.ui.map.transform;

import kmlib.opengl.GlScissor;

import org.lwjgl.util.vector.Vector2f;

/**
 * One resolved reading of where the cursor is on the map: the pixel it was taken from, the transform
 * it was mapped through, and the world point that came out.
 *
 * <p>A value rather than three separate reads because a wrong hover is a disagreement between them.
 * The cell a hover names is not wrong at the hit-test end - that cell really does hold the point it
 * was given - so what is wrong is upstream, in the pixel or in the transform that pixel was mapped
 * through, and neither is diagnosable without the point they produced. Carrying them together is
 * also what makes a description of a hover a description of <em>that</em> hover: a second read taken
 * to describe the first can capture a different transform, and would then account for a hover that
 * never happened.
 *
 * <p>Holding the reading apart from wording it is what keeps that free. The parts are captured on
 * every frame that hovers, and the string is built only by a caller that means to print one.
 *
 * @param cursorPixelX the cursor x the reading was taken at, in window pixels from the left edge
 * @param cursorPixelY the cursor y the reading was taken at, in window pixels from the bottom edge
 * @param transform    the snapshot that pixel was mapped through
 * @param worldPoint   the world point under the cursor, never null - a reading that could not
 *                     produce one is not a reading, and {@link MapCursor} reports none instead
 */
public record MapCursorRead(
    int cursorPixelX,
    int cursorPixelY,
    CampaignMapTransform transform,
    Vector2f worldPoint) {

    /**
     * Words this reading for a diagnostic line: the pixel it started from, the snapshot it was
     * mapped through, the clip the pass was drawing under, and the point it landed on.
     *
     * <p>Must be called from inside the same render pass the reading was taken in, because the clip
     * is read live rather than carried in the value. It is left out of the value deliberately: the
     * clip is wanted only on the frames that print, while the reading itself is taken on every
     * frame that hovers.
     *
     * <p>The clip is reported beside the cursor because together they say whether the pass owns the
     * pixel being asked about at all - a pass that may not paint there cannot be the one that knows
     * what is under it.
     *
     * @return a one-line description of this reading
     */
    public String describeRead() {

        var scissorBox = GlScissor.readScissorBox();
        
        return "cursorPixel=(" + cursorPixelX + "," + cursorPixelY + ")"
            + " " + transform.describeSnapshot()
            + " scissor=[" + GlScissor.describeScissorBox(scissorBox) + "]"
            // An unclipped pass owns every pixel, which is what a null clip reads as here rather
            // than through a rule of this end's own.
            + " scissorHoldsCursor="
            + (scissorBox == null || scissorBox.containsPoint(cursorPixelX, cursorPixelY))
            + " worldPoint=(" + worldPoint.x + "," + worldPoint.y + ")";
    }
}
