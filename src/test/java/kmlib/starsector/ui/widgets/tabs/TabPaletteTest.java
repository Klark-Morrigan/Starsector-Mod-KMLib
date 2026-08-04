package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link TabPalette}'s two lookups: every state reaches its own value and no other. A palette is
 * five roles of the same two shapes, so a pair swapped in either switch would still compile and still
 * paint - a selected tab wearing the resting shade, or a click lifting by the hotkey blink's depth -
 * which is exactly the confusion these assertions rule out.
 *
 * <p>The palette is built from literal shades rather than {@link TabPalette#createMapTabPalette()},
 * which resolves through the live engine palette a unit test has no sector to supply.
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
}
