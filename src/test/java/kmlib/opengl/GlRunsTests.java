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
 * Pins what a run emits and, above all, that the primitive it opens is closed whatever happens
 * inside it: a throw left inside {@code glBegin} has every later state call refused, the attribute
 * restore of the pass around it among them.
 *
 * <p>The GL context is stood in for, so a case reads the calls made rather than anything drawn.
 */
final class GlRunsTests {

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
    class DrawScaled {

        @Test
        void emitsEveryVertexScaledBetweenBeginAndEnd() {

            GlRuns.drawScaled(GL11.GL_LINES, new float[] {1f, 2f, 3f, 4f}, 2f);

            glMock.verify(() -> GL11.glBegin(GL11.GL_LINES));
            glMock.verify(() -> GL11.glVertex2f(2f, 4f));
            glMock.verify(() -> GL11.glVertex2f(6f, 8f));
            glMock.verify(GL11::glEnd);
        }

        @Test
        void endsThePrimitiveWhereTheRunFaultsPartWay() {
            // An odd-length run faults on its last vertex, after the primitive is open.
            assertThatThrownBy(() -> GlRuns.drawScaled(GL11.GL_LINES, new float[] {1f, 2f, 3f}, 1f))
                .isInstanceOf(ArrayIndexOutOfBoundsException.class);

            glMock.verify(GL11::glEnd);
        }
    }

    @Nested
    class Draw {

        @Test
        void emitsTheRunAtItsOwnCoordinates() {

            GlRuns.draw(GL11.GL_POINTS, new float[] {1f, 2f});

            glMock.verify(() -> GL11.glBegin(GL11.GL_POINTS));
            glMock.verify(() -> GL11.glVertex2f(1f, 2f));
            glMock.verify(GL11::glEnd);
        }
    }
}
