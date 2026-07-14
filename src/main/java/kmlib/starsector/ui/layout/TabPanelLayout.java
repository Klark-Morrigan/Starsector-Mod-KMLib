package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.TabPanelPlacement;

import java.util.List;

/**
 * Composes a tab panel: a tabs-control header over the shared body composition, wrapped in one bordered
 * box. It is a SIBLING of {@link PanelLayout}, not a reuse of it - both frame {@link
 * CappedStripLayout#layoutBodyStrip} (the measured, capped, scrolled control strip) and each does its own
 * outer framing; this one adds {@link ControlStripLayout#layoutTabsHeader} for the flush header. A plain
 * panel frames {@code border + body}; a tab panel reserves a {@link ControlStripLayout#TAB_HEIGHT} header
 * band and frames {@code border + header + body}, sized to the wider of the header and the body, so the
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
        var bodyTopY = contentTopY - ControlStripLayout.TAB_HEIGHT;

        // Body: the same shared composition a plain panel frames, hung beneath the header band and capped
        // so the box (header included) clears the bottom margin - the header height counted against the
        // vertical budget the same way a plain panel counts only its own border.
        var maxBodyHeight = screenHeight - padding.top() - 2f * borderWidth
                - ControlStripLayout.TAB_HEIGHT - padding.bottom();
        var bodyStrip = CappedStripLayout.layoutBodyStrip(contentX, bodyTopY, maxBodyHeight,
                bodyControls, measurer, rawScrollOffset);
        var body = bodyStrip.bounds();

        // The one bordered frame spans header + body: as wide as the wider of the two, as tall as the
        // header band plus the body plus the border on every edge. The body placement carries this
        // whole-footprint box, so the single frame a renderer draws around it wraps the header too.
        var contentWidth = Math.max(tabsHeader.bounds().width(), body.width());
        var boxWidth = contentWidth + 2f * borderWidth;
        var boxHeight = ControlStripLayout.TAB_HEIGHT + body.height() + 2f * borderWidth;
        var box = new Rectangle(padding.left(), boxTopY - boxHeight, boxWidth, boxHeight);

        var capped = bodyStrip.placement();
        var bodyPlacement = new PanelPlacement(box, body, capped.controls(), capped.flexViewport(),
                capped.scrollOffset(), capped.scrollOverflow());
        return new TabPanelPlacement(tabsHeader, bodyPlacement);
    }
}
