package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.math.geometry.Rectangles;
import kmlib.starsector.ui.controls.RadioAlignment;
import kmlib.starsector.ui.widgets.segments.HorizontalSegments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The geometry of a row of N equal, mutually exclusive segments - the model behind a radio group,
 * built from single-selection over adjacent cells. Substrate-independent: it splits a footprint into
 * segments and resolves which one a point falls in, rendering nothing, so a GL or a UI-API renderer
 * can paint against it. The raw-GL paint lives in
 * {@link kmlib.starsector.ui.render.gl.controls.RadioRowRenderer}.
 *
 * <p>Segments flow either way ({@link RadioAlignment}): a horizontal group splits into equal columns
 * left to right, a vertical group into equal rows top to bottom, so a compact option pair reads as a
 * strip while a longer option list reads as a column. The flow is a splitting concern only - {@link
 * #splitIntoSegments} takes it, while {@link #findSegmentIndexAt} runs over the already-split segments.
 *
 * <p>A vertical list can also lay its options across more than one column via {@link #splitIntoGrid}:
 * the options fill each column top to bottom before the next (column-major), so the first column holds
 * the earliest options exactly as a single-column list would, and a longer list wraps into further
 * columns rather than running off the bottom. One column reduces to the plain vertical split, so the
 * grid is the general case a single-column list is a special case of.
 */
public final class RadioRow {
    /** {@link #findSegmentIndexAt} returns this when the point falls outside every segment. */
    public static final int NO_SEGMENT = Rectangles.NONE;

    private RadioRow() {
    }

    /**
     * Lays {@code optionCount} options across {@code columnCount} equal columns of a vertical list,
     * filling each column top to bottom before the next (column-major), and returns one rectangle per
     * option in option order. The row count is {@code ceil(optionCount / columnCount)} - the tallest a
     * column needs - so every column is that many rows tall and the last column may end short of the
     * bottom (its missing options simply have no rectangle). Option {@code i} sits in column {@code i /
     * rowCount} at row {@code i % rowCount}, so the first column holds options 0..rowCount-1 exactly as
     * a single-column split would place them. A count of one column is the plain vertical split; a
     * non-positive option or column count yields no segments.
     *
     * @param bounds      the list's whole footprint
     * @param optionCount how many options to place
     * @param columnCount how many equal columns to spread them across
     * @return the option rectangles, in option order (empty when either count is non-positive)
     */
    public static List<Rectangle> splitIntoGrid(Rectangle bounds, int optionCount, int columnCount) {
        if (optionCount <= 0 || columnCount <= 0) {
            return List.of();
        }
        var rowCount = computeRowsPerColumn(optionCount, columnCount);
        var columnWidth = bounds.width() / columnCount;
        var rowHeight = bounds.height() / rowCount;
        var segments = new ArrayList<Rectangle>(optionCount);

        for (var index = 0; index < optionCount; index++) {

            var column = index / rowCount;
            var rowInColumn = index % rowCount;

            // UI y grows up, so row 0 hangs from the top edge and each later row drops one row height.
            segments.add(new Rectangle(
                bounds.x() + column * columnWidth,
                bounds.y() + bounds.height() - (rowInColumn + 1) * rowHeight,
                columnWidth,
                rowHeight));
        }
        return List.copyOf(segments);
    }

    /**
     * Divides {@code bounds} into {@code segmentCount} equal cells in the flow {@code alignment}
     * gives: horizontal splits by width left to right, vertical by height top to bottom (the first
     * cell hangs from the top edge). A count of zero or less yields no segments. The horizontal case
     * is the even-width special case of {@link HorizontalSegments}, so it lays its equal cells through
     * that shared placement rather than re-deriving the run; the vertical case is this widget's own.
     *
     * @param bounds       the row's footprint
     * @param segmentCount how many equal cells to split it into
     * @param alignment    the direction the cells flow in
     * @return the segment rectangles, in flow order (empty when {@code segmentCount <= 0})
     */
    public static List<Rectangle> splitIntoSegments(
            Rectangle bounds,
            int segmentCount,
            RadioAlignment alignment) {

        if (segmentCount <= 0) {
            return List.of();
        }
        if (alignment != RadioAlignment.VERTICAL) {
            var equalWidth = bounds.width() / segmentCount;
            return HorizontalSegments.placeSegments(
                bounds.x(),
                bounds.y(),
                bounds.height(),
                Collections.nCopies(segmentCount, equalWidth));
        }
        // Cells stack top to bottom; the first hangs from the top edge and each later cell drops one
        // cell height, so element 0 is the topmost row (UI y grows up).
        var segments = new ArrayList<Rectangle>(segmentCount);
        var segmentHeight = bounds.height() / segmentCount;

        for (var index = 0; index < segmentCount; index++) {
            segments.add(new Rectangle(
                bounds.x(),
                bounds.y() + bounds.height() - (index + 1) * segmentHeight,
                bounds.width(),
                segmentHeight));
        }
        return List.copyOf(segments);
    }

    /**
     * How many cells tall each column of a column-major grid of {@code optionCount} options across
     * {@code columnCount} columns stands - the tallest a column needs, so the options fill the columns
     * without one running past the grid. Column-major fill keeps each column's cells contiguous, so this
     * is a plain divide-rounding-up (the last column may end short). It is the grid's height in cells:
     * the SSOT the layout sizes the grid's height with, the renderer rules its horizontal dividers by,
     * and {@link #splitIntoGrid} places its cells from, so the three cannot disagree. A non-positive
     * option or column count yields no cells.
     *
     * @param optionCount how many options the grid holds
     * @param columnCount how many columns they wrap across
     * @return the cells in the tallest column, or 0 when either count is non-positive
     */
    public static int computeRowsPerColumn(int optionCount, int columnCount) {
        if (optionCount <= 0 || columnCount <= 0) {
            return 0;
        }
        return (optionCount + columnCount - 1) / columnCount;
    }

    /**
     * The index of the segment containing {@code (pointX, pointY)}, or {@link #NO_SEGMENT} when the
     * point falls outside every segment. Runs over the already-split {@code segments}, so it needs
     * no alignment; abutting segments share an edge, which resolves to the earlier segment.
     *
     * @param segments the row's segments, in flow order (as {@link #splitIntoSegments} returns them)
     * @param pointX   the point's x, in UI coordinates
     * @param pointY   the point's y, in UI coordinates
     * @return the containing segment's index, or {@link #NO_SEGMENT}
     */
    public static int findSegmentIndexAt(List<Rectangle> segments, float pointX, float pointY) {
        return Rectangles.findIndexContaining(segments, pointX, pointY);
    }
}
