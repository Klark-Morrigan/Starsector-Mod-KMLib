package kmlib.starsector.ui.widgets.tooltip;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.TextSpanMeasurer;
import kmlib.starsector.ui.layout.TooltipBoxLayout;
import kmlib.starsector.ui.text.LabelRuns;
import kmlib.starsector.ui.text.LabelRuns.LabelRunOffsets;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.widgets.RowSlot;

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
 * {@link kmlib.starsector.ui.render.gl.tooltip.CursorTooltipRenderer}.
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
 * <p>A row carrying both a label and a value also gets the stretch between them measured as a {@link
 * TooltipLeaderLine} - the run a rule may be led along to tie the two ends of a wide row together, since
 * the value column is pinned to the box's right edge however short the label is. It is resolved here
 * because it is worked out from the very widths this widget measured to place the row; a surface deriving
 * it from the anchors would be measuring the row a second time, against which its rule could only drift.
 *
 * <p>A {@link TooltipRow.CentredRow} is laid outside that column model entirely: its label's runs centre
 * together as one span in the content region, and it is sized to that span alone, so a title or a lone
 * statement centres over whatever the box holds rather than aligning as an entry of it. Such a row holds
 * no slots at all, which is what lets the box be sized to the span alone: there is nothing of it that
 * could land in a column the box was not widened for. An image it shows travels inside its label as a
 * run and is charged to that same span, so a centred line showing a crest centres crest and words
 * together and still cannot reach past an edge.
 *
 * <p>The box is a stack of {@link TooltipSection blocks}, not of loose rows, and how far apart its lines
 * stand is settled before any of this: the blocks are flattened into a {@link TooltipRowStack}, each
 * line carrying the look it resolved and the room it takes above itself. This widget steps the placement
 * down by exactly those two amounts, so the box is always as tall as the rows drawn into it and the
 * grouping is spent by the time anything is placed.
 *
 * <p>That the look is a row's own rather than the box's is what lets one box hold a heading and a body
 * line at once: a heading drawn in the game's blockier title atlas is far wider than a body line of the
 * same size and stacks at its own height. So the line height a row occupies and the face its spans are
 * measured on are both read per row, and a heading is measured on exactly the face it will be painted
 * in.
 *
 * <p>How tall a box comes to is therefore answerable on its own, without laying one out - a caller
 * weighing whether its content fits the room it has holds no cursor to place a box at and no measurer to
 * charge widths with. Both answers come off the one stack, so a box weighed as fitting is the box that
 * is then drawn.
 *
 * <p>Cursor-follow placement is what parts this from a docked panel: a panel pins to an edge and its
 * content is laid out to fit, where a tooltip is sized by its content and then trails the pointer,
 * clamping at whichever screen edge it would otherwise run past. The sizing and the clamp are
 * {@code kmlib.starsector.ui.layout} geometry rather than derived here.
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

    private CursorTooltip() {
    }

    /**
     * Lays {@code sections} into a cursor-following box: sizes it to the widest row across all tiers,
     * places it up-and-right of the cursor clamped inside the given bound, and resolves each row's line
     * and column anchors within it. Each row stacks at - and hangs its leading slot as tall as - the line
     * height its own kind of line draws at, so a heading takes the room its face needs. The crest column
     * is reserved only when some table row fills its leading slot, and then at one width across the box:
     * in a mixed box a crest-less row still reserves it so its label aligns under the crested rows, while
     * an all-crest-less box reserves nothing and lays its labels flush.
     *
     * @param sections    the content blocks, top to bottom; an empty list yields a padding-only box
     * @param style       the look each kind of line draws in and how far apart the blocks stand, from
     *                    which every row's face, size, casing, and parting is resolved
     * @param measurer    the font-agnostic width measurement, asked per face
     * @param cursorX     the cursor x, in UI coordinates (UI origin is bottom-left)
     * @param cursorY     the cursor y, in UI coordinates
     * @param screenBound the region the box must stay within, in UI coordinates
     * @return the placed box and the per-row anchors, in reading order
     */
    public static TooltipLayout layOut(
            List<TooltipSection> sections,
            TooltipStyle style,
            TextSpanMeasurer measurer,
            float cursorX,
            float cursorY,
            Rectangle screenBound) {

        var rowStack = TooltipRowStack.stackRows(sections, style);
        var styledRows = bindRowsToStyles(rowStack.readStackedRows(), measurer);
        var crestColumnWidth = measureCrestColumnWidth(styledRows);

        var box = TooltipBoxLayout.computeBox(
            measureContentWidth(styledRows, crestColumnWidth),
            rowStack.measureContentHeight(),
            cursorX,
            cursorY,
            screenBound);

        return new TooltipLayout(
            box,
            placeRows(styledRows, box, crestColumnWidth));
    }

    /**
     * Answers how tall {@code sections} would stand in {@code style}, the box's own padding included,
     * without laying anything out or placing it anywhere.
     *
     * <p>For a caller that has to know whether its content fits the room it has before it commits to
     * drawing it: a box is sized by its content and then clamped, so one taller than the region it is
     * clamped inside is drawn with its ends past the edges, and nothing on screen says what was cut off.
     * Answered off the very {@link TooltipRowStack} {@link #layOut} stacks its rows into, so a height
     * weighed here and the box later drawn cannot disagree.
     *
     * <p>The height alone, and with no measurer, because that is all this question needs: how tall a box
     * stands follows from the looks its rows resolve and the room the blocks put between them, none of
     * which is a width. A caller wanting the placed box lays it out.
     *
     * @param sections the content blocks, top to bottom; an empty list yields a padding-only box
     * @param style    the look each kind of line draws in and how far apart the blocks stand, from which
     *                 every row's line height and the parting above it is resolved
     * @return the height the box would occupy, in UI units
     */
    public static float measureBoxHeight(List<TooltipSection> sections, TooltipStyle style) {

        return TooltipBoxLayout.computeBoxHeight(
            TooltipRowStack
                .stackRows(sections, style)
                .measureContentHeight());
    }

    // Binds a width measurement to each stacked line, on the face that line already resolved. The one
    // step of the layout a measurer is needed for, which is what leaves the stack itself askable by a
    // caller holding none.
    private static List<StyledRow> bindRowsToStyles(
            List<TooltipRowStack.StackedRow> stackedRows,
            TextSpanMeasurer measurer) {

        var styledRows = new ArrayList<StyledRow>(stackedRows.size());

        for (var stackedRow : stackedRows) {
            styledRows.add(StyledRow.bindRowToStyle(stackedRow, measurer));
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

        for (var styledRow : rows) {
            rowTopY -= styledRow.leadingGap();

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

        // Walked once for the whole row and read by every part of it that is placed off the label: where
        // each run anchors, where a centred span starts, and where the label stops for a rule to lead
        // from. Walked per reader instead, one of them would eventually be placing a row against a
        // measurement the others had not made.
        var labelRunOffsets = measureLabelRunOffsets(styledRow);

        // A centred line holds no columns to place. Its label anchors off the content region's midpoint
        // instead - its later runs still ride off that anchor, so a whole label centres as one span -
        // and the column anchors it is handed are the box's own content edges, which it draws nothing in.
        // It leads no rule either: a rule leads to a value column, and this line has left the table
        // that has one.
        if (!(styledRow.row() instanceof TooltipRow.TableRow tableRow)) {

            return new TooltipLayout.TooltipRowLayout(
                rowTopY,
                (float) styledRow.lineHeight(),
                leftX,
                anchorLabelRuns(labelRunOffsets, centreLabelX(labelRunOffsets, leftX, rightX)),
                TooltipLeaderLine.NONE,
                rightX);
        }
        var leadingRowSlotX = leftX + tableRow.indent();
        var labelStartX = leadingRowSlotX + measureCrestOffset(tableRow, crestColumnWidth);

        return new TooltipLayout.TooltipRowLayout(
            rowTopY,
            (float) styledRow.lineHeight(),
            leadingRowSlotX,
            anchorLabelRuns(labelRunOffsets, labelStartX),
            measureLeaderLine(
                styledRow,
                tableRow.labelledRow().trailingRowSlot(),
                labelStartX + labelRunOffsets.runsWidth(),
                rightX),
            rightX);
    }

    // The stretch this row leads a rule along, from where its label stopped to where its value starts.
    // The visual aid a wide row needs: the value column is anchored to the box's right edge whatever the
    // label measures, so a short name and its number can end up a long way apart, and a reader tracking
    // one back to the other has nothing to follow across the gap.
    //
    // Stood off each end by the face's own word space - the same space the label parts its own runs by -
    // so the rule reads as a mark set between two words rather than as a stroke run into them. Led only
    // where what is left is at least that space long: shorter than the gap it stands in, a rule is a
    // smudge between two columns already close enough to read as one line, and the aid is only wanted
    // where the eye could actually lose the line.
    //
    // Measured here, beside the label and column widths it is worked out from, rather than by whatever
    // paints it: those widths are this layout's own, and a rule derived from a second measurement of the
    // row could only ever drift from the columns it is meant to join.
    private static TooltipLeaderLine measureLeaderLine(
            StyledRow styledRow,
            RowSlot trailingRowSlot,
            float labelEndX,
            float rightX) {

        // Both ends have to be there for a rule to join them: a row trailing with nothing has no value to
        // lead to, and one whose runs all came out blank has no name to lead back from. Both are asked of
        // the content rather than of a width, so the two ends are settled by one kind of question.
        if (!trailingRowSlot.isFilled()
                || !LabelRuns.hasDrawnRun(styledRow.row().labelRuns())) {
            return TooltipLeaderLine.NONE;
        }
        var wordSpaceWidth = styledRow.measureWordSpaceWidth();
        var leaderLine = new TooltipLeaderLine(
            labelEndX + wordSpaceWidth,
            rightX - styledRow.measureSlotWidth(trailingRowSlot) - wordSpaceWidth);

        return leaderLine.computeWidth() >= wordSpaceWidth
            ? leaderLine
            : TooltipLeaderLine.NONE;
    }

    // Where each of a row's label runs anchors in the placed box: the offsets the runs measured out at,
    // shifted to wherever the label itself starts. Shifting one measured walk rather than re-deriving
    // the offsets per placement is what keeps the anchors and the width the box was sized to in step
    // whatever the row's placement turns out to be.
    private static List<Float> anchorLabelRuns(
            LabelRunOffsets labelRunOffsets,
            float labelStartX) {

        var runOffsetXs = labelRunOffsets.runOffsetXs();
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
    // come to together, measured on this row's own face. The one walk the sizing pass and the placement
    // pass each read - the placement taking it once per row and handing it to everything placed off the
    // label - so a centred row is placed against exactly the span the box was sized to hold, a rule leads
    // from where the label the box was widened for actually stops, and no anchor can drift from the width
    // it was charged. How runs compose into a line is LabelRuns' rule, shared with every other surface
    // that lays a label.
    //
    // The row's own line height goes along because an image run squares itself off it, the same size the
    // crest column reserves for a leading image - so a crest set among a line's words and one set in its
    // gutter come out the same size whichever the caller reached for.
    private static LabelRunOffsets measureLabelRunOffsets(StyledRow styledRow) {

        return LabelRuns.measureRunOffsets(
            styledRow.row().labelRuns(),
            (float) styledRow.lineHeight(),
            styledRow::measureSpanWidth);
    }

    // Where a centred row's label starts: its span set in the middle of the content region, the leftover
    // split evenly to either side. A centred row that is itself the widest row sized the region to its
    // own span, so it lands flush at the content edge and nothing shifts.
    private static float centreLabelX(
            LabelRunOffsets labelRunOffsets,
            float leftX,
            float rightX) {

        var slack = rightX
            - leftX
            - labelRunOffsets.runsWidth();

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
     * One row bound to the look its kind of line resolved to and to where the grouping put it: the
     * height it stacks at, the room it takes above itself, and a width measurement already bound to its
     * face and its casing. Bound once per layout so the width measurement and the placement cannot read
     * two different looks for one row - and so no step below has to carry the box's style, its blocks,
     * and the measurer alongside the row it is working on.
     *
     * @param row          the content row as its caller authored it
     * @param lineHeight   the height the row stacks at, which is also its crest square's side
     * @param leadingGap   the room taken above the row before its own line, in UI units
     * @param measureWidth the width of one of this row's span texts, in this row's own face
     */
    private record StyledRow(
        TooltipRow row,
        double lineHeight,
        float leadingGap,
        ToDoubleFunction<String> measureWidth) {

        // Binds a measurement to one stacked row, on the look and at the height that row already
        // resolved - neither is read a second time here, so the line a row is placed on and the line the
        // box was sized for are one statement of what a line of that look costs.
        private static StyledRow bindRowToStyle(
                TooltipRowStack.StackedRow stackedRow,
                TextSpanMeasurer measurer) {

            var textStyle = stackedRow.textStyle();

            // Measured through the style's own display text, not the authored text: a shouted line
            // measured as authored measures narrower than it paints, so the box sized from that
            // measurement would clip the text drawn into it.
            return new StyledRow(
                stackedRow.row(),
                stackedRow.measureLineHeight(),
                stackedRow.leadingGap(),
                spanText -> measurer.measureSpanWidth(
                    textStyle.face(),
                    textStyle.resolveDisplayText(spanText)));
        }

        // The word space this row's own face sets between two words. What a mark set among the line's
        // words rather than on them stands off by, asked of the row so that the space a rule keeps and
        // the space the label parts its runs by are one measurement on one face - restated as a number
        // here, the two would read alike on the face they were chosen for and part on every other.
        private float measureWordSpaceWidth() {
            return LabelRuns.measureWordSpaceWidth(this::measureSpanWidth);
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
