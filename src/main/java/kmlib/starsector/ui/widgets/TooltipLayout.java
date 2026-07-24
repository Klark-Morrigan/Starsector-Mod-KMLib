package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

import java.util.List;

/**
 * The resolved geometry of a laid-out {@link CursorTooltip}: the box footprint and, per row in draw
 * order, where its crest, label, and value anchor within that box. The pure output {@link
 * CursorTooltip#layOut} hands a renderer, so the renderer paints against fixed rectangles and anchors
 * rather than re-deriving the box maths - the drawn crest, label, and value cannot drift from the
 * measured box.
 *
 * @param box  the tooltip's full footprint, lower-left origin, clamped on screen
 * @param rows one placement per content row, in the same order the rows were given
 */
public record TooltipLayout(Rectangle box, List<TooltipRowLayout> rows) {

    public TooltipLayout {
        rows = List.copyOf(rows);
    }

    /**
     * Where one row's three parts anchor: the crest square (null when the row carries no crest), the
     * label's top-left corner, and the value's top-right corner. The crest and label share a baseline
     * at the row's top edge, and the value right-aligns to the box's right content edge, so a stack of
     * rows reads as an icon-label-value table.
     *
     * @param crestBox the square the crest draws into, or null when the row has no crest
     * @param textX    the label's left-anchor x, in UI coordinates
     * @param textY    the label's top-anchor y, in UI coordinates (UI origin is bottom-left)
     * @param valueX   the value's right-anchor x, in UI coordinates
     * @param valueY   the value's top-anchor y, in UI coordinates
     */
    public record TooltipRowLayout(
            Rectangle crestBox,
            float textX,
            float textY,
            float valueX,
            float valueY) {
    }
}
