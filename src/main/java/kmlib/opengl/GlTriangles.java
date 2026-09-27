package kmlib.opengl;

import org.lwjgl.opengl.GL11;

/**
 * Fills a single triangle from its three already-scaled corners.
 *
 * <p>The three-vertex sibling of {@link GlQuads}: colour and blend state are the
 * caller's current GL state, this only emits the vertices. Split from a quad fill
 * because a triangle is a distinct GL primitive ({@code GL_TRIANGLES}, exactly three
 * vertices).
 */
public final class GlTriangles {
    private GlTriangles() {
    }

    /**
     * Fills the triangle whose corners are {@code vertices}, a flat
     * {@code [x1, y1, x2, y2, x3, y3]} run in winding order.
     *
     * @param vertices the three corners, already scaled to draw coordinates
     */
    public static void fillTriangle(float[] vertices) {
        GlRuns.draw(GL11.GL_TRIANGLES, vertices);
    }
}
