package kmlib.color;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the contracts of {@link Colors}:
 *  - {@link Colors#getGlComponents} - 0-255 channels normalize to 0..1, the colour's
 *    own alpha is honoured, and alphaMult scales the final alpha and nothing else;
 *  - {@link Colors#scaleAlpha} - RGB is kept, the colour's own alpha folds into the
 *    multiplier as a rounded channel, and an over-1 multiplier saturates rather than
 *    throwing.
 */
final class ColorsTest {

    @Nested
    class GetGlComponents {
        @Test
        void normalizes_channels_to_unit_range() {
            var rgba = Colors.getGlComponents(Color.YELLOW, 1f);

            assertThat(rgba[0]).isCloseTo(1f, within(1e-6f));
            assertThat(rgba[1]).isCloseTo(1f, within(1e-6f));
            assertThat(rgba[2]).isCloseTo(0f, within(1e-6f));
            assertThat(rgba[3]).isCloseTo(1f, within(1e-6f));
        }

        @Test
        void scales_only_alpha_by_the_multiplier() {
            var rgba = Colors.getGlComponents(Color.YELLOW, 0.5f);

            assertThat(rgba[0]).isCloseTo(1f, within(1e-6f));
            assertThat(rgba[1]).isCloseTo(1f, within(1e-6f));
            assertThat(rgba[2]).isCloseTo(0f, within(1e-6f));
            assertThat(rgba[3]).isCloseTo(0.5f, within(1e-6f));
        }

        @Test
        void folds_the_colours_own_alpha_into_the_multiplier() {
            var rgba = Colors.getGlComponents(new Color(0, 0, 0, 128), 0.5f);

            // 128/255 * 0.5
            assertThat(rgba[3]).isCloseTo(128f / 255f * 0.5f, within(1e-6f));
        }
    }

    @Nested
    class ScaleAlpha {
        @Test
        void keeps_the_rgb_channels_and_scales_a_full_alpha() {
            var faded = Colors.scaleAlpha(new Color(10, 20, 30, 255), 0.5f);

            assertThat(faded.getRed()).isEqualTo(10);
            assertThat(faded.getGreen()).isEqualTo(20);
            assertThat(faded.getBlue()).isEqualTo(30);
            // 255 * 0.5 = 127.5, rounded to 128.
            assertThat(faded.getAlpha()).isEqualTo(128);
        }

        @Test
        void folds_the_colours_own_alpha_into_the_multiplier() {
            var faded = Colors.scaleAlpha(new Color(0, 0, 0, 128), 0.5f);

            // 128 * 0.5 = 64.
            assertThat(faded.getAlpha()).isEqualTo(64);
        }

        @Test
        void saturates_at_the_max_channel_when_the_multiplier_exceeds_one() {
            // 200 * 2 = 400 would overflow Color's 0-255 range, so it clamps to 255
            // instead of throwing.
            var faded = Colors.scaleAlpha(new Color(0, 0, 0, 200), 2f);

            assertThat(faded.getAlpha()).isEqualTo(255);
        }

        @Test
        void reaches_zero_alpha_at_a_zero_multiplier() {
            var faded = Colors.scaleAlpha(new Color(0, 0, 0, 255), 0f);

            assertThat(faded.getAlpha()).isEqualTo(0);
        }
    }
}
