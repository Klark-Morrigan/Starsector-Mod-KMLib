package kmlib.starsector.ui.map.transform;

import kmlib.opengl.GlMatrix;

/**
 * Source port for the modelview matrix a render pass is drawing under. Whether that matrix can be
 * read back is a property of the renderer, not of the drawing code: stock GL always answers, while
 * Fast Rendering answers only from one release on and refuses the read mid-render before it - see
 * {@code docs/dev/rendering-environment.md}. Depending on the port keeps the transform maths free of
 * that, and free of a GL static nothing can stand in for.
 *
 * <p>{@link GlModelviewMatrixReader} is the binding for the stock renderer and
 * {@link FastRenderingModelviewMatrixReader} the one for Fast Rendering, which guards the same read.
 * {@link UnavailableModelviewMatrixReader} reports nothing, so a caller with no matrix to read is
 * handed a reader rather than none. {@link ModelviewMatrixReaders#selectForActiveRenderer} picks
 * between the first two.
 */
public interface ModelviewMatrixReader {

    /** The float count of a 4x4 matrix, the shape every reading of this port reports. */
    int MATRIX_FLOAT_COUNT = GlMatrix.FLOAT_COUNT;

    /**
     * @return the current modelview as {@value #MATRIX_FLOAT_COUNT} floats in column-major order,
     *         the layout GL states matrices in, or {@code null} when this binding cannot report
     *         one at all
     */
    float[] readModelviewMatrix();
}
