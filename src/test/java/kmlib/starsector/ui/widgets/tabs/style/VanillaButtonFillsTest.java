package kmlib.starsector.ui.widgets.tabs.style;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the interiors a vanilla raised button settles on against the engine's own values: the stock
 * player faction's dark and base steps, over the black backing a button stands on. The expected colours
 * are written down and checked against what the engine's own intel-screen buttons sample as, so a change
 * to the rule shows as a shade that no longer matches the buttons this row sits among rather than as
 * arithmetic agreeing with itself.
 *
 * <p>The samples are close but not equal to what the rule produces, and deliberately so: a screen grab is
 * taken over live visor content rather than over the pure black the rule composites against, so it lands
 * a channel value or two above. What the sample settles is the shape of the rule - which colour is added
 * and how much of it - and that is what these cases stand on.
 */
final class VanillaButtonFillsTest {

    // The engine's own values on a stock install: the player faction's dark step, which the engine fills
    // and frames a button with, and its base step, which the button is built with and brightens by.
    private static final Color DARK_ACCENT = new Color(31, 94, 112, 175);
    private static final Color BASE_ACCENT = new Color(165, 230, 255);
    private static final Color BACKDROP = Color.BLACK;

    private static final VanillaButtonPaint VANILLA_PAINT =
        new VanillaButtonPaint(DARK_ACCENT, BASE_ACCENT, BACKDROP);

    private static final int OPAQUE_ALPHA = 255;
    private static final int UNPAINTED_ALPHA = 0;

    @Nested
    class ResolveFillAtGlow {

        @Test
        void resolveFillAtGlowCompositesTheShownButtonsInteriorOntoTheBacking() {
            // The dark step at alpha 175 over black. Samples as #17424f off the engine's own buttons; the
            // two channel values between that and this are the visor content the grab was taken over.
            assertThat(resolveFillAt(VanillaButtonFills.NO_GLOW))
                .isEqualTo(new Color(21, 65, 77, OPAQUE_ALPHA));
        }

        @Test
        void resolveFillAtGlowAddsTheBaseAccentUndilutedUnderThePointer() {
            // The shown button's interior plus 0.175 of the base accent as it comes: (21, 65, 77) gains
            // 29, 40, and 45. Samples as #346a7c, which is what fixed the weight - solving that against
            // the shade above gives the same 0.175 on all three channels, where a whitened glow of the
            // kind a tab takes gives three different answers and so cannot be the rule here.
            assertThat(resolveFillAt(VanillaButtonFills.POINTED_GLOW))
                .isEqualTo(new Color(50, 105, 122, OPAQUE_ALPHA));
        }

        @Test
        void resolveFillAtGlowKeepsThePointedButtonBrighterThanTheShownOne() {
            // The interior is the whole signal - the frame around it never moves - so a pointed-at button
            // has to outshine the shown one on every channel or the two states read alike under the
            // pointer.
            var shown = resolveFillAt(VanillaButtonFills.NO_GLOW);
            var pointed = resolveFillAt(VanillaButtonFills.POINTED_GLOW);

            assertThat(pointed.getRed()).isGreaterThan(shown.getRed());
            assertThat(pointed.getGreen()).isGreaterThan(shown.getGreen());
            assertThat(pointed.getBlue()).isGreaterThan(shown.getBlue());
        }

        @Test
        void resolveFillAtGlowFollowsARestyledAccent() {
            // The point of computing rather than sampling: a panel pointed at another palette moves these
            // buttons. An opaque dark step composites to itself, so what comes back is the accent handed
            // in and nothing of the stock one.
            var restyled = new VanillaButtonPaint(
                new Color(200, 40, 40, OPAQUE_ALPHA),
                BASE_ACCENT,
                BACKDROP);

            assertThat(VanillaButtonFills.resolveFillAtGlow(restyled, VanillaButtonFills.NO_GLOW))
                .isEqualTo(new Color(200, 40, 40, OPAQUE_ALPHA));
        }

        @Test
        void resolveFillAtGlowReturnsAnOpaqueInterior() {
            // A lit interior covers the backing rather than tinting it: left translucent, the shade a
            // button reads at would depend on what the row happens to stand over.
            assertThat(resolveFillAt(VanillaButtonFills.POINTED_GLOW).getAlpha())
                .isEqualTo(OPAQUE_ALPHA);
        }
    }

    @Nested
    class ResolveUnpaintedFill {

        @Test
        void resolveUnpaintedFillLaysDownNothingOfTheShownButtonsShade() {
            // Zero alpha is what makes "no interior" a value rather than a quad the paint pass skips, and
            // the RGB is the shown button's own - so the travel between the two states is one interior
            // fading in over an unchanged backing rather than two colours crossing.
            assertThat(VanillaButtonFills.resolveUnpaintedFill(VANILLA_PAINT))
                .isEqualTo(new Color(21, 65, 77, UNPAINTED_ALPHA));
        }
    }

    // The engine's own paint at the given glow, the pairing every case above varies only the glow of.
    private static Color resolveFillAt(float glowAmount) {
        return VanillaButtonFills.resolveFillAtGlow(VANILLA_PAINT, glowAmount);
    }
}
