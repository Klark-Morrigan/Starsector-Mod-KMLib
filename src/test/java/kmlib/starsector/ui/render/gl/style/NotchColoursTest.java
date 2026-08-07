package kmlib.starsector.ui.render.gl.style;

import kmlib.starsector.ui.render.gl.panel.NotchState;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link NotchColours}' one rule: the shade the chevron takes at each end of the handle's hover travel
 * and between them, and that a look holding one colour across both states never moves. The travel's range is
 * {@link NotchState}'s to confine, so nothing here feeds it a fraction from outside one.
 */
final class NotchColoursTest {

    // Two shades a channel apart in both directions, so a midpoint is a value neither end could produce.
    private static final Color RESTING = new Color(0, 0, 100);
    private static final Color HOVERED = new Color(200, 0, 0);
    private static final NotchColours COLOURS = new NotchColours(RESTING, HOVERED);

    // The blend's own ends, and the point midway between them.
    private static final float FULLY_RESTING = 0f;
    private static final float HALF_LIT = 0.5f;
    private static final float FULLY_LIT = 1f;

    @Nested
    class ComputeChevronColour {

        @Test
        void computeChevronColourTakesTheRestingShadeWhileTheHandleIsUnlit() {
            assertThat(COLOURS.computeChevronColour(FULLY_RESTING))
                .isEqualTo(RESTING);
        }

        @Test
        void computeChevronColourTakesTheHoveredShadeOnceTheHandleIsFullyLit() {
            assertThat(COLOURS.computeChevronColour(FULLY_LIT))
                .isEqualTo(HOVERED);
        }

        @Test
        void computeChevronColourSitsBetweenTheTwoShadesPartWayThroughTheTravel() {
            // The whole reason the pick became a blend: a half-faded handle draws a colour neither state
            // holds, rather than jumping between them at some threshold.
            assertThat(COLOURS.computeChevronColour(HALF_LIT))
                .isEqualTo(new Color(100, 0, 50));
        }

        @Test
        void computeChevronColourHoldsOneShadeThroughoutForALookThatDoesNotDistinguishTheStates() {
            // The gold choice passes the same colour twice; every point of its travel must be that colour,
            // so the handle answers a hover by its wash alone rather than by a drifting glyph.
            var singleShade = new NotchColours(RESTING, RESTING);

            assertThat(singleShade.computeChevronColour(HALF_LIT))
                .isEqualTo(RESTING);
        }
    }
}
