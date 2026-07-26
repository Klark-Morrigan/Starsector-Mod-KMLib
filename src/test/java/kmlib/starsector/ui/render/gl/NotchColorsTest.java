package kmlib.starsector.ui.render.gl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link NotchColors}' one rule: which of the handle's two shades the chevron takes for a given
 * hover state.
 */
final class NotchColorsTest {
    // Two distinguishable shades, so which one the pick returned is unambiguous.
    private static final Color RESTING = Color.BLUE;
    private static final Color HOVERED = Color.YELLOW;
    private static final NotchColors COLORS = new NotchColors(RESTING, HOVERED);

    @Nested
    class ChooseChevronColour {

        @Test
        void chooseChevronColourTakesTheRestingShadeWhenTheHandleIsNotHovered() {
            assertThat(COLORS.chooseChevronColour(false)).isEqualTo(RESTING);
        }

        @Test
        void chooseChevronColourTakesTheHoveredShadeWhenTheHandleIsHovered() {
            assertThat(COLORS.chooseChevronColour(true)).isEqualTo(HOVERED);
        }
    }
}
