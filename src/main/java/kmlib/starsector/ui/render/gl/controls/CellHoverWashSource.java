package kmlib.starsector.ui.render.gl.controls;

import kmlib.starsector.ui.controls.ControlHoverSource;
import kmlib.starsector.ui.controls.specs.ControlSpec;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.style.ControlHoverWash;

/**
 * Where a control's paint pass gets each cell's hover wash from: asked per cell, one finished
 * {@link UiElementPaint} per answer. The seam exists so a widget renderer stays a chrome pass - it fills a
 * rectangle with a paint it was handed, never learning which look named that shade, how far a fade has run,
 * or that a fade exists at all.
 *
 * <p>Asked per cell rather than handed one hovered rectangle, because more than one cell of a control can
 * be part-way at once: the cell the pointer has left is still winding down while the cell it reached rises.
 * A resting cell answers a hidden paint, which the fill primitives skip, so a renderer needs no test of its
 * own before painting one.
 */
@FunctionalInterface
public interface CellHoverWashSource {

    /**
     * Binds a look's hovered wash to a control's live fades: each cell washes in that shade at however far
     * onto its hovered look it currently stands, faded by the panel's opacity. This is where the panel's
     * animator and the control's paint meet, and the split either side of it is the point - the animator
     * counts frames and knows no colour, the look names a colour and knows no time, and only this binding
     * needs both.
     *
     * @param wash    what a fully hovered cell is washed in
     * @param hovers  how far onto its hovered look each cell currently stands
     * @param opacity the panel's overall alpha, 0..1
     * @return a source answering the resolved wash for any cell of the control
     */
    static CellHoverWashSource createHoverFadedWashSource(
            ControlHoverWash wash,
            ControlHoverSource hovers,
            float opacity) {

        return cell -> wash.resolvePaintAtHoverFraction(hovers.resolveHoverFractionAt(cell), opacity);
    }

    /**
     * The wash the cell at {@code cell} currently takes.
     *
     * @param cell the cell being asked about, numbered as the hit resolver numbers it
     * @return its wash paint, hidden when nothing is hovering it
     */
    UiElementPaint resolveWashPaintAt(int cell);

    /**
     * The wash a whole-row control takes - a tick box, a toggle - which has one cell and so one answer.
     *
     * <p>Here rather than at each such widget so the cell a single-cell control is numbered by is spelt
     * once: a widget spelling it for itself is a widget that has to know the hit resolver's numbering to
     * paint, and two of them spelling it are two places for that number to be got wrong.
     *
     * @return its wash paint, hidden when nothing is hovering it
     */
    default UiElementPaint resolveSingleCellWashPaint() {
        return resolveWashPaintAt(ControlSpec.SINGLE_CELL);
    }
}
