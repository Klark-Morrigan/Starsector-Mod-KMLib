package kmlib.starsector.ui.render.gl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link NotchColours}' one rule: which of the handle's two shades the chevron takes for a given
 * hover state.
 */
final class NotchColoursTest {
    // Two distinguishable shades, so which one the pick returned is unambiguous.
    private static final Color RESTING = Color.BLUE;
    private static final Color HOVERED = Color.YELLOW;
    private static final NotchColours COLOURS = new NotchColours(RESTING, HOVERED);

    @Nested
    class ResolveChevronColour {

        @Test
        void resolveChevronColourTakesTheRestingShadeWhenTheHandleIsNotHovered() {
            assertThat(COLOURS.resolveChevronColour(false)).isEqualTo(RESTING);
        }

        @Test
        void resolveChevronColourTakesTheHoveredShadeWhenTheHandleIsHovered() {
            assertThat(COLOURS.resolveChevronColour(true)).isEqualTo(HOVERED);
        }
    }
}
