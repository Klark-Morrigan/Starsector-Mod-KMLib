package kmlib.starsector.ui.controls;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the resting pair both of a control's channels fall back to. It is what a tabs row - which answers the
 * pointer through its own palette - and a control drawn with no animator behind it are painted with, so a
 * channel wired live into it would show an interaction on a control nothing is touching.
 */
final class ControlInteractionSourcesTest {

    private static final float TOLERANCE = 0.001f;

    private static final int ANY_CELL = 0;

    @Nested
    class Resting {

        @Test
        void restingHoversNoCell() {
            assertThat(ControlInteractionSources.RESTING.hovers().resolveHoverFractionAt(ANY_CELL))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void restingLiftsNoCell() {
            assertThat(ControlInteractionSources.RESTING.presses().resolvePressFractionAt(ANY_CELL))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }
}
