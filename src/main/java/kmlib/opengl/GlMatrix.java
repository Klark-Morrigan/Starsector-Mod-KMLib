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

    // The diagonal a 4x4 identity carries, which is the whole of it: every other slot is zero, and
    // a fresh array is zero already.
    private static final int[] DIAGONAL_SLOTS = {0, 5, 10, 15};

    private GlMatrix() {
    }

    /**
     * The matrix that transforms nothing, as its own array each call so a holder cannot have one
     * move under it.
     *
     * <p>Stated once because it is read for two different reasons and written out identically for
     * both: as the tell that a matrix describes no render pass, and as the base a test composes a
     * pass's own transform onto. Written as a diagonal rather than as sixteen literals because the
     * literals are what a transposed or misread layout hides in, and identity is the one matrix
     * where row-major and column-major agree - so a copy of it says nothing about which layout it
     * was written in.
     *
     * @return {@value #FLOAT_COUNT} floats: ones down the diagonal, zero everywhere else
     */
    public static float[] createIdentity() {

        var identity = new float[FLOAT_COUNT];
        for (var diagonalSlot : DIAGONAL_SLOTS) {
            identity[diagonalSlot] = 1f;
        }
        return identity;
    }
}
