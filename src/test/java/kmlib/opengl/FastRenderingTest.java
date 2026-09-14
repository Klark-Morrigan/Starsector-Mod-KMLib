package kmlib.opengl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Matrix4f;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two Fast Rendering facts that are checkable without it: that a stock classpath reports
 * the renderer as absent, and that its row-major matrices are converted to the column-major layout
 * GL states matrices in.
 *
 * <p>The conversion is the half worth pinning hardest. It is a silent failure by nature - a matrix
 * reported the wrong way round still has sixteen plausible floats in it, so nothing throws and a
 * map overlay simply resolves the wrong point - and it cannot be caught in-engine except by
 * noticing that a hover is subtly wrong.
 */
class FastRenderingTest {

    // A translation, which is the whole shape of the campaign map's own modelview and the case the
    // two layouts disagree about most legibly: transposed, these land at indices 3/7/11 instead of
    // 12/13/14. Three different values so an axis swap cannot coincidentally pass.
    private static final float TRANSLATE_X = 7f;
    private static final float TRANSLATE_Y = 11f;
    private static final float TRANSLATE_Z = 13f;

    private static final int TRANSLATE_X_SLOT = 12;

    // Builds a translation the way Fast Rendering does, reading Matrix4f's fields as m<row><col>:
    // its MatrixStack.glTranslatef writes the translation into m03/m13/m23, transposed from the
    // m<col><row> LWJGL itself uses. Written out rather than built with Matrix4f.translate, which
    // would apply LWJGL's own convention and so assume away the very difference under test.
    private static Matrix4f buildRowMajorTranslationMatrix() {
        var matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.m03 = TRANSLATE_X;
        matrix.m13 = TRANSLATE_Y;
        matrix.m23 = TRANSLATE_Z;
        return matrix;
    }

    @Nested
    class IsFastRenderingActive {

        @Test
        void reportsFalseWhenGlIsStockLwjgl() {
            // Nothing rewrote this test's class references, so GL11 is the real one. This pins the
            // stock half of the detection - the patched half needs a patched game to observe.
            assertThat(FastRendering.isFastRenderingActive()).isFalse();
        }
    }

    @Nested
    class IsBridgeClassName {

        @Test
        void reportsTrueWhenTheNameIsTheRelocatedBridge() {
            // The layout from v0.7.4 onwards. Fast Rendering relocated its bridge within its own
            // package without saying so, and a check keyed to one release's full class name
            // reported "stock" afterwards - which sent callers into GL reads it cannot serve.
            assertThat(FastRendering.isBridgeClassName("com.genir.renderer.bridge.commands.GL11"))
                .isTrue();
        }

        @Test
        void reportsTrueWhenTheNameIsTheEarlierBridgeLayout() {
            // The layout up to v0.7.3, still in the field on installs that have not updated.
            assertThat(FastRendering.isBridgeClassName("com.genir.renderer.bridge.GL11")).isTrue();
        }

        @Test
        void reportsFalseWhenTheNameIsStockLwjgl() {
            assertThat(FastRendering.isBridgeClassName("org.lwjgl.opengl.GL11")).isFalse();
        }
    }

    @Nested
    class CopyAsColumnMajorFloats {

        @Test
        void putsTheTranslationLastWhenTheMatrixIsRowMajor() {
            var columnMajorFloats =
                FastRendering.copyAsColumnMajorFloats(buildRowMajorTranslationMatrix());

            assertThat(columnMajorFloats).containsExactly(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                TRANSLATE_X, TRANSLATE_Y, TRANSLATE_Z, 1f);
        }

        @Test
        void reportsASnapshotWhenTheSourceChangesAfterwards() {
            // Fast Rendering hands out its live mutable matrix, so a result that aliased it would
            // change under a caller mid-frame as the renderer pushes and pops.
            var matrix = buildRowMajorTranslationMatrix();

            var columnMajorFloats = FastRendering.copyAsColumnMajorFloats(matrix);
            matrix.m03 = TRANSLATE_X + 1f;

            assertThat(columnMajorFloats[TRANSLATE_X_SLOT]).isEqualTo(TRANSLATE_X);
        }
    }
}
