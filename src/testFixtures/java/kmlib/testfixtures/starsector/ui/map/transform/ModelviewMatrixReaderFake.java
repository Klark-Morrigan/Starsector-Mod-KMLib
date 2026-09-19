package kmlib.testfixtures.starsector.ui.map.transform;

import kmlib.starsector.ui.map.transform.ModelviewMatrixReader;

/**
 * A {@link ModelviewMatrixReader} that reports a matrix handed to it, so transform maths can be
 * exercised against a known modelview without a GL context. Published as a fixture variant so both KMLib's and
 * consuming mods' tests drive the matrix seam through one shared double.
 *
 * <p>A {@code null} matrix stands for the reading a binding cannot serve, which is a state the
 * port models and callers are expected to survive.
 */
public final class ModelviewMatrixReaderFake implements ModelviewMatrixReader {
    private final float[] modelviewMatrix;

    /**
     * @param modelviewMatrix the matrix to report, column-major, or {@code null} to report that
     *                        none can be read
     */
    public ModelviewMatrixReaderFake(float[] modelviewMatrix) {
        this.modelviewMatrix = modelviewMatrix == null ? null : modelviewMatrix.clone();
    }

    /**
     * Reports a fresh copy each reading, matching a live binding: a caller that mutates what it
     * was handed must not be able to change what the next reading says.
     */
    @Override
    public float[] readModelviewMatrix() {
        return modelviewMatrix == null ? null : modelviewMatrix.clone();
    }
}
