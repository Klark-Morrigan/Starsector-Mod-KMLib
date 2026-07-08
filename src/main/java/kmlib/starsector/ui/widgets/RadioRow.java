package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.math.geometry.Rectangles;
import kmlib.starsector.ui.render.UiBoxes;
import kmlib.starsector.ui.render.UiFill;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A row of N equal-width, mutually exclusive segments - the raw-GL stand-in for a radio group,
 * built from single-selection over adjacent cells since neither vanilla nor a raw-GL library
 * ships one. The selected segment is lit; the rest read as one framed strip divided by hairline
 * rules. The consumer owns which segment is selected and draws each segment's label; this owns
 * only where the segments sit and which one a point falls in.
 *
 * <p>{@link #splitIntoSegments}, {@link #findSegmentIndexAt}, and {@link #findHitElement} are
 * pure geometry and unit-tested; {@link #render} is the raw GL passthrough, exercised in-engine.
 */
public final class RadioRow {
    /** {@link #findSegmentIndexAt} returns this when the point falls outside the row. */
    public static final int NO_SEGMENT = Rectangles.NONE;

    // The lit segment is a wash over the frame, not a second opaque block, so it reads as a
    // highlight; the dividers are fainter still so they separate without competing.
    private static final float SELECTED_FILL_ALPHA_MULT = 0.30f;
    private static final float DIVIDER_ALPHA_MULT = 0.40f;
    private static final float DIVIDER_THICKNESS = 1f;
    private static final float OUTLINE_THICKNESS = 1f;

    private RadioRow() {
    }

    /**
     * Divides {@code bounds} into {@code segmentCount} equal-width cells, left to right. A count
     * of zero or less yields no segments.
     *
     * @param bounds       the row's footprint
     * @param segmentCount how many equal cells to split it into
     * @return the segment rectangles, left to right (empty when {@code segmentCount <= 0})
     */
    public static List<Rectangle> splitIntoSegments(Rectangle bounds, int segmentCount) {
        if (segmentCount <= 0) {
            return List.of();
        }
        var segments = new ArrayList<Rectangle>(segmentCount);
        var segmentWidth = bounds.width() / segmentCount;
        for (var index = 0; index < segmentCount; index++) {
            segments.add(new Rectangle(bounds.x() + index * segmentWidth, bounds.y(),
                    segmentWidth, bounds.height()));
        }
        return List.copyOf(segments);
    }

    /**
     * The index of the segment containing {@code (pointX, pointY)}, or {@link #NO_SEGMENT} when
     * the point falls outside the row. Segments abut, so a point on a shared edge resolves to the
     * left segment.
     *
     * @param bounds       the row's footprint
     * @param segmentCount how many equal cells the row is split into
     * @param pointX       the point's x, in UI coordinates
     * @param pointY       the point's y, in UI coordinates
     * @return the containing segment's index, or {@link #NO_SEGMENT}
     */
    public static int findSegmentIndexAt(Rectangle bounds, int segmentCount, float pointX,
            float pointY) {
        return Rectangles.findIndexContaining(splitIntoSegments(bounds, segmentCount), pointX,
                pointY);
    }

    /**
     * The actionable segment a press resolves to: the segment containing {@code (pointX, pointY)},
     * or {@link #NO_SEGMENT} when the point falls outside the row OR lands on the segment already
     * selected. Re-picking the lit option changes nothing - standard radio behaviour - so a press
     * on it reports no segment, letting the caller consume the click yet fire no action. This is
     * the selection rule folded into hit detection: the caller hands in the selection it already
     * owns, and the widget alone decides which hits are worth acting on, so no consumer re-derives
     * the "already on" check.
     *
     * @param bounds        the row's footprint
     * @param segmentCount  how many equal cells the row is split into
     * @param selectedIndex the currently lit segment, whose own cell is inert; a value outside the
     *                      row (e.g. none selected) leaves every segment actionable
     * @param pointX        the point's x, in UI coordinates
     * @param pointY        the point's y, in UI coordinates
     * @return the actionable segment's index, or {@link #NO_SEGMENT}
     */
    public static int findHitElement(Rectangle bounds, int segmentCount, int selectedIndex,
            float pointX, float pointY) {
        var hitIndex = findSegmentIndexAt(bounds, segmentCount, pointX, pointY);
        if (hitIndex == selectedIndex) {
            return NO_SEGMENT;
        }
        return hitIndex;
    }

    /**
     * Washes the selected segment, rules the dividers between segments, and frames the whole row.
     * Every draw fades by {@code opacity}. A {@code selectedIndex} outside the row lights none.
     *
     * @param bounds        the row's footprint, in UI coordinates
     * @param segmentCount  how many equal cells the row is split into
     * @param selectedIndex the lit segment's index, or a value outside the row to light none
     * @param frameColor    the outline and divider colour
     * @param selectedColor the lit-segment wash colour
     * @param opacity       overall alpha, 0..1
     */
    public static void render(Rectangle bounds, int segmentCount, int selectedIndex,
            Color frameColor, Color selectedColor, float opacity) {
        var segments = splitIntoSegments(bounds, segmentCount);
        for (var index = 0; index < segments.size(); index++) {
            var segment = segments.get(index);
            if (index == selectedIndex) {
                UiFill.renderQuad(segment.x(), segment.y(), segment.width(), segment.height(),
                        selectedColor, opacity * SELECTED_FILL_ALPHA_MULT);
            }
            if (index > 0) {
                UiFill.renderQuad(segment.x(), segment.y(), DIVIDER_THICKNESS,
                        segment.height(), frameColor, opacity * DIVIDER_ALPHA_MULT);
            }
        }
        UiBoxes.renderBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                OUTLINE_THICKNESS, frameColor, opacity);
    }
}
