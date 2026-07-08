package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.LineWidthMeasurer;

import java.awt.Color;
import java.util.List;

/**
 * The full vanilla map-panel assembly as one reusable widget: an outer {@link BorderedBox} frame
 * wrapping a {@link VanillaTabStrip} header over a body region the active tab fills. It layers over
 * {@link VanillaTabStrip} exactly as that layers over {@link TabStrip} - the strip owns the header
 * geometry and chrome, this composes the frame around it, sizes the whole footprint, and frames a
 * body rectangle beneath. A mod that wants an on-map tabbed panel in the sector-map style assembles
 * it here rather than re-deriving the frame-plus-header composition.
 *
 * <p>The panel frames the body but never fills it: the consumer measures how large its body must be
 * (as a {@link TabPanelBodySize}), the panel places a body rectangle of that size, and the consumer
 * draws its own controls into {@link TabPanelPlacement#body()}. So the panel stays agnostic to what
 * a body holds - it draws the same frame and header whether the body carries a faction toggle, a
 * view selector, or nothing.
 *
 * <p>Geometry ({@link #layout}) is pure and unit-tested through the font-agnostic
 * {@link kmlib.starsector.ui.font.LineWidthMeasurer}; {@link #render} is the raw-GL passthrough
 * (over {@link BorderedBox} and {@link VanillaTabStrip}), exercised in-engine like the other draw
 * helpers. It draws only its own chrome - the frame and the header - and, like those helpers, does
 * not push or restore GL state; the consumer wraps its whole paint (this frame, this header, and
 * its own body) in one attribute save so the body draw shares the same protected block.
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

    /**
     * Paints the panel's chrome - the outer frame and the tab header - scaling every draw by one
     * {@code opacity} so the frame, its border, the tab washes, dividers, underline, and labels
     * fade as a unit. The body is not drawn here; the consumer paints it into
     * {@link TabPanelPlacement#body()}. No GL state is pushed or restored, so the consumer wraps
     * this and its own body draw in one attribute save.
     *
     * @param placement     the laid-out panel
     * @param borderWidth   the frame's border thickness; 0 draws no border
     * @param fill          the panel's backdrop colour
     * @param border        the frame's edge colour
     * @param selectedIndex the active tab's index, or a value outside the row to light none
     * @param hoveredIndex  the hovered tab's index (see {@link #findTabIndexAt}), or outside the row
     * @param colors        the tab palette (see {@link VanillaTabColors#mapTabs})
     * @param fontBasename  the {@code graphics/fonts} basename the tab labels draw in
     * @param fontSize      the tab label font size
     * @param opacity       overall alpha, 0..1, applied to every quad and text colour
     */
    public static void render(TabPanelPlacement placement, float borderWidth, Color fill,
            Color border, int selectedIndex, int hoveredIndex, VanillaTabColors colors,
            String fontBasename, double fontSize, float opacity) {
        BorderedBox.render(placement.box(), borderWidth, fill, border, opacity);
        VanillaTabStrip.render(placement.tabs(), selectedIndex, hoveredIndex, colors, fontBasename,
                fontSize, opacity);
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
