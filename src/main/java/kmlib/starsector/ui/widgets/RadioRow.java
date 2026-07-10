package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.math.geometry.Rectangles;
import kmlib.starsector.ui.controls.RadioAlignment;

import java.util.ArrayList;
import java.util.List;

/**
 * The geometry of a row of N equal, mutually exclusive segments - the model behind a radio group,
 * built from single-selection over adjacent cells. Substrate-independent: it splits a footprint into
 * segments and resolves which one a point falls in, rendering nothing, so a GL or a UI-API renderer
 * can paint against it. The raw-GL paint lives in
 * {@link kmlib.starsector.ui.render.gl.RadioRowRenderer}.
 *
 * <p>Segments flow either way ({@link RadioAlignment}): a horizontal group splits into equal columns
 * left to right, a vertical group into equal rows top to bottom, so a compact option pair reads as a
 * strip while a longer option list reads as a column. The flow is a splitting concern only - {@link
 * #splitIntoSegments} takes it, while the two hit-tests run over the already-split segments.
 */
public final class RadioRow {
    /** {@link #findSegmentIndexAt} returns this when the point falls outside every segment. */
    public static final int NO_SEGMENT = Rectangles.NONE;

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
}
