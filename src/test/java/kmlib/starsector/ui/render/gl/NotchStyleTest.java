package kmlib.starsector.ui.render.gl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link NotchStyle}'s one rule: which of the handle's two shades the chevron takes for a given
 * hover state.
 */
final class NotchStyleTest {
    // Two distinguishable shades, so which one the pick returned is unambiguous.
    private static final Color RESTING = Color.BLUE;
    private static final Color HOVERED = Color.YELLOW;
    private static final NotchStyle STYLE = new NotchStyle(RESTING, HOVERED);

    @Nested
    class ChooseChevronColour {

        @Test
        void chooseChevronColourTakesTheRestingShadeWhenTheHandleIsNotHovered() {
            assertThat(STYLE.chooseChevronColour(false)).isEqualTo(RESTING);
        }

        @Test
        void chooseChevronColourTakesTheHoveredShadeWhenTheHandleIsHovered() {
            assertThat(STYLE.chooseChevronColour(true)).isEqualTo(HOVERED);
        }
    }
}
