package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.Checkbox;

/**
 * Raw-GL paint for a {@link Checkbox}: strokes the tick box and, when checked, fills its inset
 * centre, both faded by one opacity. The box geometry lives on the substrate-independent widget;
 * this is the GL passthrough (over {@link UiFill#renderQuad} and {@link UiBoxes}), exercised
 * in-engine.
 */
public final class CheckboxRenderer {
    // The filled tick is inset inside the box outline by this fraction of the box height, so the
    // checked mark reads as a centred pip rather than touching the frame.
    private static final float TICK_INSET_FRACTION = 0.28f;
    private static final float BOX_OUTLINE_THICKNESS = 1f;

    private CheckboxRenderer() {
    }

    /**
     * Strokes the tick box outline in {@code boxPaint} and, when {@code isChecked}, fills its inset
     * centre in {@code tickPaint}. Sharing one opacity across both paints is the caller's to arrange,
     * so the checkbox can fade as one.
     *
     * @param bounds     the control row's footprint (the box is derived from its left edge)
     * @param isChecked  whether to draw the filled tick
     * @param boxPaint   the box outline colour and alpha
     * @param tickPaint  the filled-tick colour and alpha
     */
    public static void render(
            Rectangle bounds,
            boolean isChecked,
            UiElementPaint boxPaint,
            UiElementPaint tickPaint) {

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
