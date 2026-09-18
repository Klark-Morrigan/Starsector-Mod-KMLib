package kmlib.starsector.ui.map.transform;

import kmlib.opengl.FastRendering;
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

    // What the clip reads as when it was not read at all. The two fields stay in the line whichever
    // renderer is underneath, so a description keeps one shape and a reader is told the clip is
    // missing rather than being left to notice that it is.
    private static final String UNREAD_CLIP = "unread under Fast Rendering";
    private static final String UNKNOWN_CONTAINMENT = "unknown";

    /**
     * Words this reading for a diagnostic line: the pixel it started from, the snapshot it was
     * mapped through, the clip in force where the line is being built, and the point it landed on.
     *
     * <p>Must be called from inside a render pass, because the clip is read live rather than carried
     * in the value. It is left out of the value deliberately: the clip is wanted only on the frames
     * that print, while the reading itself is taken on every frame that hovers.
     *
     * <p>That makes the clip the one field describing the <em>printing</em> pass rather than the
     * reading, and the fields say so, because the two are routinely different passes: a frame can be
     * painted by several, and a caller printing on a frame's arrival prints from wherever it steps
     * its own once-a-frame work rather than from the pass whose reading won. So the clip answers
     * "could the pass building this line have painted the cursor's pixel", which is worth having -
     * it is how a foreign map's box is spotted at all - and does not answer whether the pass that
     * resolved the reading owned that pixel. Nothing here can answer the second question: a clip
     * belongs to a pass, and the pass that read has ended by the time anything reports on it.
     *
     * <p>Under Fast Rendering the clip is left unread and the line says so, which is why the
     * renderer is the one thing a description branches on. That renderer's bridge shadows the scissor
     * <em>enable</em> flag on the caller's side but not the box, so asking for the box stalls the
     * render pipeline - and it counts stalls, taking the game down once a caller stalls on half of
     * any sixty frames. A hover prints on consecutive frames, so the read that is merely slow under
     * stock LWJGL is fatal there, and a diagnostic must not be able to end a session it was turned
     * on to explain. KMLib's {@code docs/dev/rendering-environment.md} records which reads the
     * bridge answers inline and which stall.
     *
     * @return a one-line description of this reading
     */
    public String describeRead() {
        return describeReadUnderRenderer(FastRendering.isFastRenderingActive());
    }

    // Split from the live renderer read so a description can be worded against a stated renderer.
    // Which stack is underneath is fixed for the life of a process and cannot be moved either way,
    // so a caller reading it directly would leave the branch that keeps the read off Fast Rendering
    // reachable only on a machine already running it.
    String describeReadUnderRenderer(boolean isFastRenderingActive) {
        return "cursorPixel=(" + cursorPixelX + "," + cursorPixelY + ")"
            + " " + transform.describeSnapshot()
            + " " + describeClipUnderRenderer(isFastRenderingActive)
            + " worldPoint=(" + worldPoint.x + "," + worldPoint.y + ")";
    }

    private String describeClipUnderRenderer(boolean isFastRenderingActive) {

        if (isFastRenderingActive) {
            return describeClipFields(UNREAD_CLIP, UNKNOWN_CONTAINMENT);
        }

        var scissorBox = GlScissor.readScissorBox();

        return describeClipFields(
            GlScissor.describeScissorBox(scissorBox),
            // An unclipped pass owns every pixel, which is what a null clip reads as here rather
            // than through a rule of this end's own.
            String.valueOf(
                scissorBox == null || scissorBox.containsPoint(cursorPixelX, cursorPixelY)));
    }

    // The two clip fields worded in one place, so a reading that could not be taken and one that
    // was keep the same shape in a log - which is the whole of what makes the unread case legible
    // beside an ordinary one rather than a line with something missing from it.
    private static String describeClipFields(String clip, String containment) {
        return "printingPassScissor=[" + clip + "] printingPassHoldsCursor=" + containment;
    }
}
