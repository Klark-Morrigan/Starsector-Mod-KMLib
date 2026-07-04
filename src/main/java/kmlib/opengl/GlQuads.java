package kmlib.opengl;

import org.lwjgl.opengl.GL11;

/**
 * Fills a single quad from its four already-scaled corners.
 *
 * <p>The filled-primitive counterpart to {@link GlLines}: colour and blend state are
 * the caller's current GL state, this only emits the vertices. Split from a stroked
 * outline (see {@link GlLines#strokeLoop}) because a quad fill is a distinct GL
 * primitive ({@code GL_QUADS}, exactly four vertices) from a loop outline, which any
 * vertex count can trace.
 */
public final class GlQuads {
    private GlQuads() {
    }

    /**
     * Fills the quad whose corners are {@code vertices}, a flat
     * {@code [x1, y1, x2, y2, x3, y3, x4, y4]} run in winding order.
     *
     * @param vertices the four corners, already scaled to draw coordinates
     */
    public static void fillQuad(float[] vertices) {
        GL11.glBegin(GL11.GL_QUADS);
        for (var i = 0; i < vertices.length; i += GlVertexRuns.FLOATS_PER_VERTEX) {
            GL11.glVertex2f(vertices[i], vertices[i + 1]);
        }
        GL11.glEnd();
    }
}
