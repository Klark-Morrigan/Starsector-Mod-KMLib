package kmlib.opengl;

import kmlib.math.geometry.Points;

import org.lwjgl.opengl.GL11;

/**
 * Draws line primitives the fixed-function pipeline's own features cannot be
 * relied on for.
 *
 * <p>Dashed lines are the case in point: {@code glLineStipple} is legacy
 * fixed-function GL that some Starsector renderer bridges (Fast Rendering's
 * {@code GL11} among them) do not implement and fatal on, so a
 * dash is emitted as its own short {@code GL_LINES} segment instead - immediate-
 * mode vertices are always available. {@link #strokeLoop} is the plain closed-outline
 * counterpart, any vertex count, for a debug overlay tracing a polygon's edge. Like
 * {@link GlColor} this touches the GL context, so it is exercised in-engine rather
 * than in unit tests.
 */
public final class GlLines {
    private GlLines() {
    }

    /**
     * Draws each world-space segment in {@code segments} (a flat
     * {@code [x1, y1, x2, y2, ...]} run) as a dashed line, scaling every
     * coordinate by {@code worldToScreen}.
     *
     * <p>The dash on- and gap-lengths are given in post-scale (screen) units and
     * converted to world units by dividing by {@code worldToScreen}, so the dash
     * size stays constant on screen as {@code worldToScreen} (a zoom) changes -
     * the behaviour {@code glLineStipple} would have given for free. Colour and
     * line width are the caller's current GL state; this only emits the vertices.
     * A segment shorter than one dash period yields a single clipped dash.
     *
     * @param segments      world-space segments as a flat [x1, y1, x2, y2, ...] run
     * @param dashOnScreen  drawn dash length, in post-scale (screen) units
     * @param dashGapScreen gap between dashes, in post-scale (screen) units
     * @param worldToScreen scale applied to every coordinate (world -> screen); a
     *                      non-positive value draws nothing
     */
    public static void drawDashedSegments(
            float[] segments,
            float dashOnScreen,
            float dashGapScreen,
            float worldToScreen) {

        if (worldToScreen <= 0) {
            return;
        }
        var onWorldLength = dashOnScreen / worldToScreen;
        var periodWorldLength = (dashOnScreen + dashGapScreen) / worldToScreen;

        GL11.glBegin(GL11.GL_LINES);

        for (var i = 0; i < segments.length; i += GlVertexRuns.FLOATS_PER_SEGMENT) {
            emitDashes(segments, i, onWorldLength, periodWorldLength, worldToScreen);
        }
        GL11.glEnd();
    }

    /**
     * Strokes the closed outline of {@code vertices}, a flat {@code [x1, y1, x2, y2,
     * ...]} run in winding order - any vertex count, unlike {@link GlQuads#fillQuad}
     * which fills exactly four.
     *
     * @param vertices the polygon's corners, already scaled to draw coordinates
     */
    public static void strokeLoop(float[] vertices) {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (var i = 0; i < vertices.length; i += GlVertexRuns.FLOATS_PER_VERTEX) {
            GL11.glVertex2f(vertices[i], vertices[i + 1]);
        }
        GL11.glEnd();
    }

    // Emits the on-dash sub-segments of the segment at {@code index}, stepping
    // along it in period-length strides and drawing the first onWorldLength of
    // each stride.
    private static void emitDashes(
            float[] segments,
            int index,
            float onWorldLength,
            float periodWorldLength,
            float worldToScreen) {

        var x1 = segments[index];
        var y1 = segments[index + 1];
        var endIndex = index + 2;
        var deltaX = segments[endIndex] - x1;
        var deltaY = segments[endIndex + 1] - y1;
        var length = (float) Points.computeVectorLength(deltaX, deltaY);
        if (length <= 0) {
            return;
        }
        var unitX = deltaX / length;
        var unitY = deltaY / length;
        for (var along = 0f; along < length; along += periodWorldLength) {
            var onEnd = Math.min(along + onWorldLength, length);
            GL11.glVertex2f(
                (x1 + unitX * along) * worldToScreen,
                (y1 + unitY * along) * worldToScreen);
            GL11.glVertex2f(
                (x1 + unitX * onEnd) * worldToScreen,
                (y1 + unitY * onEnd) * worldToScreen);
        }
    }
}
