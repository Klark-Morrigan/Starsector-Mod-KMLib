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
        void normalizesChannelsToUnitRange() {

            var rgba = Colours.getGlComponents(Color.YELLOW, 1f);

            assertThat(rgba[0]).isCloseTo(1f, within(1e-6f));
            assertThat(rgba[1]).isCloseTo(1f, within(1e-6f));
            assertThat(rgba[2]).isCloseTo(0f, within(1e-6f));
            assertThat(rgba[3]).isCloseTo(1f, within(1e-6f));
        }

        @Test
        void scalesOnlyAlphaByTheMultiplier() {

            var rgba = Colours.getGlComponents(Color.YELLOW, 0.5f);

            assertThat(rgba[0]).isCloseTo(1f, within(1e-6f));
            assertThat(rgba[1]).isCloseTo(1f, within(1e-6f));
            assertThat(rgba[2]).isCloseTo(0f, within(1e-6f));
            assertThat(rgba[3]).isCloseTo(0.5f, within(1e-6f));
        }

        @Test
        void foldsTheColoursOwnAlphaIntoTheMultiplier() {

            var rgba = Colours.getGlComponents(new Color(0, 0, 0, 128), 0.5f);

            // 128/255 * 0.5
            assertThat(rgba[3]).isCloseTo(128f / 255f * 0.5f, within(1e-6f));
        }
    }

    @Nested
    class ScaleAlpha {

        @Test
        void keepsTheRgbChannelsAndScalesAFullAlpha() {

            var faded = Colours.scaleAlpha(new Color(10, 20, 30, 255), 0.5f);

            assertThat(faded.getRed()).isEqualTo(10);
            assertThat(faded.getGreen()).isEqualTo(20);
            assertThat(faded.getBlue()).isEqualTo(30);

            // 255 * 0.5 = 127.5, rounded to 128.
            assertThat(faded.getAlpha()).isEqualTo(128);
        }

        @Test
        void foldsTheColoursOwnAlphaIntoTheMultiplier() {

            var faded = Colours.scaleAlpha(new Color(0, 0, 0, 128), 0.5f);

            // 128 * 0.5 = 64.
            assertThat(faded.getAlpha()).isEqualTo(64);
        }

        @Test
        void saturatesAtTheMaxChannelWhenTheMultiplierExceedsOne() {
            // 200 * 2 = 400 would overflow Color's 0-255 range, so it clamps to 255
            // instead of throwing.
            var faded = Colours.scaleAlpha(new Color(0, 0, 0, 200), 2f);

            assertThat(faded.getAlpha()).isEqualTo(255);
        }

        @Test
        void reachesZeroAlphaAtAZeroMultiplier() {

            var faded = Colours.scaleAlpha(new Color(0, 0, 0, 255), 0f);

            assertThat(faded.getAlpha()).isEqualTo(0);
        }
    }

    @Nested
    class Darken {

        @Test
        void scalesEachRgbChannelByTheFactorAndKeepsTheAlpha() {

            var darker = Colours.darken(new Color(200, 100, 40, 255), 0.5f);

            assertThat(darker.getRed()).isEqualTo(100);
            assertThat(darker.getGreen()).isEqualTo(50);

            // 40 * 0.5 = 20.
            assertThat(darker.getBlue()).isEqualTo(20);
            assertThat(darker.getAlpha()).isEqualTo(255);
        }

        @Test
        void leavesTheColourUnchangedAtAFactorOfOne() {

            var same = Colours.darken(new Color(10, 20, 30, 128), 1f);

            assertThat(same.getRed()).isEqualTo(10);
            assertThat(same.getGreen()).isEqualTo(20);
            assertThat(same.getBlue()).isEqualTo(30);
            assertThat(same.getAlpha()).isEqualTo(128);
        }

        @Test
        void returnsBlackAtAFactorOfZero() {

            var black = Colours.darken(new Color(200, 150, 100, 200), 0f);

            assertThat(black.getRed()).isEqualTo(0);
            assertThat(black.getGreen()).isEqualTo(0);
            assertThat(black.getBlue()).isEqualTo(0);

            // The alpha is untouched by darkening.
            assertThat(black.getAlpha()).isEqualTo(200);
        }

        @Test
        void saturatesAtTheMaxChannelWhenTheFactorExceedsOne() {
            // 200 * 2 = 400 would overflow Color's 0-255 range, so it clamps to 255.
            var brighter = Colours.darken(new Color(200, 0, 0, 255), 2f);

            assertThat(brighter.getRed()).isEqualTo(255);
        }
    }

    @Nested
    class MultiplyBy {

        @Test
        void multipliesEachChannelAsAFractionOfFullStrength() {

            var tinted = Colours.multiplyBy(new Color(200, 100, 40, 255), new Color(128, 255, 0, 255));

            // 200 * 128/255 = 100.4, rounded.
            assertThat(tinted.getRed()).isEqualTo(100);
            assertThat(tinted.getGreen()).isEqualTo(100);
            assertThat(tinted.getBlue()).isEqualTo(0);
            assertThat(tinted.getAlpha()).isEqualTo(255);
        }

        @Test
        void leavesTheColourUnchangedUnderWhite() {
            // The no-op multiply, which is what "states no tint" resolves to - so an untinted image and a
            // white-tinted one are the same draw rather than two paths through the pass that makes it.
            var same = Colours.multiplyBy(new Color(10, 20, 30, 128), Color.WHITE);

            assertThat(same.getRed()).isEqualTo(10);
            assertThat(same.getGreen()).isEqualTo(20);
            assertThat(same.getBlue()).isEqualTo(30);
            assertThat(same.getAlpha()).isEqualTo(128);
        }

        @Test
        void takesTheAlphaDownWithTheRestUnderAHalfSolidMultiplier() {
            // A translucent tint thins what it is laid over rather than only shading it, which is what a
            // multiplied draw does - an alpha carried across whole would leave it solid under a wash that
            // was not.
            var thinned = Colours.multiplyBy(new Color(200, 200, 200, 200), new Color(255, 255, 255, 128));

            assertThat(thinned.getRed()).isEqualTo(200);

            // 200 * 128/255 = 100.4, rounded.
            assertThat(thinned.getAlpha()).isEqualTo(100);
        }

        @Test
        void returnsBlackUnderBlack() {

            var black = Colours.multiplyBy(new Color(200, 150, 100, 255), Color.BLACK);

            assertThat(black.getRed()).isEqualTo(0);
            assertThat(black.getGreen()).isEqualTo(0);
            assertThat(black.getBlue()).isEqualTo(0);
        }
    }

    @Nested
    class BlendRgbTowards {

        @Test
        void lerpsEachRgbChannelTowardTheTargetAndKeepsBaseAlpha() {

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
        void returnsTheBaseRgbAtAZeroAmount() {

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
        void reachesTheTargetRgbAtAnAmountOfOneButKeepsBaseAlpha() {

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
        void saturatesAtTheMaxChannelWhenTheAmountExceedsOne() {
            // 200 + (255-200)*2 = 310 would overflow Color's 0-255 range, so it clamps to 255.
            var washed = Colours.blendRgbTowards(
                new Color(200, 0, 0, 255),
                new Color(255, 0, 0, 255),
                2f);

            assertThat(washed.getRed()).isEqualTo(255);
        }
    }

    @Nested
    class BlendTowards {

        @Test
        void lerpsTheAlphaChannelAlongWithTheRgb() {
            // The whole difference from blendRgbTowards, so the two are told apart on the one channel
            // they disagree about: 200 + (32-200)*0.5 = 116, where the RGB-only blend would answer 200.
            var blended = Colours.blendTowards(
                new Color(40, 80, 120, 200),
                new Color(240, 80, 20, 32),
                0.5f);

            assertThat(blended.getRed()).isEqualTo(140);
            assertThat(blended.getGreen()).isEqualTo(80);
            assertThat(blended.getBlue()).isEqualTo(70);
            assertThat(blended.getAlpha()).isEqualTo(116);
        }

        @Test
        void carriesAFullyTransparentBaseAllTheWayToTheTargetsAlpha() {
            // The case the blend exists for: an unpainted surface fading in. An alpha kept from the base
            // would hold this at zero the whole way, so the travel would never appear at all.
            var blended = Colours.blendTowards(
                new Color(21, 65, 77, 0),
                new Color(21, 65, 77, 255),
                1f);

            assertThat(blended.getAlpha()).isEqualTo(255);
        }

        @Test
        void returnsTheBaseAtAZeroAmount() {

            var blended = Colours.blendTowards(
                new Color(10, 20, 30, 128),
                new Color(200, 200, 200, 255),
                0f);

            assertThat(blended).isEqualTo(new Color(10, 20, 30, 128));
        }

        @Test
        void saturatesEveryChannelIncludingAlphaWhenTheAmountExceedsOne() {
            // 200 + (255-200)*2 = 310 on the RGB and 128 + (255-128)*2 = 382 on the alpha would both
            // overflow Color's 0-255 range. The alpha is the one this blend adds, so it is the one an
            // out-of-range amount could newly throw from.
            var blended = Colours.blendTowards(
                new Color(200, 200, 200, 128),
                new Color(255, 255, 255, 255),
                2f);

            assertThat(blended).isEqualTo(new Color(255, 255, 255, 255));
        }
    }

    @Nested
    class FlattenOnto {

        @Test
        void compositesEachChannelByTheSourceAlphaAndReturnsItOpaque() {

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
        void returnsTheSourceShadeWhenItIsAlreadyOpaque() {
            // An opaque colour hides whatever it is over, so flattening it is the identity on its RGB.
            var flattened = Colours.flattenOnto(
                new Color(10, 20, 30, 255),
                new Color(200, 200, 200, 255));

            assertThat(flattened).isEqualTo(new Color(10, 20, 30, 255));
        }

        @Test
        void returnsTheBackdropShadeWhenTheSourceIsFullyTransparent() {

            var flattened = Colours.flattenOnto(
                new Color(10, 20, 30, 0),
                new Color(200, 150, 100, 255));

            assertThat(flattened).isEqualTo(new Color(200, 150, 100, 255));
        }

        @Test
        void spendsTheBackdropAlphaAndAnswersOpaqueWhateverItCarried() {
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
        void addsTheOverlayChannelsScaledByItsAlphaAndTheWeight() {

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
        void keepsTheBasesOwnAlpha() {
            // The base is the surface the light lands on, so what the light carried is spent on how much
            // of it lands rather than on how solid the result is.
            var lit = Colours.addOverlay(
                new Color(20, 30, 40, 128),
                new Color(200, 200, 200, 255),
                1f);

            assertThat(lit.getAlpha()).isEqualTo(128);
        }

        @Test
        void leavesTheBaseUnchangedAtAZeroWeight() {

            var lit = Colours.addOverlay(
                new Color(20, 30, 40, 255),
                new Color(200, 200, 200, 255),
                0f);

            assertThat(lit).isEqualTo(new Color(20, 30, 40, 255));
        }

        @Test
        void saturatesAtTheMaxChannelRatherThanWrapping() {
            // 200 + 200 = 400 would overflow Color's 0-255 range; light piles up to white and stops.
            var lit = Colours.addOverlay(
                new Color(200, 200, 200, 255),
                new Color(200, 200, 200, 255),
                1f);

            assertThat(lit.getRed()).isEqualTo(255);
        }
    }

    @Nested
    class AddLight {

        @Test
        void addsTheSameChannelsAddOverlayDoes() {
            // The channel rule is shared with the method beside it - the two part over how solid the result
            // is, not over how bright - so a change to one that did not reach the other would leave the same
            // light landing in two different colours.
            var lit = Colours.addLight(
                new Color(20, 30, 40, 255),
                new Color(200, 100, 60, 128),
                0.5f);

            assertThat(lit.getRed()).isEqualTo(70);
            assertThat(lit.getGreen()).isEqualTo(55);
            assertThat(lit.getBlue()).isEqualTo(55);
        }

        @Test
        void paintsAnUnpaintedSurfaceAsSolidlyAsTheLightLandingOnIt() {
            // The whole reason this stands apart from addOverlay: a surface drawn at nothing has no
            // channels worth brightening, so the light itself is what paints it - a quarter of the way to
            // the light's own solidity at a quarter weight.
            var lit = Colours.addLight(
                new Color(20, 30, 40, 0),
                new Color(200, 200, 200, 255),
                0.25f);

            assertThat(lit.getAlpha()).isEqualTo(64);
        }

        @Test
        void leavesASolidSurfaceSolidUnderAFainterLight() {
            // Light only ever adds. A dim light on a solid surface would otherwise eat a hole in it, which
            // is the one way this rule could make something less visible than it was.
            var lit = Colours.addLight(
                new Color(20, 30, 40, 255),
                new Color(200, 200, 200, 100),
                1f);

            assertThat(lit.getAlpha()).isEqualTo(255);
        }

        @Test
        void leavesTheBaseUnchangedAtAZeroWeight() {
            // Including its alpha: what the light painted has to come back off as the light goes, or a
            // surface once lit would keep a shade of its own for good.
            var lit = Colours.addLight(
                new Color(20, 30, 40, 0),
                new Color(200, 200, 200, 255),
                0f);

            assertThat(lit).isEqualTo(new Color(20, 30, 40, 0));
        }
    }

    @Nested
    class SubtractLight {

        @Test
        void givesBackTheLightThatAddOverlayWouldAddInFull() {
            // The round trip is the whole contract: a caller measures the pass between two settled shades
            // here and lays it on something else, so what comes back has to be what the additive blend
            // takes from one to the other.
            var light = Colours.subtractLight(new Color(200, 160, 120), new Color(20, 40, 60));

            assertThat(light.getRed()).isEqualTo(180);
            assertThat(light.getGreen()).isEqualTo(120);
            assertThat(light.getBlue()).isEqualTo(60);
        }

        @Test
        void givesBackNothingOnAChannelTheLitShadeIsDarkerOn() {
            // Light only ever adds, so there is no light that could carry a channel down - and a negative
            // one handed to Color would throw rather than dim anything.
            var light = Colours.subtractLight(new Color(200, 10, 120), new Color(20, 40, 60));

            assertThat(light.getGreen()).isEqualTo(0);
        }

        @Test
        void comesBackOpaqueWhateverEitherSideWasDrawnAt() {
            // A light's own alpha scales how much of it lands, so one inheriting a see-through surface's
            // alpha would arrive at a fraction of the difference it was measured as.
            var light = Colours.subtractLight(new Color(200, 160, 120, 40), new Color(20, 40, 60, 0));

            assertThat(light.getAlpha()).isEqualTo(255);
        }
    }
}
