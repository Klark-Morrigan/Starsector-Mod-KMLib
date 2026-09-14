package kmlib.opengl;

import org.lwjgl.opengl.GL11;

/**
 * Emits a flat {@code [x, y, x, y, ...]} vertex run under any GL primitive mode - the
 * general immediate-mode counterpart to the primitive-specific {@link GlLines} and
 * {@link GlQuads}. A run is strided at {@link GlVertexRuns#FLOATS_PER_VERTEX} per vertex,
 * the packing {@link GlVertexRuns} owns, so producer and emitter agree on the layout by
 * construction. Colour and line width are the caller's current GL state; this only emits
 * the vertices. Like {@link GlColour} this touches the GL context, so it runs only
 * in-engine.
 */
public final class GlRuns {
    private GlRuns() {
    }

    /**
     * Emits {@code run} under {@code mode} at the run's own coordinates.
     *
     * @param mode the GL primitive mode (GL_TRIANGLES, GL_LINES, GL_LINE_LOOP, ...)
     * @param run  the vertices to emit, packed [x, y, x, y, ...]
     */
    public static void draw(int mode, float[] run) {
        drawScaled(mode, run, 1f);
    }

    /**
     * Emits {@code run} under {@code mode}, scaling every coordinate by {@code factor}. For a
     * layer whose GL matrix already carries pan and centering but not the zoom (a map or
     * minimap surface), so only the uniform scale is applied here.
     *
     * @param mode   the GL primitive mode (GL_TRIANGLES, GL_LINES, GL_LINE_LOOP, ...)
     * @param run    the vertices to emit, packed [x, y, x, y, ...]
     * @param factor the uniform scale applied to every coordinate
     */
    public static void drawScaled(int mode, float[] run, float factor) {
        GL11.glBegin(mode);
        for (var v = 0; v < run.length; v += GlVertexRuns.FLOATS_PER_VERTEX) {
            GL11.glVertex2f(run[v] * factor, run[v + 1] * factor);
        }
        GL11.glEnd();
    }
}
