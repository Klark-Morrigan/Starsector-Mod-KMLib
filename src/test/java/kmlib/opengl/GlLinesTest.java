package kmlib.opengl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

/**
 * Pins the primitive each line draw opens and that it is closed whatever happens inside it, for the
 * reason {@link GlRunsTest} gives.
 *
 * <p>The GL context is stood in for, so a case reads the calls made rather than anything drawn.
 */
final class GlLinesTest {

    private MockedStatic<GL11> glMock;

    @BeforeEach
    void standInForTheGlContext() {
        glMock = mockStatic(GL11.class);
    }

    @AfterEach
    void releaseTheGlContext() {
        glMock.close();
    }

    @Nested
    class DrawDashedSegments {

        @Test
        void endsThePrimitiveWhereTheSegmentsFaultPartWay() {
            // Three floats are one and a half vertices, which fault reading the segment's end after
            // the primitive is open.
            assertThatThrownBy(() -> GlLines.drawDashedSegments(new float[] {0f, 0f, 1f}, 1f, 1f, 1f))
                .isInstanceOf(ArrayIndexOutOfBoundsException.class);

            glMock.verify(GL11::glEnd);
        }
    }

    @Nested
    class StrokeLoop {

        @Test
        void strokesTheVerticesAsAClosedLoop() {

            GlLines.strokeLoop(new float[] {0f, 0f, 1f, 0f, 1f, 1f});

            glMock.verify(() -> GL11.glBegin(GL11.GL_LINE_LOOP));
            glMock.verify(() -> GL11.glVertex2f(1f, 1f));
            glMock.verify(GL11::glEnd);
        }
    }
}
