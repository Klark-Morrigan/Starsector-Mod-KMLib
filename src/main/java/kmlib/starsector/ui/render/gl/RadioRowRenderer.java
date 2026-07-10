package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.RadioAlignment;
import kmlib.starsector.ui.widgets.RadioRow;

import java.awt.Color;

/**
 * Raw-GL paint for a {@link RadioRow}: washes the selected segment, rules the dividers between
 * segments, and frames the whole row, all faded by one opacity. The segment geometry lives on the
 * substrate-independent widget; this is the GL passthrough (over {@link UiFill} and {@link UiBoxes}),
 * exercised in-engine. The lit segment is a wash over the frame rather than a second opaque block, so
 * it reads as a highlight; the dividers are fainter still so they separate without competing.
 */
public final class RadioRowRenderer {
    private static final float SELECTED_FILL_ALPHA_MULT = 0.30f;
    private static final float DIVIDER_ALPHA_MULT = 0.40f;
    private static final float DIVIDER_THICKNESS = 1f;
    private static final float OUTLINE_THICKNESS = 1f;

    private RadioRowRenderer() {
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
        var segments = RadioRow.splitIntoSegments(bounds, segmentCount, alignment);
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
