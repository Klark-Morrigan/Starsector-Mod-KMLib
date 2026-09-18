package kmlib.opengl;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import java.nio.FloatBuffer;

/**
 * What KM code has to know about the Fast Rendering mod: whether it is in force, and how to read
 * the matrix it draws with. It replaces the game's GL calls with a batching renderer, which changes
 * what some GL calls mean, so code that reads GL state back has to account for it.
 *
 * <p>Both facts live here so exactly one file has to be checked against
 * {@code docs/dev/rendering-environment.md}, which records them and their citations. Neither names
 * a Fast Rendering type: this class is about the renderer, not bound to it, so it stays loadable
 * and verifiable on a stock install.
 */
public final class FastRendering {

    // The package every Fast Rendering bridge class sits under, whatever the release calls the rest
    // of the name. Matching the prefix rather than a whole class name is deliberate: the bridge has
    // moved within this package twice, neither time announced (v0.7.4 moved GL11 from
    // com.genir.renderer.bridge to com.genir.renderer.bridge.commands, and v0.8.9 left the
    // implementations there but pointed the rewrite at a com.genir.renderer.bridge.opengl facade).
    // A full-name comparison answers "stock" for any release whose layout it does not know. That is
    // the one wrong answer with teeth - it routes callers into GL reads the bridge cannot serve,
    // which fail mid-render.
    private static final String BRIDGE_PACKAGE_PREFIX = "com.genir.renderer.";

    private FastRendering() {
    }

    /**
     * Reports whether this jar is running under Fast Rendering.
     *
     * <p>Asks the only question that actually matters - were this jar's GL references redirected? -
     * rather than inferring it from an install layout or a mod list. Fast Rendering rewrites
     * constant-pool class entries in every jar it loads, this one included, so the class literal
     * below reports the bridge under it and plain LWJGL otherwise. That needs no reflection and no
     * {@code Class.forName}, which matters because the game's script classloader denies mod code
     * {@code java.lang.reflect} outright.
     *
     * @return {@code true} when GL calls from this jar reach Fast Rendering's bridge
     */
    public static boolean isFastRenderingActive() {
        return isBridgeClassName(GL11.class.getName());
    }

    /**
     * Copies one of Fast Rendering's matrices into the column-major layout GL states matrices in.
     *
     * <p>Two things make this more than a copy. Fast Rendering reads a {@link Matrix4f}'s fields as
     * {@code m<row><col>} - its vertex transform takes the translation from {@code m03/m13/m23} -
     * which is transposed from the {@code m<col><row>} LWJGL itself uses, so {@code store} would
     * report the matrix the wrong way round and put a map's pan in the slots a projection's
     * perspective terms belong in. Transposing is what its own GPU path does when it hands the same
     * matrix to GL, so this is that conversion rather than a correction. And the matrix handed out
     * is the live mutable one, so a caller that held it would watch it change mid-frame; copying is
     * what makes the result a value.
     *
     * @param matrix a matrix in Fast Rendering's row-major field layout
     * @return {@value GlMatrix#FLOAT_COUNT} floats, column-major, owned by the caller
     */
    public static float[] copyAsColumnMajorFloats(Matrix4f matrix) {
        // Matrix4f only writes into a FloatBuffer, so the transpose lands in one and is then copied
        // into a plain array the caller owns outright.
        FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(GlMatrix.FLOAT_COUNT);
        matrix.storeTranspose(matrixBuffer);
        matrixBuffer.flip();
        var columnMajorFloats = new float[GlMatrix.FLOAT_COUNT];
        matrixBuffer.get(columnMajorFloats);
        return columnMajorFloats;
    }

    // Split from the class-literal read so the rule can be stated against names from releases this
    // machine does not have installed, which is the only way the tolerance for a relocated bridge
    // is checkable at all - the live read reports whichever renderer happens to be underneath.
    static boolean isBridgeClassName(String glClassName) {
        return glClassName.startsWith(BRIDGE_PACKAGE_PREFIX);
    }
}
