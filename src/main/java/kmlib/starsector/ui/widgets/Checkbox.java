package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

/**
 * The geometry of a tick row: the square that sits at the left of a control row, sized to the row
 * height, and the label that follows it in the remaining width. Substrate-independent - it computes
 * the box and the label's anchor and renders nothing - so a GL or a UI-API renderer can place a
 * checkbox against it. The whole row is the hit target (label included); the raw-GL paint lives in
 * {@link kmlib.starsector.ui.render.gl.controls.CheckboxRenderer}.
 *
 * <p>The row is a three-column row with its trailing column unfilled and its leading element flush
 * with the row's own left edge, so the one thing it takes from {@link RowColumnSpec} is the gap
 * parting that element from the label - which is what makes a tick row and any other three-column
 * row in the same stack start their text at the same offset past their leading columns.
 */
public final class Checkbox {

    // What the row's columns are worth. Only the gap past the leading column is read: the tick box is
    // flush with the row's left edge and nothing follows the label, so no padding at either end is
    // charged and the row's width is its box, that gap, and its text.
    private static final RowColumnSpec COLUMNS = RowColumnSpec.CONTROL_ROW;

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

    /**
     * The x the row's label starts from (a left-anchored text): past the tick box and the gap parting
     * it from the label, which is the same offset {@link #measureRowWidth} reserved the label - so the
     * drawn text sits exactly in the room the row was sized for.
     *
     * @param bounds the control row's footprint
     * @return the label's left-anchor x, in UI coordinates
     */
    public static float computeLabelAnchorX(Rectangle bounds) {
        var box = computeTickBox(bounds);
        return box.x()
            + box.width()
            + COLUMNS.leadingLabelGap();
    }

    /**
     * How wide the row must be to hold its tick box and label without clipping: the box (a square of
     * the row height), the gap past it, and the label.
     *
     * @param rowHeight  the control row's height (the tick box's side)
     * @param labelWidth the label's measured rendered width
     * @return the row's required width, in UI coordinates
     */
    public static float measureRowWidth(float rowHeight, float labelWidth) {
        return rowHeight
            + COLUMNS.leadingLabelGap()
            + labelWidth;
    }
}
