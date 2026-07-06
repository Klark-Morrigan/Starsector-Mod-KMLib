package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.math.geometry.Rectangles;
import kmlib.starsector.ui.font.LineWidthMeasurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays out and hit-tests a horizontal row of text-snapped tabs. Each tab is only as wide as
 * its own label needs (measured through the font-agnostic {@link LineWidthMeasurer}) plus a
 * fixed padding, floored at a minimum, so a short label does not carry a wide empty box and a
 * long one is never clipped. This is the reusable half of a tab bar - where each tab sits and
 * which one a point falls in; how the tab is painted (colours, selected/hover states, its
 * label text) stays with the consumer, which owns that visual state.
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
     * Lays tabs left to right from {@code originX}, each snapped to its measured label width.
     * The row hangs down from {@code rowTopY} (UI y grows up), so every tab shares the same top
     * edge and height.
     *
     * @param originX        the row's left edge, in UI coordinates
     * @param rowTopY        the row's top edge, in UI coordinates
     * @param tabHeight      the height every tab shares
     * @param textPadding    slack added to each measured label so text does not touch the edges
     * @param minTabWidth    the narrowest a tab may be, so a tiny label still gives a clickable
     *                       box
     * @param labelFontSize  the size the labels are measured (and later drawn) at
     * @param labels         the tab labels, in row order left to right
     * @param measurer       measures each label's rendered width in the label font
     * @return one {@link LabeledTab} per label, in the same order
     */
    public static List<LabeledTab> layoutTabs(float originX, float rowTopY, float tabHeight,
            float textPadding, float minTabWidth, double labelFontSize, List<String> labels,
            LineWidthMeasurer measurer) {
        var tabs = new ArrayList<LabeledTab>(labels.size());
        var rowBottomY = rowTopY - tabHeight;
        var cursorX = originX;
        for (var label : labels) {
            var measured = (float) measurer.measureLineWidth(label, labelFontSize);
            var width = Math.max(minTabWidth, measured + textPadding);
            tabs.add(new LabeledTab(label, new Rectangle(cursorX, rowBottomY, width, tabHeight)));
            cursorX += width;
        }
        return List.copyOf(tabs);
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
        return Rectangles.findIndexContaining(tabs, LabeledTab::bounds, pointX, pointY);
    }
}
