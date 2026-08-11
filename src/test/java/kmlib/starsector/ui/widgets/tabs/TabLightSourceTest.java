package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.widgets.tabs.style.TabHover;
import kmlib.starsector.ui.widgets.tabs.style.TabLight;
import kmlib.starsector.ui.widgets.tabs.style.TabLook;
import kmlib.starsector.ui.widgets.tabs.style.TabPalette;
import kmlib.starsector.ui.widgets.tabs.style.TabWash;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the hover-lit {@link TabLightSource}: the binding where a panel's live hover fractions meet the
 * palette's own pointer rule. It answers per tab, so the row and its lights have to stay in step; and it
 * answers nothing at all for a palette that brightens by shade, which is what keeps a strip from being lit
 * twice over - once in the look it has already travelled to, and again by a pass laid on top.
 *
 * <p>The palettes are literal rather than built through the factories, which resolve through a live engine
 * palette a unit test has no sector to supply.
 */
final class TabLightSourceTest {

    private static final float TOLERANCE = 0.001f;

    private static final Color STAND_IN_SHADE = Color.GRAY;
    private static final TabLook STAND_IN_LOOK = new TabLook(STAND_IN_SHADE, STAND_IN_SHADE);
    private static final TabWash STAND_IN_NO_LIFT = new TabWash(STAND_IN_SHADE, 0f);

    // The light a glowing palette adds, and how much of it at a full hover. A colour unlike the stand-in
    // shade, so a source answering off a look rather than off the pointer rule shows as a wrong colour.
    private static final Color GLOW_COLOUR = new Color(100, 200, 40);
    private static final float GLOW_AMOUNT = 0.4f;

    // Indices past either end of a row, since a source is asked per tab and must not assume the row it is
    // being walked over.
    private static final int FIRST_INDEX = 0;
    private static final int LATER_INDEX = 4;
    private static final int NO_TAB_INDEX = -1;

    private static final TabPalette GLOWING_PALETTE = new TabPalette(
        STAND_IN_SHADE,
        STAND_IN_SHADE,
        STAND_IN_LOOK,
        STAND_IN_LOOK,
        new TabHover.AddedGlow(GLOW_COLOUR, GLOW_AMOUNT),
        STAND_IN_NO_LIFT);

    private static final TabPalette SHADED_PALETTE = new TabPalette(
        STAND_IN_SHADE,
        STAND_IN_SHADE,
        STAND_IN_LOOK,
        STAND_IN_LOOK,
        new TabHover.MeetingShade(STAND_IN_LOOK),
        STAND_IN_NO_LIFT);

    @Nested
    class CreateHoverLitSource {

        @Test
        void createHoverLitSourceLightsEachTabByHowFarItsOwnFadeHasRun() {
            // Per tab, off that tab's own fraction: one light shared across the row would leave a
            // neighbour lit by the pointer resting on the tab beside it.
            var lights = TabLightSource.createHoverLitSource(
                GLOWING_PALETTE,
                tabIndex -> tabIndex == FIRST_INDEX ? 1f : 0.5f);

            assertThat(lights.resolveLightAt(FIRST_INDEX).weight())
                .isCloseTo(GLOW_AMOUNT, within(TOLERANCE));
            assertThat(lights.resolveLightAt(LATER_INDEX).weight())
                .isCloseTo(GLOW_AMOUNT / 2f, within(TOLERANCE));
        }

        @Test
        void createHoverLitSourceLightsEveryTabInTheOnePaletteColour() {
            // Only the weight follows the fade. A colour that moved with it would walk the light through
            // shades the palette never named on the way to the one it did.
            var lights = TabLightSource.createHoverLitSource(GLOWING_PALETTE, tabIndex -> 0.5f);

            assertThat(lights.resolveLightAt(FIRST_INDEX).colour())
                .isEqualTo(GLOW_COLOUR);
        }

        @Test
        void createHoverLitSourceAddsNothingToAnUntouchedTab() {
            // The resting row has to be exactly unlit rather than faintly lit, or every tab on screen would
            // stand a step above the palette it was built from.
            var lights = TabLightSource.createHoverLitSource(
                GLOWING_PALETTE,
                TabHoverSource.createRestingHoverSource());

            assertThat(lights.resolveLightAt(FIRST_INDEX).isLit())
                .isFalse();
        }

        @Test
        void createHoverLitSourceAddsNothingForAPaletteThatBrightensByShade() {
            // A strip's tabs travel to the shade they are meant to reach, so light over the top would carry
            // them past it - and past it is where a hovered tab stops being told from the shown one.
            var lights = TabLightSource.createHoverLitSource(SHADED_PALETTE, tabIndex -> 1f);

            assertThat(lights.resolveLightAt(FIRST_INDEX))
                .isEqualTo(TabLight.NONE);
        }

        @Test
        void createHoverLitSourceAnswersForAnIndexOutsideTheRow() {
            // Asked per tab by whoever is walking a row, so it cannot assume the row: an index off either
            // end has to answer rather than throw.
            var lights = TabLightSource.createHoverLitSource(GLOWING_PALETTE, tabIndex -> 0f);

            assertThat(lights.resolveLightAt(NO_TAB_INDEX).isLit())
                .isFalse();
        }
    }

    @Nested
    class CreateUnlitSource {

        @Test
        void createUnlitSourceLightsNoTabAtAll() {
            // What a row drawn with no animator behind it takes, and the reason a chrome needs no separate
            // path for one: it is handed a light that draws nothing rather than nothing at all.
            var lights = TabLightSource.createUnlitSource();

            assertThat(lights.resolveLightAt(FIRST_INDEX))
                .isEqualTo(TabLight.NONE);
            assertThat(lights.resolveLightAt(LATER_INDEX))
                .isEqualTo(TabLight.NONE);
        }
    }
}
