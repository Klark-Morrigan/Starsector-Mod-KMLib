package kmlib.starsector.ui.map;

/**
 * Source port for the modelview matrix a render pass is drawing under. Where that matrix lives is
 * a property of the renderer, not of the drawing code: the stock renderer keeps it in GL, while a
 * renderer that batches on the CPU keeps its own and leaves GL untouched, so "read it back from
 * GL" is one binding of this role rather than the definition of it - see
 * {@code docs/dev/rendering-environment.md}. Depending on the port keeps the transform maths free
 * of that choice, and free of an un-mockable third-party static.
 *
 * <p>{@link GlModelviewMatrixReader} is the binding for the stock renderer and
 * {@link FastRenderingModelviewMatrixReader} the one for Fast Rendering.
 * {@link ModelviewMatrixReaders#selectForActiveRenderer} picks between them.
 */
public interface ModelviewMatrixReader {

    /** The float count of a 4x4 matrix, the shape every reading of this port reports. */
    int MATRIX_FLOAT_COUNT = 16;

    /**
     * @return the current modelview as {@value #MATRIX_FLOAT_COUNT} floats in column-major order,
     *         the layout GL states matrices in, or {@code null} when this binding cannot report
     *         one at all
     */
    float[] readModelviewMatrix();
}
