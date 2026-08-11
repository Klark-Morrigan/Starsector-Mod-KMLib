package kmlib.starsector.ui.widgets.tooltip;

import kmlib.math.geometry.Rectangle;

import java.util.List;
import java.util.Objects;

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
     * <p>Between the label and the trailing column sits the row's {@link TooltipLeaderLine} - the stretch
     * a rule may be led along to tie the two ends of a wide row together. It is resolved here, with the
     * columns it runs between, rather than left to a renderer to work out from the anchors: the label's
     * width and the value's are measurements this layout already made, and a paint measuring them again
     * would be joining columns it had derived a second time.
     *
     * @param rowTopY          the row's top edge, in UI coordinates (UI origin is bottom-left), which
     *                         every part of the row anchors from
     * @param lineHeight       the height of the line the row occupies, in UI units
     * @param leadingRowSlotX  the leading column's left-anchor x, in UI coordinates
     * @param labelRunXs       each label run's left-anchor x, in UI coordinates, in run order
     * @param leaderLine       the stretch between label and value a rule runs along, or
     *                         {@link TooltipLeaderLine#NONE} where the row rules none
     * @param trailingRowSlotX the trailing column's right-anchor x, in UI coordinates
     */
    public record TooltipRowLayout(
        float rowTopY,
        float lineHeight,
        float leadingRowSlotX,
        List<Float> labelRunXs,
        TooltipLeaderLine leaderLine,
        float trailingRowSlotX) {

        /**
         * Rejects a null rule at construction: a row that rules none carries
         * {@link TooltipLeaderLine#NONE}, so a null is a placement built wrongly rather than a row with
         * nothing to rule - and would otherwise surface inside a draw call, past the point that could
         * say which row was meant.
         */
        public TooltipRowLayout {
            Objects.requireNonNull(leaderLine, "leaderLine");
            labelRunXs = List.copyOf(labelRunXs);
        }
    }
}
