package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.math.geometry.Rectangles;
import kmlib.starsector.ui.render.UiBoxes;
import kmlib.starsector.ui.render.UiFill;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A row of N equal-size, mutually exclusive segments - the raw-GL stand-in for a radio group,
 * built from single-selection over adjacent cells since neither vanilla nor a raw-GL library
 * ships one. The selected segment is lit; the rest read as one framed strip divided by hairline
 * rules. The consumer owns which segment is selected and draws each segment's label; this owns
 * only where the segments sit and which one a point falls in.
 *
 * <p>Segments flow either way ({@link RadioAlignment}): a horizontal group splits its footprint
 * into equal columns left to right, a vertical group into equal rows top to bottom, so a compact
 * option pair reads as a strip while a longer option list reads as a column. The flow is a
 * splitting-and-drawing concern only - {@link #splitIntoSegments} and {@link #render} take it,
 * while the two hit-tests run over the already-split segments and so need no alignment.
 *
 * <p>{@link #splitIntoSegments}, {@link #findSegmentIndexAt}, and {@link #findHitElement} are
 * pure geometry and unit-tested; {@link #render} is the raw GL passthrough, exercised in-engine.
 */
public final class RadioRow {
    /** {@link #findSegmentIndexAt} returns this when the point falls outside every segment. */
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
     * Divides {@code bounds} into {@code segmentCount} equal cells in the flow {@code alignment}
     * gives: horizontal splits by width left to right, vertical by height top to bottom (the first
     * cell hangs from the top edge). A count of zero or less yields no segments.
     *
     * @param bounds       the row's footprint
     * @param segmentCount how many equal cells to split it into
     * @param alignment    the direction the cells flow in
     * @return the segment rectangles, in flow order (empty when {@code segmentCount <= 0})
     */
    public static List<Rectangle> splitIntoSegments(Rectangle bounds, int segmentCount,
            RadioAlignment alignment) {
        if (segmentCount <= 0) {
            return List.of();
        }
        var segments = new ArrayList<Rectangle>(segmentCount);
        if (alignment == RadioAlignment.VERTICAL) {
            // Cells stack top to bottom; the first hangs from the top edge and each later cell
            // drops one cell height, so element 0 is the topmost row (UI y grows up).
            var segmentHeight = bounds.height() / segmentCount;
            for (var index = 0; index < segmentCount; index++) {
                segments.add(new Rectangle(bounds.x(),
                        bounds.y() + bounds.height() - (index + 1) * segmentHeight,
                        bounds.width(), segmentHeight));
            }
        } else {
            var segmentWidth = bounds.width() / segmentCount;
            for (var index = 0; index < segmentCount; index++) {
                segments.add(new Rectangle(bounds.x() + index * segmentWidth, bounds.y(),
                        segmentWidth, bounds.height()));
            }
        }
        return List.copyOf(segments);
    }

    /**
     * The index of the segment containing {@code (pointX, pointY)}, or {@link #NO_SEGMENT} when the
     * point falls outside every segment. Runs over the already-split {@code segments}, so it needs
     * no alignment; abutting segments share an edge, which resolves to the earlier segment.
     *
     * @param segments the row's segments, in flow order (as {@link #splitIntoSegments} returns them)
     * @param pointX   the point's x, in UI coordinates
     * @param pointY   the point's y, in UI coordinates
     * @return the containing segment's index, or {@link #NO_SEGMENT}
     */
    public static int findSegmentIndexAt(List<Rectangle> segments, float pointX, float pointY) {
        return Rectangles.findIndexContaining(segments, pointX, pointY);
    }

    /**
     * The actionable segment a press resolves to: the segment containing {@code (pointX, pointY)},
     * or {@link #NO_SEGMENT} when the point falls outside every segment OR lands on the segment
     * already selected. Re-picking the lit option changes nothing - standard radio behaviour - so a
     * press on it reports no segment, letting the caller consume the click yet fire no action. This
     * is the selection rule folded into hit detection: the caller hands in the selection it already
     * owns, and the widget alone decides which hits are worth acting on, so no consumer re-derives
     * the "already on" check. A radio that instead toggles off on a re-pick skips this and reads the
     * raw hit through {@link #findSegmentIndexAt}.
     *
     * @param segments      the row's segments, in flow order
     * @param selectedIndex the currently lit segment, whose own cell is inert; a value outside the
     *                      row (e.g. none selected) leaves every segment actionable
     * @param pointX        the point's x, in UI coordinates
     * @param pointY        the point's y, in UI coordinates
     * @return the actionable segment's index, or {@link #NO_SEGMENT}
     */
    public static int findHitElement(List<Rectangle> segments, int selectedIndex, float pointX,
            float pointY) {
        var hitIndex = findSegmentIndexAt(segments, pointX, pointY);
        if (hitIndex == selectedIndex) {
            return NO_SEGMENT;
        }
        return hitIndex;
    }

    /**
     * Washes the selected segment, rules the dividers between segments, and frames the whole row.
     * Every draw fades by {@code opacity}. A {@code selectedIndex} outside the row lights none. The
     * dividers run across the flow: vertical rules between horizontal columns, horizontal rules
     * between vertical rows.
     *
     * @param bounds        the row's footprint, in UI coordinates
     * @param segmentCount  how many equal cells the row is split into
     * @param selectedIndex the lit segment's index, or a value outside the row to light none
     * @param alignment     the direction the segments flow in
     * @param frameColor    the outline and divider colour
     * @param selectedColor the lit-segment wash colour
     * @param opacity       overall alpha, 0..1
     */
    public static void render(Rectangle bounds, int segmentCount, int selectedIndex,
            RadioAlignment alignment, Color frameColor, Color selectedColor, float opacity) {
        var segments = splitIntoSegments(bounds, segmentCount, alignment);
        for (var index = 0; index < segments.size(); index++) {
            var segment = segments.get(index);
            if (index == selectedIndex) {
                UiFill.renderQuad(segment.x(), segment.y(), segment.width(), segment.height(),
                        selectedColor, opacity * SELECTED_FILL_ALPHA_MULT);
            }
            if (index > 0) {
                renderDivider(segment, alignment, frameColor, opacity);
            }
        }
        UiBoxes.renderBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                OUTLINE_THICKNESS, frameColor, opacity);
    }

    // Rules the divider on the edge each non-first segment shares with the one before it: the left
    // edge for a horizontal column, the top edge for a vertical row (its shared edge with the row
    // above). Kept a hairline thickness so it separates the cells without competing with the frame.
    private static void renderDivider(Rectangle segment, RadioAlignment alignment, Color frameColor,
            float opacity) {
        if (alignment == RadioAlignment.VERTICAL) {
            UiFill.renderQuad(segment.x(), segment.y() + segment.height() - DIVIDER_THICKNESS,
                    segment.width(), DIVIDER_THICKNESS, frameColor, opacity * DIVIDER_ALPHA_MULT);
        } else {
            UiFill.renderQuad(segment.x(), segment.y(), DIVIDER_THICKNESS, segment.height(),
                    frameColor, opacity * DIVIDER_ALPHA_MULT);
        }
    }
}
