package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.TextSpanMeasurer;
import kmlib.starsector.ui.layout.TooltipBoxLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * The layout of a free-floating tooltip that follows the cursor: a vertical stack of {@link
 * TooltipRow}s - each an optional crest, a label, an optional marker trailing it, and an optional
 * right-aligned value - sized to hold its widest row and placed near the pointer.
 * Substrate-independent: it measures text through a {@link TextSpanMeasurer} port and returns
 * rectangles and anchors, rendering nothing, so a GL or a UI-API renderer paints against the same
 * geometry. The raw-GL paint lives in
 * {@link kmlib.starsector.ui.render.gl.CursorTooltipRenderer}.
 *
 * <p>The padding and the screen clamp are {@link TooltipBoxLayout}'s; this widget adds the row model
 * on top - it measures how wide and how tall the rows stack, and lays each row's crest square, label
 * anchor, marker anchor, and value anchor within the placed box. The value gap is reserved on every
 * row, so a value-less row keeps its label clear of the value column; the marker gap is not, since a
 * marker reads as part of its label's line rather than as a column the other rows align to. The crest
 * column is reserved per box, not per row: it is reserved only when some crest-aligned row carries a
 * crest, so a box whose rows are all crest-less lays its labels at the content edge with no empty
 * gutter, while a box with any crest reserves the column on every crest-aligned row so a crest-less one
 * still lines up under the crested ones. One width for the whole box, wide enough for its tallest crest:
 * the column exists so that labels line up, which a width settled row by row would defeat. Which rows
 * align to it at all is each row's own {@link TooltipLabelPlacement}.
 *
 * <p>A centred row is laid outside that column model entirely: its label and marker centre as one span
 * in the content region, and it is sized to that span alone, so a title or a lone statement centres over
 * whatever the box holds rather than aligning as an entry of it.
 *
 * <p>Rows stack a line apart, and a row that opens a section takes half a line more above it. The
 * break is the widget's rather than the caller's arithmetic: a caller says which rows start a block,
 * and how far apart blocks stand is one decision made here for every tooltip.
 *
 * <p>Every row's own look is resolved up front from the box's {@link TooltipStyle}, because a box's rows
 * need not share a face: a heading drawn in the game's blockier title atlas is far wider than a body
 * line of the same size, and it stacks at its own height. So the line height a row occupies and the face
 * its spans are measured on are both read per row rather than threaded once through the whole layout,
 * and a heading is measured on exactly the face it will be painted in.
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

    // The crest column's width in a box that reserves none - either because no row carries a crest, or
    // for a row that steps out of the column to lay flush.
    private static final float NO_CREST_COLUMN = 0f;

    // The breathing room above a row that opens a section, as a fraction of the line height, so the
    // break scales with the text rather than being a fixed pixel step. Half a line reads as a parted
    // block without looking like a dropped row.
    private static final double SECTION_BREAK_FRACTION = 0.5;

    private CursorTooltip() {
    }

    /**
     * Lays {@code rows} into a cursor-following box: sizes it to the widest row across all tiers,
     * places it up-and-right of the cursor clamped on screen, and resolves each row's crest, label,
     * and value anchors within it. Each row stacks at - and hangs a crest square as tall as - the line
     * height its own kind of line draws at, so a heading takes the room its face needs. The crest column
     * is reserved only when some row carries a crest, and then at one width across the box: in a mixed
     * box a crest-less row still reserves it (and gets a null crest box) so its label aligns under the
     * crested rows, while an all-crest-less box reserves nothing and lays its labels flush.
     *
     * @param rows         the content rows, top to bottom; an empty list yields a padding-only box
     * @param style        the look each kind of line draws in, from which every row's face, size, and
     *                     casing is resolved
     * @param measurer     the font-agnostic width measurement, asked per face
     * @param cursorX      the cursor x, in UI coordinates (UI origin is bottom-left)
     * @param cursorY      the cursor y, in UI coordinates
     * @param screenWidth  the screen width in UI units, the right clamp bound
     * @param screenHeight the screen height in UI units, the top clamp bound
     * @return the placed box and the per-row anchors, in row order
     */
    public static TooltipLayout layOut(
            List<TooltipRow> rows,
            TooltipStyle style,
            TextSpanMeasurer measurer,
            float cursorX,
            float cursorY,
            float screenWidth,
            float screenHeight) {

        var styledRows = bindRowsToStyles(rows, style, measurer);
        var crestColumnWidth = measureCrestColumnWidth(styledRows);

        var box = TooltipBoxLayout.computeBox(
                measureContentWidth(styledRows, crestColumnWidth),
                measureContentHeight(styledRows),
                cursorX,
                cursorY,
                screenWidth,
                screenHeight);
        return new TooltipLayout(
                box,
                placeRows(styledRows, box, crestColumnWidth));
    }

    // Resolves every row's look once, before anything is measured. Everything below then reads the
    // resolved pair rather than the style and the measurer, which is what makes it impossible for one
    // row to be measured on one face and then laid out at another's line height.
    private static List<StyledRow> bindRowsToStyles(
            List<TooltipRow> rows,
            TooltipStyle style,
            TextSpanMeasurer measurer) {

        var styledRows = new ArrayList<StyledRow>(rows.size());
        for (var row : rows) {
            styledRows.add(StyledRow.bindRowToStyle(row, style, measurer));
        }
        return styledRows;
    }

    // The crest column's one width for the whole box: room for the tallest crest any row carries, plus
    // the gap to the labels past it. Nothing at all when no row carries a crest, so the column collapses
    // and the labels lay flush against the left content edge. Settled per box rather than per row even
    // though the crests are squared off their own rows' line heights, because the column exists so that
    // the labels line up - a width settled row by row would set each label at its own offset, leaving no
    // column to align to - and sized to the tallest so that no crest is clipped by the column holding it.
    private static float measureCrestColumnWidth(List<StyledRow> rows) {
        var isReserved = false;
        var tallestCrest = 0d;
        for (var styledRow : rows) {
            var row = styledRow.row();

            // Only the crests of the rows that align to the column size it. A label placed anywhere else
            // starts before it - so its crest neither needs the column nor gets a say in how wide it is,
            // and one edge-flush row cannot open a gutter that shifts every row that does align to it.
            if (row.hasCrest() && row.labelPlacement() == TooltipLabelPlacement.ALIGNED_WITH_CRESTS) {
                isReserved = true;
                tallestCrest = Math.max(tallestCrest, styledRow.lineHeight());
            }
        }
        return isReserved
                ? (float) tallestCrest + CREST_GAP
                : NO_CREST_COLUMN;
    }

    // How tall the rows stack: each row's own line height, the inter-line gap between them, and the extra
    // break above every row that opens a section. The one place that rule lives - the placement below
    // steps down by the same amounts, so the box is always exactly as tall as the rows drawn into it.
    private static double measureContentHeight(List<StyledRow> rows) {
        var height = 0d;
        for (var index = 0; index < rows.size(); index++) {
            var styledRow = rows.get(index);
            height += styledRow.lineHeight() + measureLeadingGap(styledRow, index);
        }
        return height;
    }

    // What a row adds above itself before its own line: nothing for the first row, which already sits
    // under the box's padding; otherwise the inter-line gap, plus the section break when the row opens
    // one. A break on the first row is deliberately dropped rather than padding the box's top edge. The
    // break is a fraction of the opening row's own line height, so a heading opens a section with the
    // breathing room its own size asks for rather than the body's.
    private static double measureLeadingGap(StyledRow styledRow, int index) {
        if (index == 0) {
            return 0d;
        }
        return styledRow.row().hasSectionBreak()
                ? TooltipBoxLayout.LINE_GAP + styledRow.lineHeight() * SECTION_BREAK_FRACTION
                : TooltipBoxLayout.LINE_GAP;
    }

    // Places each row's crest, label, and value within the box, stepping down its own line height plus
    // its leading gap per row from the top content edge, so the rows stack the way the box was sized.
    private static List<TooltipLayout.TooltipRowLayout> placeRows(
            List<StyledRow> rows,
            Rectangle box,
            float crestColumnWidth) {

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
            var styledRow = rows.get(index);
            rowTopY -= (float) measureLeadingGap(styledRow, index);

            placements.add(placeRow(
                    styledRow,
                    leftX,
                    rightX,
                    rowTopY,
                    crestColumnWidth));
            rowTopY -= (float) styledRow.lineHeight();
        }
        return placements;
    }

    // Places one row: the crest square in the indented icon column (null when the row has no crest, so
    // the renderer skips it while the label still clears any reserved column), the label past the
    // column, a marker one gap past the label's own width, and the value right-anchored to the box's
    // right content edge. The crest column is added to the label offset only when the box reserves it;
    // an all-crest-less box lays the label at the row's indent. A centred row takes none of that - its
    // label anchors off the content region's midpoint instead - while its marker still rides off that
    // anchor, so the label and its marker centre as one span.
    private static TooltipLayout.TooltipRowLayout placeRow(
            StyledRow styledRow,
            float leftX,
            float rightX,
            float rowTopY,
            float crestColumnWidth) {

        var row = styledRow.row();
        var crestX = leftX + row.indent();

        // The crest is squared off the row's own line height, so it sits level with the label beside it
        // whatever face that label draws in - which is why it is the row's height and not the column's.
        var crestSize = (float) styledRow.lineHeight();
        var crestBox = row.hasCrest()
                ? new Rectangle(
                        crestX,
                        rowTopY - crestSize,
                        crestSize,
                        crestSize)
                : null;

        var textX = row.labelPlacement() == TooltipLabelPlacement.CENTRED
                ? centreLabelX(styledRow, leftX, rightX)
                : crestX + measureCrestOffset(styledRow, crestColumnWidth);

        var markerX = row.hasMarker()
                ? textX
                        + (float) styledRow.measureWidth().applyAsDouble(row.labelTextSpan().text())
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
    // its label span, the value gap, and its measured value, so a wide indented member sizes the box
    // just as a wide header would. A centred row is charged its label span alone - it occupies none of
    // those columns, so charging them would widen the box for space the centred line never fills, and
    // its own centring would then push it off the middle. Each row is measured on its own face, so a
    // heading in a wider atlas sizes the box to the width it will actually paint at. The content width
    // before the box layout adds its padding.
    private static double measureContentWidth(
            List<StyledRow> rows,
            float crestColumnWidth) {

        var widest = 0d;
        for (var styledRow : rows) {
            var row = styledRow.row();
            var labelSpan = measureLabelSpan(styledRow);
            var rowWidth = row.labelPlacement() == TooltipLabelPlacement.CENTRED
                    ? labelSpan
                    : row.indent()
                            + measureCrestOffset(styledRow, crestColumnWidth)
                            + labelSpan
                            + VALUE_GAP
                            + styledRow.measureWidth().applyAsDouble(row.valueTextSpan().text());

            widest = Math.max(widest, rowWidth);
        }
        return widest;
    }

    // The span a row's own text occupies: its label plus its marker's gap and width when it carries one.
    // Shared by the width measurement and the centring below, so a centred row is placed against exactly
    // the span the box was sized to hold.
    private static double measureLabelSpan(StyledRow styledRow) {
        return styledRow.measureWidth().applyAsDouble(styledRow.row().labelTextSpan().text())
                + measureMarkerSpan(styledRow);
    }

    // Where a centred row's label starts: its span set in the middle of the content region, the leftover
    // split evenly to either side. A centred row that is itself the widest row sized the region to its
    // own span, so it lands flush at the content edge and nothing shifts.
    private static float centreLabelX(
            StyledRow styledRow,
            float leftX,
            float rightX) {

        var slack = rightX - leftX - (float) measureLabelSpan(styledRow);
        return leftX + slack / 2f;
    }

    // What a row's marker adds to its width: its gap plus its own measured text, or nothing at all for
    // a marker-less row - the gap is charged with the marker rather than reserved on every row, so an
    // unmarked box is never padded for a marker column no row fills.
    private static double measureMarkerSpan(StyledRow styledRow) {
        if (!styledRow.row().hasMarker()) {
            return 0d;
        }
        return MARKER_GAP
                + styledRow.measureWidth().applyAsDouble(styledRow.row().markerTextSpan().text());
    }

    // The horizontal space the crest gutter costs this row - the box's one column width for a label that
    // aligns to it, nothing for one that starts before it (and nothing either way in a box that reserved
    // no column, whose width is already nothing). One source so the width measurement and the label
    // placement agree on the offset, row by row.
    private static float measureCrestOffset(StyledRow styledRow, float crestColumnWidth) {
        return styledRow.row().labelPlacement() == TooltipLabelPlacement.ALIGNED_WITH_CRESTS
                ? crestColumnWidth
                : NO_CREST_COLUMN;
    }

    /**
     * One row bound to the look its kind of line resolved to: the height it stacks at, and a width
     * measurement already bound to its face and its casing. Bound once per layout so the height
     * measurement, the width measurement, and the placement cannot read three different looks for one
     * row - and so no step below has to carry the box's style and the measurer alongside the row it is
     * working on.
     *
     * @param row          the content row as its caller authored it
     * @param lineHeight   the height the row stacks at, which is also its crest square's side
     * @param measureWidth the width of one of this row's spans, in this row's own face
     */
    private record StyledRow(
            TooltipRow row,
            double lineHeight,
            ToDoubleFunction<String> measureWidth) {

        // Resolves the look for one row's kind of line and binds a measurement to it. The face doubles as
        // the line height, as a bitmap face's size is the room one line of it needs.
        private static StyledRow bindRowToStyle(
                TooltipRow row,
                TooltipStyle style,
                TextSpanMeasurer measurer) {

            var textStyle = style.resolveStyleFor(row.lineStyle());

            // Measured through the style's own display text, not the authored text: a shouted line
            // measured as authored measures narrower than it paints, so the box sized from that
            // measurement would clip the text drawn into it.
            return new StyledRow(
                    row,
                    textStyle.face().size(),
                    span -> measurer.measureSpanWidth(
                            textStyle.face(),
                            textStyle.resolveDisplayText(span)));
        }
    }
}
