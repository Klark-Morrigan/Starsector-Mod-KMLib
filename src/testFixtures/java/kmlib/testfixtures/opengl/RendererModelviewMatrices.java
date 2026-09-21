package kmlib.testfixtures.opengl;

import kmlib.opengl.GlMatrix;

import org.lwjgl.util.vector.Matrix4f;

/**
 * One map pass's modelview, stated in each of the two layouts a renderer holds it in.
 *
 * <p>Here rather than in each suite because it is one belief, not one value: the same pan sits in
 * different slots depending on which renderer was asked, and a suite that wrote it out for itself
 * would be restating the very thing the code under it exists to get right. Stated in both layouts
 * side by side, so the transpose between them is legible at the fixture rather than inferred from
 * two files that happen to disagree.
 *
 * <p>Deliberately not what {@code kmlib.opengl.FastRenderingTest} uses. That suite tests the
 * transpose itself, so its input has to be written out longhand where the assertion can be read
 * against it; everything else takes a pass's matrix as a given and is about something further
 * downstream.
 *
 * <p>The values are not round and not equal on the two axes, so an axis swap or a transpose cannot
 * pass by landing on a matching number. {@code docs/dev/rendering-environment.md} records which
 * renderer holds which layout and why.
 */
public final class RendererModelviewMatrices {

    /** The pan a map pass carries on the x axis, as the campaign UI composes it with the widget's. */
    public static final float MAP_PASS_PAN_X = 137.01f;

    /** The same on the y axis, a different magnitude so the two cannot be transposed unnoticed. */
    public static final float MAP_PASS_PAN_Y = 41.01f;

    /** Where a column-major matrix keeps the x pan, which is where a caller reads it back from. */
    public static final int COLUMN_MAJOR_PAN_X_SLOT = 12;

    /** The same for the y pan. */
    public static final int COLUMN_MAJOR_PAN_Y_SLOT = 13;

    private RendererModelviewMatrices() {
    }

    /**
     * The pass as Fast Rendering holds it: a {@link Matrix4f} whose fields it reads as
     * {@code m<row><col>}, putting the pan in {@code m03} and {@code m13}.
     *
     * <p>Written into those fields rather than built with {@code Matrix4f.translate}, which would
     * apply LWJGL's own convention and so assume away the difference this exists to state.
     *
     * @return the renderer's own modelview for that pass
     */
    public static Matrix4f createMapPassAsFastRenderingHoldsIt() {

        var matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.m03 = MAP_PASS_PAN_X;
        matrix.m13 = MAP_PASS_PAN_Y;
        return matrix;
    }

    /**
     * The same pass as stock GL reports it: column-major, so the translation is the last column and
     * lands at the end of the array.
     *
     * @return the modelview a {@code glGetFloat(GL_MODELVIEW_MATRIX)} read back would answer
     */
    public static float[] createMapPassAsGlReportsIt() {

        var matrix = GlMatrix.createIdentity();
        matrix[COLUMN_MAJOR_PAN_X_SLOT] = MAP_PASS_PAN_X;
        matrix[COLUMN_MAJOR_PAN_Y_SLOT] = MAP_PASS_PAN_Y;
        return matrix;
    }
}
