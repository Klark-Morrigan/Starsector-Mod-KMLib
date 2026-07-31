package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

/**
 * The geometry of a tick box: the square that sits at the left of a control row, sized to the row
 * height so the remaining width is the label's. Substrate-independent - it computes the box and
 * renders nothing - so a GL or a UI-API renderer can place a checkbox against it. The whole row is
 * the hit target (label included); the raw-GL paint lives in
 * {@link kmlib.starsector.ui.render.gl.CheckboxRenderer}.
 */
public final class Checkbox {
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
        return new Rectangle(
            bounds.x(),
            bounds.y(),
            bounds.height(),
            bounds.height());
    }
}
