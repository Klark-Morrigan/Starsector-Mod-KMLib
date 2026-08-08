package kmlib.starsector.ui.widgets.tabs.style;

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

    // The other end of the same channel, for the one chrome whose resting interior is not painted at all.
    private static final int UNPAINTED_ALPHA = 0;

    // A lift that moves a tab nowhere, for the chrome copying an engine control with no pressed state.
    private static final float NO_LIFT = 0f;

    private static final TabPalette PALETTE = new TabPalette(
        CHROME_ACCENT,
        UNSELECTED_LOOK,
        SELECTED_LOOK,
        HOVERED_LOOK,
        CLICKED_WASH);

    // The two engine roles the map-tab factory reads: the button text every fill is worked out from and an
    // untouched tab reads its label in, and the standard text a lit one switches to. Its chrome accent
    // arrives as an argument instead, so nothing else about the engine's palette reaches this factory.
    private static final Color STUBBED_BUTTON_TEXT = new Color(180, 180, 180);

    // Unlike the button text on every channel, so a label taking the wrong role of the two comes out a
    // number these cases do not expect rather than a shade near the one they do.
    private static final Color STUBBED_STANDARD_TEXT = new Color(220, 210, 200);

    // The accent handed to the factory. Unlike every stubbed engine shade here, so an accent read off the
    // engine rather than off the argument comes out a number these cases do not expect.
    private static final Color SUPPLIED_CHROME_ACCENT = new Color(20, 40, 60);

    // The engine's dark button fill, stubbed at the settings key the fills are built from. A shade unlike
    // every other stub here and unlike white, so a fill that reached for the wrong source - or that
    // skipped the composite and took the raw value - comes out a number this test does not expect.
    private static final String BUTTON_BG_DARK_KEY = "buttonBgDark";
    private static final Color STUBBED_BUTTON_BG_DARK = new Color(31, 94, 112, 175);

    // The three steps of the accent a raised-button row is built from. Handed in rather than stubbed: a
    // vanilla button takes its whole look from an accent set, so that factory reads no engine role at all.
    // The stock install's own dark and base steps, so the shades below are the ones the engine's own
    // buttons wear; the lit label is a shade unlike either, so a label taking the wrong step shows as a
    // number this test does not expect.
    private static final Color BUTTON_DARK_ACCENT = new Color(31, 94, 112, 175);
    private static final Color BUTTON_BASE_ACCENT = new Color(165, 230, 255);
    private static final Color BUTTON_LIT_LABEL = new Color(203, 245, 255);

    // The map-tab palette built with the engine's own roles stubbed out. The settings proxy goes in before
    // Mockito touches Misc, whose static initialiser reads it; the palette is resolved inside the stub's
    // scope, since every role is read as the record is built.
    private static TabPalette buildMapTabPaletteUnderStubbedEngine() {

        StarsectorSettingsFake.installSettings(
            StarsectorSettingsFake.EMPTY_STRINGS,
            key -> BUTTON_BG_DARK_KEY.equals(key)
                ? STUBBED_BUTTON_BG_DARK
                : null);
        try (var miscMock = Mockito.mockStatic(Misc.class)) {

            miscMock
                .when(Misc::getButtonTextColor)
                .thenReturn(STUBBED_BUTTON_TEXT);
            miscMock
                .when(Misc::getTextColor)
                .thenReturn(STUBBED_STANDARD_TEXT);

            // Both player colours are deliberately left unstubbed. Nothing in this palette answers to the
            // player's faction any more - a tab's label is one of the engine's own two text roles, and the
            // chrome accent arrives as an argument - and an unstubbed static returns null, which the
            // palette rejects outright. Reaching for either again therefore fails loudly here rather than
            // shipping a strip that recolours with whichever faction the player flies.

            return TabPalette.createMapTabPalette(SUPPLIED_CHROME_ACCENT);

        } finally {
            StarsectorSettingsFake.clearSettings();
        }
    }

    // The raised-button palette over the three accent steps above, the pairing its cases all read. No
    // engine stub around it: a vanilla button is built from an accent set and reads no settings role, so
    // the factory is handed everything it paints from.
    private static TabPalette buildRaisedButtonPalette() {
        return TabPalette.createRaisedButtonPalette(
            BUTTON_DARK_ACCENT,
            BUTTON_BASE_ACCENT,
            BUTTON_LIT_LABEL);
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
        void createMapTabPaletteRulesTheRowInTheAccentItIsHanded() {
            // The one value with no vanilla counterpart to copy, so it comes from the panel the row
            // belongs to. Pinned because the factory reads the engine for everything else, and an accent
            // that quietly went back to reading it would look right on a stock install and wrong on any
            // other.
            assertThat(buildMapTabPaletteUnderStubbedEngine().chromeAccent())
                .isEqualTo(SUPPLIED_CHROME_ACCENT);
        }

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
        void createMapTabPaletteLightsTheHoveredFillAboveTheShownTabs() {
            // The same shade at the pointer's amount (0.2868), which stands above the shown tab's - the
            // ordering a strip marking selection by fill alone rests on.
            var palette = buildMapTabPaletteUnderStubbedEngine();

            assertThat(palette.hovered().fill())
                .isEqualTo(new Color(84, 128, 140, OPAQUE_ALPHA));
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
        void createMapTabPaletteReadsAnUntouchedTabsLabelInTheButtonText() {
            // The blue every button's text is, which an untouched tab wears as it comes - and the reason a
            // deselected tab is the only state reading as plain blue in game.
            assertThat(buildMapTabPaletteUnderStubbedEngine().unselected().label())
                .isEqualTo(STUBBED_BUTTON_TEXT);
        }

        @Test
        void createMapTabPaletteSwitchesALitTabsLabelToTheStandardText() {
            // The engine parts a lit tab's label from a resting one by colour, not by amount: the shown tab
            // reads in the grey the rest of the interface reads in. Measured off its own Sector/System
            // tabs, whose lit label cannot be reconciled with any lifted form of the button blue - which is
            // exactly what a rule computing one from the other produced, and why there is no such rule now.
            assertThat(buildMapTabPaletteUnderStubbedEngine().selected().label())
                .isEqualTo(STUBBED_STANDARD_TEXT);
        }

        @Test
        void createMapTabPaletteReadsBothLitTabsLabelsAlike() {
            // The two lit states are told apart by their fills, which stand at different glows, rather than
            // by their text. Only the shown tab's label was measured, so the pointed-at one matching it is
            // the smaller claim - and pinning them equal is what makes a future divergence deliberate.
            var palette = buildMapTabPaletteUnderStubbedEngine();

            assertThat(palette.hovered().label())
                .isEqualTo(palette.selected().label());
        }
    }

    @Nested
    class CreateRaisedButtonPalette {

        @Test
        void createRaisedButtonPaletteRulesTheRowInTheDarkStepItIsHanded() {
            // The engine frames its own buttons in the dark member of the accent it builds them from, so
            // this chrome's rule is that step rather than a shade chosen beside it - which is the whole of
            // why the frame stops moving with the fill.
            assertThat(buildRaisedButtonPalette().chromeAccent())
                .isEqualTo(BUTTON_DARK_ACCENT);
        }

        @Test
        void createRaisedButtonPaletteLeavesARestingButtonsInteriorUnpainted() {
            // The departure from every other palette here, and the one the chrome reads as "draw nothing":
            // a button not being shown is the backing and the frame with no interior, stated as a fill at
            // zero alpha rather than as a state the paint pass tests for.
            assertThat(buildRaisedButtonPalette().unselected().fill().getAlpha())
                .isEqualTo(UNPAINTED_ALPHA);
        }

        @Test
        void createRaisedButtonPaletteFillsTheShownButtonWithTheDarkStepOverTheBacking() {
            // The dark step (31, 94, 112 at alpha 175) composited onto black - what the engine's own lit
            // buttons wear, taken from the accent handed in rather than named here.
            assertThat(buildRaisedButtonPalette().selected().fill())
                .isEqualTo(new Color(21, 65, 77, OPAQUE_ALPHA));
        }

        @Test
        void createRaisedButtonPaletteLightsThePointedButtonWithItsBaseStep() {
            // The shown button's interior plus 0.175 of the base accent undiluted, which is how a vanilla
            // button brightens - not the whitened glow a tab takes.
            assertThat(buildRaisedButtonPalette().hovered().fill())
                .isEqualTo(new Color(50, 105, 122, OPAQUE_ALPHA));
        }

        @Test
        void createRaisedButtonPaletteReadsAnUntouchedButtonsLabelInTheBaseStep() {
            // The colour the engine builds a button's text from, which an untouched one wears as it comes.
            assertThat(buildRaisedButtonPalette().unselected().label())
                .isEqualTo(BUTTON_BASE_ACCENT);
        }

        @Test
        void createRaisedButtonPaletteSwitchesALitButtonsLabelToTheBrighterStep() {
            // Both lit states, as on the strip: the labels part by colour, and the fills - which stand at
            // different glows - are what tell the shown button from the pointed-at one.
            var palette = buildRaisedButtonPalette();

            assertThat(palette.selected().label())
                .isEqualTo(BUTTON_LIT_LABEL);
            assertThat(palette.hovered().label())
                .isEqualTo(BUTTON_LIT_LABEL);
        }

        @Test
        void createRaisedButtonPaletteAnswersAPressWithNoLiftAtAll() {
            // The engine's own intel buttons hold no shade while the pointer is down. Stated as a depth of
            // zero rather than as a wash the chrome declines to paint, so the pulse still runs and the
            // press still sounds - the palette says what a press looks like, not whether one happened.
            assertThat(buildRaisedButtonPalette().resolveWash(TabWashState.CLICKED).strength())
                .isEqualTo(NO_LIFT);
        }
    }
}
