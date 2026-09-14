package kmlib.starsector.ui.widgets.tabs.style;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two interiors a vanilla raised button settles on against the engine's own values: the stock
 * player faction's dark step over the black backing a button stands on, and that same shade unpainted.
 * The expected colours are written down and checked against what the engine's own intel-screen buttons
 * sample as, so a change to the rule shows as a shade that no longer matches the buttons this row sits
 * among rather than as arithmetic agreeing with itself.
 *
 * <p>The samples are close but not equal to what the rule produces, and deliberately so: a screen grab is
 * taken over live visor content rather than over the pure black the rule composites against, so it lands
 * a channel value or two above. What the sample settles is the shape of the rule, and that is what these
 * cases stand on.
 *
 * <p>What the pointer adds is not pinned here. It is a lift over whichever of these two a button has
 * settled on rather than a shade of its own, so it is pinned where the hover rule is.
 */
final class VanillaButtonFillsTest {

    // The engine's own values on a stock install: the player faction's dark step, which the engine fills
    // and frames a button with, over the black a button stands on.
    private static final Color DARK_ACCENT = new Color(31, 94, 112, 175);
    private static final Color BACKDROP = Color.BLACK;

    private static final VanillaButtonPaint VANILLA_PAINT =
        new VanillaButtonPaint(DARK_ACCENT, BACKDROP);

    private static final int OPAQUE_ALPHA = 255;
    private static final int UNPAINTED_ALPHA = 0;

    @Nested
    class ResolveShownFill {

        @Test
        void resolveShownFillCompositesTheInteriorOntoTheBacking() {
            // The dark step at alpha 175 over black. Samples as #17424f off the engine's own buttons; the
            // two channel values between that and this are the visor content the grab was taken over.
            assertThat(VanillaButtonFills.resolveShownFill(VANILLA_PAINT))
                .isEqualTo(new Color(21, 65, 77, OPAQUE_ALPHA));
        }

        @Test
        void resolveShownFillFollowsARestyledAccent() {
            // The point of computing rather than sampling: a panel pointed at another palette moves these
            // buttons. An opaque dark step composites to itself, so what comes back is the accent handed
            // in and nothing of the stock one.
            var restyled = new VanillaButtonPaint(new Color(200, 40, 40, OPAQUE_ALPHA), BACKDROP);

            assertThat(VanillaButtonFills.resolveShownFill(restyled))
                .isEqualTo(new Color(200, 40, 40, OPAQUE_ALPHA));
        }

        @Test
        void resolveShownFillReturnsAnOpaqueInterior() {
            // A shown interior covers the backing rather than tinting it: left translucent, the shade a
            // button reads at would depend on what the row happens to stand over.
            assertThat(VanillaButtonFills.resolveShownFill(VANILLA_PAINT).getAlpha())
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

}
