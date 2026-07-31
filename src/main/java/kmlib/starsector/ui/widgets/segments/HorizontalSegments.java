package kmlib.starsector.ui.widgets.segments;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.SegmentSizing;
import kmlib.starsector.ui.font.LineWidthMeasurer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The width and placement geometry of a horizontal row of text-labelled segments - the single source
 * both a horizontal radio and a tab strip size themselves through, so segment width is a control
 * property ({@link SegmentSizing}) rather than a per-widget re-derivation. It owns three things: each
 * segment's width under the sizing, the row width those widths sum to, and where the segments sit once
 * a host lays them from an origin. Pure geometry in UI coordinates (origin bottom-left), measured
 * through the injected {@link LineWidthMeasurer}, so it holds no state and touches no GL surface.
 *
 * <p>The two sizings differ only in how a segment's width is chosen: {@link SegmentSizing#UNIFORM}
 * gives every segment the widest label plus the padding (even cells, an option pair), {@link
 * SegmentSizing#SNAPPED} gives each segment its own label plus the padding, floored at a minimum (the
 * tab strip). Measuring and placing both read the same per-segment widths, so a measured row is exactly
 * as wide as the laid-out segments and the two cannot drift - the consistency the tab strip and the
 * radio previously each maintained on their own.
 */
public final class HorizontalSegments {
    private HorizontalSegments() {
    }

    /**
     * Each segment's width for {@code labels} under {@code spec}, in label order. {@link
     * SegmentSizing#UNIFORM} returns the widest label plus the padding (floored at the minimum)
     * repeated once per label, so the cells are even; {@link SegmentSizing#SNAPPED} returns each
     * label's own measured width plus the padding, floored at the minimum. No labels yields no widths.
     *
     * @param labels   the segment labels, in row order left to right
     * @param spec     the padding, minimum, font size, and sizing rule the widths derive from
     * @param measurer measures each label's rendered width in the label font
     * @return one width per label, in the same order (empty for no labels)
     */
    public static List<Float> computeSegmentWidths(
            List<String> labels,
            SegmentSpec spec,
            LineWidthMeasurer measurer) {
                
        if (labels.isEmpty()) {
            return List.of();
        }
        if (spec.sizing() == SegmentSizing.UNIFORM) {
            var widest = 0f;
            for (var label : labels) {
                widest = Math.max(widest, (float) measurer.measureLineWidth(label, spec.fontSize()));
            }
            return Collections.nCopies(labels.size(), Math.max(spec.minWidth(), widest + spec.padding()));
        }
        var widths = new ArrayList<Float>(labels.size());
        for (var label : labels) {
            var measured = (float) measurer.measureLineWidth(label, spec.fontSize());
            widths.add(Math.max(spec.minWidth(), measured + spec.padding()));
        }
        return List.copyOf(widths);
    }

    /**
     * The width the whole row spans: the per-segment widths under {@code spec} summed, so a host sizing
     * its chrome around the row reserves exactly the width {@link #placeSegments} then lays the segments
     * to. Reads the same {@link #computeSegmentWidths} the placement does, so the measured row and the
     * laid-out segments cannot drift on any segment's width.
     *
     * @param labels   the segment labels, in row order (the same labels handed to the placement)
     * @param spec     the padding, minimum, font size, and sizing rule, matching {@link
     *                 #computeSegmentWidths}
     * @param measurer measures each label's rendered width
     * @return the summed segment width of the row, or 0 for no labels
     */
    public static float measureRowWidth(
            List<String> labels,
            SegmentSpec spec,
            LineWidthMeasurer measurer) {

        var total = 0f;
        for (var width : computeSegmentWidths(labels, spec, measurer)) {
            total += width;
        }
        return total;
    }

    /**
     * Lays each width from {@code widths} out side by side from {@code originX}, every segment {@code
     * height} tall and hanging from {@code bottomY} (UI y grows up), so the segments share one top and
     * bottom edge and abut left to right in width order. Placement is width-driven, so a caller places
     * either sizing's widths the same way - the sizing decided the widths, not how they sit.
     *
     * @param originX the row's left edge, in UI coordinates
     * @param bottomY the row's bottom edge, in UI coordinates
     * @param height  the height every segment shares
     * @param widths  each segment's width, in row order (from {@link #computeSegmentWidths})
     * @return one rectangle per width, in the same order (empty for no widths)
     */
    public static List<Rectangle> placeSegments(
            float originX,
            float bottomY,
            float height,
            List<Float> widths) {

        var segments = new ArrayList<Rectangle>(widths.size());
        var cursorX = originX;

        for (var width : widths) {
            segments.add(new Rectangle(cursorX, bottomY, width, height));
            cursorX += width;
        }
        return List.copyOf(segments);
    }

    /**
     * One divider rect per interior seam of {@code segments}: a {@code thickness}-wide rule at each
     * non-first segment's left edge, spanning that segment's height. The seams are read straight off the
     * laid segments, so the dividers land on the real boundaries whether the segments are even (uniform)
     * or ragged (snapped) - the chrome cannot part from the segments the labels draw over. A row of one
     * segment or none has no interior seam and yields no dividers.
     *
     * @param segments  the laid-out segments, in row order (from {@link #placeSegments})
     * @param thickness the divider rule's width in UI units
     * @return one divider rect per non-first segment, in row order (empty for fewer than two segments)
     */
    public static List<Rectangle> computeDividers(
            List<Rectangle> segments,
            float thickness) {

        var dividers = new ArrayList<Rectangle>(Math.max(0, segments.size() - 1));

        for (var index = 1; index < segments.size(); index++) {
            var segment = segments.get(index);
            dividers.add(new Rectangle(segment.x(), segment.y(), thickness, segment.height()));
        }
        return List.copyOf(dividers);
    }
}
