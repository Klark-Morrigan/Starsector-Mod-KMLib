package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;

/**
 * Sizes and places a free-floating tooltip box: it grows the box to hold the measured text plus its
 * padding and sits it near the cursor, clamped so it never runs off a screen edge. Pure geometry -
 * no GL, no engine reads - so the sizing and the edge clamp stand on plain numbers, and the caller
 * feeds in the measured widths and the live screen and cursor coordinates.
 *
 * <p>The cursor-relative counterpart to {@link BoxPlacement}, which pins a fixed-size box to a screen
 * anchor: this one follows the pointer and clamps, the placement a tooltip needs rather than a docked
 * panel.
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
     * Builds the tooltip box for measured content of {@code contentWidth} spanning {@code lineCount}
     * lines of {@code lineHeight} each, offset from the cursor and clamped fully on screen. When the
     * box is larger than the screen on an axis the clamp pins that axis to the origin, so the box
     * stays anchored rather than sliding off the far edge.
     *
     * @param contentWidth the widest measured text line, in UI units
     * @param lineCount    how many text lines the box holds (at least one)
     * @param lineHeight   the height of one text line, in UI units
     * @param cursorX      the cursor x, in UI coordinates (UI origin is bottom-left)
     * @param cursorY      the cursor y, in UI coordinates
     * @param screenWidth  the screen width in UI units, the right clamp bound
     * @param screenHeight the screen height in UI units, the top clamp bound
     * @return the box footprint, lower-left origin, fully within the screen
     */
    public static Rectangle computeBox(
            double contentWidth,
            int lineCount,
            double lineHeight,
            float cursorX,
            float cursorY,
            float screenWidth,
            float screenHeight) {
        var width = (float) contentWidth + PADDING + PADDING;
        var textHeight = (float) (lineCount * lineHeight) + (lineCount - 1) * LINE_GAP;
        var height = textHeight + PADDING + PADDING;
        var x = Math.min(cursorX + CURSOR_OFFSET, screenWidth - width);
        var y = Math.min(cursorY + CURSOR_OFFSET, screenHeight - height);
        return new Rectangle(Math.max(x, 0f), Math.max(y, 0f), width, height);
    }
}
