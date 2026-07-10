package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.widgets.Checkbox;

import java.awt.Color;

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
     * Strokes the tick box outline and, when {@code isChecked}, fills its inset centre. Both draws
     * fade by {@code opacity}.
     *
     * @param bounds     the control row's footprint (the box is derived from its left edge)
     * @param isChecked  whether to draw the filled tick
     * @param boxColor   the box outline colour
     * @param tickColor  the filled-tick colour
     * @param opacity    overall alpha, 0..1
     */
    public static void render(Rectangle bounds, boolean isChecked, Color boxColor, Color tickColor,
            float opacity) {
        var box = Checkbox.computeTickBox(bounds);
        UiBoxes.renderBorder(box.x(), box.y(), box.width(), box.height(), BOX_OUTLINE_THICKNESS,
                boxColor, opacity);
        if (isChecked) {
            var inset = box.height() * TICK_INSET_FRACTION;
            UiFill.renderQuad(box.x() + inset, box.y() + inset, box.width() - 2f * inset,
                    box.height() - 2f * inset, tickColor, opacity);
        }
    }
}
