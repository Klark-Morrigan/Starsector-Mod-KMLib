package kmlib.starsector.ui.screen;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the pixel-to-UI scaling both ways: a coordinate rescales by the axis's own ratio, an equal
 * ratio is the identity, and an axis with no extent (no display, or a window collapsed to nothing)
 * answers a negative parked value rather than dividing by zero.
 *
 * <p>Each direction guards the length it divides by and not the other, which is why the degenerate
 * cases are asserted per direction: converting into UI units divides by the pixel length, and
 * converting back divides by the UI one.
 */
class ScreenAxisTest {

    // A 2x-scaled axis: 1920 UI units across 3840 pixels, so the ratio is visible in every
    // expectation rather than cancelling to 1.
    private static final ScreenAxis SCALED_AXIS = new ScreenAxis(1920f, 3840f);

    // The unscaled case, where UI units and pixels are the same number.
    private static final ScreenAxis UNSCALED_AXIS = new ScreenAxis(1920f, 1920f);

    @Nested
    class ConvertPixelToUi {

        @Test
        void rescalesAPixelByTheUiToPixelRatio() {
            assertThat(SCALED_AXIS.convertPixelToUi(500f))
                .isEqualTo(250f);
        }

        @Test
        void isTheIdentityWhenUiUnitsMatchPixels() {
            assertThat(UNSCALED_AXIS.convertPixelToUi(640f))
                .isEqualTo(640f);
        }

        @Test
        void returnsNegativeWhenThePixelLengthIsZero() {
            assertThat(new ScreenAxis(1920f, 0f).convertPixelToUi(500f))
                .isEqualTo(-1f);
        }

        @Test
        void returnsNegativeWhenThePixelLengthIsNegative() {
            assertThat(new ScreenAxis(1920f, -10f).convertPixelToUi(500f))
                .isEqualTo(-1f);
        }
    }

    @Nested
    class ConvertUiToPixel {

        @Test
        void rescalesAUiCoordinateByThePixelToUiRatio() {
            // The exact inverse of the pixel-to-UI case above: 250 UI units back out to 500 pixels.
            assertThat(SCALED_AXIS.convertUiToPixel(250f))
                .isEqualTo(500f);
        }

        @Test
        void isTheIdentityWhenUiUnitsMatchPixels() {
            assertThat(UNSCALED_AXIS.convertUiToPixel(640f))
                .isEqualTo(640f);
        }

        @Test
        void returnsNegativeWhenTheUiLengthIsZero() {
            assertThat(new ScreenAxis(0f, 3840f)
                .convertUiToPixel(250f)).isEqualTo(-1f);
        }

        @Test
        void returnsNegativeWhenTheUiLengthIsNegative() {
            assertThat(new ScreenAxis(-10f, 3840f)
                .convertUiToPixel(250f)).isEqualTo(-1f);
        }
    }
}
