package kmlib.starsector.ui.render.gl.controls;

import kmlib.starsector.ui.controls.ControlPressSource;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.style.ControlPressLight;

/**
 * Where a control's paint pass gets each cell's press light from: asked per cell, one finished
 * {@link UiElementPaint} per answer. The seam exists for the reason the hover wash's does - a widget
 * renderer stays a chrome pass, filling a rectangle with a paint it was handed, never learning which look
 * named that shade or how far a lift has run.
 *
 * <p>Asked per cell rather than handed one pressed rectangle, because a press decays on its own clock: the
 * cell pressed a moment ago is still falling while the pointer has moved on, and a control settling after a
 * quick pair of clicks has more than one cell part-way. A cell with no press running answers a hidden
 * paint, which the fill primitives skip, so a renderer needs no test of its own before painting one.
 */
@FunctionalInterface
public interface CellPressLightSource {

    /**
     * Binds a look's press light to a control's live lifts: each cell lights in that shade at however far
     * through its press it currently stands, faded by the panel's opacity. This is where the panel's
     * animator and the control's paint meet, and the split either side of it is the point - the animator
     * counts frames and knows no colour, the look names a colour and knows no time, and only this binding
     * needs both.
     *
     * @param light   what a cell at a lift's peak is lit in
     * @param presses how far through its press lift each cell currently stands
     * @param opacity the panel's overall alpha, 0..1
     * @return a source answering the resolved light for any cell of the control
     */
    static CellPressLightSource createPressLitLightSource(
            ControlPressLight light,
            ControlPressSource presses,
            float opacity) {

        return cell -> light.resolvePaintAtPressFraction(presses.resolvePressFractionAt(cell), opacity);
    }

    /**
     * The light the cell at {@code cell} currently takes.
     *
     * @param cell the cell being asked about, numbered as the hit resolver numbers it
     * @return its light paint, hidden when no press is running on it
     */
    UiElementPaint resolveLightPaintAt(int cell);

    /**
     * The light a whole-row control takes - a tick box, a toggle - which has one cell and so one answer.
     *
     * <p>Here rather than at each such widget so the cell a single-cell control is numbered by is spelt
     * once: a widget spelling it for itself is a widget that has to know the hit resolver's numbering to
     * paint, and two of them spelling it are two places for that number to be got wrong.
     *
     * @return its light paint, hidden when no press is running on it
     */
    default UiElementPaint resolveSingleCellLightPaint() {
        return resolveLightPaintAt(ControlSpec.SINGLE_CELL);
    }
}
