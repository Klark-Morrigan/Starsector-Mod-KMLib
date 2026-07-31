package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

import java.util.List;

/**
 * The resolved geometry of a laid-out {@link CursorTooltip}: the box footprint and, per row in draw
 * order, where that row's line sits and where each of its columns starts. The pure output
 * {@link CursorTooltip#layOut} hands a renderer, so the renderer paints against fixed anchors rather
 * than re-deriving the box maths - what is drawn cannot drift from the measured box.
 *
 * @param box  the tooltip's full footprint, lower-left origin, clamped on screen
 * @param rows one placement per content row, in the same order the rows were given
 */
public record TooltipLayout(
    Rectangle box,
    List<TooltipRowLayout> rows) {

    public TooltipLayout {
        rows = List.copyOf(rows);
    }

    /**
     * Where one row lands: the line it occupies, and one anchor per column across it - the leading slot
     * from its left edge, each label run from its own left edge, and the trailing slot from its right
     * edge, so a stack of rows reads as a table with the label's runs flowing inside the label column.
     *
     * <p>The line is stated once, as a top edge and a height, rather than once per column. Every part of
     * a row shares the line it sits on, so a y per part would be one value handed over three times and a
     * reader would have to check all three to learn they never differ. The height is what a slot drawing
     * a shape rather than glyphs squares itself off, and what a renderer hangs the leading image in.
     *
     * <p>The two slot anchors are the edges the columns are reserved from, not boxes: what fills each
     * column decides how much of it it takes, and the trailing column is filled from the right so that a
     * stack of rows aligns its values whatever each measures. One anchor per label run, in the row's own
     * run order, so a renderer walks the runs and the anchors together rather than re-measuring what the
     * layout already measured. A run with nothing to draw anchors where the run before it ended: nothing
     * is drawn there, so the anchor is unused rather than wrong, and the run costs the line no gap.
     *
     * @param rowTopY          the row's top edge, in UI coordinates (UI origin is bottom-left), which
     *                         every part of the row anchors from
     * @param lineHeight       the height of the line the row occupies, in UI units
     * @param leadingRowSlotX  the leading column's left-anchor x, in UI coordinates
     * @param labelRunXs       each label run's left-anchor x, in UI coordinates, in run order
     * @param trailingRowSlotX the trailing column's right-anchor x, in UI coordinates
     */
    public record TooltipRowLayout(
        float rowTopY,
        float lineHeight,
        float leadingRowSlotX,
        List<Float> labelRunXs,
        float trailingRowSlotX) {

        public TooltipRowLayout {
            labelRunXs = List.copyOf(labelRunXs);
        }
    }
}
