package kmlib.starsector.ui.layout;

import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;

import java.util.List;

/**
 * Composes a tab panel: a tabs-control header over the shared body composition, wrapped in one bordered
 * box. It REUSES {@link PanelLayout}'s framing - the same {@link PanelLayout#computeContentOrigin} anchor
 * and {@link PanelLayout#framePlacement} that frame a plain panel - passing header-inclusive content
 * dimensions (the wider of header and body, and the header band plus the body) so the one border wraps
 * the header too. Because {@code framePlacement} takes content dimensions, not a bordered box, reusing it
 * frames one border, not two. It also reuses {@link CappedStripLayout#layoutBodyStrip} for the body and
 * adds {@link ControlStripLayout#layoutTabsHeader} for the flush header, so the only thing unique here is
 * where the header sits.
 *
 * <p>The header is laid flush at the interior top (no body inset) through {@link
 * ControlStripLayout#layoutTabsHeader}, so a header tab measures, draws, and hit-tests through the same
 * generic control path a body {@link ControlSpec.Tabs} control uses. UI
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
            int borderWidth, ControlSpec.Tabs tabsSpec, List<ControlSpec> bodyControls,
            LineWidthMeasurer measurer, float rawScrollOffset) {
        // The box hangs from the screen's top-left, same anchor a plain panel uses; the header sits flush
        // under the top border and the body hangs beneath the header band.
        var origin = PanelLayout.computeContentOrigin(screenHeight, padding, borderWidth);

        // Header: the tabs control laid flush at the content top, reusing the strip's tab measurement and
        // segment split so it is not bespoke tab-strip framing.
        var tabsHeader = ControlStripLayout.layoutTabsHeader(tabsSpec, origin.contentX(),
                origin.contentTopY(), measurer);
        var bodyTopY = origin.contentTopY() - ControlStripLayout.TAB_HEIGHT;

        // Body: the same shared composition a plain panel frames, hung beneath the header band and capped
        // so the box (header included) clears the bottom margin - the header height counted against the
        // vertical budget the same way a plain panel counts only its own border.
        var maxBodyHeight = screenHeight - padding.top() - 2f * borderWidth
                - ControlStripLayout.TAB_HEIGHT - padding.bottom();
        var bodyStrip = CappedStripLayout.layoutBodyStrip(origin.contentX(), bodyTopY, maxBodyHeight,
                bodyControls, measurer, rawScrollOffset);
        var body = bodyStrip.bounds();

        // Reuse the plain panel's framing, but with header-inclusive content: the box spans the wider of
        // the header and the body, and the header band plus the body tall, so its whole-footprint box wraps
        // the header too. The body placement carries that box; the tab panel pairs it with the header.
        var bodyPlacement = PanelLayout.framePlacement(padding.left(), origin.boxTopY(), borderWidth,
                Math.max(tabsHeader.bounds().width(), body.width()),
                ControlStripLayout.TAB_HEIGHT + body.height(), bodyStrip);
        return new TabPanelPlacement(tabsHeader, bodyPlacement);
    }
}
