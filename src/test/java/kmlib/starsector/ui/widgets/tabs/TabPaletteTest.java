package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link TabPalette}'s two lookups: every state reaches its own value and no other. A palette is
 * six roles of the same two shapes, so a pair swapped in either switch would still compile and still
 * paint - a selected tab wearing the resting fill, or a click lifting by the hover's depth - which is
 * exactly the confusion these assertions rule out.
 *
 * <p>The palette is built from literal shades rather than {@link TabPalette#createMapTabPalette()},
 * which resolves through the live engine palette a unit test has no sector to supply.
 */
final class TabPaletteTest {
    
    private static final Color CHROME_ACCENT = new Color(10, 10, 10);
    private static final TabBaseLook UNSELECTED_LOOK = new TabBaseLook(
        new Color(20, 20, 20),
        new Color(30, 30, 30));
    private static final TabBaseLook SELECTED_LOOK = new TabBaseLook(
        new Color(40, 40, 40),
        new Color(50, 50, 50));

    // One distinct target per momentary role, so a lookup returning the wrong wash names the wrong
    // colour rather than the same colour at a different depth.
    private static final TabWash HOVERED_WASH = new TabWash(new Color(60, 60, 60), 0.1f);
    private static final TabWash CLICKED_WASH = new TabWash(new Color(70, 70, 70), 0.2f);
    private static final TabWash HOTKEYED_WASH = new TabWash(new Color(80, 80, 80), 0.3f);

    private static final TabPalette PALETTE = new TabPalette(
        CHROME_ACCENT,
        UNSELECTED_LOOK,
        SELECTED_LOOK,
        HOVERED_WASH,
        CLICKED_WASH,
        HOTKEYED_WASH);

    @Nested
    class ResolveBaseLook {

        @Test
        void resolveBaseLookReturnsTheRestingLookWhenUnselected() {
            assertThat(PALETTE.resolveBaseLook(TabBaseState.UNSELECTED))
                .isEqualTo(new TabBaseLook(new Color(20, 20, 20), new Color(30, 30, 30)));
        }

        @Test
        void resolveBaseLookReturnsTheLitLookWhenSelected() {
            assertThat(PALETTE.resolveBaseLook(TabBaseState.SELECTED))
                .isEqualTo(new TabBaseLook(new Color(40, 40, 40), new Color(50, 50, 50)));
        }
    }

    @Nested
    class ResolveWash {

        @Test
        void resolveWashReturnsTheHoverLiftWhenHovered() {
            assertThat(PALETTE.resolveWash(TabWashState.HOVERED).target())
                .isEqualTo(new Color(60, 60, 60));
        }

        @Test
        void resolveWashReturnsTheClickLiftWhenClicked() {
            assertThat(PALETTE.resolveWash(TabWashState.CLICKED).target())
                .isEqualTo(new Color(70, 70, 70));
        }

        @Test
        void resolveWashReturnsTheHotkeyLiftWhenHotkeyed() {
            assertThat(PALETTE.resolveWash(TabWashState.HOTKEYED).target())
                .isEqualTo(new Color(80, 80, 80));
        }
    }
}
