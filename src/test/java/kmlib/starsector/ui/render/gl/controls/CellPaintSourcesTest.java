package kmlib.starsector.ui.render.gl.controls;

import kmlib.starsector.ui.render.gl.UiElementPaint;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins that a widget handed both of a cell's paints reads each from the channel it was built with. The pair
 * is two seams of one shape - a cell number in, a paint out - so a carrier that crossed them would wash every
 * cell in the press's shade and light it in the pointer's, which is a panel painted wrongly on every frame
 * rather than a draw that fails.
 */
final class CellPaintSourcesTest {

    private static final float TOLERANCE = 0.0001f;

    private static final float WASH_ALPHA = 0.15f;
    private static final float LIGHT_ALPHA = 0.25f;

    private static final int ANY_CELL = 0;

    @Nested
    class Accessors {

        @Test
        void cellPaintSourcesKeepsEachChannelOnItsOwnSide() {

            var cellPaints = new CellPaintSources(
                cell -> new UiElementPaint(Color.WHITE, WASH_ALPHA),
                cell -> new UiElementPaint(Color.WHITE, LIGHT_ALPHA));

            assertThat(cellPaints.hoverWashes().resolveWashPaintAt(ANY_CELL).alpha())
                .isCloseTo(0.15f, within(TOLERANCE));
            assertThat(cellPaints.pressLights().resolveLightPaintAt(ANY_CELL).alpha())
                .isCloseTo(0.25f, within(TOLERANCE));
        }
    }
}
