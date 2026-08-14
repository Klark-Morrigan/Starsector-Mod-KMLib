package kmlib.starsector.ui.render.gl.controls;

import kmlib.starsector.ui.controls.ControlPressSource;
import kmlib.starsector.ui.render.gl.style.ControlPressLight;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the binding where a control's live press lifts meet the look its cells light in - the seam a widget
 * renderer reads, so what it is handed is finished paint and not a fraction to combine with a colour it would
 * have to be given as well. The pairing is what a case can hold: a source answering the lift of some other
 * cell flashes the wrong one, which no draw reports.
 */
final class CellPressLightSourceTest {

    private static final float TOLERANCE = 0.0001f;

    private static final ControlPressLight LIGHT = new ControlPressLight(new Color(200, 220, 255), 0.4f);

    private static final float FULLY_OPAQUE = 1f;

    // Two cells at different points of their lifts - the state a row is in after a quick pair of clicks, one
    // at its peak while the other is already falling - so a source reading the wrong cell answers a fraction
    // the case can tell apart.
    private static final int PEAKING_CELL = 1;
    private static final int FALLING_CELL = 2;

    private static final ControlPressSource PRESSES = cell -> switch (cell) {
        case PEAKING_CELL -> 1f;
        case FALLING_CELL -> 0.5f;
        default -> 0f;
    };

    @Nested
    class CreatePressLitLightSource {

        @Test
        void createPressLitLightSourceLightsEachCellAtItsOwnLiftsPoint() {

            var lights = CellPressLightSource.createPressLitLightSource(LIGHT, PRESSES, FULLY_OPAQUE);

            assertThat(lights.resolveLightPaintAt(PEAKING_CELL).alpha())
                .isCloseTo(0.4f, within(TOLERANCE));
            assertThat(lights.resolveLightPaintAt(FALLING_CELL).alpha())
                .isCloseTo(0.2f, within(TOLERANCE));
        }

        @Test
        void createPressLitLightSourceHidesTheLightOfACellNothingHasPressed() {
            // A renderer lights every cell it lays and lets the hidden ones fall away, so a cell with no lift
            // running has to answer a paint the fill skips rather than one it composites at nothing.
            var lights = CellPressLightSource.createPressLitLightSource(LIGHT, PRESSES, FULLY_OPAQUE);

            assertThat(lights.resolveLightPaintAt(0).isHidden())
                .isTrue();
        }

        @Test
        void createPressLitLightSourceFadesEveryCellWithThePanelsOpacity() {

            var lights = CellPressLightSource.createPressLitLightSource(LIGHT, PRESSES, 0.5f);

            assertThat(lights.resolveLightPaintAt(PEAKING_CELL).alpha())
                .isCloseTo(0.2f, within(TOLERANCE));
        }
    }
}
