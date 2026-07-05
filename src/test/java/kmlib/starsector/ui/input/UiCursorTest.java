package kmlib.starsector.ui.input;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the pixel-to-UI scaling: a raw pixel rescales by the UI-to-pixel ratio, an equal ratio
 * is the identity, and a zero or negative pixel span (no display) returns a negative parked
 * value rather than dividing by zero.
 */
class UiCursorTest {

    @Nested
    class ConvertPixelToUi {
        @Test
        void rescalesAPixelByTheUiToPixelRatio() {
            // 500 px of a 3840 px axis, mapped onto a 1920 UI axis -> half the ratio.
            assertThat(UiCursor.convertPixelToUi(500f, 1920f, 3840f)).isEqualTo(250f);
        }

        @Test
        void isTheIdentityWhenUiUnitsMatchPixels() {
            assertThat(UiCursor.convertPixelToUi(640f, 1920f, 1920f)).isEqualTo(640f);
        }

        @Test
        void returnsNegativeWhenPixelSpanIsZero() {
            assertThat(UiCursor.convertPixelToUi(500f, 1920f, 0f)).isEqualTo(-1f);
        }

        @Test
        void returnsNegativeWhenPixelSpanIsNegative() {
            assertThat(UiCursor.convertPixelToUi(500f, 1920f, -10f)).isEqualTo(-1f);
        }
    }
}
