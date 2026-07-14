package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.widgets.HorizontalSegments;
import kmlib.starsector.ui.widgets.RadioRow;

import java.util.List;

/**
 * Raw-GL paint for a radio group: washes the selected segment, rules the dividers between segments,
 * and frames the whole row, all faded by one opacity. The segment geometry lives on the
 * substrate-independent widgets ({@link RadioRow} for the vertical grid, {@link HorizontalSegments}
 * for a horizontal row); this is the GL passthrough (over {@link UiFill} and {@link UiBoxes}),
 * exercised in-engine. The lit segment is a wash over the frame rather than a second opaque block, so
 * it reads as a highlight; the dividers are fainter still so they separate without competing.
 *
 * <p>The two flows have separate entry points because they read their segments differently. A vertical
 * group draws as a grid of {@code columnCount} columns (one column is the ordinary stacked list) it
 * re-derives from the footprint - the wash lands on the selected option's cell and the dividers rule
 * the whole grid, so the cells read as a table. A horizontal group draws over the laid segment rects
 * the caller already hit-tests and labels, so the chrome (wash, dividers, frame) lands on the same
 * geometry the labels do whether the segments are even or snapped, never a re-derived equal split.
 */
public final class RadioRowRenderer {
    private static final float SELECTED_FILL_ALPHA_MULT = 0.30f;
    private static final float DIVIDER_ALPHA_MULT = 0.40f;
    private static final float DIVIDER_THICKNESS = 1f;
    private static final float OUTLINE_THICKNESS = 1f;

    private RadioRowRenderer() {
    }

    /**
     * Washes the selected segment, rules a vertical divider on each interior seam of the laid {@code
     * segments}, and frames the whole row - the side-by-side option strip a horizontal radio reads as.
     * Every draw fades by {@code opacity}. A {@code selectedIndex} outside the row lights none. The
     * chrome is drawn from the caller's own segment rects (the ones it hit-tests and labels), so it
     * follows the real seams whether the segments are even (uniform) or ragged (snapped) rather than a
     * re-derived equal split.
     *
     * @param bounds        the row's footprint, in UI coordinates (for the outer frame)
     * @param segments      the laid-out segment rects, in row order left to right
     * @param selectedIndex the lit segment's index, or a value outside the row to light none
     * @param colors        the frame stroke and selected-wash palette
     * @param opacity       overall alpha, 0..1
     */
    public static void renderHorizontalRow(Rectangle bounds, List<Rectangle> segments,
            int selectedIndex, RadioColors colors, float opacity) {
        if (selectedIndex >= 0 && selectedIndex < segments.size()) {
            var selected = segments.get(selectedIndex);
            UiFill.renderQuad(selected.x(), selected.y(), selected.width(), selected.height(),
                    colors.selectedWash(), opacity * SELECTED_FILL_ALPHA_MULT);
        }
        for (var divider : HorizontalSegments.computeDividers(segments, DIVIDER_THICKNESS)) {
            UiFill.renderQuad(divider.x(), divider.y(), divider.width(), divider.height(),
                    colors.frame(), opacity * DIVIDER_ALPHA_MULT);
        }
        UiBoxes.renderBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                OUTLINE_THICKNESS, colors.frame(), opacity);
    }

    /**
     * Washes the selected column-major cell, rules a grid of column and row dividers, and frames the
     * whole list. The cells come from the same grid split the layout hit-tests and the icon list draws
     * icons over, so the wash lands on the option the player clicks. One column reduces to a plain
     * stacked list: no column rule, one horizontal rule between each option. A {@code selectedIndex}
     * outside the list lights none. Every draw fades by {@code opacity}.
     *
     * @param bounds        the list's footprint, in UI coordinates
     * @param optionCount   how many options the list holds
     * @param selectedIndex the lit option's index, or a value outside the list to light none
     * @param columnCount   how many columns the options wrap across (one is a single stack)
     * @param colors        the frame stroke and selected-wash palette
     * @param opacity       overall alpha, 0..1
     */
    public static void renderVerticalGrid(Rectangle bounds, int optionCount, int selectedIndex,
            int columnCount, RadioColors colors, float opacity) {
        var segments = RadioRow.splitIntoGrid(bounds, optionCount, columnCount);
        if (selectedIndex >= 0 && selectedIndex < segments.size()) {
            var selected = segments.get(selectedIndex);
            UiFill.renderQuad(selected.x(), selected.y(), selected.width(), selected.height(),
                    colors.selectedWash(), opacity * SELECTED_FILL_ALPHA_MULT);
        }
        // The tallest column, matching the grid split, so the row rules land on the same boundaries
        // the cells abut on.
        var rowCount = RadioRow.computeRowsPerColumn(optionCount, columnCount);
        var columnWidth = bounds.width() / columnCount;
        var rowHeight = bounds.height() / rowCount;
        for (var column = 1; column < columnCount; column++) {
            var columnX = bounds.x() + column * columnWidth;
            UiFill.renderQuad(columnX, bounds.y(), DIVIDER_THICKNESS, bounds.height(), colors.frame(),
                    opacity * DIVIDER_ALPHA_MULT);
        }
        for (var row = 1; row < rowCount; row++) {
            var boundaryY = bounds.y() + bounds.height() - row * rowHeight;
            UiFill.renderQuad(bounds.x(), boundaryY - DIVIDER_THICKNESS, bounds.width(),
                    DIVIDER_THICKNESS, colors.frame(), opacity * DIVIDER_ALPHA_MULT);
        }
        UiBoxes.renderBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                OUTLINE_THICKNESS, colors.frame(), opacity);
    }
}
