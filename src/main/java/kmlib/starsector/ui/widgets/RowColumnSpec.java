package kmlib.starsector.ui.widgets;

/**
 * What the columns of a three-column row are worth: the inset off each end of the row, and the gap
 * parting the label from whatever flanks it on either side. A row read as something small leading, a
 * label, and something small trailing is placed by more than one widget, so the spacing is named here
 * once - two widgets holding their own copy of a gap start their text at two different offsets down one
 * stack of rows as soon as one copy moves, and a comment tying the copies together is not a thing that
 * can fail when it does.
 *
 * <p>It states what the columns are worth and nothing about where they land. Placing a row is left with
 * whichever widget lays one out, because a row snapped into a rectangle it is handed and a row whose box
 * is derived from its own content run in opposite directions, and one placement serving both would bend
 * one of them.
 *
 * <p>A row fills only the columns it has, and reads only the components it has a boundary for: a leading
 * element flush with the row's own left edge charges no leading padding, and a row with nothing past its
 * label charges neither trailing component.
 *
 * @param leadingPadding   the inset off the row's left edge the leading column starts at
 * @param leadingLabelGap  the gap between the leading column's right edge and the label's anchor
 * @param labelTrailingGap the least gap kept between the label and the trailing column, so a snug row's
 *                         label and trailing element do not touch
 * @param trailingPadding  the inset off the row's right edge the trailing column ends at
 */
public record RowColumnSpec(
    float leadingPadding,
    float leadingLabelGap,
    float labelTrailingGap,
    float trailingPadding) {

    /**
     * The spacing a KM control row is laid out at: content clears the row's frame by a small inset at
     * either end, and each flanking column is parted from the label by a gap half again as wide, so a
     * flank reads as its own column rather than as part of the sentence beside it. One value for every
     * such row, so a strip mixing row shapes still lines its text up.
     */
    public static final RowColumnSpec CONTROL_ROW =
        new RowColumnSpec(4f, 6f, 6f, 4f);
}
