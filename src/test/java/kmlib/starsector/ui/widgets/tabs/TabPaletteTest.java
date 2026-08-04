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
 * factory's one derived shade. A palette is five roles of the same two shapes, so a pair swapped in either
 * switch would still compile and still paint - a selected tab wearing the resting shade, or a click
 * lifting by the hotkey blink's depth - which is exactly the confusion these assertions rule out.
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

    // One distinct target per pulse role, so a lookup returning the wrong wash names the wrong colour
    // rather than the same colour at a different depth.
    private static final TabWash CLICKED_WASH = new TabWash(new Color(80, 80, 80), 0.2f);
    private static final TabWash HOTKEYED_WASH = new TabWash(new Color(90, 90, 90), 0.3f);

    private static final TabPalette PALETTE = new TabPalette(
        CHROME_ACCENT,
        UNSELECTED_LOOK,
        SELECTED_LOOK,
        HOVERED_LOOK,
        CLICKED_WASH,
        HOTKEYED_WASH);

    // The player-tinted roles the map-tab factory reads, stubbed to flat shades: only the bright player
    // colour reaches an assertion (it is the selected label the hovered one is lifted from), and the
    // other two are stubbed because an unstubbed static returns null, which the palette rejects outright.
    private static final Color STUBBED_PLAYER_BASE = new Color(20, 40, 60);
    private static final Color STUBBED_PLAYER_BRIGHT = new Color(100, 100, 100);
    private static final Color STUBBED_BUTTON_TEXT = new Color(180, 180, 180);

    // The map-tab palette built with the engine's player-tinted roles stubbed out. The settings proxy goes
    // in before Mockito touches Misc, whose static initialiser reads it; the palette is resolved inside the
    // stub's scope, since every role is read as the record is built.
    private static TabPalette buildMapTabPaletteUnderStubbedEngine() {
        
        StarsectorSettingsFake.installSettings();
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
    class ResolveWash {

        @Test
        void resolveWashReturnsTheClickLiftWhenClicked() {
            assertThat(PALETTE.resolveWash(TabWashState.CLICKED).target())
                .isEqualTo(new Color(80, 80, 80));
        }

        @Test
        void resolveWashReturnsTheHotkeyLiftWhenHotkeyed() {
            assertThat(PALETTE.resolveWash(TabWashState.HOTKEYED).target())
                .isEqualTo(new Color(90, 90, 90));
        }
    }

    @Nested
    class CreateMapTabPalette {

        @Test
        void createMapTabPaletteKeepsTheSampledSteelBlueAsTheSelectedFill() {
            // The shade measured off the real map tabs, and the one the hovered fill below is lifted
            // from - so a change to it is a deliberate retune of the vanilla match, not a side effect.
            assertThat(buildMapTabPaletteUnderStubbedEngine().selected().fill())
                .isEqualTo(new Color(105, 179, 206, 175));
        }

        @Test
        void createMapTabPaletteLiftsTheSelectedFillIntoTheHoveredFill() {
            // The selected fill moved a small way toward white, which is what lets the resting tab travel
            // the whole way up to meet it: writing this shade by hand instead would let the two drift.
            assertThat(buildMapTabPaletteUnderStubbedEngine().hovered().fill())
                .isEqualTo(new Color(128, 190, 213, 175));
        }

        @Test
        void createMapTabPaletteLiftsTheSelectedLabelByTheSameAmount() {
            // The label travels with its fill, so a hovered tab brightens as one piece; the bright player
            // colour stands in for the live palette here, lifted by the same fraction as the fill above.
            assertThat(buildMapTabPaletteUnderStubbedEngine().hovered().label())
                .isEqualTo(new Color(123, 123, 123, 255));
        }
    }
}
