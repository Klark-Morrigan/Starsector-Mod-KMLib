package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.layout.TooltipBoxLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * The layout of a free-floating tooltip that follows the cursor: a vertical stack of {@link
 * TooltipRow}s - each an optional crest, a label, an optional marker trailing it, and an optional
 * right-aligned value - sized to hold its widest row and placed near the pointer. Substrate-independent: it measures text through a
 * {@link LineWidthMeasurer} port and returns rectangles and anchors, rendering nothing, so a GL or a
 * UI-API renderer paints against the same geometry. The raw-GL paint lives in
 * {@link kmlib.starsector.ui.render.gl.CursorTooltipRenderer}.
 *
 * <p>The padding and the screen clamp are {@link TooltipBoxLayout}'s; this widget adds the row model
 * on top - it measures how wide and how tall the rows stack, and lays each row's crest square, label
 * anchor, marker anchor, and value anchor within the placed box. The value gap is reserved on every
 * row, so a value-less row keeps its label clear of the value column; the marker gap is not, since a
 * marker reads as part of its label's line rather than as a column the other rows align to. The crest
 * column is reserved per box, not per row: it is reserved only when at least one row carries a crest,
 * so a box whose rows are all crest-less lays its labels flush with no empty crest gutter, while a box
 * with any crest reserves the column on every row so a crest-less row still aligns under the crested
 * ones - except a row that steps out of the column deliberately, which lays flush regardless.
 *
 * <p>Rows stack a line apart, and a row that opens a section takes half a line more above it. The
 * break is the widget's rather than the caller's arithmetic: a caller says which rows start a block,
 * and how far apart blocks stand is one decision made here for every tooltip.
 *
 * <p>Cursor-follow placement is the counterpart to {@link kmlib.starsector.ui.layout.BoxPlacement}'s
 * fixed screen-anchor placement: a docked panel pins to an edge, a tooltip trails the pointer and
 * clamps, and both size and place a box off the same {@code kmlib.starsector.ui.layout} geometry
 * rather than each re-deriving it.
 */
public final class CursorTooltip {
    // The gap between a row's crest and its label, between the label and a marker trailing it, and
    // between the label and a right-aligned value, in UI units. The value gap is reserved on every row
    // (a value-less row measures a zero-width value), so the value column stays clear of the widest
    // label whether or not each row fills it. The marker gap is a word space rather than a column, so
    // it is charged only to the rows that carry a marker.
    private static final float CREST_GAP = 6f;
    private static final float MARKER_GAP = 6f;
    private static final float VALUE_GAP = 16f;

    // The breathing room above a row that opens a section, as a fraction of the line height, so the
    // break scales with the text rather than being a fixed pixel step. Half a line reads as a parted
    // block without looking like a dropped row.
    private static final double SECTION_BREAK_FRACTION = 0.5;

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

        // Every width below is measured at this one size, so the size is bound in here once rather
        // than travelling beside the measurer through each step that measures a span.
        ToDoubleFunction<String> measureWidth = line ->
                measurer.measureLineWidth(line, lineHeight);

        var contentWidth = measureContentWidth(
                rows,
                reservesCrestColumn,
                crestSize,
                measureWidth);
        var box = TooltipBoxLayout.computeBox(
                contentWidth,
                measureContentHeight(rows, lineHeight),
                cursorX,
                cursorY,
                screenWidth,
                screenHeight);
        var rowLayouts = placeRows(
                rows,
                box,
                reservesCrestColumn,
                crestSize,
                measureWidth,
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

    // How tall the rows stack: a line each, the inter-line gap between them, and the extra break above
    // every row that opens a section. The one place that rule lives - the placement below steps down by
    // the same amounts, so the box is always exactly as tall as the rows drawn into it.
    private static double measureContentHeight(List<TooltipRow> rows, double lineHeight) {
        var height = 0d;
        for (var index = 0; index < rows.size(); index++) {
            height += lineHeight + measureLeadingGap(
                    rows.get(index),
                    index,
                    lineHeight);
        }
        return height;
    }

    // What a row adds above itself before its own line: nothing for the first row, which already sits
    // under the box's padding; otherwise the inter-line gap, plus the section break when the row opens
    // one. A break on the first row is deliberately dropped rather than padding the box's top edge.
    private static double measureLeadingGap(TooltipRow row, int index, double lineHeight) {
        if (index == 0) {
            return 0d;
        }
        return row.hasSectionBreak()
                ? TooltipBoxLayout.LINE_GAP + lineHeight * SECTION_BREAK_FRACTION
                : TooltipBoxLayout.LINE_GAP;
    }

    // Places each row's crest, label, and value within the box, stepping down one line height plus its
    // leading gap per row from the top content edge, so the rows stack the way the box was sized.
    private static List<TooltipLayout.TooltipRowLayout> placeRows(
            List<TooltipRow> rows,
            Rectangle box,
            boolean reservesCrestColumn,
            float crestSize,
            ToDoubleFunction<String> measureWidth,
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

        // Walks the stack downward, spending each row's leading gap before its own line, so a section
        // break parts the rows exactly where the height measurement said it would.
        var rowTopY = topY;

        for (var index = 0; index < rows.size(); index++) {
            var row = rows.get(index);
            rowTopY -= (float) measureLeadingGap(row, index, lineHeight);

            placements.add(placeRow(
                    row,
                    leftX,
                    rightX,
                    rowTopY,
                    reservesCrestColumn,
                    crestSize,
                    measureWidth));
            rowTopY -= (float) lineHeight;
        }
        return placements;
    }

    // Places one row: the crest square in the indented icon column (null when the row has no crest, so
    // the renderer skips it while the label still clears any reserved column), the label past the
    // column, a marker one gap past the label's own width, and the value right-anchored to the box's
    // right content edge. The crest column is added to the label offset only when the box reserves it;
    // an all-crest-less box lays the label at the row's indent. Only a marked row pays to measure its
    // label, since that width is what the marker anchors off and nothing else here needs it.
    private static TooltipLayout.TooltipRowLayout placeRow(
            TooltipRow row,
            float leftX,
            float rightX,
            float rowTopY,
            boolean reservesCrestColumn,
            float crestSize,
            ToDoubleFunction<String> measureWidth) {

        var crestX = leftX + row.indent();
        var crestBox = row.crestSpritePath() == null
                ? null
                : new Rectangle(
                        crestX,
                        rowTopY - crestSize,
                        crestSize,
                        crestSize);

        var textX = crestX + crestColumnWidth(row, reservesCrestColumn, crestSize);
        var markerX = row.hasMarker()
                ? textX
                        + (float) measureWidth.applyAsDouble(row.text())
                        + MARKER_GAP
                : textX;
                
        return new TooltipLayout.TooltipRowLayout(
                crestBox,
                textX,
                rowTopY,
                markerX,
                rowTopY,
                rightX,
                rowTopY);
    }

    // The widest laid-out row: each row is its indent, the reserved crest column (when the box has one),
    // its measured label, its marker span when it carries one, the value gap, and its measured value, so
    // a wide indented member sizes the box just as a wide header would. The content width before the box
    // layout adds its padding.
    private static double measureContentWidth(
            List<TooltipRow> rows,
            boolean reservesCrestColumn,
            float crestSize,
            ToDoubleFunction<String> measureWidth) {

        var widest = 0d;
        for (var row : rows) {
            var textWidth = measureWidth.applyAsDouble(row.text());
            var valueWidth = measureWidth.applyAsDouble(row.value());

            var rowWidth = row.indent()
                    + crestColumnWidth(row, reservesCrestColumn, crestSize)
                    + textWidth
                    + measureMarkerSpan(row, measureWidth)
                    + VALUE_GAP
                    + valueWidth;

            widest = Math.max(widest, rowWidth);
        }
        return widest;
    }

    // What a row's marker adds to its width: its gap plus its own measured text, or nothing at all for
    // a marker-less row - the gap is charged with the marker rather than reserved on every row, so an
    // unmarked box is never padded for a marker column no row fills.
    private static double measureMarkerSpan(
            TooltipRow row,
            ToDoubleFunction<String> measureWidth) {
        if (!row.hasMarker()) {
            return 0d;
        }
        return MARKER_GAP + measureWidth.applyAsDouble(row.marker());
    }

    // The horizontal space the crest column costs this row - the crest square plus its gap when the box
    // reserves the column, or nothing when no row carries a crest or when this row steps out of the
    // column to lay flush. One source so the width measurement and the label placement agree on the
    // offset, row by row.
    private static float crestColumnWidth(
            TooltipRow row, boolean reservesCrestColumn, float crestSize) {
        if (row.isOutsideCrestColumn() || !reservesCrestColumn) {
            return 0f;
        }
        return crestSize + CREST_GAP;
    }
}
