package kmlib.starsector.ui.widgets.tabs;

import com.fs.starfarer.api.util.Misc;

import kmlib.starsector.testing.StarsectorSettingsFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link TabPalette}'s two lookups - every state reaches its own value and no other - and the map-tab
 * factory's one derived shade. A palette is four roles of the same two shapes, so a pair swapped in either
 * switch would still compile and still paint - a selected tab wearing the resting shade, or a click lifting
 * toward the hovered fill - which is exactly the confusion these assertions rule out.
 *
 * <p>The lookups are exercised against literal shades; the factory is exercised against a stubbed engine
 * palette, since it resolves through a live one a unit test has no sector to supply.
 */
final class TabPaletteTest {

    private static final Color CHROME_ACCENT = new Color(10, 10, 10);
    private static final TabLook UNSELECTED_LOOK = new TabLook(
        new Color(20, 20, 20),
        new Color(30, 30, 30));
    private static final TabLook SELECTED_LOOK = new TabLook(
        new Color(40, 40, 40),
        new Color(50, 50, 50));
    private static final TabLook HOVERED_LOOK = new TabLook(
        new Color(60, 60, 60),
        new Color(70, 70, 70));

    // A target no look names, so a lift lookup answering off a look's fill shows as a wrong colour rather
    // than as the right colour at a different depth.
    private static final TabWash CLICKED_WASH = new TabWash(new Color(80, 80, 80), 0.2f);

    // Named so a hover lookup reads as "the lit tab" / "a resting tab" rather than as a bare flag.
    private static final boolean SELECTED = true;
    private static final boolean UNSELECTED = false;

    // A fully opaque alpha channel, so a fill assertion reads as "a surface" rather than as a bare 255.
    private static final int OPAQUE_ALPHA = 255;

    private static final TabPalette PALETTE = new TabPalette(
        CHROME_ACCENT,
        UNSELECTED_LOOK,
        SELECTED_LOOK,
        HOVERED_LOOK,
        CLICKED_WASH);

    // The player-tinted roles the map-tab factory reads, stubbed to flat shades: only the bright player
    // colour reaches an assertion (it is the selected label the hovered one is lifted from), and the
    // other two are stubbed because an unstubbed static returns null, which the palette rejects outright.
    private static final Color STUBBED_PLAYER_BASE = new Color(20, 40, 60);
    private static final Color STUBBED_PLAYER_BRIGHT = new Color(100, 100, 100);
    private static final Color STUBBED_BUTTON_TEXT = new Color(180, 180, 180);

    // The engine's dark button fill, stubbed at the settings key the fills are built from. A shade unlike
    // every other stub here and unlike white, so a fill that reached for the wrong source - or that
    // skipped the composite and took the raw value - comes out a number this test does not expect.
    private static final String BUTTON_BG_DARK_KEY = "buttonBgDark";
    private static final Color STUBBED_BUTTON_BG_DARK = new Color(31, 94, 112, 175);

    // The map-tab palette built with the engine's player-tinted roles stubbed out. The settings proxy goes
    // in before Mockito touches Misc, whose static initialiser reads it; the palette is resolved inside the
    // stub's scope, since every role is read as the record is built.
    private static TabPalette buildMapTabPaletteUnderStubbedEngine() {

        StarsectorSettingsFake.installSettings(
            StarsectorSettingsFake.EMPTY_STRINGS,
            key -> BUTTON_BG_DARK_KEY.equals(key)
                ? STUBBED_BUTTON_BG_DARK
                : null);
        try (var miscMock = Mockito.mockStatic(Misc.class)) {

            miscMock
                .when(Misc::getBasePlayerColor)
                .thenReturn(STUBBED_PLAYER_BASE);
            miscMock
                .when(Misc::getBrightPlayerColor)
                .thenReturn(STUBBED_PLAYER_BRIGHT);
            miscMock
                .when(Misc::getButtonTextColor)
                .thenReturn(STUBBED_BUTTON_TEXT);

            return TabPalette.createMapTabPalette();

        } finally {
            StarsectorSettingsFake.clearSettings();
        }
    }

    @Nested
    class ResolveLook {

        @Test
        void resolveLookReturnsTheRestingShadeWhenUnselected() {
            assertThat(PALETTE.resolveLook(TabLookState.UNSELECTED))
                .isEqualTo(new TabLook(new Color(20, 20, 20), new Color(30, 30, 30)));
        }

        @Test
        void resolveLookReturnsTheLitShadeWhenSelected() {
            assertThat(PALETTE.resolveLook(TabLookState.SELECTED))
                .isEqualTo(new TabLook(new Color(40, 40, 40), new Color(50, 50, 50)));
        }

        @Test
        void resolveLookReturnsTheHoveredShadeWhenHovered() {
            // The one shade the resting and the selected tab both meet at under the pointer, so it
            // answers off its own role rather than being derived from either of theirs.
            assertThat(PALETTE.resolveLook(TabLookState.HOVERED))
                .isEqualTo(new TabLook(new Color(60, 60, 60), new Color(70, 70, 70)));
        }
    }

    @Nested
    class ResolveLookAtHoverFraction {

        @Test
        void resolveLookAtHoverFractionReturnsTheSettledLookWhenFullyOffTheHover() {
            assertThat(PALETTE.resolveLookAtHoverFraction(UNSELECTED, 0f))
                .isEqualTo(new TabLook(new Color(20, 20, 20), new Color(30, 30, 30)));
            assertThat(PALETTE.resolveLookAtHoverFraction(SELECTED, 0f))
                .isEqualTo(new TabLook(new Color(40, 40, 40), new Color(50, 50, 50)));
        }

        @Test
        void resolveLookAtHoverFractionBringsBothTabsToTheHoveredShadeWhenFullyOnIt() {
            // The resting and the lit tab meeting at one shade is what makes hovering a look rather than a
            // lift, so the two must arrive at the same value rather than merely both brightening.
            assertThat(PALETTE.resolveLookAtHoverFraction(UNSELECTED, 1f))
                .isEqualTo(new TabLook(new Color(60, 60, 60), new Color(70, 70, 70)));
            assertThat(PALETTE.resolveLookAtHoverFraction(SELECTED, 1f))
                .isEqualTo(new TabLook(new Color(60, 60, 60), new Color(70, 70, 70)));
        }

        @Test
        void resolveLookAtHoverFractionPlacesAPartWayTabBetweenItsOwnLookAndTheHoveredShade() {
            // Part-way is where the two tabs are still apart, so each has to travel from its own end
            // rather than from a shared one. A quarter of the way rather than half, so neither result
            // coincides with another role's shade and a look read off the wrong end shows as a wrong
            // number.
            assertThat(PALETTE.resolveLookAtHoverFraction(UNSELECTED, 0.25f))
                .isEqualTo(new TabLook(new Color(30, 30, 30), new Color(40, 40, 40)));
            assertThat(PALETTE.resolveLookAtHoverFraction(SELECTED, 0.25f))
                .isEqualTo(new TabLook(new Color(45, 45, 45), new Color(55, 55, 55)));
        }
    }

    @Nested
    class ResolveWash {

        @Test
        void resolveWashReturnsTheClickLiftWhenClicked() {
            // The lift channel's only role, so this pins that it answers off the wash rather than off one of
            // the looks the same palette carries - both being colours a lift could plausibly be built from.
            assertThat(PALETTE.resolveWash(TabWashState.CLICKED))
                .isEqualTo(new TabWash(new Color(80, 80, 80), 0.2f));
        }
    }

    @Nested
    class CreateMapTabPalette {

        @Test
        void createMapTabPaletteCompositesTheRestingFillFromTheEnginesButtonFill() {
            // The dark button fill (31, 94, 112 at alpha 175) laid over black - the shade a vanilla tab
            // rests at, taken from the engine's own colour rather than named here, so a restyled install
            // moves this strip exactly as it moves the tabs beside it.
            assertThat(buildMapTabPaletteUnderStubbedEngine().unselected().fill())
                .isEqualTo(new Color(21, 65, 77, OPAQUE_ALPHA));
        }

        @Test
        void createMapTabPaletteLightsTheSelectedFillAtTheShownTabsGlow() {
            // The resting shade with the shown tab's glow on top: the label colour (180, 180, 180 here)
            // half way to white is (218, 218, 218), added at 0.45 * 0.5 * (175 + 50) / 255 = 0.1985 - so
            // (21, 65, 77) gains 43 on every channel. A selected tab that merely re-tinted its resting
            // fill could not land here; the glow is added light, not a blend.
            assertThat(buildMapTabPaletteUnderStubbedEngine().selected().fill())
                .isEqualTo(new Color(64, 108, 120, OPAQUE_ALPHA));
        }

        @Test
        void createMapTabPaletteLightsTheHoveredFillAtTheFullGlow() {
            // The same shade at the whole glow (0.441), which stands above the shown tab's - the ordering
            // a strip marking selection by fill alone rests on.
            var palette = buildMapTabPaletteUnderStubbedEngine();

            assertThat(palette.hovered().fill())
                .isEqualTo(new Color(117, 161, 173, OPAQUE_ALPHA));
            assertThat(palette.hovered().fill().getBlue())
                .isGreaterThan(palette.selected().fill().getBlue());
        }

        @Test
        void createMapTabPaletteLeavesEveryFillOpaque() {
            // A tab is a surface, not a tint over one: a see-through fill would read as whatever the strip
            // happens to be drawn over, so the row would change shade with its surroundings and show the
            // map through itself wherever no panel sits beneath it.
            var palette = buildMapTabPaletteUnderStubbedEngine();

            assertThat(palette.unselected().fill().getAlpha())
                .isEqualTo(OPAQUE_ALPHA);
            assertThat(palette.selected().fill().getAlpha())
                .isEqualTo(OPAQUE_ALPHA);
            assertThat(palette.hovered().fill().getAlpha())
                .as("the hovered shade is derived from the selected one, so it inherits its opacity")
                .isEqualTo(OPAQUE_ALPHA);
        }

        @Test
        void createMapTabPaletteLiftsTheSelectedLabelByTheSameAmount() {
            // The label travels with its fill, so a hovered tab brightens as one piece; the bright player
            // colour stands in for the live palette here, lifted by the same fraction as the fill above.
            assertThat(buildMapTabPaletteUnderStubbedEngine().hovered().label())
                .isEqualTo(new Color(123, 123, 123, 255));
        }

        @Test
        void createMapTabPaletteAimsThePressLiftAlongTheGlowRatherThanAtWhite() {
            // The direction a press travels, which is the whole of why it reads as one of the engine's own
            // states rather than as ours: the engine brightens a tab by adding its glow - the label colour
            // (180, 180, 180 here) half way to white - so a lift aimed at white would be the one shade on
            // the strip moving somewhere none of the fills do, and it would show exactly when the player is
            // looking at it. A depth alone could not correct that, so the target is pinned and not just the
            // strength.
            assertThat(buildMapTabPaletteUnderStubbedEngine().clicked().target())
                .isEqualTo(new Color(218, 218, 218));
        }

        @Test
        void createMapTabPaletteLiftsAPressOnlyPartWayAlongThatGlow() {
            // Modest because it is measured along the same axis the fills are: a press has only to stand
            // above the pointed-at tab it is necessarily already showing.
            assertThat(buildMapTabPaletteUnderStubbedEngine().clicked().strength())
                .isEqualTo(0.25f);
        }
    }
}
