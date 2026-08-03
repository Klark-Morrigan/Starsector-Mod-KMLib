package kmlib.starsector.ui.map.transform;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import kmlib.testfixtures.starsector.ui.map.transform.ModelviewMatrixReaderFake;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.mockito.MockedStatic;

import java.nio.IntBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins the pixel-to-world inversion against hand-built transforms: an identity transform maps the
 * viewport onto normalised space, pan and zoom baked into the matrices are undone, the per-vertex
 * factor is divided back out, and a transform that cannot be inverted parks rather than returning
 * a meaningless point. Pins the synthesized campaign-UI projection too, both as floats and by
 * what it unprojects to, and the capture's rule that a modelview which cannot have come from the
 * map's pass yields no snapshot at all.
 *
 * <p>These build the matrices directly instead of capturing them from GL, which is what makes the
 * coordinate maths testable at all - {@code gluUnProject} is plain arithmetic, so only the
 * matrix bindings themselves need a live context.
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

    // A screen the two axes disagree on, so a transposed or axis-swapped matrix cannot
    // coincidentally pass, and whose spans divide into exact decimals, so an expected matrix can
    // be stated outright instead of by repeating the formula that produced it. The pixel scale is
    // what makes the viewport distinguishable from the screen: a display that applies one reports
    // a viewport in physical pixels while the UI stays in its own virtual units.
    private static final float SCREEN_WIDTH = 1600f;
    private static final float SCREEN_HEIGHT = 1000f;
    private static final int PIXEL_SCALE = 2;

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
    class CaptureFromMapPass {
        // A viewport the screen size cannot stand in for: it is the screen scaled up, so a capture
        // that built the viewport out of the screen size instead of reading GL back lands the
        // expected point somewhere else rather than passing by coincidence.
        private static final int[] PIXEL_SCALED_VIEWPORT = {
            0, 0, (int) SCREEN_WIDTH * PIXEL_SCALE, (int) SCREEN_HEIGHT * PIXEL_SCALE};
        private static final float PAN_X = 10f;
        private static final float PAN_Y = 20f;
        // A zoom other than 1, so a capture that dropped the factor on the floor cannot pass: at
        // 1 the division that undoes it is invisible.
        private static final float MAP_ZOOM = 2f;

        // Stated here rather than read off the class under test, because the number is LWJGL's
        // requirement and not this code's choice: a test that borrowed the production constant
        // would follow it wherever it moved instead of holding it to the external contract.
        private static final int GL_GET_INTEGER_MIN_BUFFER_INTS = 16;

        private MockedStatic<Global> globalMock;
        private MockedStatic<GL11> glMock;
        private int viewportBufferElementsOfferedToGl;

        @BeforeEach
        void setUp() {
            var settingsMock = mock(SettingsAPI.class);
            when(settingsMock.getScreenWidth()).thenReturn(SCREEN_WIDTH);
            when(settingsMock.getScreenHeight()).thenReturn(SCREEN_HEIGHT);
            globalMock = mockStatic(Global.class);
            globalMock.when(Global::getSettings).thenReturn(settingsMock);
            // glGetInteger reports through the buffer it is handed and leaves its position alone,
            // so the stub writes absolutely, the way the capture reads it back.
            glMock = mockStatic(GL11.class);
            glMock.when(() -> GL11.glGetInteger(eq(GL11.GL_VIEWPORT), any(IntBuffer.class)))
                .thenAnswer(invocation -> {
                    IntBuffer buffer = invocation.getArgument(1);
                    // Recorded at call time: the caller reads its four ints back out of the
                    // buffer afterwards, which moves the position LWJGL's check reads from.
                    viewportBufferElementsOfferedToGl = buffer.remaining();
                    for (var slot = 0; slot < PIXEL_SCALED_VIEWPORT.length; slot++) {
                        buffer.put(slot, PIXEL_SCALED_VIEWPORT[slot]);
                    }
                    return null;
                });
        }

        @AfterEach
        void tearDown() {
            glMock.close();
            globalMock.close();
        }

        @Test
        void composesAReadModelviewIntoASnapshotThatUnprojects() {
            var modelviewMatrixReaderFake =
                new ModelviewMatrixReaderFake(buildTranslationMatrix(PAN_X, PAN_Y));

            var transform = CampaignMapTransform.captureFromMapPass(
                MAP_ZOOM, modelviewMatrixReaderFake);

            // Every input the capture is responsible for threading shows up in this one expected
            // point, so none of them can be dropped unnoticed: the viewport's centre pixel
            // unprojects to the screen centre only if the GL viewport reached the snapshot, the
            // pan comes back off only if the port's matrix did, and the zoom divides out only if
            // the factor did.
            var world = transform.unprojectToWorld(
                SCREEN_WIDTH * PIXEL_SCALE / 2f, SCREEN_HEIGHT * PIXEL_SCALE / 2f);
            assertThat(world.x).isCloseTo((SCREEN_WIDTH / 2f - PAN_X) / MAP_ZOOM, TOLERANCE);
            assertThat(world.y).isCloseTo((SCREEN_HEIGHT / 2f - PAN_Y) / MAP_ZOOM, TOLERANCE);
        }

        @Test
        void asksGlForAViewportInABufferItsSizeCheckAccepts() {
            var modelviewMatrixReaderFake =
                new ModelviewMatrixReaderFake(buildTranslationMatrix(PAN_X, PAN_Y));

            CampaignMapTransform.captureFromMapPass(MAP_ZOOM, modelviewMatrixReaderFake);

            // Stock LWJGL rejects a glGetInteger buffer holding fewer than 16 elements whatever
            // the pname would fill, so the size is asserted directly: a viewport-sized buffer
            // throws in-engine while sailing through a mocked GL11 unnoticed.
            assertThat(viewportBufferElementsOfferedToGl)
                .isGreaterThanOrEqualTo(GL_GET_INTEGER_MIN_BUFFER_INTS);
        }

        @Test
        void reportsNoSnapshotWhenTheModelviewCannotBeRead() {
            var modelviewMatrixReaderFake = new ModelviewMatrixReaderFake(null);

            assertThat(CampaignMapTransform.captureFromMapPass(MAP_ZOOM, modelviewMatrixReaderFake))
                .isNull();
        }

        @Test
        void reportsNoSnapshotWhenTheModelviewIsIdentity() {
            // Identity transforms nothing, so it cannot be the map's pass; the snapshot is refused
            // rather than resolving pixels against a transform nothing was drawn with.
            var modelviewMatrixReaderFake = new ModelviewMatrixReaderFake(IDENTITY_MATRIX);

            assertThat(CampaignMapTransform.captureFromMapPass(MAP_ZOOM, modelviewMatrixReaderFake))
                .isNull();
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
