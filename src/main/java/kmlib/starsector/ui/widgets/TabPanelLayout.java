package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.layout.CappedStripLayout;
import kmlib.starsector.ui.layout.ControlStripLayout;
import kmlib.starsector.ui.layout.Padding;

import java.util.List;

/**
 * Composes a tab panel: a {@link PanelLayout} body with a tabs-control header overlaid on the top band
 * of the same bordered box. It is that same panel plus a header, so it builds on the SAME shared blocks
 * {@link PanelLayout} does - {@link ControlStripLayout} + {@link CappedStripLayout} for the measured,
 * capped, scrolled body strip - and adds only the header band and the outer framing that spans it. The
 * one place the two layouts differ is the frame: a plain panel frames {@code border + body}, a tab panel
 * frames {@code border + header band + body} and sizes to the wider of the header and the body, so the
 * body placement it returns carries the WHOLE footprint as its box - the single frame a renderer draws.
 *
 * <p>The header is laid flush at the interior top (no body inset) through {@link
 * ControlStripLayout#layoutTabsHeader}, so a header tab measures, draws, and hit-tests through the same
 * generic control path a body {@link kmlib.starsector.ui.controls.ControlKind#TABS} control uses. UI
 * coordinates throughout (origin bottom-left, y grows up); text snapping runs through the injected
 * {@link LineWidthMeasurer}, so the layout is a pure computation. The panel hangs from the screen's
 * top-left by its paddings and caps its height to a bottom margin; that anchoring is the caller's to
 * supply through the paddings.
 */
public final class TabPanelLayout {
    private TabPanelLayout() {
    }

    /**
     * Lays the tab panel out for the given screen height, padding, border, tabs, and body controls: a
     * {@link ControlStripLayout#TAB_HEIGHT} header band flush under the top border carrying the tabs
     * control, and the body strip framed beneath it (capped to the bottom margin). The returned body's
     * {@link PanelPlacement#box()} spans the whole footprint so the one border wraps the header too. An
     * empty {@code bodyControls} leaves the bordered tab row with no body beneath.
     *
     * @param screenHeight    the UI-coordinate screen height, giving the top edge to hang from
     * @param padding         the panel's edge margins: the top-left anchor and the bottom keep-clear
     *                        margin the body caps to (the right inset is unused - a panel grows rightward)
     * @param borderWidth     the outer border thickness framing the footprint; 0 leaves no inset
     * @param tabsSpec        the tabs control (labels + per-tab shortcuts) drawn across the header band
     * @param bodyControls    the active tab's body controls, top to bottom (empty for no body)
     * @param measurer        measures each label's rendered width for text snapping
     * @param rawScrollOffset the requested scroll offset for the body's scrolling control, in pixels;
     *                        clamped to its overflow by the capped layout
     * @return the laid-out tabs header and the body placement carrying the whole-footprint box
     */
    public static TabPanelPlacement computePlacement(float screenHeight, Padding padding,
            int borderWidth, ControlSpec tabsSpec, List<ControlSpec> bodyControls,
            LineWidthMeasurer measurer, float rawScrollOffset) {
        var boxTopY = screenHeight - padding.top();
        // Content is inset by the border on every edge; the header sits flush under the top border and the
        // body hangs beneath the header band.
        var contentX = padding.left() + (float) borderWidth;
        var contentTopY = boxTopY - borderWidth;

        // Header: the tabs control laid flush at the content top, reusing the strip's tab measurement and
        // segment split so it is not bespoke tab-strip framing.
        var tabsHeader = ControlStripLayout.layoutTabsHeader(tabsSpec, contentX, contentTopY, measurer);
        var headerWidth = tabsHeader.bounds().width();
        var bodyTopY = contentTopY - ControlStripLayout.TAB_HEIGHT;

        // Body: the same measured, capped, placed strip a plain panel frames - the shared blocks - but hung
        // beneath the header band. The cap keeps the box clear of the bottom margin, the header height
        // counted against the vertical budget the same way a plain panel counts only its own border.
        var strip = ControlStripLayout.measureStrip(bodyControls, measurer);
        var flexIndex = CappedStripLayout.findScrollingIndex(bodyControls);
        var maxBodyHeight = screenHeight - padding.top() - 2f * borderWidth
                - ControlStripLayout.TAB_HEIGHT - padding.bottom();
        var bodyHeight = CappedStripLayout.capBodyHeight(strip, flexIndex, maxBodyHeight);
        var bodyWidth = strip.bodyWidth();
        var bodyRect = new Rectangle(contentX, bodyTopY - bodyHeight, bodyWidth, bodyHeight);
        var capped = CappedStripLayout.layoutCappedControls(bodyRect, bodyControls, strip.rowHeights(),
                strip.rowWidths(), flexIndex, rawScrollOffset, measurer);

        // The one bordered frame spans header + body: as wide as the wider of the two, as tall as the
        // header band plus the body plus the border on every edge. The body placement carries this
        // whole-footprint box, so the single frame a renderer draws around it wraps the header too.
        var contentWidth = Math.max(headerWidth, bodyWidth);
        var boxWidth = contentWidth + 2f * borderWidth;
        var boxHeight = ControlStripLayout.TAB_HEIGHT + bodyHeight + 2f * borderWidth;
        var box = new Rectangle(padding.left(), boxTopY - boxHeight, boxWidth, boxHeight);

        var body = new PanelPlacement(box, bodyRect, capped.controls(), capped.flexViewport(),
                capped.scrollOffset(), capped.scrollOverflow());
        return new TabPanelPlacement(tabsHeader, body);
    }
}
