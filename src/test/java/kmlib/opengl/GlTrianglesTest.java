package kmlib.opengl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.mockStatic;

/**
 * Pins that a triangle is filled under the triangle primitive, closed once its corners are emitted.
 *
 * <p>The GL context is stood in for, so a case reads the calls made rather than anything drawn.
 */
final class GlTrianglesTest {

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
    class FillTriangle {

        @Test
        void fillsTheCornersAsATriangle() {

            GlTriangles.fillTriangle(new float[] {0f, 0f, 1f, 0f, 1f, 1f});

            glMock.verify(() -> GL11.glBegin(GL11.GL_TRIANGLES));
            glMock.verify(() -> GL11.glVertex2f(1f, 1f));
            glMock.verify(GL11::glEnd);
        }
    }
}
