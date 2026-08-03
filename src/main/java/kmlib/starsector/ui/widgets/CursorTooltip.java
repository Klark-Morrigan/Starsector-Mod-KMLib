package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.TextSpanMeasurer;
import kmlib.starsector.ui.layout.TooltipBoxLayout;
import kmlib.starsector.ui.text.LabelRuns;
import kmlib.starsector.ui.text.LabelRuns.LabelRunOffsets;
import kmlib.starsector.ui.text.TextSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * The layout of a free-floating tooltip that follows the cursor: a vertical stack of {@link
 * TooltipRow}s - table rows of an optional crest, a label of one or more runs, and an optional
 * right-aligned value, and centred rows of a label alone - sized to hold its widest row and placed near
 * the pointer. Substrate-independent: it measures text through a {@link TextSpanMeasurer} port and
 * returns rectangles and anchors, rendering nothing, so a GL or a UI-API renderer paints against the same
 * geometry. The raw-GL paint lives in
 * {@link kmlib.starsector.ui.render.gl.CursorTooltipRenderer}.
 *
 * <p>The padding and the screen clamp are {@link TooltipBoxLayout}'s; this widget adds the row model
 * on top - it measures how wide and how tall the rows stack, and lays each row's line and its three
 * column anchors within the placed box. The value gap is reserved on every row, so a value-less row
 * keeps its label clear of the value column; the gap between two label runs is not, since the runs read
 * as one sentence rather than as columns the other rows align to. The crest column is reserved per box,
 * not per row: it is reserved only when some crest-aligned row fills its leading slot, so a box whose
 * rows all leave it unfilled lays its labels at the content edge with no empty gutter, while a box with
 * any crest reserves the column on every crest-aligned row so a crest-less one still lines up under the
 * crested ones. One width for the whole box, wide enough for its widest crest: the column exists so that
 * labels line up, which a width settled row by row would defeat. Which rows align to it at all is each
 * row's own {@link TooltipLabelPlacement}.
 *
 * <p>Both columns are reserved from what the rows' own {@link RowSlot}s report, never from what those
 * slots turn out to hold, so a row that leads or trails with something other than the crest and value a
 * tooltip usually carries is measured and placed by the same arithmetic as the rest.
 *
 * <p>A {@link TooltipRow.CentredRow} is laid outside that column model entirely: its label's runs centre
 * together as one span in the content region, and it is sized to that span alone, so a title or a lone
 * statement centres over whatever the box holds rather than aligning as an entry of it. Such a row holds
 * no slots at all, which is what lets the box be sized to the span alone: there is nothing of it that
 * could land in a column the box was not widened for.
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

    // The gap between a row's crest and its label and between the label and a right-aligned value, in UI
    // units. Both are reserved on every row (a value-less row measures a zero-width value), so the value
    // column stays clear of the widest label whether or not each row fills it. The gap between two runs
    // of one label is not settled here: runs read as one sentence wherever they are laid, so LabelRuns
    // owns that spacing for every surface at once.
    private static final float CREST_GAP = 6f;
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
     * places it up-and-right of the cursor clamped on screen, and resolves each row's line and column
     * anchors within it. Each row stacks at - and hangs its leading slot as tall as - the line height
     * its own kind of line draws at, so a heading takes the room its face needs. The crest column is
     * reserved only when some table row fills its leading slot, and then at one width across the box: in
     * a mixed box a crest-less row still reserves it so its label aligns under the crested rows, while an
     * all-crest-less box reserves nothing and lays its labels flush.
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

    // The crest column's one width for the whole box: room for the widest leading slot any row fills,
    // plus the gap to the labels past it. Nothing at all when no row fills one, so the column collapses
    // and the labels lay flush against the left content edge. Settled per box rather than per row even
    // though a crest is squared off its own row's line height, because the column exists so that the
    // labels line up - a width settled row by row would set each label at its own offset, leaving no
    // column to align to - and sized to the widest so that nothing is clipped by the column holding it.
    //
    // Each slot is asked its own width rather than being read for a crest, so a row leading with
    // something other than an image reserves exactly what that thing takes and this measurement never
    // learns which kinds exist.
    private static float measureCrestColumnWidth(List<StyledRow> rows) {
        var widestLeadingRowSlotWidth = RowSlot.NO_WIDTH;
        for (var styledRow : rows) {

            // Only the rows that align to the column size it. A centred line holds no leading slot at
            // all, and a label placed at the content edge starts before the column - so neither needs it
            // nor gets a say in how wide it is, and one edge-flush row cannot open a gutter that shifts
            // every row that does align to it.
            if (!(styledRow.row() instanceof TooltipRow.TableRow tableRow)
                    || tableRow.labelPlacement() != TooltipLabelPlacement.ALIGNED_WITH_CRESTS) {
                continue;
            }
            widestLeadingRowSlotWidth = Math.max(
                widestLeadingRowSlotWidth,
                styledRow.measureSlotWidth(tableRow.labelledRow().leadingRowSlot()));
        }

        // An unfilled slot - and a run that came out blank - is worth nothing, so a box whose rows fill
        // no leading slot opens no gutter rather than one of zero width plus a gap.
        return widestLeadingRowSlotWidth > RowSlot.NO_WIDTH
            ? widestLeadingRowSlotWidth + CREST_GAP
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

    // Places one row's three columns off its own line: the leading slot at the indented icon column, the
    // label's runs past that column, and the trailing slot right-anchored to the box's right content
    // edge. The crest column is added to the label offset only when the box reserves it; an
    // all-crest-less box lays the label at the row's indent.
    //
    // The line is handed over once, as a top edge and the height the row stacks at, since every column
    // of the row sits on it. What each column then fills is the drawing side's to size, off its own
    // slot, so nothing here has to know which kinds a row can carry.
    private static TooltipLayout.TooltipRowLayout placeRow(
            StyledRow styledRow,
            float leftX,
            float rightX,
            float rowTopY,
            float crestColumnWidth) {

        // A centred line holds no columns to place. Its label anchors off the content region's midpoint
        // instead - its later runs still ride off that anchor, so a whole label centres as one span -
        // and the column anchors it is handed are the box's own content edges, which it draws nothing in.
        if (!(styledRow.row() instanceof TooltipRow.TableRow tableRow)) {
            return new TooltipLayout.TooltipRowLayout(
                rowTopY,
                (float) styledRow.lineHeight(),
                leftX,
                anchorLabelRuns(styledRow, centreLabelX(styledRow, leftX, rightX)),
                rightX);
        }
        var leadingRowSlotX = leftX + tableRow.indent();

        return new TooltipLayout.TooltipRowLayout(
            rowTopY,
            (float) styledRow.lineHeight(),
            leadingRowSlotX,
            anchorLabelRuns(
                styledRow,
                leadingRowSlotX + measureCrestOffset(tableRow, crestColumnWidth)),
            rightX);
    }

    // Where each of a row's label runs anchors in the placed box: the offsets the runs measured out at,
    // shifted to wherever the label itself starts. Shifting one measured walk rather than re-deriving
    // the offsets per placement is what keeps the anchors and the width the box was sized to in step
    // whatever the row's placement turns out to be.
    private static List<Float> anchorLabelRuns(StyledRow styledRow, float labelStartX) {

        var runOffsetXs = measureLabelRunOffsets(styledRow).runOffsetXs();
        var runXs = new ArrayList<Float>(runOffsetXs.size());

        for (var runOffsetX : runOffsetXs) {
            runXs.add(labelStartX + runOffsetX);
        }
        return runXs;
    }

    // The widest laid-out row: each row is its indent, the reserved crest column (when the box has one),
    // its label span, the value gap, and whatever its trailing slot reports, so a wide indented member
    // sizes the box just as a wide header would. A centred row is charged its label span alone - it
    // fills neither flanking slot and occupies neither column, so charging them would widen the box for
    // space the centred line never fills, and its own centring would then push it off the middle. Each
    // row is measured on its own face, so a heading in a wider atlas sizes the box to the width it will
    // actually paint at. The content width before the box layout adds its padding.
    private static double measureContentWidth(
            List<StyledRow> rows,
            float crestColumnWidth) {

        var widest = 0d;
        for (var styledRow : rows) {
            var labelSpan = measureLabelRunOffsets(styledRow).runsWidth();
            var rowWidth = styledRow.row() instanceof TooltipRow.TableRow tableRow
                ? tableRow.indent()
                    + measureCrestOffset(tableRow, crestColumnWidth)
                    + labelSpan
                    + VALUE_GAP
                    + styledRow.measureSlotWidth(tableRow.labelledRow().trailingRowSlot())
                : labelSpan;

            widest = Math.max(widest, rowWidth);
        }
        return widest;
    }

    // Where each of a row's label runs sits relative to the label's own left edge, and how wide the runs
    // come to together, measured on this row's own face. The one walk the width measurement, the
    // centring, and the placement all read, so a centred row is placed against exactly the span the box
    // was sized to hold and no anchor can drift from the width it was charged. How runs compose into a
    // line is LabelRuns' rule, shared with every other surface that lays a label.
    private static LabelRunOffsets measureLabelRunOffsets(StyledRow styledRow) {
        return LabelRuns.measureRunOffsets(
            styledRow.row().labelTextSpans(),
            styledRow::measureSpanWidth);
    }

    // Where a centred row's label starts: its span set in the middle of the content region, the leftover
    // split evenly to either side. A centred row that is itself the widest row sized the region to its
    // own span, so it lands flush at the content edge and nothing shifts.
    private static float centreLabelX(
            StyledRow styledRow,
            float leftX,
            float rightX) {

        var slack = rightX
            - leftX
            - measureLabelRunOffsets(styledRow).runsWidth();

        return leftX + slack / 2f;
    }

    // The horizontal space the crest gutter costs this row - the box's one column width for a label that
    // aligns to it, nothing for one that starts before it (and nothing either way in a box that reserved
    // no column, whose width is already nothing). One source so the width measurement and the label
    // placement agree on the offset, row by row.
    private static float measureCrestOffset(TooltipRow.TableRow tableRow, float crestColumnWidth) {
        return tableRow.labelPlacement() == TooltipLabelPlacement.ALIGNED_WITH_CRESTS
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
     * @param measureWidth the width of one of this row's span texts, in this row's own face
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
                spanText -> measurer.measureSpanWidth(
                    textStyle.face(),
                    textStyle.resolveDisplayText(spanText)));
        }

        // The width one of this row's flanking slots occupies. Each kind of slot answers for itself off
        // the line height and, where its width is glyphs rather than geometry, the measurement bound to
        // this row - so a column can be reserved for whatever a row was filled with without this
        // widget branching on the kind.
        private float measureSlotWidth(RowSlot rowSlot) {
            return rowSlot.computeWidth((float) lineHeight, this::measureSpanWidth);
        }

        // The width one of this row's spans occupies, in this row's own face. Asked of the span rather
        // than of its text, so no step above has to unpack a span and reach past the bound measurement
        // to charge it - and so the binding itself stays this record's own business.
        private double measureSpanWidth(TextSpan textSpan) {
            return measureWidth.applyAsDouble(textSpan.text());
        }
    }
}
