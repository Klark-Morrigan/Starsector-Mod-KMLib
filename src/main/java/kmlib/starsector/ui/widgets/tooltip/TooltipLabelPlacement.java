package kmlib.starsector.ui.widgets.tooltip;

/**
 * Where a {@link TooltipRow.TableRow}'s label starts across the box, and so which of the box's columns it
 * aligns to. One value rather than a flag per placement, because a label starts exactly one of these ways
 * - stated as separate flags, a label both aligned with the crests and clear of them would be
 * expressible, and the layout would have to pick a winner.
 *
 * <p>Framed on the label because the label is what a row is: the crest is an optional lead-in and the
 * value an optional trailer, so where a row sits is a fact about its text rather than about the
 * decorations around it. It is not about how a run of text is anchored within its own box - that is a
 * {@link kmlib.starsector.ui.text.TextAlignment} on the look, and a tooltip pins its anchors from the
 * layout rather than taking them from a style.
 *
 * <p>Both values are entries in the box's table and differ solely over the crest gutter: each keeps the
 * value column clear to its right, whether or not it fills it, so a value trails a title exactly as it
 * trails an ordinary row. Leaving the table altogether is not a placement here but a
 * {@link TooltipRow.CentredRow}, which carries no columns to be placed against.
 */
public enum TooltipLabelPlacement {

    /**
     * Starts past the crest gutter, so the label lines up with the labels of the box's crested rows -
     * whether or not this row carries a crest of its own. What an ordinary content row is.
     */
    ALIGNED_WITH_CRESTS,

    /**
     * Starts at the box's left content edge, ignoring the crest gutter its neighbours align past - for a
     * line that names the whole box rather than sitting as one entry in it, which aligning it inside that
     * table would misfile. Still an entry in the value column, like the rows it heads.
     */
    AT_CONTENT_EDGE
}
