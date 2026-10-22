package kmlib.opengl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.mockito.MockedStatic;

import java.awt.Color;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.floatThat;
import static org.mockito.Mockito.mockStatic;

/**
 * Pins that a colour reaches GL as its four normalised channels, with only the alpha scaled by the
 * fade.
 *
 * <p>The GL context is stood in for, so a case reads the call made rather than anything drawn.
 */
final class GlColourTests {

    private static final float CHANNEL_SLACK = 1e-6f;

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
    class Set {

        // The fade dims what is drawn without shifting its hue, so it reaches the alpha alone.
        @Test
        void setsTheNormalisedChannelsWithOnlyTheAlphaFaded() {

            // ARRANGE
            var colour = new Color(255, 51, 0, 255);
            var expectedRed = 1f;
            var expectedGreen = 0.2f;
            var expectedBlue = 0f;
            var expectedAlpha = 0.5f;

            // ACT
            GlColour.set(colour, 0.5f);

            // ASSERT
            glMock.verify(() -> GL11.glColor4f(
                eq(expectedRed),
                floatThat(green -> Math.abs(green - expectedGreen) < CHANNEL_SLACK),
                eq(expectedBlue),
                eq(expectedAlpha)));
        }
    }
}
