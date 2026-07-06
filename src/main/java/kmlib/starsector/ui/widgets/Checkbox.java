package kmlib.starsector.ui.widgets;

import com.fs.starfarer.api.util.Misc;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.render.UiBoxes;

import java.awt.Color;

/**
 * A square tick box that fills when checked, sitting at the left of a control row. The whole
 * row is the hit target (label included), so the consumer hands the row's {@code bounds} both
 * to hit-test (via {@link Rectangle#containsPoint}) and to draw: the box occupies the left of
 * that row and the label is drawn to its right by the consumer, which owns the text.
 *
 * <p>{@link #computeTickBox} is pure geometry and unit-tested; {@link #render} is the raw GL
 * passthrough (over {@link Misc#renderQuadAlpha} and {@link UiBoxes}), exercised in-engine.
 */
public final class Checkbox {
    // The filled tick is inset inside the box outline by this fraction of the box height, so the
    // checked mark reads as a centred pip rather than touching the frame.
    private static final float TICK_INSET_FRACTION = 0.28f;
    private static final float BOX_OUTLINE_THICKNESS = 1f;

    private Checkbox() {
    }

    /**
     * The tick box: a square of side equal to the row height, flush with the row's left edge, so
     * the box lines up with the row and the remaining width is the label's.
     *
     * @param bounds the control row's footprint
     * @return the square tick box at the row's left
     */
    public static Rectangle computeTickBox(Rectangle bounds) {
        return new Rectangle(bounds.x(), bounds.y(), bounds.height(), bounds.height());
    }

    /**
     * Strokes the tick box outline and, when {@code isChecked}, fills its inset centre. Both
     * draws fade by {@code opacity}.
     *
     * @param bounds     the control row's footprint (the box is derived from its left edge)
     * @param isChecked  whether to draw the filled tick
     * @param boxColor   the box outline colour
     * @param tickColor  the filled-tick colour
     * @param opacity    overall alpha, 0..1
     */
    public static void render(Rectangle bounds, boolean isChecked, Color boxColor, Color tickColor,
            float opacity) {
        var box = computeTickBox(bounds);
        UiBoxes.renderBorder(box.x(), box.y(), box.width(), box.height(), BOX_OUTLINE_THICKNESS,
                boxColor, opacity);
        if (isChecked) {
            var inset = box.height() * TICK_INSET_FRACTION;
            Misc.renderQuadAlpha(box.x() + inset, box.y() + inset, box.width() - 2f * inset,
                    box.height() - 2f * inset, tickColor, opacity);
        }
    }
}
