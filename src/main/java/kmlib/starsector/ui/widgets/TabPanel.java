package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.LineWidthMeasurer;

import java.util.List;

/**
 * The geometry of the full vanilla map-panel assembly: an outer {@link BorderedBox} frame wrapping a
 * {@link VanillaTabStrip} header over a body region the active tab fills. Substrate-independent - it
 * lays the panel out and hit-tests it, rendering nothing - so a GL or a UI-API renderer can paint the
 * panel against it. It composes the frame around the strip, sizes the whole footprint, and frames a
 * body rectangle beneath; the raw-GL paint lives in
 * {@link kmlib.starsector.ui.render.gl.TabPanelRenderer}.
 *
 * <p>The panel frames the body but never fills it: the consumer measures how large its body must be
 * (as a {@link TabPanelBodySize}), {@link #layout} places a body rectangle of that size, and the
 * consumer draws its own controls into {@link TabPanelPlacement#body()}. So the panel stays agnostic
 * to what a body holds - it frames the same header whether the body carries a toggle, a selector, or
 * nothing. Geometry is pure and unit-tested through the font-agnostic {@link LineWidthMeasurer}.
 */
public final class TabPanel {
    private TabPanel() {
    }

    /**
     * Lays the panel out from the screen's top-left: the box's top edge sits {@code paddingTop}
     * below the screen top and its left edge {@code paddingLeft} in from the left, the border insets
     * the content on every edge, the tab row caps the content snapped to its labels, and the body
     * hangs beneath at {@code bodySize}. The box grows right and down from that corner, so screen
     * width plays no part. An empty {@code bodySize} leaves the tab row with a zero-size body.
     *
     * @param screenHeight   the UI-coordinate screen height, giving the top edge to hang from
     * @param paddingTop     pixels from the screen top to the box's top edge
     * @param paddingLeft    pixels from the screen left to the box's left edge
     * @param borderWidth    the outer border thickness framing the footprint; 0 leaves no inset
     * @param tabHeight      the height every tab shares
     * @param tabTextPadding slack added to each measured tab label so text does not touch the edges
     * @param minTabWidth    the narrowest a tab may be
     * @param tabFontSize    the size the tab labels are measured (and later drawn) at
     * @param tabContents    the tabs' labels and optional shortcuts, in row order left to right
     * @param bodySize       the active tab's body footprint, or {@link TabPanelBodySize#NONE}
     * @param measurer       measures each tab label's rendered width for text snapping
     * @return the box, tabs, and body rectangle, all in UI coordinates
     */
    public static TabPanelPlacement layout(float screenHeight, float paddingTop, float paddingLeft,
            float borderWidth, float tabHeight, float tabTextPadding, float minTabWidth,
            double tabFontSize, List<VanillaTabContent> tabContents, TabPanelBodySize bodySize,
            LineWidthMeasurer measurer) {
        var boxTopY = screenHeight - paddingTop;
        // Content is inset by the border on every edge, so the tab row and body clear the stroke.
        var contentX = paddingLeft + borderWidth;
        var contentTopY = boxTopY - borderWidth;

        var tabs = VanillaTabStrip.layoutTabs(contentX, contentTopY, tabHeight, tabTextPadding,
                minTabWidth, tabFontSize, tabContents, measurer);
        var tabRowWidth = measureTabRowWidth(tabs, contentX);
        var tabRowBottomY = contentTopY - tabHeight;

        // The body hangs from the tab row's bottom edge at the size the consumer measured; an empty
        // size collapses it to a zero rectangle at that edge, so a bodyless tab reserves nothing.
        var body = bodySize.isEmpty()
                ? new Rectangle(contentX, tabRowBottomY, 0f, 0f)
                : new Rectangle(contentX, tabRowBottomY - bodySize.height(), bodySize.width(),
                        bodySize.height());

        // The box is as wide as the wider of the tab row and the body, and as tall as the tab row
        // plus the body, all wrapped by the border on every edge.
        var contentWidth = Math.max(tabRowWidth, body.width());
        var contentHeight = tabHeight + body.height();
        var boxWidth = contentWidth + 2f * borderWidth;
        var boxHeight = contentHeight + 2f * borderWidth;
        var box = new Rectangle(paddingLeft, boxTopY - boxHeight, boxWidth, boxHeight);
        return new TabPanelPlacement(box, tabs, body);
    }

    /**
     * Whether {@code (pointX, pointY)} lies anywhere on the panel's drawn footprint - the whole box,
     * border included - so a consumer can swallow every pointer event over the panel and stop the
     * surface behind it acting on the same click.
     *
     * @param placement the laid-out panel
     * @param pointX    the point's x, in UI coordinates
     * @param pointY    the point's y, in UI coordinates
     * @return whether the point is over the panel
     */
    public static boolean containsPoint(TabPanelPlacement placement, float pointX, float pointY) {
        return placement.box().containsPoint(pointX, pointY);
    }

    /**
     * The index of the tab containing {@code (pointX, pointY)}, or {@link TabStrip#NO_TAB} when the
     * point falls in no tab, delegating to the header strip's own hit-test.
     *
     * @param placement the laid-out panel
     * @param pointX    the point's x, in UI coordinates
     * @param pointY    the point's y, in UI coordinates
     * @return the containing tab's index, or {@link TabStrip#NO_TAB}
     */
    public static int findTabIndexAt(TabPanelPlacement placement, float pointX, float pointY) {
        return VanillaTabStrip.findTabIndexAt(placement.tabs(), pointX, pointY);
    }

    // The tab row spans from the content's left edge to the right edge of the last tab; an empty row
    // is zero wide, so a panel with no tabs sizes its box to the body alone.
    private static float measureTabRowWidth(List<VanillaTab> tabs, float contentX) {
        if (tabs.isEmpty()) {
            return 0f;
        }
        var last = tabs.get(tabs.size() - 1).bounds();
        return last.x() + last.width() - contentX;
    }
}
