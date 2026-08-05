package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the resting pair both channels fall back to. It is what a body control - which has no tabs at all -
 * and a strip drawn with no animator behind it are painted with, so a channel wired live into it would show
 * an interaction on a panel nothing is touching.
 */
final class TabInteractionSourcesTest {

    private static final float TOLERANCE = 0.001f;

    private static final int ANY_INDEX = 0;

    @Nested
    class Resting {

        @Test
        void restingHoversNoTab() {
            assertThat(TabInteractionSources.RESTING.hoverSource().resolveHoverFractionAt(ANY_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void restingLiftsNoTab() {
            assertThat(TabInteractionSources.RESTING.pulseSource().resolvePulseFractionAt(ANY_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }
}
