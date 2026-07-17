package kmlib.starsector.ui.map;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the pixel-to-world inversion against hand-built transforms: an identity transform maps the
 * viewport onto normalised space, pan and zoom baked into the matrices are undone, the per-vertex
 * factor is divided back out, and a transform that cannot be inverted parks rather than returning
 * a meaningless point. Pins the synthesized campaign-UI projection too, both as floats and by
 * what it unprojects to.
 *
 * <p>These build the matrices directly instead of capturing them from GL, which is what makes the
 * coordinate maths testable at all - {@code gluUnProject} is plain arithmetic, so only
 * {@code captureFromGl} needs a live context.
 */
class CampaignMapTransformTest {

    // Column-major GL matrices, the layout glGetFloat reports and gluUnProject expects.
    private static final float[] IDENTITY_MATRIX = {
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f,
    };

    // A 100x100 viewport at the window origin: a pixel maps linearly onto -1..1, so an expected
    // world point can be read off by eye (pixel 50 is the centre, pixel 100 the far edge).
    private static final int[] VIEWPORT = {0, 0, 100, 100};

    private static final float NO_SCALE = 1f;
    private static final Offset<Float> TOLERANCE = within(1e-4f);
    // Tighter than TOLERANCE because a matrix slot is asserted against an exact expected float
    // rather than against the far end of a round trip through gluUnProject.
    private static final float MATRIX_TOLERANCE = 1e-6f;

    private static float[] buildTranslationMatrix(float translateX, float translateY) {
        var matrix = IDENTITY_MATRIX.clone();
        matrix[12] = translateX;
        matrix[13] = translateY;
        return matrix;
    }

    private static float[] buildScaleMatrix(float scale) {
        var matrix = IDENTITY_MATRIX.clone();
        matrix[0] = scale;
        matrix[5] = scale;
        return matrix;
    }

    @Nested
    class BuildUiOrthoProjectionMatrix {
        // A screen the two axes disagree on, so a transposed or axis-swapped matrix cannot
        // coincidentally pass, and whose spans divide into exact decimals, so the expected
        // matrix below can be stated outright instead of by repeating the formula that
        // produced it.
        private static final float SCREEN_WIDTH = 1600f;
        private static final float SCREEN_HEIGHT = 1000f;
        private static final int PIXEL_SCALE = 2;

        private static float[] buildProjection() {
            return CampaignMapTransform.buildUiOrthoProjectionMatrix(SCREEN_WIDTH, SCREEN_HEIGHT);
        }

        @Test
        void producesTheOrthoTheCampaignUiSetsUp() {
            var projection = buildProjection();

            // glOrtho's published result for (0, 1600, 0, 1000, -6000, 6000): the diagonal
            // scales each span onto -1..1, the last column shifts the 0..size origin onto the
            // corner, and the depth range is symmetric so its shift stays zero.
            assertThat(projection).usingComparatorWithPrecision(MATRIX_TOLERANCE).containsExactly(
                    0.00125f, 0f, 0f, 0f,
                    0f, 0.002f, 0f, 0f,
                    0f, 0f, -2f / 12000f, 0f,
                    -1f, -1f, 0f, 1f);
        }

        @Test
        void unprojectsTheViewportCentreToTheScreenCentre() {
            var viewport = new int[] {0, 0, (int) SCREEN_WIDTH, (int) SCREEN_HEIGHT};
            var transform = new CampaignMapTransform(
                    IDENTITY_MATRIX, buildProjection(), viewport, NO_SCALE);

            var world = transform.unprojectToWorld(SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f);

            assertThat(world.x).isCloseTo(SCREEN_WIDTH / 2f, TOLERANCE);
            assertThat(world.y).isCloseTo(SCREEN_HEIGHT / 2f, TOLERANCE);
        }

        @Test
        void unprojectsIntoUiUnitsWhenTheViewportIsPixelScaled() {
            // A display with a pixel scale reports a viewport larger than the UI's virtual
            // size. The ortho stays in UI units, so the far-corner pixel must still land on
            // the UI's far corner rather than on the scaled-up one.
            var pixelScaledViewport = new int[] {
                0, 0, (int) SCREEN_WIDTH * PIXEL_SCALE, (int) SCREEN_HEIGHT * PIXEL_SCALE};
            var transform = new CampaignMapTransform(
                    IDENTITY_MATRIX, buildProjection(), pixelScaledViewport, NO_SCALE);

            var world = transform.unprojectToWorld(
                    SCREEN_WIDTH * PIXEL_SCALE, SCREEN_HEIGHT * PIXEL_SCALE);

            assertThat(world.x).isCloseTo(SCREEN_WIDTH, TOLERANCE);
            assertThat(world.y).isCloseTo(SCREEN_HEIGHT, TOLERANCE);
        }
    }

    @Nested
    class Construction {
        @Test
        void rejectsAWrongSizedModelviewMatrix() {
            assertThatIllegalArgumentException().isThrownBy(() -> new CampaignMapTransform(
                    new float[] {1f}, IDENTITY_MATRIX, VIEWPORT, NO_SCALE));
        }

        @Test
        void rejectsAWrongSizedProjectionMatrix() {
            assertThatIllegalArgumentException().isThrownBy(() -> new CampaignMapTransform(
                    IDENTITY_MATRIX, new float[] {1f}, VIEWPORT, NO_SCALE));
        }

        @Test
        void rejectsAWrongSizedViewport() {
            assertThatIllegalArgumentException().isThrownBy(() -> new CampaignMapTransform(
                    IDENTITY_MATRIX, IDENTITY_MATRIX, new int[] {0, 0}, NO_SCALE));
        }
    }

    @Nested
    class UnprojectToWorld {
        @Test
        void mapsTheViewportCentreToTheOriginUnderAnIdentityTransform() {
            var transform = new CampaignMapTransform(
                    IDENTITY_MATRIX, IDENTITY_MATRIX, VIEWPORT, NO_SCALE);

            var world = transform.unprojectToWorld(50f, 50f);

            assertThat(world.x).isCloseTo(0f, TOLERANCE);
            assertThat(world.y).isCloseTo(0f, TOLERANCE);
        }

        @Test
        void mapsTheViewportCornerToTheFarEdgeOfNormalisedSpace() {
            var transform = new CampaignMapTransform(
                    IDENTITY_MATRIX, IDENTITY_MATRIX, VIEWPORT, NO_SCALE);

            var world = transform.unprojectToWorld(100f, 100f);

            assertThat(world.x).isCloseTo(1f, TOLERANCE);
            assertThat(world.y).isCloseTo(1f, TOLERANCE);
        }

        @Test
        void undoesAPanBakedIntoTheModelviewMatrix() {
            // The map widget's centring shows up here: with the view shifted by (10, 20), the
            // world point under the centre pixel is the one that lands back on the origin.
            var transform = new CampaignMapTransform(
                    buildTranslationMatrix(10f, 20f), IDENTITY_MATRIX, VIEWPORT, NO_SCALE);

            var world = transform.unprojectToWorld(50f, 50f);

            assertThat(world.x).isCloseTo(-10f, TOLERANCE);
            assertThat(world.y).isCloseTo(-20f, TOLERANCE);
        }

        @Test
        void undoesAZoomBakedIntoTheProjectionMatrix() {
            // A projection that halves coordinates means the far edge of normalised space is
            // twice as far out in the space being unprojected into.
            var transform = new CampaignMapTransform(
                    IDENTITY_MATRIX, buildScaleMatrix(0.5f), VIEWPORT, NO_SCALE);

            var world = transform.unprojectToWorld(100f, 100f);

            assertThat(world.x).isCloseTo(2f, TOLERANCE);
            assertThat(world.y).isCloseTo(2f, TOLERANCE);
        }

        @Test
        void dividesOutThePerVertexFactor() {
            // The same pixel as the corner case above, but the render pass scaled every vertex
            // by 2 on the way in, so the world coordinate behind it is half as far out.
            var transform = new CampaignMapTransform(
                    IDENTITY_MATRIX, IDENTITY_MATRIX, VIEWPORT, 2f);

            var world = transform.unprojectToWorld(100f, 100f);

            assertThat(world.x).isCloseTo(0.5f, TOLERANCE);
            assertThat(world.y).isCloseTo(0.5f, TOLERANCE);
        }

        @Test
        void returnsNoPointWhenTheFactorIsZero() {
            var transform = new CampaignMapTransform(
                    IDENTITY_MATRIX, IDENTITY_MATRIX, VIEWPORT, 0f);

            assertThat(transform.unprojectToWorld(50f, 50f)).isNull();
        }

        @Test
        void returnsNoPointWhenTheTransformCannotBeInverted() {
            // An all-zero projection collapses every world point onto one, so no pixel has a
            // world point behind it.
            var transform = new CampaignMapTransform(
                    IDENTITY_MATRIX, new float[IDENTITY_MATRIX.length], VIEWPORT, NO_SCALE);

            assertThat(transform.unprojectToWorld(50f, 50f)).isNull();
        }
    }
}
