package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two shades a vanilla tab settles on against the engine's own values: vanilla's {@code
 * buttonBgDark} and {@code buttonText}, over the dark backing its tabs stand on. The expected colours are
 * written down, so a change to the rule shows as a shade that no longer matches the tabs this strip sits
 * beside rather than as arithmetic agreeing with itself.
 */
final class VanillaTabFillsTest {

    // The engine's own values, from settings.json: the dark button fill a tab rests at, and the button
    // text colour its glow is a whitened form of.
    private static final Color BUTTON_BG_DARK = new Color(31, 94, 112, 175);
    private static final Color BUTTON_TEXT = new Color(170, 222, 255, 255);

    private static final Color BACKDROP = Color.BLACK;

    private static final int OPAQUE_ALPHA = 255;

    @Nested
    class ResolveRestingFill {

        @Test
        void resolveRestingFillCompositesTheButtonFillOntoTheBackdrop() {
            // buttonBgDark at alpha 175 over black.
            assertThat(VanillaTabFills.resolveRestingFill(BUTTON_BG_DARK, BACKDROP))
                .isEqualTo(new Color(21, 65, 77, OPAQUE_ALPHA));
        }

        @Test
        void resolveRestingFillTakesTheBackdropWhereTheFillIsAbsent() {
            // A fully transparent fill leaves the tab the colour of what it stands on - the degenerate end
            // of the same composite, not a special case.
            assertThat(VanillaTabFills.resolveRestingFill(new Color(31, 94, 112, 0), BACKDROP))
                .isEqualTo(new Color(0, 0, 0, OPAQUE_ALPHA));
        }

        @Test
        void resolveRestingFillFollowsARestyledButtonFill() {
            // The whole point of computing rather than sampling: a restyled install moves this tab.
            assertThat(VanillaTabFills.resolveRestingFill(new Color(200, 40, 40, OPAQUE_ALPHA), BACKDROP))
                .isEqualTo(new Color(200, 40, 40, OPAQUE_ALPHA));
        }
    }

    @Nested
    class ResolveLitFill {

        @Test
        void resolveLitFillAddsTheGlowOntoTheRestingShade() {
            // The resting shade (21, 65, 77) plus the glow: buttonText half-way to white, (213, 239, 255),
            // at half strength tempered by the fill's alpha - 0.5 * (175 + 50) / 255 = 0.441.
            assertThat(VanillaTabFills.resolveLitFill(BUTTON_BG_DARK, BUTTON_TEXT, BACKDROP))
                .isEqualTo(new Color(115, 170, 190, OPAQUE_ALPHA));
        }

        @Test
        void resolveLitFillStandsBrighterThanTheRestingShadeOnEveryChannel() {
            // A glow adds light; it never takes any. Stated separately from the numbers above because it
            // is the property that makes the lit tab read as lit whatever the two colours become.
            var resting = VanillaTabFills.resolveRestingFill(BUTTON_BG_DARK, BACKDROP);
            var lit = VanillaTabFills.resolveLitFill(BUTTON_BG_DARK, BUTTON_TEXT, BACKDROP);

            assertThat(lit.getRed()).isGreaterThan(resting.getRed());
            assertThat(lit.getGreen()).isGreaterThan(resting.getGreen());
            assertThat(lit.getBlue()).isGreaterThan(resting.getBlue());
        }

        @Test
        void resolveLitFillTakesTheFullGlowOnceTheFillIsSolidEnough() {
            // The headroom means an opaque fill and one a little short of it take the same glow. Asked of
            // a black fill so only the glow is left standing: any coloured fill composites to a different
            // resting shade at each alpha, which would move the answer for a reason this is not about.
            assertThat(VanillaTabFills.resolveLitFill(new Color(0, 0, 0, 205), BUTTON_TEXT, BACKDROP))
                .isEqualTo(VanillaTabFills.resolveLitFill(new Color(0, 0, 0, 255), BUTTON_TEXT, BACKDROP));
        }

        @Test
        void resolveLitFillDimsTheGlowOnASeeThroughFill() {
            // Below the headroom the glow is scaled down with the fill, so a barely-there fill does not
            // light up as strongly as a solid one. Black again, for the reason above: (213, 239, 255) at
            // 0.5 * (25 + 50) / 255 = 0.147 rather than at the full half.
            assertThat(VanillaTabFills.resolveLitFill(new Color(0, 0, 0, 25), BUTTON_TEXT, BACKDROP))
                .isEqualTo(new Color(31, 35, 38, OPAQUE_ALPHA));
        }

        @Test
        void resolveLitFillReturnsAnOpaqueSurface() {
            // A tab fill is a surface: left translucent the row would be the colour of whatever it is
            // drawn over, which is exactly what standing over the bare map would expose.
            assertThat(VanillaTabFills.resolveLitFill(BUTTON_BG_DARK, BUTTON_TEXT, BACKDROP).getAlpha())
                .isEqualTo(OPAQUE_ALPHA);
        }
    }
}
