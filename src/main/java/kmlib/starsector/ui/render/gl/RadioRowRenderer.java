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
 *
 * <p>A vertical group draws as a grid of {@code columnCount} columns (one column is the ordinary
 * stacked list): the wash lands on the selected option's cell, and the dividers rule the whole grid -
 * a vertical rule at each column boundary and a horizontal rule at each row boundary - so the cells
 * read as a table. A horizontal group stays a single row of side-by-side segments.
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
     * Every draw fades by {@code opacity}. A {@code selectedIndex} outside the row lights none. A
     * horizontal group rules vertical dividers between its side-by-side segments; a vertical group
     * lays its options across {@code columnCount} columns and rules a grid of dividers between them.
     *
     * @param bounds        the row's footprint, in UI coordinates
     * @param segmentCount  how many equal cells the row is split into
     * @param selectedIndex the lit segment's index, or a value outside the row to light none
     * @param alignment     the direction the segments flow in
     * @param columnCount   how many columns a vertical group spreads its options across; ignored for
     *                      a horizontal group
     * @param frameColor    the outline and divider colour
     * @param selectedColor the lit-segment wash colour
     * @param opacity       overall alpha, 0..1
     */
    public static void render(Rectangle bounds, int segmentCount, int selectedIndex,
            RadioAlignment alignment, int columnCount, Color frameColor, Color selectedColor,
            float opacity) {
        if (alignment == RadioAlignment.VERTICAL) {
            renderVerticalGrid(bounds, segmentCount, selectedIndex, columnCount, frameColor,
                    selectedColor, opacity);
        } else {
            renderHorizontalRow(bounds, segmentCount, selectedIndex, frameColor, selectedColor,
                    opacity);
        }
    }

    // Washes the selected column-major cell, rules a grid of column and row dividers, and frames the
    // whole list. The cells come from the same grid split the layout hit-tests and the icon list
    // draws icons over, so the wash lands on the option the player clicks. One column reduces to a
    // plain stacked list: no column rule, one horizontal rule between each option.
    private static void renderVerticalGrid(Rectangle bounds, int optionCount, int selectedIndex,
            int columnCount, Color frameColor, Color selectedColor, float opacity) {
        var segments = RadioRow.splitIntoGrid(bounds, optionCount, columnCount);
        if (selectedIndex >= 0 && selectedIndex < segments.size()) {
            var selected = segments.get(selectedIndex);
            UiFill.renderQuad(selected.x(), selected.y(), selected.width(), selected.height(),
                    selectedColor, opacity * SELECTED_FILL_ALPHA_MULT);
        }
        // The tallest column, matching the grid split, so the row rules land on the same boundaries
        // the cells abut on.
        var rowCount = RadioRow.computeRowsPerColumn(optionCount, columnCount);
        var columnWidth = bounds.width() / columnCount;
        var rowHeight = bounds.height() / rowCount;
        for (var column = 1; column < columnCount; column++) {
            var columnX = bounds.x() + column * columnWidth;
            UiFill.renderQuad(columnX, bounds.y(), DIVIDER_THICKNESS, bounds.height(), frameColor,
                    opacity * DIVIDER_ALPHA_MULT);
        }
        for (var row = 1; row < rowCount; row++) {
            var boundaryY = bounds.y() + bounds.height() - row * rowHeight;
            UiFill.renderQuad(bounds.x(), boundaryY - DIVIDER_THICKNESS, bounds.width(),
                    DIVIDER_THICKNESS, frameColor, opacity * DIVIDER_ALPHA_MULT);
        }
        UiBoxes.renderBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                OUTLINE_THICKNESS, frameColor, opacity);
    }

    // Washes the selected segment, rules a vertical divider on each non-first segment's shared left
    // edge, and frames the whole row - the side-by-side option strip a horizontal radio reads as.
    private static void renderHorizontalRow(Rectangle bounds, int segmentCount, int selectedIndex,
            Color frameColor, Color selectedColor, float opacity) {
        var segments = RadioRow.splitIntoSegments(bounds, segmentCount, RadioAlignment.HORIZONTAL);
        for (var index = 0; index < segments.size(); index++) {
            var segment = segments.get(index);
            if (index == selectedIndex) {
                UiFill.renderQuad(segment.x(), segment.y(), segment.width(), segment.height(),
                        selectedColor, opacity * SELECTED_FILL_ALPHA_MULT);
            }
            if (index > 0) {
                UiFill.renderQuad(segment.x(), segment.y(), DIVIDER_THICKNESS, segment.height(),
                        frameColor, opacity * DIVIDER_ALPHA_MULT);
            }
        }
        UiBoxes.renderBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                OUTLINE_THICKNESS, frameColor, opacity);
    }
}
