package kmlib.starsector.ui.render.gl.controls;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.render.gl.UiBoxes;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.RadioRow;

import java.util.List;
import java.util.function.IntFunction;

/**
 * Raw-GL paint for a radio group: washes the segments the pointer has lifted, washes the selected one,
 * lights the segments a press is still falling on, rules the dividers between segments,
 * and frames the whole row, all faded by one opacity. The shared segmented-row chrome (the wash and the
 * seam dividers) is drawn through {@link HorizontalSegmentsRenderer} so a radio row and a tab strip
 * cannot drift on it; only the outer frame is the radio's own. The segment geometry lives on the
 * substrate-independent {@link RadioRow} widget; this is the GL passthrough, run only in-engine. The
 * lit segment is a wash over the frame rather than a second opaque block, so it reads as a highlight;
 * the dividers are fainter still so they separate without competing.
 *
 * <p>The two flows have separate entry points because they read their segments differently. A vertical
 * group draws as a grid of {@code columnCount} columns (one column is the ordinary stacked list) it
 * re-derives from the footprint - the wash lands on the selected option's cell and the dividers rule
 * the whole grid, so the cells read as a table. A horizontal group draws over the laid segment rects
 * the caller already hit-tests and labels, so the chrome (wash, dividers, frame) lands on the same
 * geometry the labels do whether the segments are even or snapped, never a re-derived equal split.
 */
public final class RadioRowRenderer {
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
     * @param colours       the frame stroke and selected-wash palette
     * @param cellPaints    the wash and press light each segment takes
     * @param opacity       overall alpha, 0..1
     */
    public static void renderHorizontalRow(
            Rectangle bounds,
            List<Rectangle> segments,
            int selectedIndex,
            RadioColours colours,
            CellPaintSources cellPaints,
            float opacity) {

        // The hover wash goes under the selected one so a pointed-at segment reads as light behind the row,
        // and the press light over both so a click reads whatever the segment was already showing.
        fillSegments(segments, cellPaints.hoverWashes()::resolveWashPaintAt);
        washSelectedSegment(segments, selectedIndex, colours, opacity);
        fillSegments(segments, cellPaints.pressLights()::resolveLightPaintAt);
        HorizontalSegmentsRenderer.renderSeamDividers(segments, colours.frame(), opacity);
        strokeOuterFrame(bounds, colours, opacity);
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
     * @param colours       the frame stroke and selected-wash palette
     * @param cellPaints    the wash and press light each option takes
     * @param opacity       overall alpha, 0..1
     */
    public static void renderVerticalGrid(
            Rectangle bounds,
            int optionCount,
            int selectedIndex,
            int columnCount,
            RadioColours colours,
            CellPaintSources cellPaints,
            float opacity) {

        var segments = RadioRow.splitIntoGrid(bounds, optionCount, columnCount);

        // Same order as the row above: the pointer's wash under the selection, the press's light over both.
        fillSegments(segments, cellPaints.hoverWashes()::resolveWashPaintAt);
        washSelectedSegment(segments, selectedIndex, colours, opacity);
        fillSegments(segments, cellPaints.pressLights()::resolveLightPaintAt);

        // The tallest column, matching the grid split, so the row rules land on the same boundaries
        // the cells abut on. The grid's column and row rules are the vertical list's own (a flat seam
        // list cannot reconstruct them), but draw at the shared divider strength and thickness so they
        // read the same as a horizontal row's seams.
        var dividerPaint = new UiElementPaint(
            colours.frame(),
            opacity * HorizontalSegmentsRenderer.DIVIDER_ALPHA_MULT);
            
        var thickness = HorizontalSegmentsRenderer.DIVIDER_THICKNESS;
        var rowCount = RadioRow.computeRowsPerColumn(optionCount, columnCount);
        var columnWidth = bounds.width() / columnCount;
        var rowHeight = bounds.height() / rowCount;

        for (var column = 1; column < columnCount; column++) {
            var columnX = bounds.x() + column * columnWidth;
            UiFill.renderQuad(
                new Rectangle(columnX, bounds.y(), thickness, bounds.height()),
                dividerPaint);
        }

        for (var row = 1; row < rowCount; row++) {
            var boundaryY = bounds.y() + bounds.height() - row * rowHeight;
            UiFill.renderQuad(
                new Rectangle(bounds.x(), boundaryY - thickness, bounds.width(), thickness),
                dividerPaint);
        }

        strokeOuterFrame(bounds, colours, opacity);
    }

    // Fills every segment with whatever paint the given channel answers for it. Walked rather than asked
    // for the one segment something is happening to, because either channel can have several segments
    // part-way at once: a row settling after a sweep has the segment just left winding down while the one
    // reached is rising, and a press decays on its own clock, so the segment clicked a moment ago is still
    // falling while the pointer has moved on.
    //
    // Each answer carries its own alpha, the panel's opacity already spent in it, so a segment with nothing
    // happening to it answers a hidden paint the fill skips. That is what keeps this walk free on a row
    // nobody is touching.
    //
    // One walk for both channels because they differ only in which paint they ask for; where each is spent
    // in the draw order is the caller's, and that is the part that actually differs.
    private static void fillSegments(
            List<Rectangle> segments,
            IntFunction<UiElementPaint> paintAt) {

        for (var index = 0; index < segments.size(); index++) {
            UiFill.renderQuad(segments.get(index), paintAt.apply(index));
        }
    }

    // Washes the lit segment when the selection falls inside the laid segments; a selectedIndex
    // outside the row lights none. Shared by both flows, which differ only in how they derive the
    // segments they hand in.
    private static void washSelectedSegment(
            List<Rectangle> segments,
            int selectedIndex,
            RadioColours colours,
            float opacity) {

        if (selectedIndex >= 0 && selectedIndex < segments.size()) {
            HorizontalSegmentsRenderer.renderSelectedWash(
                segments.get(selectedIndex),
                colours.selectedWash(),
                opacity);
        }
    }

    // The radio's own outline around the whole footprint - the one piece of chrome not shared with a
    // tab strip. Both the horizontal row and the vertical grid frame their bounds this same way.
    private static void strokeOuterFrame(Rectangle bounds, RadioColours colours, float opacity) {
        var framePaint = new UiElementPaint(colours.frame(), opacity);
        UiBoxes.renderBorder(bounds, new BoxBorder(OUTLINE_THICKNESS), framePaint);
    }
}
