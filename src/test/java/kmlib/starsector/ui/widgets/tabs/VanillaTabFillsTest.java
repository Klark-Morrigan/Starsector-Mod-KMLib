package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shades a vanilla tab settles on against the engine's own values: vanilla's {@code
 * buttonBgDark} and {@code buttonText}, over the dark backing its tabs stand on. The expected colours are
 * written down, so a change to the rule shows as a shade that no longer matches the tabs this strip sits
 * beside rather than as arithmetic agreeing with itself.
 *
 * <p>The ordering of the three glow amounts is pinned as its own case: a strip that marks the shown tab
 * by fill alone depends on the pointed-at tab outshining it, so the day the amounts are retuned is the
 * day that has to fail rather than quietly leaving two states looking alike.
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
    class ResolveFillAtGlow {

        @Test
        void resolveFillAtGlowLightsTheShownTabShortOfTheFullGlow() {
            // The resting shade (21, 65, 77) plus 0.85 of the glow: buttonText half-way to white,
            // (213, 239, 255), at half strength tempered by the fill's alpha - 0.85 * 0.5 * (175 + 50) /
            // 255 = 0.375.
            assertThat(resolveFillAt(VanillaTabFills.SELECTED_GLOW))
                .isEqualTo(new Color(101, 155, 173, OPAQUE_ALPHA));
        }

        @Test
        void resolveFillAtGlowLightsThePointedTabAtTheFullGlow() {
            // The same shade at the whole glow - 0.5 * (175 + 50) / 255 = 0.441 - which is where the tab
            // under the pointer stands.
            assertThat(resolveFillAt(VanillaTabFills.POINTED_GLOW))
                .isEqualTo(new Color(115, 170, 190, OPAQUE_ALPHA));
        }

        @Test
        void resolveFillAtGlowLeavesAnUnlitTabAtItsRestingShade() {
            // No glow is the same statement as no glow pass at all, so the two ways of asking agree.
            assertThat(resolveFillAt(VanillaTabFills.NO_GLOW))
                .isEqualTo(VanillaTabFills.resolveRestingFill(BUTTON_BG_DARK, BACKDROP));
        }

        @Test
        void resolveFillAtGlowKeepsThePointedTabBrighterThanTheShownOne() {
            // The load-bearing ordering: nothing but the fill marks the shown tab, so a pointed-at tab has
            // to outshine it on every channel or the two states read alike while the pointer is on the row.
            var resting = resolveFillAt(VanillaTabFills.NO_GLOW);
            var selected = resolveFillAt(VanillaTabFills.SELECTED_GLOW);
            var pointed = resolveFillAt(VanillaTabFills.POINTED_GLOW);

            assertThat(selected.getRed()).isGreaterThan(resting.getRed());
            assertThat(selected.getGreen()).isGreaterThan(resting.getGreen());
            assertThat(selected.getBlue()).isGreaterThan(resting.getBlue());

            assertThat(pointed.getRed()).isGreaterThan(selected.getRed());
            assertThat(pointed.getGreen()).isGreaterThan(selected.getGreen());
            assertThat(pointed.getBlue()).isGreaterThan(selected.getBlue());
        }

        @Test
        void resolveFillAtGlowTakesTheFullGlowOnceTheFillIsSolidEnough() {
            // The headroom means an opaque fill and one a little short of it take the same glow. Asked of
            // a black fill so only the glow is left standing: any coloured fill composites to a different
            // resting shade at each alpha, which would move the answer for a reason this is not about.
            assertThat(resolveBlackFillAt(205, VanillaTabFills.POINTED_GLOW))
                .isEqualTo(resolveBlackFillAt(255, VanillaTabFills.POINTED_GLOW));
        }

        @Test
        void resolveFillAtGlowDimsTheGlowOnASeeThroughFill() {
            // Below the headroom the glow is scaled down with the fill, so a barely-there fill does not
            // light up as strongly as a solid one. Black again, for the reason above: (213, 239, 255) at
            // 0.5 * (25 + 50) / 255 = 0.147 rather than at the full half.
            assertThat(resolveBlackFillAt(25, VanillaTabFills.POINTED_GLOW))
                .isEqualTo(new Color(31, 35, 38, OPAQUE_ALPHA));
        }

        @Test
        void resolveFillAtGlowReturnsAnOpaqueSurface() {
            // A tab fill is a surface: left translucent the row would be the colour of whatever it is
            // drawn over, which is exactly what standing over the bare map would expose.
            assertThat(resolveFillAt(VanillaTabFills.POINTED_GLOW).getAlpha())
                .isEqualTo(OPAQUE_ALPHA);
        }
    }

    // The engine's own fill and label at the given glow, the pairing every case above varies only the
    // glow of.
    private static Color resolveFillAt(float glowAmount) {
        return VanillaTabFills.resolveFillAtGlow(BUTTON_BG_DARK, BUTTON_TEXT, BACKDROP, glowAmount);
    }

    // A black fill at the given alpha, for the cases about how solid the fill is: with nothing under the
    // glow, what comes back is the glow alone.
    private static Color resolveBlackFillAt(int fillAlpha, float glowAmount) {
        return VanillaTabFills.resolveFillAtGlow(
            new Color(0, 0, 0, fillAlpha),
            BUTTON_TEXT,
            BACKDROP,
            glowAmount);
    }
}
