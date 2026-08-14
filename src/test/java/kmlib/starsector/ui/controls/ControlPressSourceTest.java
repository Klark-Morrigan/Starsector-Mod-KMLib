package kmlib.starsector.ui.controls;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the resting {@link ControlPressSource}: no cell is lifted, whichever one is asked for. A widget paints
 * whatever this hands back without checking it, so a source answering with any lift at all would flash a
 * control nothing has been pressed on.
 */
final class ControlPressSourceTest {

    private static final float TOLERANCE = 0.001f;

    // A whole-row control's cell and a segment well down a row, since a source is asked per cell and must not
    // assume the control it is being walked over.
    private static final int LATE_CELL = 4;

    @Nested
    class CreateRestingPressSource {

        @Test
        void createRestingPressSourceLiftsNoCellOfTheControl() {

            var presses = ControlPressSource.createRestingPressSource();

            assertThat(presses.resolvePressFractionAt(ControlSpec.SINGLE_CELL))
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(presses.resolvePressFractionAt(LATE_CELL))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }
}
