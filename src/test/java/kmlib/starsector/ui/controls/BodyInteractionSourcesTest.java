package kmlib.starsector.ui.controls;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the body's two channels travelling as one value: the pair a strip walk picks a control's paint out of,
 * and the resting pair a panel with no live state behind it is drawn with. What a case can hold here is that
 * each half is read at the position it was asked for - a binding that reached one channel at a different
 * place would light one control and lift another, which no draw reports.
 */
final class BodyInteractionSourcesTest {

    private static final float TOLERANCE = 0.0001f;

    // Two positions with different readings on each channel, so a half read at the wrong place answers a
    // fraction the case can tell apart.
    private static final int PRESSED_CONTROL_INDEX = 1;
    private static final int HOVERED_CONTROL_INDEX = 2;
    private static final int ANY_CELL = 0;

    private static final BodyInteractionSources INTERACTIONS = new BodyInteractionSources(
        controlIndex -> cell -> controlIndex == HOVERED_CONTROL_INDEX ? 1f : 0f,
        controlIndex -> cell -> controlIndex == PRESSED_CONTROL_INDEX ? 1f : 0f);

    @Nested
    class ResolveControlInteractionSourcesAt {

        @Test
        void resolveControlInteractionSourcesAtReadsEachChannelAtThePositionAskedFor() {

            var hoveredControl = INTERACTIONS.resolveControlInteractionSourcesAt(HOVERED_CONTROL_INDEX);

            assertThat(hoveredControl.hovers().resolveHoverFractionAt(ANY_CELL))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(hoveredControl.presses().resolvePressFractionAt(ANY_CELL))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void resolveControlInteractionSourcesAtCarriesAPressOnAControlNothingIsPointingAt() {
            // The two channels are independent readings of one strip: a press falls on its own clock, so the
            // control still carrying one is often not the control the pointer has moved on to.
            var pressedControl = INTERACTIONS.resolveControlInteractionSourcesAt(PRESSED_CONTROL_INDEX);

            assertThat(pressedControl.presses().resolvePressFractionAt(ANY_CELL))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(pressedControl.hovers().resolveHoverFractionAt(ANY_CELL))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class Resting {

        @Test
        void restingHoversNoCellOfAnyControl() {
            assertThat(BodyInteractionSources.RESTING
                    .bodyHovers()
                    .resolveControlHoverSourceAt(HOVERED_CONTROL_INDEX)
                    .resolveHoverFractionAt(ANY_CELL))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void restingLiftsNoCellOfAnyControl() {
            assertThat(BodyInteractionSources.RESTING
                    .bodyPresses()
                    .resolveControlPressSourceAt(PRESSED_CONTROL_INDEX)
                    .resolvePressFractionAt(ANY_CELL))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }
}
