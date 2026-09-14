package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;

/**
 * Sizes and places a free-floating tooltip box: it grows the box to hold the measured text plus its
 * padding and sits it near the cursor, clamped so it never runs off a screen edge. Pure geometry -
 * no GL, no engine reads - so the sizing and the edge clamp stand on plain numbers, and the caller
 * feeds in the measured widths and the live screen and cursor coordinates.
 *
 * <p>Cursor-relative rather than anchored, which is the placement a tooltip needs and a docked panel
 * does not: a panel's corner is fixed and its content laid out to fit, where this is sized by its
 * content and then placed wherever the pointer happens to be.
 */
public final class TooltipBoxLayout {
    // Interior padding around the text, the gap stacked between text lines, and the offset from the
    // cursor to the box's lower-left corner so the box sits up-and-right of the pointer, never under
    // it. The box geometry's single source, shared with the text placement that draws into it.
    public static final float PADDING = 8f;
    public static final float LINE_GAP = 4f;
    public static final float CURSOR_OFFSET = 18f;

    private TooltipBoxLayout() {
    }

    /**
     * Builds the tooltip box around measured content of {@code contentWidth} by {@code contentHeight},
     * offset from the cursor and clamped fully on screen. When the box is larger than the screen on an
     * axis the clamp pins that axis to the origin, so the box stays anchored rather than sliding off
     * the far edge.
     *
     * <p>The content's own height is the input rather than a line count, because how tall a stack of
     * content stands is the content's rule, not the box's: lines of one height, a break opening a
     * section, a rule between blocks all stack differently. This adds the padding around whatever that
     * comes to and places the result, which is the whole of what a box knows.
     *
     * <p>The region to stay inside travels as one rectangle rather than as a width and a height,
     * because that is what it is: two loose floats can be handed over swapped, and a bound is a value
     * the geometry package already has a name for.
     *
     * @param contentWidth  the widest measured content line, in UI units
     * @param contentHeight the full height the content stacks to, in UI units
     * @param cursorX       the cursor x, in UI coordinates (UI origin is bottom-left)
     * @param cursorY       the cursor y, in UI coordinates
     * @param screenBound   the region the box must stay within, in UI coordinates
     * @return the box footprint, lower-left origin, fully within the bound
     */
    public static Rectangle computeBox(
            double contentWidth,
            double contentHeight,
            float cursorX,
            float cursorY,
            Rectangle screenBound) {

        var width = padContentLength(contentWidth);
        var height = padContentLength(contentHeight);
        var x = Math.min(cursorX + CURSOR_OFFSET, screenBound.x() + screenBound.width() - width);
        var y = Math.min(cursorY + CURSOR_OFFSET, screenBound.y() + screenBound.height() - height);

        // Floored at the bound's own near corner, so a box too large to fit stays anchored there rather
        // than sliding off the far edge and taking its text with it.
        return new Rectangle(
            Math.max(x, screenBound.x()),
            Math.max(y, screenBound.y()),
            width,
            height);
    }

    /**
     * Answers how tall a box wrapping content of {@code contentHeight} comes to: that height plus the
     * padding above and below it.
     *
     * <p>Offered apart from {@link #computeBox} because a caller weighing whether its content will fit
     * the room it has holds no cursor to place a box at - and where the box would sit has no say in how
     * tall it stands. Asked through here rather than padded at that caller, so the height it tests and
     * the box it later draws are one arithmetic.
     *
     * @param contentHeight the full height the content stacks to, in UI units
     * @return the box height that content wraps to, in UI units
     */
    public static float computeBoxHeight(double contentHeight) {
        return padContentLength(contentHeight);
    }

    // What a measured content length grows to on either axis: the length plus the padding either side of
    // it. One arithmetic for both axes and for the height asked about before anything is placed, so a
    // box weighed against a bound cannot be padded differently from the box drawn.
    private static float padContentLength(double contentLength) {
        return (float) contentLength + PADDING + PADDING;
    }
}
