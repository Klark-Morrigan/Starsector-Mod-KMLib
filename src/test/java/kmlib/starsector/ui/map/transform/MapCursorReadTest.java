package kmlib.starsector.ui.map.transform;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;

import java.nio.IntBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

/**
 * Pins the diagnostic line a hover is accounted for by. Every field in it is there because a wrong
 * hover is a disagreement between them, so the whole line is asserted at once rather than field by
 * field: a description missing one of them cannot settle which end was wrong.
 *
 * <p>The clip is the part with two readings rather than one. A pass drawing unclipped owns every
 * pixel and a pass drawing inside a box owns only that box, and whether the cursor is in it is what
 * says if the pass could be the one that knows what is under the pointer.
 */
class MapCursorReadTest {

    // A pan and a scale the two axes disagree on, so a line that swapped them could not pass.
    private static final float PAN_X = 10f;
    private static final float PAN_Y = 20f;
    private static final float MAP_ZOOM = 2f;

    private static final int[] VIEWPORT = {0, 0, 800, 600};

    private static final int CURSOR_X = 410;
    private static final int CURSOR_Y = 320;

    private static final Vector2f WORLD_POINT = new Vector2f(200f, 150f);

    // A clip holding the cursor, and one well clear of it - the two readings the containment line
    // exists to tell apart.
    private static final int[] CLIP_AROUND_THE_CURSOR = {400, 300, 100, 100};
    private static final int[] CLIP_AWAY_FROM_THE_CURSOR = {0, 0, 100, 100};

    @Nested
    class DescribeRead {

        private MockedStatic<GL11> glMock;

        @BeforeEach
        void setUp() {
            glMock = mockStatic(GL11.class);
        }

        @AfterEach
        void tearDown() {
            glMock.close();
        }

        @Test
        void describeReadWordsThePixelTheSnapshotAndThePointItLandedOn() {
            // Asserted whole because the fields are only diagnostic together: the pixel is what the
            // player pointed at, the viewport and modelview are what it was mapped through, and the
            // world point is what came out. Read apart, none of them says which was wrong.
            stubScissorTestOff();

            assertThat(buildRead().describeRead())
                .isEqualTo("cursorPixel=(410,320)"
                    + " viewport=x=0 y=0 w=800 h=600"
                    + " factor=2.0"
                    + " modelviewTranslate=(10.0,20.0)"
                    + " modelviewScale=(1.0,1.0)"
                    + " scissor=[unclipped]"
                    + " scissorHoldsCursor=true"
                    + " worldPoint=(200.0,150.0)");
        }

        @Test
        void describeReadReportsAClipThatHoldsTheCursor() {
            // The pass may paint where the pointer is, so its answer about what is under the cursor
            // can be the frame's.
            stubScissorBoxAt(CLIP_AROUND_THE_CURSOR);

            assertThat(buildRead().describeRead())
                .contains("scissor=[x=400 y=300 w=100 h=100]")
                .contains("scissorHoldsCursor=true");
        }

        @Test
        void describeReadReportsAClipThatDoesNotHoldTheCursor() {
            // The reading that names a foreign pass: it resolved a point for a pixel it is not
            // permitted to paint, so whatever it says is under the cursor is not the map's answer.
            stubScissorBoxAt(CLIP_AWAY_FROM_THE_CURSOR);

            assertThat(buildRead().describeRead())
                .contains("scissor=[x=0 y=0 w=100 h=100]")
                .contains("scissorHoldsCursor=false");
        }

        private void stubScissorTestOff() {
            glMock
                .when(() -> GL11.glIsEnabled(GL11.GL_SCISSOR_TEST))
                .thenReturn(false);
        }

        // glGetInteger reports through the buffer it is handed and leaves its position alone, so the
        // stub writes absolutely, the way the read takes it back.
        private void stubScissorBoxAt(int[] scissorBox) {
            glMock
                .when(() -> GL11.glIsEnabled(GL11.GL_SCISSOR_TEST))
                .thenReturn(true);
            glMock
                .when(() -> GL11.glGetInteger(eq(GL11.GL_SCISSOR_BOX), any(IntBuffer.class)))
                .thenAnswer(invocation -> {
                    IntBuffer buffer = invocation.getArgument(1);
                    for (var slot = 0; slot < scissorBox.length; slot++) {
                        buffer.put(slot, scissorBox[slot]);
                    }
                    return null;
                });
        }
    }

    // A reading of the shape the map's pass produces: the pass's pan in the modelview's last column,
    // the viewport it was mapped through, and the point that came out.
    private static MapCursorRead buildRead() {

        var modelviewMatrix = new float[] {
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f,
        };
        
        modelviewMatrix[12] = PAN_X;
        modelviewMatrix[13] = PAN_Y;

        var transform = new CampaignMapTransform(
            modelviewMatrix,
            new float[16],
            VIEWPORT,
            MAP_ZOOM);

        return new MapCursorRead(CURSOR_X, CURSOR_Y, transform, WORLD_POINT);
    }
}
