package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.layout.TooltipBoxLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * The layout of a free-floating tooltip that follows the cursor: a vertical stack of {@link
 * TooltipRow}s - each an optional crest, a label, and an optional right-aligned value - sized to hold
 * its widest row and placed near the pointer. Substrate-independent: it measures text through a
 * {@link LineWidthMeasurer} port and returns rectangles and anchors, rendering nothing, so a GL or a
 * UI-API renderer paints against the same geometry. The raw-GL paint lives in
 * {@link kmlib.starsector.ui.render.gl.CursorTooltipRenderer}.
 *
 * <p>The sizing and the screen clamp are {@link TooltipBoxLayout}'s; this widget adds the row model on
 * top - it lays each row's crest square, label anchor, and value anchor within the placed box, and
 * measures each row across its indent, crest column, label, and value so the box holds the widest row
 * across all tiers. The value gap is reserved on every row, so a value-less row keeps its label clear
 * of the value column. The crest column is reserved per box, not per row: it is reserved only when at
 * least one row carries a crest, so a box whose rows are all crest-less lays its labels flush with no
 * empty crest gutter, while a box with any crest reserves the column on every row so a crest-less row
 * still aligns under the crested ones.
 *
 * <p>Cursor-follow placement is the counterpart to {@link kmlib.starsector.ui.layout.BoxPlacement}'s
 * fixed screen-anchor placement: a docked panel pins to an edge, a tooltip trails the pointer and
 * clamps, and both size and place a box off the same {@code kmlib.starsector.ui.layout} geometry
 * rather than each re-deriving it.
 */
public final class CursorTooltip {
    // The gap between a row's crest and its label, and between the label and a right-aligned value, in
    // UI units. The value gap is reserved on every row (a value-less row measures a zero-width value),
    // so the value column stays clear of the widest label whether or not each row fills it.
    private static final float CREST_GAP = 6f;
    private static final float VALUE_GAP = 16f;

    private CursorTooltip() {
    }

    /**
     * Lays {@code rows} into a cursor-following box: sizes it to the widest row across all tiers,
     * places it up-and-right of the cursor clamped on screen, and resolves each row's crest, label,
     * and value anchors within it. The crest square is one line tall, so it sits level with its label.
     * The crest column is reserved only when some row carries a crest: in a mixed box a crest-less row
     * still reserves it (and gets a null crest box) so its label aligns under the crested rows, while
     * an all-crest-less box reserves nothing and lays its labels flush.
     *
     * @param rows         the content rows, top to bottom; an empty list yields a padding-only box
     * @param lineHeight   the text line height, which is also each crest square's side, in UI units
     * @param measurer     the font-agnostic width measurement for the labels and values
     * @param cursorX      the cursor x, in UI coordinates (UI origin is bottom-left)
     * @param cursorY      the cursor y, in UI coordinates
     * @param screenWidth  the screen width in UI units, the right clamp bound
     * @param screenHeight the screen height in UI units, the top clamp bound
     * @return the placed box and the per-row anchors, in row order
     */
    public static TooltipLayout layOut(
            List<TooltipRow> rows,
            double lineHeight,
            LineWidthMeasurer measurer,
            float cursorX,
            float cursorY,
            float screenWidth,
            float screenHeight) {
        var crestSize = (float) lineHeight;
        var reservesCrestColumn = anyRowCarriesCrest(rows);
        var contentWidth = measureContentWidth(
                rows,
                reservesCrestColumn,
                crestSize,
                measurer,
                lineHeight);
        var box = TooltipBoxLayout.computeBox(
                contentWidth,
                rows.size(),
                lineHeight,
                cursorX,
                cursorY,
                screenWidth,
                screenHeight);
        var rowLayouts = placeRows(
                rows,
                box,
                reservesCrestColumn,
                crestSize,
                lineHeight);
        return new TooltipLayout(box, rowLayouts);
    }

    // Whether any row carries a crest, so the box reserves the crest column for every row. When no row
    // does, the column collapses and the labels lay flush against the left content edge.
    private static boolean anyRowCarriesCrest(List<TooltipRow> rows) {
        for (var row : rows) {
            if (row.crestSpritePath() != null) {
                return true;
            }
        }
        return false;
    }

    // Places each row's crest, label, and value within the box, stepping down one line height plus the
    // inter-line gap per row from the top content edge, so the rows stack the way the box was sized.
    private static List<TooltipLayout.TooltipRowLayout> placeRows(
            List<TooltipRow> rows,
            Rectangle box,
            boolean reservesCrestColumn,
            float crestSize,
            double lineHeight) {

        var leftX = box.x()
                + TooltipBoxLayout.PADDING;

        var rightX = box.x()
                + box.width()
                - TooltipBoxLayout.PADDING;

        var topY = box.y()
                + box.height()
                - TooltipBoxLayout.PADDING;

        var placements = new ArrayList<TooltipLayout.TooltipRowLayout>(rows.size());

        for (var index = 0; index < rows.size(); index++) {
            var row = rows.get(index);
            var rowTopY = topY - index * ((float) lineHeight + TooltipBoxLayout.LINE_GAP);

            placements.add(placeRow(row, leftX, rightX, rowTopY, reservesCrestColumn, crestSize));
        }
        return placements;
    }

    // Places one row: the crest square in the indented icon column (null when the row has no crest, so
    // the renderer skips it while the label still clears any reserved column), the label past the
    // column, and the value right-anchored to the box's right content edge. The crest column is added
    // to the label offset only when the box reserves it; an all-crest-less box lays the label at the
    // row's indent.
    private static TooltipLayout.TooltipRowLayout placeRow(
            TooltipRow row,
            float leftX,
            float rightX,
            float rowTopY,
            boolean reservesCrestColumn,
            float crestSize) {

        var crestX = leftX + row.indent();
        var crestBox = row.crestSpritePath() == null
                ? null
                : new Rectangle(crestX, rowTopY - crestSize, crestSize, crestSize);
        var textX = crestX + crestColumnWidth(reservesCrestColumn, crestSize);
        return new TooltipLayout.TooltipRowLayout(crestBox, textX, rowTopY, rightX, rowTopY);
    }

    // The widest laid-out row: each row is its indent, the reserved crest column (when the box has one),
    // its measured label, the value gap, and its measured value, so a wide indented member sizes the box
    // just as a wide header would. The content width before the box layout adds its padding.
    private static double measureContentWidth(
            List<TooltipRow> rows,
            boolean reservesCrestColumn,
            float crestSize,
            LineWidthMeasurer measurer,
            double lineHeight) {

        var crestColumn = crestColumnWidth(reservesCrestColumn, crestSize);
        var widest = 0d;
        for (var row : rows) {
            var textWidth = measurer.measureLineWidth(row.text(), lineHeight);
            var valueWidth = measurer.measureLineWidth(row.value(), lineHeight);

            var rowWidth = row.indent()
                    + crestColumn
                    + textWidth
                    + VALUE_GAP
                    + valueWidth;

            widest = Math.max(widest, rowWidth);
        }
        return widest;
    }

    // The horizontal space the crest column occupies - the crest square plus its gap when the box
    // reserves the column, or nothing when no row carries a crest. One source so the width measurement
    // and the label placement agree on the offset.
    private static float crestColumnWidth(boolean reservesCrestColumn, float crestSize) {
        return reservesCrestColumn ? crestSize + CREST_GAP : 0f;
    }
}
