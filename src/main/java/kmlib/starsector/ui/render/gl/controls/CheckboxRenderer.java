package kmlib.starsector.ui.render.gl.controls;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.render.gl.UiBoxes;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.Checkbox;

/**
 * Raw-GL paint for a {@link Checkbox}: washes the row under the pointer, lights it for a press it
 * answered, strokes the tick box and, when checked, fills its inset centre, all faded by one opacity. The
 * box geometry lives on the substrate-independent widget; this is the GL passthrough (over
 * {@link UiFill#renderQuad} and {@link UiBoxes}), exercised in-engine.
 */
public final class CheckboxRenderer {
    // The filled tick is inset inside the box outline by this fraction of the box height, so the
    // checked mark reads as a centred pip rather than touching the frame.
    private static final float TICK_INSET_FRACTION = 0.28f;
    private static final float BOX_OUTLINE_THICKNESS = 1f;

    private CheckboxRenderer() {
    }

    /**
     * Washes the row by however far the pointer has lifted it, lights it by however far through its press
     * lift it stands, strokes the tick box outline in {@code boxPaint} and, when {@code isChecked}, fills
     * its inset centre in {@code tickPaint}. Sharing one opacity across the paints is the caller's to
     * arrange, so the checkbox can fade as one.
     *
     * <p>The hover wash covers the whole row rather than the tick box alone, because the whole row is
     * the cell: a press anywhere along it toggles the box, so lighting only the box would leave most
     * of what the player can hit unlit. It is drawn first, so the outline and the tick read over it
     * rather than through it.
     *
     * <p>The press light lands on the same row and directly over that wash - a press is made on a row the
     * pointer is already holding washed, so it has to add to what is there rather than travel toward it -
     * and still under the outline and the tick, which say what the box <em>is</em> and must not move
     * because it was clicked.
     *
     * @param bounds     the control row's footprint (the box is derived from its left edge)
     * @param isChecked  whether to draw the filled tick
     * @param cellPaints the wash and press light each cell takes; a checkbox has the one cell
     * @param boxPaint   the box outline colour and alpha
     * @param tickPaint  the filled-tick colour and alpha
     */
    public static void render(
            Rectangle bounds,
            boolean isChecked,
            CellPaintSources cellPaints,
            UiElementPaint boxPaint,
            UiElementPaint tickPaint) {

        UiFill.renderQuad(bounds, cellPaints.hoverWashes().resolveSingleCellWashPaint());
        UiFill.renderQuad(bounds, cellPaints.pressLights().resolveSingleCellLightPaint());

        var box = Checkbox.computeTickBox(bounds);

        UiBoxes.renderBorder(box, new BoxBorder(BOX_OUTLINE_THICKNESS), boxPaint);

        if (isChecked) {

            var inset = box.height() * TICK_INSET_FRACTION;
            var tickBounds = new Rectangle(
                box.x() + inset,
                box.y() + inset,
                box.width() - 2f * inset,
                box.height() - 2f * inset);

            UiFill.renderQuad(tickBounds, tickPaint);
        }
    }
}
