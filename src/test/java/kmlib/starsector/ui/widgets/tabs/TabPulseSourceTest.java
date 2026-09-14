package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the resting {@link TabPulseSource}: no tab is lifted, whichever index is asked for. The strip paints
 * whatever this hands back without checking it, so a source answering with any lift at all would brighten a
 * tab nothing has happened to.
 */
final class TabPulseSourceTest {

    private static final float TOLERANCE = 0.001f;

    // Indices past either end of a row, since a source is asked per tab and must not assume the row it is
    // being walked over.
    private static final int FIRST_INDEX = 0;
    private static final int LATER_INDEX = 4;
    private static final int NO_TAB_INDEX = -1;

    @Nested
    class CreateRestingPulseSource {

        @Test
        void createRestingPulseSourceLiftsNoTabInTheRow() {

            var pulses = TabPulseSource.createRestingPulseSource();

            assertThat(pulses.resolvePulseFractionAt(FIRST_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(pulses.resolvePulseFractionAt(LATER_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void createRestingPulseSourceAnswersForAnIndexOutsideTheRow() {

            var pulses = TabPulseSource.createRestingPulseSource();

            assertThat(pulses.resolvePulseFractionAt(NO_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }
}
