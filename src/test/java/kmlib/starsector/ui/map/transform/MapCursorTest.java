package kmlib.starsector.ui.map.transform;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import kmlib.testfixtures.starsector.ui.map.transform.ModelviewMatrixReaderFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.mockito.MockedStatic;

import java.nio.IntBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins the fold this seam exists for: three ways of having no trustworthy answer, all reported as
 * one. Each is asserted on its own because they fail independently in the wild - a cursor that has
 * left the window still reports its last position, a degraded matrix binding reads back something
 * that is not the map's, and a snapshot at an unusable zoom will not invert - and a caller that
 * acted on any of them would be pointing at a place the cursor is not.
 *
 * <p>The resolving case is asserted through to a world point rather than merely non-null, since
 * "the cursor reached the transform" is the only part of the threading this seam is responsible
 * for; the inversion maths behind it is {@link CampaignMapTransformTest}'s.
 */
class MapCursorTest {

    // A screen the two axes disagree on, so an axis swap in the threading cannot pass by
    // coincidence. The viewport matches it 1:1 - the pixel-scaled case belongs to the transform.
    private static final float SCREEN_WIDTH = 800f;
    private static final float SCREEN_HEIGHT = 600f;
    private static final int[] VIEWPORT = {0, 0, (int) SCREEN_WIDTH, (int) SCREEN_HEIGHT};

    // A pan and a zoom baked into the pass, both non-trivial: at zero pan or a zoom of 1 the
    // arithmetic that undoes them is invisible, so a seam that dropped either would pass.
    private static final float PAN_X = 10f;
    private static final float PAN_Y = 20f;
    private static final float MAP_ZOOM = 2f;

    // The zoom a snapshot cannot be used at: it is what the world point would be divided by, so
    // the inversion reports no point rather than one at infinity.
    private static final float UNUSABLE_ZOOM = 0f;

    private static final int CURSOR_X = 410;
    private static final int CURSOR_Y = 320;

    // Where the cursor above lands once the pan comes off and the zoom divides out:
    // ((410 - 10) / 2, (320 - 20) / 2).
    private static final float EXPECTED_WORLD_X = 200f;
    private static final float EXPECTED_WORLD_Y = 150f;

    private static final float TOLERANCE = 1e-4f;

    @Nested
    class ResolveWorldPointDuringMapPass {

        private MockedStatic<Global> globalMock;
        private MockedStatic<GL11> glMock;
        private MockedStatic<Mouse> mouseMock;

        @BeforeEach
        void setUp() {
            var settingsMock = mock(SettingsAPI.class);
            when(settingsMock.getScreenWidth())
                .thenReturn(SCREEN_WIDTH);
            when(settingsMock.getScreenHeight())
                .thenReturn(SCREEN_HEIGHT);

            globalMock = mockStatic(Global.class);
            globalMock
                .when(Global::getSettings)
                .thenReturn(settingsMock);

            // glGetInteger reports through the buffer it is handed and leaves its position alone,
            // so the stub writes absolutely, the way the capture reads it back.
            glMock = mockStatic(GL11.class);
            glMock
                .when(() -> GL11.glGetInteger(eq(GL11.GL_VIEWPORT), any(IntBuffer.class)))
                .thenAnswer(invocation -> {
                    IntBuffer buffer = invocation.getArgument(1);
                    for (var slot = 0; slot < VIEWPORT.length; slot++) {
                        buffer.put(slot, VIEWPORT[slot]);
                    }
                    return null;
                });

            mouseMock = mockStatic(Mouse.class);
            mouseMock
                .when(Mouse::isInsideWindow)
                .thenReturn(true);
            mouseMock
                .when(Mouse::getX)
                .thenReturn(CURSOR_X);
            mouseMock
                .when(Mouse::getY)
                .thenReturn(CURSOR_Y);
        }

        @AfterEach
        void tearDown() {
            mouseMock.close();
            glMock.close();
            globalMock.close();
        }

        @Test
        void resolvesTheWorldPointTheCursorSitsOver() {
            var worldPoint = MapCursor.resolveWorldPointDuringMapPass(
                MAP_ZOOM,
                readerOnALiveMap());

            assertThat(worldPoint.x).isCloseTo(EXPECTED_WORLD_X, within(TOLERANCE));
            assertThat(worldPoint.y).isCloseTo(EXPECTED_WORLD_Y, within(TOLERANCE));
        }

        @Test
        void reportsNoPointWhenTheCursorHasLeftTheWindow() {
            // The cursor goes on reporting the last position it held inside the window, so without
            // this check a caller would keep resolving a point it is no longer over.
            mouseMock
                .when(Mouse::isInsideWindow)
                .thenReturn(false);

            assertThat(MapCursor.resolveWorldPointDuringMapPass(MAP_ZOOM, readerOnALiveMap()))
                .isNull();
        }

        @Test
        void reportsNoPointWhenTheTransformCannotBeCaptured() {
            // A reader that serves no matrix is the reading a degraded binding gives: there is no
            // snapshot to invert, so there is no point to report.
            mouseMock
                .when(Mouse::isInsideWindow)
                .thenReturn(false);

            assertThat(MapCursor.resolveWorldPointDuringMapPass(
                    MAP_ZOOM,
                    new ModelviewMatrixReaderFake(null)))
                .isNull();
        }

        @Test
        void reportsNoPointWhenTheSnapshotWillNotInvert() {
            // A zoom of zero is what the world point would be divided by, so the snapshot captures
            // but resolves nothing - and a cursor sitting over a real place still gets no answer.
            mouseMock
                .when(Mouse::isInsideWindow)
                .thenReturn(false);

            assertThat(MapCursor.resolveWorldPointDuringMapPass(
                    UNUSABLE_ZOOM,
                    readerOnALiveMap()))
                .isNull();
        }
    }

    // A reader serving back exactly what the map's pass would have left bound, so a test that is
    // not about a degraded binding gets one that captures.
    private static ModelviewMatrixReaderFake readerOnALiveMap() {
        // Column-major, the layout gluUnProject expects, carrying the pass's pan in the last
        // column. Identity is refused by the capture as a reading that cannot be the map's.
        var matrix = new float[] {
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f,
        };
        matrix[12] = PAN_X;
        matrix[13] = PAN_Y;
        return new ModelviewMatrixReaderFake(matrix);
    }
}
