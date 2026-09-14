package kmlib.starsector.ui.render.gl.controls;

import kmlib.starsector.ui.controls.ControlHoverSource;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.style.ControlHoverWash;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the binding where a control's live fades meet the look its cells wash in - the seam a widget renderer
 * reads, so what it is handed is finished paint and not a fraction to combine with a colour it would have to
 * be given as well. The pairing is what a case can hold: a source answering the fade of some other cell
 * lights the wrong one, which no draw reports.
 */
final class CellHoverWashSourceTest {

    private static final float TOLERANCE = 0.0001f;

    private static final ControlHoverWash WASH = new ControlHoverWash(new Color(40, 80, 160), 0.4f);

    private static final float FULLY_OPAQUE = 1f;

    // Two cells at different points of their fades - the state a row settling after a sweep is in, one cell
    // rising while the one just left winds down - so a source reading the wrong cell answers a fraction the
    // case can tell apart.
    private static final int RISING_CELL = 1;
    private static final int SETTLING_CELL = 2;

    // The cell a checkbox or a toggle is numbered by, spelt as the literal the hit resolver uses rather than
    // read off the constant the seam itself reads - a case taking that constant would agree with the code
    // whatever the number became, which is the one thing it exists to catch.
    private static final int THE_ONLY_CELL_OF_A_WHOLE_ROW_CONTROL = 0;

    private static final ControlHoverSource HOVERS = cell -> switch (cell) {
        case RISING_CELL -> 1f;
        case SETTLING_CELL -> 0.5f;
        default -> 0f;
    };

    @Nested
    class CreateHoverFadedWashSource {

        @Test
        void createHoverFadedWashSourceWashesEachCellAtItsOwnFadesPoint() {

            var washes = CellHoverWashSource.createHoverFadedWashSource(WASH, HOVERS, FULLY_OPAQUE);

            assertThat(washes.resolveWashPaintAt(RISING_CELL).alpha())
                .isCloseTo(0.4f, within(TOLERANCE));
            assertThat(washes.resolveWashPaintAt(SETTLING_CELL).alpha())
                .isCloseTo(0.2f, within(TOLERANCE));
        }

        @Test
        void createHoverFadedWashSourceHidesTheWashOfACellNothingIsPointingAt() {
            // A renderer washes every cell it lays and lets the hidden ones fall away, so a resting cell has
            // to answer a paint the fill skips rather than one it composites at nothing.
            var washes = CellHoverWashSource.createHoverFadedWashSource(WASH, HOVERS, FULLY_OPAQUE);

            assertThat(washes.resolveWashPaintAt(0).isHidden())
                .isTrue();
        }

        @Test
        void createHoverFadedWashSourceFadesEveryCellWithThePanelsOpacity() {

            var washes = CellHoverWashSource.createHoverFadedWashSource(WASH, HOVERS, 0.5f);

            assertThat(washes.resolveWashPaintAt(RISING_CELL).alpha())
                .isCloseTo(0.2f, within(TOLERANCE));
        }
    }

    @Nested
    class ResolveSingleCellWashPaint {

        @Test
        void resolveSingleCellWashPaintAnswersTheCellAWholeRowControlIsNumberedBy() {
            // The whole point of the default: a tick box and a toggle have one hit target, numbered zero by
            // the hit resolver, and a widget spelling that out for itself is a widget that has to know the
            // resolver's numbering to paint.
            CellHoverWashSource washes = cell -> cell == THE_ONLY_CELL_OF_A_WHOLE_ROW_CONTROL
                ? new UiElementPaint(Color.WHITE, 0.4f)
                : new UiElementPaint(Color.WHITE, 0f);

            assertThat(washes.resolveSingleCellWashPaint().alpha())
                .isCloseTo(0.4f, within(TOLERANCE));
        }
    }
}
