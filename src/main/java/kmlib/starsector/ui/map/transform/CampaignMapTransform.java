package kmlib.starsector.ui.map.transform;

import kmlib.opengl.GlRuns;
import kmlib.opengl.GlViewport;
import kmlib.starsector.ui.screen.VanillaScreen;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Answers "which world point is the cursor over?" for an overlay painting on the sector (M) map.
 * A map overlay draws world coordinates but the mouse arrives as a screen pixel, and nothing in
 * the game's API inverts the map's pan/centre/zoom, so an overlay that wants to hit-test what it
 * drew has to undo that transform itself. This is the one place that inversion lives.
 *
 * <p>The transform is a value snapshot, not a live reader, because its inputs only exist for an
 * instant: the map widget sets up its matrices around its render pass and tears them down after,
 * so the modelview describes the map only from inside {@code renderOnMap}.
 * {@link #captureFromMapPass} is called there to freeze it, and the resulting value stays usable
 * for the rest of the frame.
 *
 * <p>Two coordinate steps stack, and both must be undone. The map widget bakes pan and centring
 * into the GL matrices, which {@link GLU#gluUnProject} inverts. The uniform zoom is not in those
 * matrices: it reaches the vertices per-coordinate at draw time via {@code factor} (see
 * {@link GlRuns#drawScaled}), so unprojecting alone lands in scaled space and the result must be
 * divided by {@code factor} to reach the world coordinates the overlay's geometry is expressed
 * in.
 *
 * <p>{@link #unprojectToWorld} is deliberately pure - it reads only this snapshot's own arrays, and
 * {@code gluUnProject} is plain matrix arithmetic rather than a driver call - so the coordinate
 * maths, the part that is actually easy to get wrong, is verifiable without a GL context.
 *
 * @param modelviewMatrix  the map's {@code GL_MODELVIEW_MATRIX}, 16 floats, column-major
 * @param projectionMatrix the campaign UI's ortho projection, 16 floats, column-major
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

    // The matrix that transforms nothing, held to recognise a modelview that describes no pass.
    private static final float[] IDENTITY_MATRIX = {
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f,
    };

    // The depth range the campaign UI's ortho is set up with. Named for honesty about what the
    // synthesized matrix models rather than because the value matters: an axis-aligned ortho has
    // no shear, so a map overlay's unprojected x/y are independent of it.
    private static final float UI_ORTHO_NEAR_PLANE = -6000f;
    private static final float UI_ORTHO_FAR_PLANE = 6000f;

    // The slots a column-major 4x4 keeps a scale and a translation in. Named because the layout
    // is the whole subtlety of writing one by hand: the translation is the last column, which in
    // column-major order lands at the end of the array rather than every fourth float.
    private static final int SCALE_X_SLOT = 0;
    private static final int SCALE_Y_SLOT = 5;
    private static final int SCALE_Z_SLOT = 10;
    private static final int TRANSLATE_X_SLOT = 12;
    private static final int TRANSLATE_Y_SLOT = 13;
    private static final int TRANSLATE_Z_SLOT = 14;
    private static final int HOMOGENEOUS_W_SLOT = 15;

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
            throw new IllegalArgumentException("Modelview matrix must be "
                + MATRIX_FLOAT_COUNT
                + " floats, got "
                + modelviewMatrix.length);
        }
        if (projectionMatrix.length != MATRIX_FLOAT_COUNT) {
            throw new IllegalArgumentException("Projection matrix must be "
                + MATRIX_FLOAT_COUNT
                + " floats, got "
                + projectionMatrix.length);
        }
        if (viewport.length != VIEWPORT_INT_COUNT) {
            throw new IllegalArgumentException("Viewport must be "
                + VIEWPORT_INT_COUNT
                + " ints, got "
                + viewport.length);
        }
    }

    /**
     * Freezes the map's modelview and the live viewport into a snapshot, pairing them with the
     * campaign UI's projection.
     *
     * <p>Must be called from inside the map's render pass: that is the window in which the
     * modelview describes the map, so it is the precondition the caller honours rather than
     * something this can check. Called anywhere else it captures whatever unrelated transform
     * happens to be in force.
     *
     * @param factor                the scale the same render pass applies per vertex
     * @param modelviewMatrixReader the source of the modelview in force
     * @return the snapshot to unproject against for the rest of the frame, or {@code null} when
     *         the modelview read back cannot be the map's, so a caller parks rather than resolving
     *         a wrong point from a degraded read
     */
    public static CampaignMapTransform captureFromMapPass(
            float factor, ModelviewMatrixReader modelviewMatrixReader) {

        var modelviewMatrix = modelviewMatrixReader.readModelviewMatrix();

        // Identity is the tell that a reading did not come from the map: a real map pass composes
        // the UI's own translate with the map widget's, so identity means the source was not
        // carrying the map's transform when asked (see docs/dev/rendering-environment.md). The
        // rule is the same under either renderer, so it belongs here rather than in a binding.
        if (modelviewMatrix == null || Arrays.equals(modelviewMatrix, IDENTITY_MATRIX)) {
            return null;
        }

        // Whatever rectangle the pass in force left bound, which is what gluUnProject maps the
        // cursor pixel through - so a pass that narrowed it and did not restore it is measured
        // against here, not the screen.
        var viewport = GlViewport.readViewport();
        return new CampaignMapTransform(
            modelviewMatrix,
            // The UI axes rather than the pixel ones: the campaign's ortho projection spans the UI
            // units the layout runs in, which is what the modelview above was composed against.
            buildUiOrthoProjectionMatrix(
                VanillaScreen.resolveUiWidth(),
                VanillaScreen.resolveUiHeight()),
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

    /**
     * Words this snapshot for a diagnostic line: the two inputs a wrong cursor mapping comes from,
     * and the scale that undoes the pass's own zoom.
     *
     * <p>The viewport is the one worth reading first. It is not this mod's to set - it is whatever
     * the pass in force left bound - so a rectangle that is not the screen means the cursor pixel
     * is being mapped through somebody else's frame, and every point resolved from it is wrong by
     * however far that rectangle sits from the one the player is pointing at.
     *
     * <p>The modelview is reported as its translation and scale rather than all sixteen floats: a
     * map transform is a scale and an offset, and the twelve floats that are always the same say
     * nothing a reader of a log would use.
     *
     * @return the viewport, the modelview's placement, and the per-vertex scale
     */
    public String describeSnapshot() {
        return "viewport=" + GlViewport.describeViewport(viewport)
            + " factor=" + factor
            + " modelviewTranslate=(" + modelviewMatrix[TRANSLATE_X_SLOT]
            + "," + modelviewMatrix[TRANSLATE_Y_SLOT] + ")"
            + " modelviewScale=(" + modelviewMatrix[SCALE_X_SLOT]
            + "," + modelviewMatrix[SCALE_Y_SLOT] + ")";
    }

    /**
     * Reconstructs the campaign UI's projection arithmetically rather than reading it back from
     * GL. The UI enters 2D mode with {@code glOrtho(0, screenWidth, 0, screenHeight, near, far)}
     * before rendering the panel tree the map hangs off, so the matrix is fully known from the
     * screen size alone and the read would only ask GL to repeat what the caller can already
     * derive - see {@code docs/dev/rendering-environment.md} for the setup and its citations.
     *
     * @param screenWidth  the UI's virtual width, {@link VanillaScreen#resolveUiWidth}. Note this
     *                     is UI units, not the physical pixels the viewport is measured in; the two
     *                     differ whenever the display applies a pixel scale, and reconciling them
     *                     is the viewport's job inside {@code gluUnProject}
     * @param screenHeight the UI's virtual height, {@link VanillaScreen#resolveUiHeight}
     * @return the ortho as 16 floats, column-major, the layout {@code gluUnProject} expects
     */
    static float[] buildUiOrthoProjectionMatrix(float screenWidth, float screenHeight) {

        var depthSpan = UI_ORTHO_NEAR_PLANE - UI_ORTHO_FAR_PLANE;
        var matrix = new float[MATRIX_FLOAT_COUNT];
        matrix[SCALE_X_SLOT] = 2f / screenWidth;
        matrix[SCALE_Y_SLOT] = 2f / screenHeight;
        matrix[SCALE_Z_SLOT] = 2f / depthSpan;

        // The x and y ortho spans run 0..size rather than being centred, so each axis shifts a
        // full half-span to move its origin onto the viewport's bottom-left corner. The depth
        // range is symmetric about zero and so needs no shift.
        matrix[TRANSLATE_X_SLOT] = -1f;
        matrix[TRANSLATE_Y_SLOT] = -1f;
        matrix[TRANSLATE_Z_SLOT] = (UI_ORTHO_NEAR_PLANE + UI_ORTHO_FAR_PLANE) / depthSpan;
        matrix[HOMOGENEOUS_W_SLOT] = 1f;

        return matrix;
    }
}
