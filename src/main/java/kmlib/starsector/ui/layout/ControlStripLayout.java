package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlKind;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.RadioAlignment;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.IconLabelRow;
import kmlib.starsector.ui.widgets.RadioRow;
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
        var stackedHeight = 0f;
        for (var spec : specs) {
            var rowWidth = measureRowWidth(spec, measurer);
            rowWidths.add(rowWidth);
            var rowHeight = measureRowHeight(spec);
            rowHeights.add(rowHeight);
            stackedHeight += rowHeight;
            contentWidth = Math.max(contentWidth, rowWidth + measureTrailingWidth(spec, measurer));
        }
        var bodyWidth = contentWidth + 2f * BODY_PADDING;
        var bodyHeight = 2f * BODY_PADDING + stackedHeight + (specs.size() - 1) * ROW_GAP;
        return new StripMeasurement(bodyWidth, bodyHeight, List.copyOf(rowWidths),
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
     * @return the laid-out controls, top to bottom; empty when {@code specs} is empty
     */
    public static List<Control> layoutControls(Rectangle body, List<ControlSpec> specs,
            List<Float> rowHeights, List<Float> rowWidths) {
        if (specs.isEmpty()) {
            return List.of();
        }
        var bodyTopY = body.y() + body.height();
        var rows = RowStack.layoutRows(body.x() + BODY_PADDING, bodyTopY - BODY_PADDING,
                ROW_GAP, rowHeights, rowWidths);
        var controls = new ArrayList<Control>(specs.size());
        for (var index = 0; index < specs.size(); index++) {
            var spec = specs.get(index);
            var row = rows.get(index);
            // A radio (including the icon-list variant, which is a vertical radio that also draws an
            // icon) splits into per-option hit segments under its alignment; every other kind is a
            // single-hit row with no segments.
            var segments = spec.kind() == ControlKind.RADIO
                    ? RadioRow.splitIntoSegments(row, spec.labels().size(), spec.alignment())
                    : List.<Rectangle>of();
            controls.add(new Control(spec, row, segments));
        }
        return List.copyOf(controls);
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
        };
    }

    // The width a radio row needs. A horizontal group lays its equal segments side by side. A
    // vertical group is one segment column wide: a plain vertical radio sizes that column to its
    // widest label plus padding, while an icon-list radio (non-empty icon paths) sizes it to its
    // widest icon-and-label row, so the icon and name clear the frame.
    private static float measureRadioRowWidth(ControlSpec spec, LineWidthMeasurer measurer) {
        if (spec.alignment() != RadioAlignment.VERTICAL) {
            return spec.labels().size() * measureRadioSegmentWidth(spec, measurer);
        }
        if (spec.iconPaths().isEmpty()) {
            return measureRadioSegmentWidth(spec, measurer);
        }
        return measureIconListRowWidth(spec, measurer);
    }

    // The height of a control's row: one control-row tall for every control except a vertical radio,
    // which stacks its options and so stands one control-row tall per option (the icon-list variant
    // included, since it is a vertical radio).
    private static float measureRowHeight(ControlSpec spec) {
        if (spec.kind() == ControlKind.RADIO && spec.alignment() == RadioAlignment.VERTICAL) {
            return spec.labels().size() * CONTROL_ROW_HEIGHT;
        }
        return CONTROL_ROW_HEIGHT;
    }

    // The width an icon-list radio needs: its widest option row, each sized to hold its icon (present
    // when the option carries a non-null path) and its label without clipping. The option rows are
    // one control-row tall, the height the icon square derives from, so every stacked row shows an
    // equal icon and the column is as wide as the longest name.
    private static float measureIconListRowWidth(ControlSpec spec, LineWidthMeasurer measurer) {
        var widest = 0f;
        for (var index = 0; index < spec.labels().size(); index++) {
            var labelWidth = measureWidth(measurer, spec.labels().get(index));
            var rowWidth = IconLabelRow.measureRowWidth(CONTROL_ROW_HEIGHT, labelWidth,
                    spec.hasIconAt(index));
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
