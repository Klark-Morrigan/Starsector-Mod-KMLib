package kmlib.starsector.ui.widgets.scroll;

/**
 * How thick a {@link Scrollbar} draws: the width of its track, and of the thumb sized within it, in
 * pixels. It is the one dimension of the bar a host varies - how fat it is - so the geometry beside it
 * holds no width of its own and takes this instead.
 *
 * <p>A value type rather than a bare float because the number travels alongside the other floats a
 * scrolled region is described by (an offset, an overflow), which are positional and same-typed: a
 * transposed argument would compile, and a track sized from a scroll offset paints without complaint.
 * A distinct type is what refuses that at the compiler rather than on screen.
 *
 * <p>It carries the two fixed gaps flanking the track, since what the bar occupies across is the track
 * plus both of them: {@link #RIGHT_MARGIN} holds the track off the container's right edge so it clears a
 * border, and {@link #CONTENT_CLEARANCE} parts it from the content to its left. Those stay constants -
 * the thing being varied is the bar's width, not its spacing.
 *
 * @param pixels the drawn width of the track and thumb; 0 for no bar at all
 */
public record ScrollbarThickness(float pixels) {

    /** The thin bar a scrolled region draws when its host names no thickness of its own. */
    public static final ScrollbarThickness DEFAULT = new ScrollbarThickness(3f);

    /** The gap holding the track off the container's right edge, so it clears a border drawn there. */
    public static final float RIGHT_MARGIN = 3f;

    /** The gap parting the track's left edge from the content it scrolls, so the two never abut. */
    public static final float CONTENT_CLEARANCE = 2f;

    /**
     * @return whether the bar is drawn at all - a zero thickness takes the track and thumb away rather
     *         than drawing them at no width, leaving nothing to see and nothing to grab
     */
    public boolean isTrackDrawn() {
        return pixels > 0f;
    }

    /**
     * States the gutter and stops there - it does not know what padding a container already insets its
     * content by, nor subtract one from the other. That arithmetic belongs to whatever lays the
     * container out, and reaching for it here would point this package at the layout tier that arranges
     * it, reversing the one edge between the two.
     *
     * @return the width the bar needs clear of the content: the track, the margin off the container's
     *         edge, and the clearance off the content
     */
    public float computeGutterWidth() {
        return pixels + RIGHT_MARGIN + CONTENT_CLEARANCE;
    }
}
