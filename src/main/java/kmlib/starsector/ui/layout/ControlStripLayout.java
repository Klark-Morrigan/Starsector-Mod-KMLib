package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.SegmentSizing;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.text.LabelRun;
import kmlib.starsector.ui.text.LabelRuns;
import kmlib.starsector.ui.text.StyledSpanMeasurer;
import kmlib.starsector.ui.widgets.Checkbox;
import kmlib.starsector.ui.widgets.IconLabelRow;
import kmlib.starsector.ui.widgets.RadioRow;
import kmlib.starsector.ui.widgets.segments.HorizontalSegments;
import kmlib.starsector.ui.widgets.segments.SegmentSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays a column of {@link ControlSpec}s into rows: it measures how wide and tall the strip must be,
 * then - once a host has framed a body rectangle of that size - snaps each control into its row. It
 * is the generic body of a control strip, knowing nothing about what frames it: a host (an on-map
 * sidebar, a tab panel) measures the strip, sizes its own chrome around the returned footprint, and
 * hands back the framed body rectangle for placement. Splitting it out this way lets the strip serve
 * any frame while the frame math stays with the host.
 *
 * <p>Every control sizes to a body-font label inside a fixed-height row, save one: a {@link
 * ControlSpec.Tabs} row snaps to a larger face and stands a tab band tall, so how a tabs row measures
 * and splits is {@link TabsControlLayout}'s, and this stacks the result like any other row.
 *
 * <p>UI coordinates throughout (origin bottom-left, y grows up); text snapping runs through the
 * injected {@link LineWidthMeasurer}, so the layout depends on a width measurement rather than a
 * concrete font and stays a pure computation. Measuring once and placing from the same row
 * dimensions lets a renderer and an input listener share one placement, so what is drawn is exactly
 * what the player clicks.
 */
public final class ControlStripLayout {

    // Body geometry: one fixed-height row per control, a gap between rows, and an inset framing the
    // controls off the body edge so they clear the host's border. Public where the renderer must
    // place a label at the same offset the measurement reserved, keeping one source of the spacing.
    public static final float CONTROL_ROW_HEIGHT = 20f;
    public static final float ROW_GAP = 4f;
    public static final float BODY_PADDING = 8f;

    // The horizontal gap parting the two columns of a side-by-side group, wider than the inter-row gap
    // so the two runs read as distinct blocks rather than one continuous row. Public so a test places
    // the right column at the same offset the layout reserved for the gap.
    public static final float COLUMN_GAP = 8f;

    // Per-control slack: the padding sizing each radio segment past its option label, the gap before
    // a control's trailing label, and the padding sizing a toggle button past its label. The trailing
    // gap is public so the renderer places that label at the same offset this reserved for it. A
    // checkbox's own box-to-label gap is the widget's, taken from the shared column spec, so the row
    // this sizes and the label the renderer places both read one value.
    static final float RADIO_SEGMENT_PADDING = 12f;

    // A radio cell has no minimum width - it sizes purely to its widest label plus the padding - unlike
    // a tab, which floors at TabsControlLayout.MIN_TAB_WIDTH so a short tab still gives a clickable box.
    static final float RADIO_SEGMENT_MIN_WIDTH = 0f;
    public static final float TRAILING_LABEL_GAP = 6f;
    static final float TOGGLE_TEXT_PADDING = 16f;

    // The body face size, measured here and drawn by the renderer at the one value, so a snapped row
    // width matches the text painted into it.
    public static final double BODY_FONT_SIZE = 13d;

    private ControlStripLayout() {
    }

    /**
     * Measures the strip: each control row's width and height and the footprint that holds them. The
     * body is as wide as the widest row (a trailing label counts, so a host's backdrop covers it)
     * plus the inset, and as tall as the stacked rows plus their gaps and the inset. Empty controls
     * measure to a zero footprint with no rows, so a host reserves nothing for a bodyless strip. A
     * vertical radio stands one option-row taller per segment, so row heights vary and the body sums
     * them rather than assuming one height per control. A side-by-side group's one row is as wide as its
     * two columns plus their gap and as tall as its taller column, and is later flattened into its
     * children's controls at placement.
     *
     * @param specs    the controls to measure, top to bottom
     * @param measurer measures each label's rendered width for text snapping
     * @return the strip footprint and the per-row dimensions behind it, for {@link #layoutControls}
     */
    public static StripMeasurement measureStrip(
            List<ControlSpec> specs,
            LineWidthMeasurer measurer) {

        if (specs.isEmpty()) {
            return StripMeasurement.EMPTY;
        }
        var rowWidths = new ArrayList<Float>(specs.size());
        var rowHeights = new ArrayList<Float>(specs.size());
        var contentWidth = 0f;

        for (var spec : specs) {
            var rowWidth = measureRowWidth(spec, measurer);
            rowWidths.add(rowWidth);
            rowHeights.add(measureRowHeight(spec));
            contentWidth = Math.max(
                contentWidth,
                rowWidth + measureTrailingWidth(spec, measurer));
        }
        // A divider carries no intrinsic width here (it measures zero) - it is stretched to the full
        // framed body at placement time, once the host has framed a body rectangle, so it never drives
        // the content width it later spans.
        var bodyWidth = contentWidth + 2f * BODY_PADDING;
        var bodyHeight = 2f * BODY_PADDING + measureStackedHeight(rowHeights);

        return new StripMeasurement(
            bodyWidth,
            bodyHeight,
            List.copyOf(rowWidths),
            List.copyOf(rowHeights));
    }

    /**
     * Stacks each control row inside the framed body (via the shared row-stacker, starting from the
     * body's top-left plus the inset), snapping each row to its measured width and height and
     * splitting a radio row into its option segments so the hit rects are the drawn ones. The body
     * rectangle the host framed must carry the same top-left the measurement assumed, so the controls
     * land where the strip was sized for.
     *
     * @param body       the framed body rectangle, sized from {@link #measureStrip}
     * @param specs      the controls to place, top to bottom (must match the measured specs)
     * @param rowHeights the measured row heights, from {@link StripMeasurement#rowHeights()}
     * @param rowWidths  the measured row widths, from {@link StripMeasurement#rowWidths()}
     * @param measurer   measures each label's rendered width, for snapping a tabs row's per-tab segments
     * @return the laid-out controls, top to bottom; empty when {@code specs} is empty
     */
    public static List<Control> layoutControls(
            Rectangle body,
            List<ControlSpec> specs,
            List<Float> rowHeights,
            List<Float> rowWidths,
            LineWidthMeasurer measurer) {

        if (specs.isEmpty()) {
            return List.of();
        }
        var bodyTopY = body.y() + body.height();
        var rows = RowStack.layoutRows(
            body.x() + BODY_PADDING,
            bodyTopY - BODY_PADDING,
            ROW_GAP,
            rowHeights,
            rowWidths);

        return toControls(
            specs,
            spanDividerRowsToBody(specs, rows, body),
            measurer);
    }

    /**
     * The height a run of rows occupies when stacked with a gap between each: the summed heights plus one
     * gap per seam. The SSOT for "how tall does this run of rows stand", read by {@link #measureStrip}
     * for the whole strip's body and by the capped strip layout for its pinned footer block, so the two
     * size a stacked run the same way. An empty run is zero, and a single row is its own height with no
     * gap.
     *
     * @param rowHeights each row's height, in stack order
     * @return the stacked height including the inter-row gaps, or 0 for an empty run
     */
    static float measureStackedHeight(List<Float> rowHeights) {
        var total = 0f;
        for (var rowHeight : rowHeights) {
            total += rowHeight;
        }
        return total + Math.max(0, rowHeights.size() - 1) * ROW_GAP;
    }

    /**
     * Pairs each spec with the row it was snapped into, in order, splitting a radio (or a tabs row) into
     * its segments and leaving every other kind a single-hit row. The SSOT for turning a run of (spec,
     * row) pairs into laid-out controls, so this layout's plain stack and the capped strip layout's
     * pinned header and footer runs build their controls the same way rather than each re-zipping.
     *
     * @param specs    the controls, in stack order
     * @param rows     their snapped rows, the same size and order as {@code specs}
     * @param measurer measures each label's rendered width, for snapping a tabs row's per-tab segments
     * @return the laid-out controls, in the same order
     */
    static List<Control> toControls(
            List<ControlSpec> specs,
            List<Rectangle> rows,
            LineWidthMeasurer measurer) {

        var controls = new ArrayList<Control>(specs.size());

        for (var index = 0; index < specs.size(); index++) {
            var spec = specs.get(index);

            // A side-by-side group is not one control but two columns of them: it expands into its
            // children's laid-out controls here, so downstream sees only ordinary controls with absolute
            // bounds. Every other kind is its own single control.
            if (spec instanceof ControlSpec.SideBySide pair) {
                controls.addAll(expandSideBySide(
                    pair,
                    rows.get(index),
                    measurer));
            } else {
                controls.add(toControl(
                    spec,
                    rows.get(index),
                    measurer));
            }
        }
        return List.copyOf(controls);
    }

    /**
     * Pairs one spec with the row it was snapped into, splitting a radio or a tabs row into its per-hit
     * segments and leaving every other kind a single-hit row. Package-private so the capped strip layout,
     * which places the flex list itself, builds its list control through the same segment rule this
     * layout uses rather than re-deriving it. The measurer is only read for a tabs row, whose per-tab
     * segments snap to text; every other kind splits geometrically and ignores it.
     *
     * @param spec     the control to pair with its row
     * @param row      the row the strip snapped it into
     * @param measurer measures each tab label's rendered width, for a tabs row's per-tab segments
     * @return the laid-out control, its segments split for a radio or tabs row and empty for other kinds
     */
    static Control toControl(ControlSpec spec, Rectangle row, LineWidthMeasurer measurer) {
        // A radio (horizontal cells or the vertical stacked table) and a tabs row split into per-hit
        // segments; a tabs row snaps its tabs to text, so it alone reads the measurer. Every other kind
        // is a single-hit row with no segments.
        List<Rectangle> segments;

        if (spec instanceof ControlSpec.HorizontalRadio radio) {
            segments = splitHorizontalRadioIntoSegments(
                radio,
                row,
                measurer);
        } else if (spec instanceof ControlSpec.VerticalTable table) {
            segments = RadioRow.splitIntoGrid(
                row,
                table.labelledRows().size(),
                table.columnCount());
        } else if (spec instanceof ControlSpec.Tabs tabs) {
            segments = TabsControlLayout.splitIntoSegments(
                tabs,
                row,
                measurer);
        } else {
            segments = List.of();
        }
        return new Control(spec, row, segments);
    }

    /**
     * Stretches every divider row to span the full framed body - edge to edge inside the border inset,
     * across the row padding the other controls sit within - so a section rule reaches the frame rather
     * than stopping at the content column. Every non-divider row keeps the padded placement {@link
     * RowStack} gave it. The body is known only once the host frames it, so the span is applied at
     * placement rather than measurement, where a divider has no intrinsic width. Shared with the capped
     * strip layout, whose pinned header and footer runs span their dividers through this one rule.
     *
     * @param specs the controls, in the same order and size as {@code rows}
     * @param rows  the padded rows the stacker produced, one per spec
     * @param body  the framed body rectangle whose full width a divider spans
     * @return the rows with each divider widened to the body, every other row unchanged
     */
    static List<Rectangle> spanDividerRowsToBody(
            List<ControlSpec> specs,
            List<Rectangle> rows,
            Rectangle body) {

        var spanned = new ArrayList<Rectangle>(rows.size());
        for (var index = 0; index < rows.size(); index++) {
            var row = rows.get(index);
            spanned.add(specs.get(index) instanceof ControlSpec.Divider
                ? new Rectangle(
                    body.x(),
                    row.y(),
                    body.width(),
                    row.height())
                : row);
        }
        return List.copyOf(spanned);
    }

    // Lays a side-by-side group's two columns into the group's row and returns their controls: the left
    // column stacked from the row's top-left, the right column stacked from one COLUMN_GAP past the left
    // column's width, each a vertical run placed exactly as the top-level strip stacks its rows. The
    // group has no chrome of its own, so it contributes only its children (with absolute bounds), which
    // is why the caller flattens it into the strip rather than keeping it as one control.
    private static List<Control> expandSideBySide(
            ControlSpec.SideBySide pair,
            Rectangle row,
            LineWidthMeasurer measurer) {

        var rowTopY = row.y() + row.height();
        var leftControls = layoutColumn(
            pair.leftColumn(),
            row.x(),
            rowTopY,
            measurer);

        var rightX = row.x()
            + measureColumnWidth(pair.leftColumn(), measurer)
            + COLUMN_GAP;
        var rightControls = layoutColumn(
            pair.rightColumn(),
            rightX,
            rowTopY,
            measurer);

        var controls = new ArrayList<Control>(leftControls.size() + rightControls.size());
        controls.addAll(leftControls);
        controls.addAll(rightControls);
        return List.copyOf(controls);
    }

    // Stacks one column of a side-by-side group top to bottom from (columnX, columnTopY), snapping each
    // control to its own measured width and height exactly as the top-level strip stacks its rows, then
    // turning the snapped rows into controls through the shared zip so a column's radio splits into the
    // same segments a top-level radio would. A column holds only ordinary controls, so the zip never
    // recurses back into another group.
    private static List<Control> layoutColumn(
            List<ControlSpec> specs,
            float columnX,
            float columnTopY,
            LineWidthMeasurer measurer) {

        var rowHeights = new ArrayList<Float>(specs.size());
        var rowWidths = new ArrayList<Float>(specs.size());
        for (var spec : specs) {
            rowHeights.add(measureRowHeight(spec));
            rowWidths.add(measureRowWidth(spec, measurer));
        }
        var rows = RowStack.layoutRows(
            columnX,
            columnTopY,
            ROW_GAP,
            rowHeights,
            rowWidths);

        return toControls(specs, rows, measurer);
    }

    // The per-cell hit segments of a horizontal radio row, sized through the radio's own segment rule
    // and laid from the row's left edge. The same split the renderer draws against, so the drawn cells
    // are the clickable ones. A vertical table splits geometrically into its column grid instead.
    private static List<Rectangle> splitHorizontalRadioIntoSegments(
            ControlSpec.HorizontalRadio radio,
            Rectangle row,
            LineWidthMeasurer measurer) {

        var widths = HorizontalSegments.computeSegmentWidths(
            radio.labels(),
            radioSegmentSpec(radio.segmentSizing()),
            measurer);

        return HorizontalSegments.placeSegments(
            row.x(),
            row.y(),
            row.height(),
            widths);
    }

    // The width of a control's row, snapped to its label(s): a checkbox is its tick box plus a gap
    // plus its label; a horizontal radio is its equal segments side by side, a vertical table is one
    // segment column wide; a toggle is its label plus padding; a label is just its measured text,
    // since it has no widget chrome around it. A divider has no intrinsic width - it measures zero and
    // is stretched to the full framed body at placement - so it contributes nothing to the width here.
    private static float measureRowWidth(ControlSpec spec, LineWidthMeasurer measurer) {
        if (spec instanceof ControlSpec.Checkbox checkbox) {
            return Checkbox.measureRowWidth(
                CONTROL_ROW_HEIGHT,
                measureLabelWidth(checkbox.labelRuns(), measurer));
        }
        if (spec instanceof ControlSpec.Toggle toggle) {
            return measureLabelWidth(toggle.labelRuns(), measurer) + TOGGLE_TEXT_PADDING;
        }
        if (spec instanceof ControlSpec.Label label) {
            return measureLabelWidth(label.labelRuns(), measurer);
        }
        if (spec instanceof ControlSpec.HorizontalRadio radio) {
            return HorizontalSegments.measureRowWidth(
                radio.labels(),
                radioSegmentSpec(radio.segmentSizing()),
                measurer);
        }
        if (spec instanceof ControlSpec.VerticalTable table) {
            return measureVerticalTableRowWidth(table, measurer);
        }
        if (spec instanceof ControlSpec.Tabs tabs) {
            return TabsControlLayout.measureRowWidth(tabs, measurer);
        }
        if (spec instanceof ControlSpec.SideBySide pair) {
            // The two columns side by side: the left column, the gap parting them, then the right.
            return measureColumnWidth(pair.leftColumn(), measurer)
                + COLUMN_GAP
                + measureColumnWidth(pair.rightColumn(), measurer);
        }
        return 0f;
    }

    // The width one column of a side-by-side group needs: its widest control row, each sized as the top-
    // level strip sizes it (its snapped row plus any trailing caption), so a column reserves exactly the
    // room the same controls take when stacked at the top level. An empty column needs no width.
    private static float measureColumnWidth(List<ControlSpec> specs, LineWidthMeasurer measurer) {
        var widest = 0f;
        for (var spec : specs) {
            widest = Math.max(
                widest,
                measureRowWidth(spec, measurer) + measureTrailingWidth(spec, measurer));
        }
        return widest;
    }

    // The height one column of a side-by-side group stands: its controls stacked with a gap between
    // each, through the same stacked-height rule the whole strip uses, so the group is as tall as its
    // taller column. An empty column stands zero tall.
    private static float measureColumnHeight(List<ControlSpec> specs) {
        var rowHeights = new ArrayList<Float>(specs.size());
        for (var spec : specs) {
            rowHeights.add(measureRowHeight(spec));
        }
        return measureStackedHeight(rowHeights);
    }

    // The width a vertical radio table needs: its column count wide. Each column sizes to the same
    // width - the width its widest row needs under the geometry the table lays its rows out in - and the
    // columns sit side by side, so a two-column table needs twice one column's width. One column is the
    // plain single-column stack.
    private static float measureVerticalTableRowWidth(
            ControlSpec.VerticalTable table,
            LineWidthMeasurer measurer) {

        var columnWidth = switch (table.rowGeometry()) {
            case COLUMNS -> measureColumnTableRowWidth(table, measurer);
            case UNIFORM_SEGMENTS -> measureSegmentedListColumnWidth(table, measurer);
        };
        return table.columnCount() * columnWidth;
    }

    // The height of a control's row: one control-row tall for every control except a vertical table,
    // which stacks its options and so stands one control-row tall per row. A single-column table has
    // one row per option; a multi-column table wraps its options across columns, so it needs only as
    // many rows as its tallest column - the grid's row count - rather than one per option. A tabs row
    // stands one tab-height tall (taller than a body row), drawn in the larger tab face.
    private static float measureRowHeight(ControlSpec spec) {
        if (spec instanceof ControlSpec.VerticalTable table) {
            var rowCount = RadioRow.computeRowsPerColumn(
                table.labelledRows().size(),
                table.columnCount());
            return rowCount * CONTROL_ROW_HEIGHT;
        }
        if (spec instanceof ControlSpec.Tabs) {
            return TabsControlLayout.TAB_HEIGHT;
        }
        if (spec instanceof ControlSpec.SideBySide pair) {
            // The group stands as tall as its taller column, so the shorter column top-aligns and
            // leaves the space below it empty rather than stretching the group.
            return Math.max(
                measureColumnHeight(pair.leftColumn()),
                measureColumnHeight(pair.rightColumn()));
        }
        return CONTROL_ROW_HEIGHT;
    }

    // The width a column table needs: its widest row, each sized to hold what it leads with, its label,
    // and what it trails with without clipping. Each flank charges the width its own slot answers for,
    // so a row trailing a triangle reserves the drawn triangle and one trailing a value reserves its
    // glyphs, without this branching on which kind of thing sits there. The rows are one control-row
    // tall, the height a slot sizes itself against, so every stacked row shows an equal leading square
    // and the column is wide enough that the longest name still clears its right-aligned value.
    private static float measureColumnTableRowWidth(
            ControlSpec.VerticalTable table,
            LineWidthMeasurer measurer) {

        var spanMeasurer = bindBodySpanMeasurer(measurer);
        var widest = 0f;
        for (var labelledRow : table.labelledRows()) {
            var rowWidth = IconLabelRow.measureRowWidth(
                CONTROL_ROW_HEIGHT,
                measureLabelWidth(labelledRow.labelRuns(), spanMeasurer),
                labelledRow.leadingRowSlot().isFilled(),
                labelledRow.trailingRowSlot().computeWidth(CONTROL_ROW_HEIGHT, spanMeasurer));

            widest = Math.max(widest, rowWidth);
        }
        return widest;
    }

    // The width a label occupies at the body face: its runs laid out as one sentence, so a label that
    // picks a stretch of itself out in another colour - or sets a small image among its words - is
    // charged the gap between its runs as well as what each of them draws. Every label a control carries
    // is measured through this, so a control snapped to its text is snapped to exactly what the renderer
    // will lay into it run by run.
    private static float measureLabelWidth(
            List<LabelRun> labelRuns,
            LineWidthMeasurer measurer) {

        return measureLabelWidth(labelRuns, bindBodySpanMeasurer(measurer));
    }

    // The same width, for a caller that has already bound the body measurement and is charging a row
    // several things through it - so a stack of rows binds once rather than once per row. An image run
    // squares off the control row, the same height a row's flanking slots size themselves against, so an
    // image among a label's words comes out the size a crest in its leading column would.
    private static float measureLabelWidth(
            List<LabelRun> labelRuns,
            StyledSpanMeasurer spanMeasurer) {

        return LabelRuns
            .measureRunOffsets(labelRuns, CONTROL_ROW_HEIGHT, spanMeasurer)
            .runsWidth();
    }

    // The body-line measurement bound to the face and size the strip paints its rows in, so a slot
    // charges its own width without learning which face it will be drawn in - the binding the styled
    // measurement exists to hold.
    private static StyledSpanMeasurer bindBodySpanMeasurer(LineWidthMeasurer measurer) {
        return textSpan -> measurer.measureLineWidth(textSpan.text(), BODY_FONT_SIZE);
    }

    // The width of one uniform-cell list column: the uniform segment width its widest option needs, read
    // through the shared segment rule so a stacked column sizes exactly as a horizontal cell would. Such
    // a list's columns are uniform by construction, so this reads UNIFORM. An option-less list has no
    // widths, so it falls back to the bare segment padding.
    private static float measureSegmentedListColumnWidth(
            ControlSpec.VerticalTable table,
            LineWidthMeasurer measurer) {

        var widths = HorizontalSegments.computeSegmentWidths(
            table.labels(),
            radioSegmentSpec(SegmentSizing.UNIFORM),
            measurer);
            
        return widths.isEmpty()
            ? RADIO_SEGMENT_PADDING
            : widths.get(0);
    }

    // The segment-sizing rule for a radio row: the radio padding, no floor, and the body font under the
    // given SegmentSizing - UNIFORM cells for a vertical table (uniform by construction) or an even-cell
    // horizontal radio, SNAPPED per-label when a snapped horizontal radio opts in. The horizontal split,
    // the horizontal row-width, and the vertical column width all size through this one rule.
    private static SegmentSpec radioSegmentSpec(SegmentSizing sizing) {
        return new SegmentSpec(
            RADIO_SEGMENT_PADDING,
            RADIO_SEGMENT_MIN_WIDTH,
            BODY_FONT_SIZE,
            sizing);
    }

    // Extra footprint a trailing label adds past the control's own row, or none when it is blank. Only a
    // horizontal radio carries a trailing caption; every other control reserves nothing here.
    private static float measureTrailingWidth(ControlSpec spec, LineWidthMeasurer measurer) {
        if (!(spec instanceof ControlSpec.HorizontalRadio radio) || !radio.hasTrailingCaption()) {
            return 0f;
        }
        return TRAILING_LABEL_GAP + measureWidth(measurer, radio.trailingLabel());
    }

    private static float measureWidth(LineWidthMeasurer measurer, String text) {
        return (float) measurer.measureLineWidth(text, BODY_FONT_SIZE);
    }

    /**
     * The strip's measured footprint and the per-row dimensions behind it, returned together so the
     * size feeds the host's frame and the same row dimensions place the controls without measuring
     * twice. A strip holding no controls measures to {@link StripMeasurement#EMPTY}.
     *
     * @param bodyWidth  the strip footprint width, inset included
     * @param bodyHeight the strip footprint height, inset included
     * @param rowWidths  each control row's measured width, top to bottom
     * @param rowHeights each control row's measured height, top to bottom
     */
    public record StripMeasurement(
            float bodyWidth,
            float bodyHeight,
            List<Float> rowWidths,
            List<Float> rowHeights) {
        /**
         * The measurement of a strip holding no controls: a zero footprint with no rows behind it. A
         * named value rather than a literal spelled out wherever the empty case is reached, so "no
         * controls measures to nothing" is stated once and a reader meets the state by name.
         */
        public static final StripMeasurement EMPTY =
            new StripMeasurement(0f, 0f, List.of(), List.of());
    }
}
