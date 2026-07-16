package kmlib.starsector.ui.map;

import kmlib.opengl.GlRuns;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Answers "which world point is the cursor over?" for an overlay painting on the sector (M) map.
 * A map overlay draws world coordinates but the mouse arrives as a screen pixel, and nothing in
 * the game's API inverts the map's pan/centre/zoom, so an overlay that wants to hit-test what it
 * drew has to undo that transform itself. This is the one place that inversion lives.
 *
 * <p>The transform is a value snapshot, not a live reader, because its inputs only exist for an
 * instant: the map widget sets up the GL matrices around its render pass and tears them down
 * after, so they are readable only from inside {@code renderOnMap}. {@link #captureFromGl} is
 * called there to freeze them, and the resulting value stays usable for the rest of the frame.
 *
 * <p>Two coordinate steps stack, and both must be undone. The map widget bakes pan and centring
 * into the GL matrices, which {@link GLU#gluUnProject} inverts. The uniform zoom is not in those
 * matrices: it reaches the vertices per-coordinate at draw time via {@code factor} (see
 * {@link GlRuns#drawScaled}), so unprojecting alone lands in scaled space and the result must be
 * divided by {@code factor} to reach the world coordinates the overlay's geometry is expressed
 * in.
 *
 * <p>{@link #captureFromGl} touches the GL context and is exercised in-engine.
 * {@link #unprojectToWorld} is deliberately pure - it reads only this snapshot's own arrays, and
 * {@code gluUnProject} is plain matrix arithmetic rather than a driver call - so the coordinate
 * maths, the part that is actually easy to get wrong, is verifiable without a GL context.
 *
 * @param modelviewMatrix  the map's {@code GL_MODELVIEW_MATRIX}, 16 floats, column-major
 * @param projectionMatrix the map's {@code GL_PROJECTION_MATRIX}, 16 floats, column-major
 * @param viewport         the {@code GL_VIEWPORT} as {@code {x, y, width, height}} in pixels
 * @param factor           the per-vertex scale the map render pass applies to world coordinates
 */
public record CampaignMapTransform(
        float[] modelviewMatrix,
        float[] projectionMatrix,
        int[] viewport,
        float factor) {

    private static final int MATRIX_FLOAT_COUNT = 16;
    private static final int VIEWPORT_INT_COUNT = 4;
    // gluUnProject unprojects a 3D window point, so it needs a depth as well as a pixel. The map
    // is a flat layer viewed head-on, so every depth along the ray through the cursor gives the
    // same x/y; the near plane is chosen simply because it is a well-defined end of that ray.
    private static final float NEAR_PLANE_DEPTH = 0f;
    // gluUnProject writes x, y and z, so it is handed a 3-float target even though a flat map
    // overlay only reads x and y back.
    private static final int WORLD_POINT_FLOAT_COUNT = 3;

    public CampaignMapTransform {
        // A wrong-sized matrix or viewport would otherwise be read out of bounds deep inside
        // gluUnProject, so the shape is rejected at the boundary where the size is still named.
        if (modelviewMatrix.length != MATRIX_FLOAT_COUNT) {
            throw new IllegalArgumentException(
                    "Modelview matrix must be " + MATRIX_FLOAT_COUNT + " floats, got "
                            + modelviewMatrix.length);
        }
        if (projectionMatrix.length != MATRIX_FLOAT_COUNT) {
            throw new IllegalArgumentException(
                    "Projection matrix must be " + MATRIX_FLOAT_COUNT + " floats, got "
                            + projectionMatrix.length);
        }
        if (viewport.length != VIEWPORT_INT_COUNT) {
            throw new IllegalArgumentException(
                    "Viewport must be " + VIEWPORT_INT_COUNT + " ints, got " + viewport.length);
        }
    }

    /**
     * Freezes the live GL matrices and viewport into a snapshot. Only meaningful when called
     * from inside the map's render pass, where the map widget's matrices are the current ones;
     * called anywhere else it captures whatever unrelated transform happens to be bound.
     *
     * @param factor the scale the same render pass applies per vertex
     * @return the snapshot to unproject against for the rest of the frame
     */
    public static CampaignMapTransform captureFromGl(float factor) {
        // glGet* only writes into direct buffers, so the reads land in these and are then copied
        // into plain arrays: the snapshot must own its data rather than alias scratch buffers,
        // and arrays keep unprojectToWorld free of any native-buffer setup.
        var modelviewBuffer = BufferUtils.createFloatBuffer(MATRIX_FLOAT_COUNT);
        var projectionBuffer = BufferUtils.createFloatBuffer(MATRIX_FLOAT_COUNT);
        var viewportBuffer = BufferUtils.createIntBuffer(VIEWPORT_INT_COUNT);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelviewBuffer);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionBuffer);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuffer);

        var modelviewMatrix = new float[MATRIX_FLOAT_COUNT];
        var projectionMatrix = new float[MATRIX_FLOAT_COUNT];
        var viewport = new int[VIEWPORT_INT_COUNT];
        modelviewBuffer.get(modelviewMatrix);
        projectionBuffer.get(projectionMatrix);
        viewportBuffer.get(viewport);
        return new CampaignMapTransform(
                modelviewMatrix,
                projectionMatrix,
                viewport,
                factor);
    }

    /**
     * Maps a cursor pixel to the world point under it.
     *
     * @param pixelX the cursor x in window pixels from the left edge, as
     *               {@code org.lwjgl.input.Mouse#getX} reports it and {@code GL_VIEWPORT}
     *               measures it
     * @param pixelY the cursor y in window pixels from the bottom edge, matching
     *               {@code org.lwjgl.input.Mouse#getY}
     * @return the world point under that pixel, or {@code null} when this snapshot cannot be
     *         inverted (a singular or degenerate transform), so a caller parks the hover rather
     *         than acting on a meaningless point
     */
    public Vector2f unprojectToWorld(float pixelX, float pixelY) {
        // A zero factor would divide the unprojected point to infinity. It cannot happen on a
        // live map (it is the zoom), so this reads as "the snapshot is not usable" rather than
        // as a case to be handled.
        if (factor == 0f) {
            return null;
        }
        var worldPoint = BufferUtils.createFloatBuffer(WORLD_POINT_FLOAT_COUNT);
        var isUnprojected = GLU.gluUnProject(
                pixelX,
                pixelY,
                NEAR_PLANE_DEPTH,
                FloatBuffer.wrap(modelviewMatrix),
                FloatBuffer.wrap(projectionMatrix),
                IntBuffer.wrap(viewport),
                worldPoint);
        if (!isUnprojected) {
            return null;
        }
        // Undo the per-vertex scale the render pass applied on the way out, landing back in the
        // unscaled world coordinates the overlay's geometry is stored in.
        return new Vector2f(
                worldPoint.get(0) / factor,
                worldPoint.get(1) / factor);
    }
}
