package kmlib.opengl;

/**
 * The shape GL states a matrix in: a 4x4, laid out as sixteen floats in column-major order.
 *
 * <p>Stated once because more than one reader and writer hands such a matrix around - a modelview
 * read back from GL or copied off a renderer that tracks its own, a projection built by hand - and
 * each needs the count to size a buffer or reject a wrong-sized array. A count each spelled for
 * itself could drift apart without any of them being wrong on its own.
 */
public final class GlMatrix {

    /** The float count of a 4x4 matrix, whichever matrix and whichever way it was obtained. */
    public static final int FLOAT_COUNT = 16;

    private GlMatrix() {
    }
}
