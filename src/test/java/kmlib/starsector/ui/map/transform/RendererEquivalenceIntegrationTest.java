package kmlib.starsector.ui.map.transform;

import kmlib.opengl.FastRendering;
import kmlib.testfixtures.starsector.ui.map.transform.ModelviewMatrixReaderFake;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Matrix4f;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the claim the two {@link ModelviewMatrixReader} bindings exist to make true: that the map
 * resolves a cursor to the same world point whichever renderer is underneath. The bindings read
 * different sources in different layouts, so "they agree" is a real assertion rather than a
 * restatement of one of them - and it is the claim a player would find broken, since a hover that
 * is right on a stock install and subtly wrong under Fast Rendering is the exact defect this whole
 * seam guards against.
 *
 * <p>Both readings are driven through the same {@link CampaignMapTransform}, so the two paths are
 * compared where it counts - at the world point - rather than at the matrix they happen to share
 * on the way. This catches a transposed matrix, which no single-sided test can: an expected array
 * written by hand encodes the same belief the production code encodes, so both are wrong together
 * and agree.
 *
 * <p>What this cannot do is confirm that real GL and the real bridge produce the inputs stated
 * here; those are read out of the game and {@code fr.jar} in
 * {@code docs/dev/rendering-environment.md} and confirmed in-engine. Given those inputs, this pins
 * that everything downstream of them converges.
 */
class RendererEquivalenceIntegrationTest {

    // The map's modelview, as docs/dev/rendering-environment.md records it: the campaign UI's base
    // translate composed with the map widget's own centring translate. Deliberately not round
    // numbers and not equal on the two axes, so a transpose or an axis swap cannot pass by
    // landing on a value that happens to match.
    private static final float PAN_X = 0.01f + 137f;
    private static final float PAN_Y = 0.01f + 41f;

    private static final int[] VIEWPORT = {0, 0, 1600, 1000};
    private static final float[] PROJECTION =
        CampaignMapTransform.buildUiOrthoProjectionMatrix(1600f, 1000f);

    // A zoom, so the comparison runs through the factor divide rather than past it.
    private static final float MAP_ZOOM = 0.75f;

    private static final float CURSOR_PIXEL_X = 612f;
    private static final float CURSOR_PIXEL_Y = 383f;

    private static final Offset<Float> TOLERANCE = within(1e-4f);

    // What glGetFloat(GL_MODELVIEW_MATRIX) reports for that pass on a stock install: column-major,
    // so the translation is the last column and lands at the end of the array.
    private static float[] buildMatrixAsStockGlReportsIt() {
        var matrix = new float[] {
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f,
        };
        matrix[12] = PAN_X;
        matrix[13] = PAN_Y;
        return matrix;
    }

    // What Fast Rendering's TransformManager holds for the same pass: a Matrix4f whose fields it
    // reads as m<row><col>, so its MatrixStack.glTranslatef puts the translation in m03/m13 - the
    // transpose of the layout above, and the reason the two bindings cannot share a copy step.
    private static Matrix4f buildMatrixAsFastRenderingHoldsIt() {
        var matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.m03 = PAN_X;
        matrix.m13 = PAN_Y;
        return matrix;
    }

    // Drives a reading all the way to a world point, the way a map overlay does, so the two
    // renderers are compared on the answer rather than on an intermediate.
    private static org.lwjgl.util.vector.Vector2f resolveWorldPointFrom(float[] modelviewMatrix) {
        var transform = new CampaignMapTransform(modelviewMatrix, PROJECTION, VIEWPORT, MAP_ZOOM);
        return transform.unprojectToWorld(CURSOR_PIXEL_X, CURSOR_PIXEL_Y);
    }

    @Nested
    class UnprojectToWorld {

        @Test
        void mapTransform_ResolvesTheSameWorldPoint_UnderEitherRenderer() {
            var stockGlPoint = resolveWorldPointFrom(buildMatrixAsStockGlReportsIt());

            var fastRenderingPoint = resolveWorldPointFrom(
                FastRendering.copyAsColumnMajorFloats(buildMatrixAsFastRenderingHoldsIt()));

            assertThat(fastRenderingPoint.x).isCloseTo(stockGlPoint.x, TOLERANCE);
            assertThat(fastRenderingPoint.y).isCloseTo(stockGlPoint.y, TOLERANCE);
        }

        @Test
        void mapTransform_ResolvesAWorldPointThatMovesWithThePan_UnderEitherRenderer() {
            // Guards the way the check above could pass on a lie: if both paths were broken into
            // reporting a pan-independent point, they would agree with each other perfectly. So
            // the agreed point also has to be the one the pan actually implies.
            var stockGlPoint = resolveWorldPointFrom(buildMatrixAsStockGlReportsIt());

            // gluUnProject inverts the pass, so the pan subtracts, and the factor divides out the
            // zoom the same way it does for a live overlay's geometry.
            assertThat(stockGlPoint.x)
                .isCloseTo((CURSOR_PIXEL_X - PAN_X) / MAP_ZOOM, TOLERANCE);
            assertThat(stockGlPoint.y)
                .isCloseTo((CURSOR_PIXEL_Y - PAN_Y) / MAP_ZOOM, TOLERANCE);
        }
    }

    @Nested
    class ReadModelviewMatrix {

        @Test
        void modelviewMatrixReader_ReportsTheSameMatrix_UnderEitherRenderer() {
            // The bindings themselves need a GL context and a live bridge, so each is stood in for
            // by a reader over the reading it would return. What is under test is the layouts
            // converging, which is the part that is theirs rather than the renderers'.
            ModelviewMatrixReader stockGlReaderFake =
                new ModelviewMatrixReaderFake(buildMatrixAsStockGlReportsIt());
            ModelviewMatrixReader fastRenderingReaderFake = new ModelviewMatrixReaderFake(
                FastRendering.copyAsColumnMajorFloats(buildMatrixAsFastRenderingHoldsIt()));

            assertThat(fastRenderingReaderFake.readModelviewMatrix())
                .containsExactly(stockGlReaderFake.readModelviewMatrix());
        }
    }
}
