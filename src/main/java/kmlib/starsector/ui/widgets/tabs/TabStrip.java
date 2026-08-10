package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangles;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.segments.HorizontalSegments;
import kmlib.starsector.ui.widgets.segments.SegmentSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays out and hit-tests a horizontal row of tabs, sized and parted however the {@link SegmentSpec} it
 * is handed says: each tab snapped to its own label, or every tab taking one stated box, and the row
 * abutting or parted by a channel. All of it delegates to the shared {@link HorizontalSegments} rule, so
 * a tab row and a radio row cannot come to size a segment differently. This is the reusable half of a
 * tab bar - where each tab sits and which one a point falls in; how the tab is painted (colours,
 * selected/hover states, its label text) stays with the consumer, which owns that visual state.
 *
 * <p>Pure geometry in UI coordinates (origin bottom-left), unit-testable with a fake measurer:
 * it touches no GL surface and holds no state.
 */
public final class TabStrip {
    /** {@link #findTabIndexAt} returns this when the point falls in no tab. */
    public static final int NO_TAB = Rectangles.NONE;

    private TabStrip() {
    }

    /**
     * Lays tabs left to right from {@code originX}, each sized and parted per {@code spec}. The row hangs
     * down from {@code rowTopY} (UI y grows up), so every tab shares the same top edge and height - a
     * caller wanting a tab shorter than its band passes the shorter height and keeps the remainder below.
     * Sizes and places through {@link HorizontalSegments} so the tabs land exactly where {@link
     * #measureRowWidth} reserved for them, interior channels included.
     *
     * @param originX   the row's left edge, in UI coordinates
     * @param rowTopY   the row's top edge, in UI coordinates
     * @param tabHeight the height every tab shares
     * @param spec      the tab-sizing rule (padding, minimum, font size, sizing, box and channel)
     * @param labels    the tab labels, in row order left to right
     * @param measurer  measures each label's rendered width in the label font
     * @return one {@link LabeledTab} per label, in the same order
     */
    public static List<LabeledTab> layoutTabs(
            float originX,
            float rowTopY,
            float tabHeight,
            SegmentSpec spec,
            List<String> labels,
            LineWidthMeasurer measurer) {

        var widths = HorizontalSegments.computeSegmentWidths(
            labels,
            spec,
            measurer);

        var rects = HorizontalSegments.placeSegments(
            originX,
            rowTopY - tabHeight,
            tabHeight,
            widths,
            spec.neighbourGap());

        var tabs = new ArrayList<LabeledTab>(labels.size());

        for (var index = 0; index < labels.size(); index++) {
            tabs.add(new LabeledTab(labels.get(index), rects.get(index)));
        }
        return List.copyOf(tabs);
    }

    /**
     * The width the whole row spans: each label snapped to its own tab width and summed, so a host sizing
     * its chrome around the row reserves exactly the width {@link #layoutTabs} then lays the tabs to. Reads
     * the same {@link HorizontalSegments} snap the layout does, so the measured row and the laid-out tabs
     * cannot drift.
     *
     * @param labels   the tab labels, in row order (the same labels handed to {@link #layoutTabs})
     * @param spec     the tab-sizing rule, matching {@link #layoutTabs}
     * @param measurer measures each label's rendered width
     * @return the summed snapped width of the row, or 0 for no labels
     */
    public static float measureRowWidth(
            List<String> labels,
            SegmentSpec spec,
            LineWidthMeasurer measurer) {

        return HorizontalSegments.measureRowWidth(labels, spec, measurer);
    }

    /**
     * The index of the tab containing {@code (pointX, pointY)}, or {@link #NO_TAB} when the
     * point falls outside every tab. Tabs abut, so a point on a shared edge resolves to the
     * left tab (the first match wins).
     *
     * @param tabs   the laid-out tabs to test against
     * @param pointX the point's x, in UI coordinates
     * @param pointY the point's y, in UI coordinates
     * @return the containing tab's index, or {@link #NO_TAB}
     */
    public static int findTabIndexAt(List<LabeledTab> tabs, float pointX, float pointY) {
        return Rectangles.findIndexContaining(
            tabs,
            LabeledTab::bounds,
            pointX,
            pointY);
    }
}
