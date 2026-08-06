package kmlib.colour;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the contracts of {@link Colours}:
 *  - {@link Colours#getGlComponents} - 0-255 channels normalize to 0..1, the colour's
 *    own alpha is honoured, and alphaMult scales the final alpha and nothing else;
 *  - {@link Colours#scaleAlpha} - RGB is kept, the colour's own alpha folds into the
 *    multiplier as a rounded channel, and an over-1 multiplier saturates rather than
 *    throwing;
 *  - {@link Colours#darken} - each RGB channel scales toward black by the factor, the
 *    alpha is kept, and an over-1 factor saturates at white rather than throwing;
 *  - {@link Colours#blendRgbTowards} - each RGB channel lerps from base toward target
 *    by the amount, base's own alpha is kept (the target's is ignored), and an
 *    out-of-range amount saturates rather than throwing;
 *  - {@link Colours#flattenOnto} - the colour's alpha is spent compositing it over the
 *    backdrop and the result comes back opaque, whatever either carried;
 *  - {@link Colours#addOverlay} - the overlay's channels are added onto the base scaled
 *    by its own alpha and the weight, the base keeps its alpha, and the sum saturates
 *    at white rather than wrapping.
 */
final class ColoursTest {

    @Nested
    class GetGlComponents {

        @Test
        void normalizes_channels_to_unit_range() {

            var rgba = Colours.getGlComponents(Color.YELLOW, 1f);

            assertThat(rgba[0]).isCloseTo(1f, within(1e-6f));
            assertThat(rgba[1]).isCloseTo(1f, within(1e-6f));
            assertThat(rgba[2]).isCloseTo(0f, within(1e-6f));
            assertThat(rgba[3]).isCloseTo(1f, within(1e-6f));
        }

        @Test
        void scales_only_alpha_by_the_multiplier() {

            var rgba = Colours.getGlComponents(Color.YELLOW, 0.5f);

            assertThat(rgba[0]).isCloseTo(1f, within(1e-6f));
            assertThat(rgba[1]).isCloseTo(1f, within(1e-6f));
            assertThat(rgba[2]).isCloseTo(0f, within(1e-6f));
            assertThat(rgba[3]).isCloseTo(0.5f, within(1e-6f));
        }

        @Test
        void folds_the_colours_own_alpha_into_the_multiplier() {

            var rgba = Colours.getGlComponents(new Color(0, 0, 0, 128), 0.5f);

            // 128/255 * 0.5
            assertThat(rgba[3]).isCloseTo(128f / 255f * 0.5f, within(1e-6f));
        }
    }

    @Nested
    class ScaleAlpha {
        @Test
        void keeps_the_rgb_channels_and_scales_a_full_alpha() {

            var faded = Colours.scaleAlpha(new Color(10, 20, 30, 255), 0.5f);

            assertThat(faded.getRed()).isEqualTo(10);
            assertThat(faded.getGreen()).isEqualTo(20);
            assertThat(faded.getBlue()).isEqualTo(30);

            // 255 * 0.5 = 127.5, rounded to 128.
            assertThat(faded.getAlpha()).isEqualTo(128);
        }

        @Test
        void folds_the_colours_own_alpha_into_the_multiplier() {

            var faded = Colours.scaleAlpha(new Color(0, 0, 0, 128), 0.5f);

            // 128 * 0.5 = 64.
            assertThat(faded.getAlpha()).isEqualTo(64);
        }

        @Test
        void saturates_at_the_max_channel_when_the_multiplier_exceeds_one() {
            // 200 * 2 = 400 would overflow Color's 0-255 range, so it clamps to 255
            // instead of throwing.
            var faded = Colours.scaleAlpha(new Color(0, 0, 0, 200), 2f);

            assertThat(faded.getAlpha()).isEqualTo(255);
        }

        @Test
        void reaches_zero_alpha_at_a_zero_multiplier() {

            var faded = Colours.scaleAlpha(new Color(0, 0, 0, 255), 0f);

            assertThat(faded.getAlpha()).isEqualTo(0);
        }
    }

    @Nested
    class Darken {
        @Test
        void scales_each_rgb_channel_by_the_factor_and_keeps_the_alpha() {

            var darker = Colours.darken(new Color(200, 100, 40, 255), 0.5f);

            assertThat(darker.getRed()).isEqualTo(100);
            assertThat(darker.getGreen()).isEqualTo(50);

            // 40 * 0.5 = 20.
            assertThat(darker.getBlue()).isEqualTo(20);
            assertThat(darker.getAlpha()).isEqualTo(255);
        }

        @Test
        void leaves_the_colour_unchanged_at_a_factor_of_one() {

            var same = Colours.darken(new Color(10, 20, 30, 128), 1f);

            assertThat(same.getRed()).isEqualTo(10);
            assertThat(same.getGreen()).isEqualTo(20);
            assertThat(same.getBlue()).isEqualTo(30);
            assertThat(same.getAlpha()).isEqualTo(128);
        }

        @Test
        void returns_black_at_a_factor_of_zero() {

            var black = Colours.darken(new Color(200, 150, 100, 200), 0f);

            assertThat(black.getRed()).isEqualTo(0);
            assertThat(black.getGreen()).isEqualTo(0);
            assertThat(black.getBlue()).isEqualTo(0);

            // The alpha is untouched by darkening.
            assertThat(black.getAlpha()).isEqualTo(200);
        }

        @Test
        void saturates_at_the_max_channel_when_the_factor_exceeds_one() {
            // 200 * 2 = 400 would overflow Color's 0-255 range, so it clamps to 255.
            var brighter = Colours.darken(new Color(200, 0, 0, 255), 2f);

            assertThat(brighter.getRed()).isEqualTo(255);
        }
    }

    @Nested
    class BlendRgbTowards {
        @Test
        void lerps_each_rgb_channel_toward_the_target_and_keeps_base_alpha() {

            var washed = Colours.blendRgbTowards(
                new Color(40, 80, 120, 200),
                new Color(240, 80, 20, 255),
                0.5f);

            // 40 + (240-40)*0.5 = 140, 80 + (80-80)*0.5 = 80, 120 + (20-120)*0.5 = 70.
            assertThat(washed.getRed()).isEqualTo(140);
            assertThat(washed.getGreen()).isEqualTo(80);
            assertThat(washed.getBlue()).isEqualTo(70);

            // Base's own alpha is kept; the target's 255 is ignored.
            assertThat(washed.getAlpha()).isEqualTo(200);
        }

        @Test
        void returns_the_base_rgb_at_a_zero_amount() {

            var washed = Colours.blendRgbTowards(
                new Color(10, 20, 30, 128),
                new Color(200, 200, 200, 255),
                0f);

            assertThat(washed.getRed()).isEqualTo(10);
            assertThat(washed.getGreen()).isEqualTo(20);
            assertThat(washed.getBlue()).isEqualTo(30);
            assertThat(washed.getAlpha()).isEqualTo(128);
        }

        @Test
        void reaches_the_target_rgb_at_an_amount_of_one_but_keeps_base_alpha() {

            var washed = Colours.blendRgbTowards(
                new Color(10, 20, 30, 128),
                new Color(200, 150, 100, 255),
                1f);

            assertThat(washed.getRed()).isEqualTo(200);
            assertThat(washed.getGreen()).isEqualTo(150);
            assertThat(washed.getBlue()).isEqualTo(100);
            
            // The target's alpha (255) is ignored - base's 128 survives the full wash.
            assertThat(washed.getAlpha()).isEqualTo(128);
        }

        @Test
        void saturates_at_the_max_channel_when_the_amount_exceeds_one() {
            // 200 + (255-200)*2 = 310 would overflow Color's 0-255 range, so it clamps to 255.
            var washed = Colours.blendRgbTowards(
                new Color(200, 0, 0, 255),
                new Color(255, 0, 0, 255),
                2f);

            assertThat(washed.getRed()).isEqualTo(255);
        }
    }

    @Nested
    class FlattenOnto {

        @Test
        void composites_each_channel_by_the_source_alpha_and_returns_it_opaque() {

            var flattened = Colours.flattenOnto(
                new Color(200, 100, 50, 128),
                new Color(0, 20, 40, 255));

            // Weight 128/255 = 0.50196: 0 + (200-0)*0.50196 = 100.4, 20 + (100-20)*0.50196 = 60.2,
            // 40 + (50-40)*0.50196 = 45.0.
            assertThat(flattened.getRed()).isEqualTo(100);
            assertThat(flattened.getGreen()).isEqualTo(60);
            assertThat(flattened.getBlue()).isEqualTo(45);
            assertThat(flattened.getAlpha()).isEqualTo(255);
        }

        @Test
        void returns_the_source_shade_when_it_is_already_opaque() {
            // An opaque colour hides whatever it is over, so flattening it is the identity on its RGB.
            var flattened = Colours.flattenOnto(
                new Color(10, 20, 30, 255),
                new Color(200, 200, 200, 255));

            assertThat(flattened).isEqualTo(new Color(10, 20, 30, 255));
        }

        @Test
        void returns_the_backdrop_shade_when_the_source_is_fully_transparent() {

            var flattened = Colours.flattenOnto(
                new Color(10, 20, 30, 0),
                new Color(200, 150, 100, 255));

            assertThat(flattened).isEqualTo(new Color(200, 150, 100, 255));
        }

        @Test
        void spends_the_backdrop_alpha_and_answers_opaque_whatever_it_carried() {
            // The backdrop is read as the surface it stands for, so a caller handing in a see-through one
            // still gets a surface back rather than a colour that is somehow half of one.
            var flattened = Colours.flattenOnto(
                new Color(0, 0, 0, 0),
                new Color(100, 100, 100, 10));

            assertThat(flattened).isEqualTo(new Color(100, 100, 100, 255));
        }
    }

    @Nested
    class AddOverlay {
        
        @Test
        void adds_the_overlay_channels_scaled_by_its_alpha_and_the_weight() {

            var lit = Colours.addOverlay(
                new Color(20, 30, 40, 255),
                new Color(200, 100, 60, 128),
                0.5f);

            // Added weight 128/255 * 0.5 = 0.25098: 20 + 200*0.25098 = 70.2, 30 + 100*0.25098 = 55.1,
            // 40 + 60*0.25098 = 55.1.
            assertThat(lit.getRed()).isEqualTo(70);
            assertThat(lit.getGreen()).isEqualTo(55);
            assertThat(lit.getBlue()).isEqualTo(55);
        }

        @Test
        void keeps_the_bases_own_alpha() {
            // The base is the surface the light lands on, so what the light carried is spent on how much
            // of it lands rather than on how solid the result is.
            var lit = Colours.addOverlay(
                new Color(20, 30, 40, 128),
                new Color(200, 200, 200, 255),
                1f);

            assertThat(lit.getAlpha()).isEqualTo(128);
        }

        @Test
        void leaves_the_base_unchanged_at_a_zero_weight() {

            var lit = Colours.addOverlay(
                new Color(20, 30, 40, 255),
                new Color(200, 200, 200, 255),
                0f);

            assertThat(lit).isEqualTo(new Color(20, 30, 40, 255));
        }

        @Test
        void saturates_at_the_max_channel_rather_than_wrapping() {
            // 200 + 200 = 400 would overflow Color's 0-255 range; light piles up to white and stops.
            var lit = Colours.addOverlay(
                new Color(200, 200, 200, 255),
                new Color(200, 200, 200, 255),
                1f);

            assertThat(lit.getRed()).isEqualTo(255);
        }
    }
}
