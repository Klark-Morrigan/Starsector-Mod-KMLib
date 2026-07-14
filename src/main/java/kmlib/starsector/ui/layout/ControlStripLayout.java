package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlKind;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.RadioAlignment;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.IconLabelRow;
import kmlib.starsector.ui.widgets.RadioRow;
import kmlib.starsector.ui.widgets.VanillaTabContent;
import kmlib.starsector.ui.widgets.VanillaTabStrip;
import kmlib.text.KmlibStrings;

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

    // Per-control slack: the gap between a checkbox's box and its label, the padding sizing each
    // radio segment past its option label, the gap before a control's trailing label, and the
    // padding sizing a toggle button past its label. The checkbox and trailing gaps are public so
    // the renderer places each label at the same offset this reserved for it.
    public static final float CHECKBOX_LABEL_GAP = 6f;
    static final float RADIO_SEGMENT_PADDING = 12f;
    public static final float TRAILING_LABEL_GAP = 6f;
    static final float TOGGLE_TEXT_PADDING = 16f;

    // The body face size, measured here and drawn by the renderer at the one value, so a snapped row
    // width matches the text painted into it.
    public static final double BODY_FONT_SIZE = 13d;

    // Tabs-control geometry: a TABS row stands one tab-height tall (taller than a body row, since the
    // tab face is larger), each tab snapped to its label-plus-shortcut width with slack so text does not
    // touch the edges and a floor so a short tab still gives a clickable box, and measured/drawn at the
    // tab face size. Public so the panel frame and the renderer read the same values the layout snapped
    // the tabs to.
    public static final float TAB_HEIGHT = 24f;
    public static final float TAB_TEXT_PADDING = 16f;
    public static final float MIN_TAB_WIDTH = 48f;
    public static final double TAB_FONT_SIZE = 15d;

    private ControlStripLayout() {
    }

    /**
     * Measures the strip: each control row's width and height and the footprint that holds them. The
     * body is as wide as the widest row (a trailing label counts, so a host's backdrop covers it)
     * plus the inset, and as tall as the stacked rows plus their gaps and the inset. Empty controls
     * measure to a zero footprint with no rows, so a host reserves nothing for a bodyless strip. A
     * vertical radio stands one option-row taller per segment, so row heights vary and the body sums
     * them rather than assuming one height per control.
     *
     * @param specs    the controls to measure, top to bottom
     * @param measurer measures each label's rendered width for text snapping
     * @return the strip footprint and the per-row dimensions behind it, for {@link #layoutControls}
     */
    public static StripMeasurement measureStrip(List<ControlSpec> specs,
            LineWidthMeasurer measurer) {
        if (specs.isEmpty()) {
            return new StripMeasurement(0f, 0f, List.of(), List.of());
        }
        var rowWidths = new ArrayList<Float>(specs.size());
        var rowHeights = new ArrayList<Float>(specs.size());
        var contentWidth = 0f;
        for (var spec : specs) {
            var rowWidth = measureRowWidth(spec, measurer);
            rowWidths.add(rowWidth);
            rowHeights.add(measureRowHeight(spec));
            contentWidth = Math.max(contentWidth, rowWidth + measureTrailingWidth(spec, measurer));
        }
        // A divider spans the strip's inner width, so once the widest content row is known its row
        // width is set to that span - it then lays out as a rule crossing the whole body rather than
        // a zero-width row. Done after the loop so it never drives the content width it stretches to.
        spanDividersToContentWidth(specs, rowWidths, contentWidth);
        var bodyWidth = contentWidth + 2f * BODY_PADDING;
        var bodyHeight = 2f * BODY_PADDING + measureStackedHeight(rowHeights);
        return new StripMeasurement(bodyWidth, bodyHeight, List.copyOf(rowWidths),
                List.copyOf(rowHeights));
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
    public static List<Control> layoutControls(Rectangle body, List<ControlSpec> specs,
            List<Float> rowHeights, List<Float> rowWidths, LineWidthMeasurer measurer) {
        if (specs.isEmpty()) {
            return List.of();
        }
        var bodyTopY = body.y() + body.height();
        var rows = RowStack.layoutRows(body.x() + BODY_PADDING, bodyTopY - BODY_PADDING,
                ROW_GAP, rowHeights, rowWidths);
        return toControls(specs, rows, measurer);
    }

    /**
     * Lays a tabs control flush as a panel header: the tabs snapped to their labels from
     * {@code (originX, topY)} down one {@link #TAB_HEIGHT} band, split into per-tab segments. Unlike a
     * body control it takes no {@link #BODY_PADDING} inset - a header sits flush at the interior top - so
     * a tab panel frames it directly under the border. Reuses the same tab measurement and segment split
     * a body {@link kmlib.starsector.ui.controls.ControlKind#TABS} control uses, so a header tab is hit
     * exactly where a body tab would be and the header is not bespoke tab-strip framing.
     *
     * @param tabsSpec the tabs control, its labels and per-tab shortcuts in row order
     * @param originX  the header's left edge (the content inset), in UI coordinates
     * @param topY     the header's top edge (the content top), in UI coordinates
     * @param measurer measures each tab label's rendered width for snapping
     * @return the laid-out tabs control, its bounds the header band and its segments split per tab
     */
    public static Control layoutTabsHeader(ControlSpec tabsSpec, float originX, float topY,
            LineWidthMeasurer measurer) {
        var rowWidth = measureTabsRowWidth(tabsSpec, measurer);
        var bounds = new Rectangle(originX, topY - TAB_HEIGHT, rowWidth, TAB_HEIGHT);
        return toControl(tabsSpec, bounds, measurer);
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
    static List<Control> toControls(List<ControlSpec> specs, List<Rectangle> rows,
            LineWidthMeasurer measurer) {
        var controls = new ArrayList<Control>(specs.size());
        for (var index = 0; index < specs.size(); index++) {
            controls.add(toControl(specs.get(index), rows.get(index), measurer));
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
        // A radio (including the icon-list variant) and a tabs row split into per-hit segments; a tabs
        // row snaps its tabs to text, so it alone reads the measurer. Every other kind is a single-hit
        // row with no segments.
        var segments = switch (spec.kind()) {
            case RADIO -> splitRadioIntoSegments(spec, row);
            case TABS -> splitTabsIntoSegments(spec, row, measurer);
            default -> List.<Rectangle>of();
        };
        return new Control(spec, row, segments);
    }

    // The per-option hit segments of a radio row, split under its flow. A vertical radio lays its
    // options into its column count (one column is a plain top-to-bottom stack, more spreads them
    // column-major); a horizontal radio splits into equal side-by-side segments. The same split the
    // renderer draws against, so the drawn rows are the clickable ones.
    private static List<Rectangle> splitRadioIntoSegments(ControlSpec spec, Rectangle row) {
        if (spec.alignment() == RadioAlignment.VERTICAL) {
            return RadioRow.splitIntoGrid(row, spec.labels().size(), spec.columnCount());
        }
        return RadioRow.splitIntoSegments(row, spec.labels().size(), spec.alignment());
    }

    // The per-tab hit segments of a tabs row, each snapped to its label-plus-shortcut width via the
    // shared VanillaTabStrip geometry, so the tabs are hit exactly where they are drawn. The tabs hang
    // from the row's top edge at the tab height, so the segments align to the row the strip snapped.
    private static List<Rectangle> splitTabsIntoSegments(ControlSpec spec, Rectangle row,
            LineWidthMeasurer measurer) {
        var tabs = VanillaTabStrip.layoutTabs(row.x(), row.y() + row.height(), TAB_HEIGHT,
                TAB_TEXT_PADDING, MIN_TAB_WIDTH, TAB_FONT_SIZE, buildTabContents(spec), measurer);
        var segments = new ArrayList<Rectangle>(tabs.size());
        for (var tab : tabs) {
            segments.add(tab.bounds());
        }
        return List.copyOf(segments);
    }

    /**
     * Turns a tabs control's parallel label and shortcut lists into the tab contents the shared strip
     * geometry measures and lays out; an empty shortcut reads as no hint (VanillaTabStrip drops a blank
     * shortcut from the composed display). Public so the renderer pairs each laid-out tab segment with
     * the same content this measured and split it under, keeping one source for the pairing.
     *
     * @param spec the tabs control, its labels and per-tab shortcuts in row order
     * @return one {@link VanillaTabContent} per tab, in row order
     */
    public static List<VanillaTabContent> buildTabContents(ControlSpec spec) {
        var contents = new ArrayList<VanillaTabContent>(spec.labels().size());
        for (var index = 0; index < spec.labels().size(); index++) {
            contents.add(new VanillaTabContent(spec.labels().get(index), spec.shortcutAt(index)));
        }
        return contents;
    }

    // Widens every divider row to the strip's inner content width, so a rule stretches across the
    // whole body while every other row stays snapped to its own text. Run after the content width is
    // known, since a divider measures zero and must not drive the width it then spans.
    private static void spanDividersToContentWidth(List<ControlSpec> specs, List<Float> rowWidths,
            float contentWidth) {
        for (var index = 0; index < specs.size(); index++) {
            if (specs.get(index).kind() == ControlKind.DIVIDER) {
                rowWidths.set(index, contentWidth);
            }
        }
    }

    // The width of a control's row, snapped to its label(s): a checkbox is its tick box plus a gap
    // plus its label; a horizontal radio is its equal segments side by side, a vertical radio is one
    // segment column wide; a toggle is its label plus padding; a label is just its measured text,
    // since it has no widget chrome around it.
    private static float measureRowWidth(ControlSpec spec, LineWidthMeasurer measurer) {
        return switch (spec.kind()) {
            case CHECKBOX -> CONTROL_ROW_HEIGHT + CHECKBOX_LABEL_GAP
                    + measureWidth(measurer, spec.labels().get(0));
            case RADIO -> measureRadioRowWidth(spec, measurer);
            case TOGGLE -> measureWidth(measurer, spec.labels().get(0)) + TOGGLE_TEXT_PADDING;
            case LABEL -> measureWidth(measurer, spec.labels().get(0));
            case TABS -> measureTabsRowWidth(spec, measurer);
            // A divider has no intrinsic width - it stretches to the strip's inner width, resolved
            // once the widest row is known - so it contributes nothing to that width itself.
            case DIVIDER -> 0f;
        };
    }

    // The width a tabs row needs: its tabs laid side by side, each snapped to its label-plus-shortcut
    // width through the shared VanillaTabStrip geometry - the same snap layoutTabs later applies - so the
    // measured strip is exactly as wide as the drawn tabs.
    private static float measureTabsRowWidth(ControlSpec spec, LineWidthMeasurer measurer) {
        return VanillaTabStrip.measureRowWidth(buildTabContents(spec), TAB_TEXT_PADDING, MIN_TAB_WIDTH,
                TAB_FONT_SIZE, measurer);
    }

    // The width a radio row needs. A horizontal group lays its equal segments side by side. A
    // vertical group is its column count wide: each column sizes to the same width - a plain vertical
    // radio to its widest label plus padding, an icon-list radio (non-empty icon paths) to its widest
    // icon-and-label row - and the columns sit side by side, so a two-column list needs twice one
    // column's width. One column is the plain single-column stack.
    private static float measureRadioRowWidth(ControlSpec spec, LineWidthMeasurer measurer) {
        if (spec.alignment() != RadioAlignment.VERTICAL) {
            return spec.labels().size() * measureRadioSegmentWidth(spec, measurer);
        }
        var columnWidth = spec.iconPaths().isEmpty()
                ? measureRadioSegmentWidth(spec, measurer)
                : measureIconListRowWidth(spec, measurer);
        return spec.columnCount() * columnWidth;
    }

    // The height of a control's row: one control-row tall for every control except a vertical radio,
    // which stacks its options and so stands one control-row tall per row. A single-column list has
    // one row per option; a multi-column list wraps its options across columns, so it needs only as
    // many rows as its tallest column - the grid's row count - rather than one per option.
    private static float measureRowHeight(ControlSpec spec) {
        if (spec.kind() == ControlKind.RADIO && spec.alignment() == RadioAlignment.VERTICAL) {
            var rowCount = RadioRow.computeRowsPerColumn(spec.labels().size(), spec.columnCount());
            return rowCount * CONTROL_ROW_HEIGHT;
        }
        // A tabs row stands one tab-height tall (taller than a body row), since it is drawn in the
        // larger tab face rather than the body face.
        if (spec.kind() == ControlKind.TABS) {
            return TAB_HEIGHT;
        }
        return CONTROL_ROW_HEIGHT;
    }

    // The width an icon-list radio needs: its widest option row, each sized to hold its icon (present
    // when the option carries a non-null path), its label, and its trailing value (present when the
    // option carries one) without clipping. The option rows are one control-row tall, the height the
    // icon square derives from, so every stacked row shows an equal icon and the column is wide enough
    // that the longest name still clears its right-aligned value.
    private static float measureIconListRowWidth(ControlSpec spec, LineWidthMeasurer measurer) {
        var widest = 0f;
        // The trailing column is measured at the control's trailing size, not the body size, so a
        // compact column (a sort selector's smaller direction letters) reserves only the room its
        // reduced text needs - the same size the renderer then draws it at.
        var trailingFontSize = BODY_FONT_SIZE * spec.trailingScale();
        for (var index = 0; index < spec.labels().size(); index++) {
            var labelWidth = measureWidth(measurer, spec.labels().get(index));
            var trailingWidth = (float) measurer.measureLineWidth(spec.trailingLabelAt(index),
                    trailingFontSize);
            var rowWidth = IconLabelRow.measureRowWidth(CONTROL_ROW_HEIGHT, labelWidth,
                    spec.hasIconAt(index), trailingWidth);
            widest = Math.max(widest, rowWidth);
        }
        return widest;
    }

    // Segments are equal width, so all fit when each is sized to the widest option label plus
    // padding.
    private static float measureRadioSegmentWidth(ControlSpec spec, LineWidthMeasurer measurer) {
        var widest = 0f;
        for (var label : spec.labels()) {
            widest = Math.max(widest, measureWidth(measurer, label));
        }
        return widest + RADIO_SEGMENT_PADDING;
    }

    // Extra footprint a trailing label adds past the control's own row, or none when it is blank.
    private static float measureTrailingWidth(ControlSpec spec, LineWidthMeasurer measurer) {
        if (!KmlibStrings.hasText(spec.trailingLabel())) {
            return 0f;
        }
        return TRAILING_LABEL_GAP + measureWidth(measurer, spec.trailingLabel());
    }

    private static float measureWidth(LineWidthMeasurer measurer, String text) {
        return (float) measurer.measureLineWidth(text, BODY_FONT_SIZE);
    }

    /**
     * The strip's measured footprint and the per-row dimensions behind it, returned together so the
     * size feeds the host's frame and the same row dimensions place the controls without measuring
     * twice. Both lists are empty and the footprint zero for an empty strip.
     *
     * @param bodyWidth  the strip footprint width, inset included
     * @param bodyHeight the strip footprint height, inset included
     * @param rowWidths  each control row's measured width, top to bottom
     * @param rowHeights each control row's measured height, top to bottom
     */
    public record StripMeasurement(float bodyWidth, float bodyHeight, List<Float> rowWidths,
            List<Float> rowHeights) {
    }
}
