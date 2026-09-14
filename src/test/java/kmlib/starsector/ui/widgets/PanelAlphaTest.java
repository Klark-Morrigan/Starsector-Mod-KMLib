package kmlib.starsector.ui.widgets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the one thing that makes two alphas worth carrying instead of one: the body honours both
 * channels and the chrome honours only the fade.
 *
 * <p>That asymmetry is the whole design, and it is invisible at rest - a panel wholly present with an
 * opaque body reports the same number from both reads, which is every frame outside a fade. So the
 * cases that separate them are a translucent body and a fade in progress, and a fade over a
 * translucent body is where a channel dropped from either read shows up as a number that happens to
 * look plausible.
 */
class PanelAlphaTest {

    private static final float TOLERANCE = 0.0001f;

    @Nested
    class ResolveBodyAlpha {

        @Test
        void resolveBodyAlphaCarriesTheLookOfAPanelWhollyPresent() {

            assertThat(new PanelAlpha(0.6f).resolveBodyAlpha())
                .isCloseTo(0.6f, within(TOLERANCE));
        }

        @Test
        void resolveBodyAlphaCompoundsTheFadeOntoTheLook() {
            // Both channels, which is what a fading translucent panel needs: a body already half
            // see-through, half way out, is a quarter there.
            assertThat(new PanelAlpha(0.5f, 0.5f).resolveBodyAlpha())
                .isCloseTo(0.25f, within(TOLERANCE));
        }

        @Test
        void resolveBodyAlphaIsNothingForAPanelFullyFadedOut() {

            assertThat(new PanelAlpha(1f, 0f).resolveBodyAlpha())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ResolveChromeAlpha {

        @Test
        void resolveChromeAlphaIgnoresTheBodysOwnOpacity() {
            // The reason chrome has a channel of its own: a tab row stands opaque on a see-through
            // body, so a row taking the body's opacity would read as one uniformly faint sheet.
            assertThat(new PanelAlpha(0.25f).resolveChromeAlpha())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void resolveChromeAlphaFollowsThePanelOutOfTheFrame() {
            // And the reason it does not ignore the fade as well: a row left at full strength over a
            // dissolving body would hang there, which reads worse than no fade at all.
            assertThat(new PanelAlpha(1f, 0.4f).resolveChromeAlpha())
                .isCloseTo(0.4f, within(TOLERANCE));
        }

        @Test
        void resolveChromeAlphaStillIgnoresTheBodysOpacityMidFade() {
            // The case a single compounded alpha would get wrong: the row is 40% of the way out
            // because the panel is, not because the body beneath it is a quarter see-through.
            assertThat(new PanelAlpha(0.25f, 0.4f).resolveChromeAlpha())
                .isCloseTo(0.4f, within(TOLERANCE));
        }
    }

    @Nested
    class WithOpaqueBody {

        @Test
        void withOpaqueBodyLeavesBothChannelsOnTheFadeAlone() {
            // What a surface standing wholly opaque on the body is handed: it keeps both channels, so
            // whatever it draws inside can go on telling body paint from chrome paint, but neither of
            // them is any longer discounted by a translucency that surface does not wear.
            var opaqueBody = new PanelAlpha(0.25f, 0.4f).withOpaqueBody();

            assertThat(opaqueBody.resolveBodyAlpha())
                .isCloseTo(0.4f, within(TOLERANCE));
            assertThat(opaqueBody.resolveChromeAlpha())
                .isCloseTo(0.4f, within(TOLERANCE));
        }

        @Test
        void withOpaqueBodyKeepsThePanelsOwnFade() {
            // The half it must not drop: a surface opting out of the look still leaves with the panel.
            assertThat(new PanelAlpha(1f, 0f).withOpaqueBody().resolveChromeAlpha())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }
}
