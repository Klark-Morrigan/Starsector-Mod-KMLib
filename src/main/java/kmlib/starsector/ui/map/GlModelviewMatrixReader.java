package kmlib.starsector.ui.map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Reports the modelview GL itself holds, by reading {@code GL_MODELVIEW_MATRIX} back. The binding
 * of {@link ModelviewMatrixReader} for the stock renderer, where GL is authoritative on the
 * matrix; it concentrates that read in one class so nothing else has to name the GL static.
 *
 * <p>A single {@link #INSTANCE}: the binding is a stateless forwarder over a static GL surface, so
 * one shared value serves every caller rather than a fresh object per construction (the same
 * enum-singleton shape KMLib uses for the live sources backing its other ports).
 */
public enum GlModelviewMatrixReader implements ModelviewMatrixReader {
    INSTANCE;

    @Override
    public float[] readModelviewMatrix() {
        // glGet* only writes into direct buffers, so the read lands in one and is then copied into
        // a plain array: the caller must own its data rather than alias a scratch buffer, and an
        // array keeps the port's contract free of any native-buffer setup.
        var matrixBuffer = BufferUtils.createFloatBuffer(MATRIX_FLOAT_COUNT);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrixBuffer);
        var modelviewMatrix = new float[MATRIX_FLOAT_COUNT];
        matrixBuffer.get(modelviewMatrix);
        return modelviewMatrix;
    }
}
