package kmlib.starsector.ui.render.gl.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.render.gl.controls.HorizontalSegmentsRenderer;
import kmlib.starsector.ui.widgets.tabs.TabPaintSources;
import kmlib.starsector.ui.widgets.tabs.VanillaTab;
import kmlib.starsector.ui.widgets.tabs.VanillaTabStrip;
import kmlib.starsector.ui.widgets.tabs.style.TabLook;
import kmlib.starsector.ui.widgets.tabs.style.TabPalette;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Raw-GL paint for a {@link VanillaTabStrip}: the sector-map Sector/System tab look - each tab a solid
 * fill (dark at rest, bright when active, travelling toward one shared shade under the pointer) lifted by
 * whatever pulse its wash source reports, with hairline dividers on the seams between them. The tab
 * geometry lives on the substrate-independent widget; this draws it. The seams are the chrome every
 * horizontal segmented control shares, so they come from {@link HorizontalSegmentsRenderer} (as a radio
 * row's do); the per-state fill, the wash, and the baseline are this strip's own, and what each tab says
 * is {@link TabLabelRenderer}'s, shared with the raised-button chrome that spells a tab out the same way.
 *
 * <p>The lit tab is marked by its fill and nothing else - no bar caps it. So the one mark of selection is
 * a shade, and the shade a tab wears under the pointer is what a reader has to keep clear of it: the two
 * meeting would leave a hovered tab and the shown tab looking alike.
 *
 * <p>Opacity scales every quad and every text colour by one value, so the whole strip fades as a unit.
 */
public final class VanillaTabStripRenderer {

    private VanillaTabStripRenderer() {
    }

    /**
     * Paints the whole strip: each tab's look, the dividers, and each tab's text with its key lit. Each
     * tab's look arrives resolved - already blended however far onto the hovered shade its fade has run -
     * and is lifted by the pulse its wash source reports for it.
     *
     * <p>Which tab is selected is not asked for. It reaches this pass inside the looks, the lit tab
     * arriving on its own shade, and a strip that marks selection by fill alone needs nothing else: a
     * second reading of the same fact would be a second chance to disagree with it.
     *
     * @param tabs    the laid-out tabs, in row order
     * @param sources where each tab's look, lift, and light come from - every one of them already resolved
     *                here, so this pass only paints them. A strip answers the pointer with a shade rather
     *                than a glow, so the light it is handed is always none and nothing is drawn for it
     * @param style   the strip's look; its palette's chrome accent (see
     *                {@link TabPalette#createMapTabPalette}), its hotkey presentation, and its face are
     *                read here, its band height having been spent laying the tabs out
     * @param opacity overall alpha, 0..1, applied to every quad and every text colour
     */
    public static void render(
            List<VanillaTab> tabs,
            TabPaintSources sources,
            TabStyle style,
            float opacity) {

        var chromeAccent = style.palette().chromeAccent();

        // The line the row stands on, laid before the tabs so a tab's own surface is never drawn over by
        // it: it belongs to the row rather than to any tab, and an empty row has none to stand on.
        if (!tabs.isEmpty()) {
            renderBaseline(tabs, chromeAccent, opacity);
        }

        // Two of the things the walk hands over are a raised button's business, not a strip's: every tab
        // here is drawn the same whatever its place in the row - the seams between them are ruled in one
        // pass below - and this chrome's pointer rule is a shade the look already carries, so the paint's
        // light is never anything to lay down.
        TabChromeRenderer.paintEachTab(tabs, sources, (rowIndex, tab, paint) -> {
            renderChrome(tab.bounds(), paint.look(), opacity);
            TabLabelRenderer.renderCentredLabel(
                tab.bounds(),
                tab.content(),
                paint.look(),
                style,
                opacity);
        });
        // The seams between tabs, ruled once over the laid boxes through the shared segmented-row
        // primitive so this strip and a radio row divide their segments the same way. Drawn after the
        // per-tab chrome (a divider must sit over the backdrops it parts) and clear of the centred
        // labels, so the single pass reads identically to a per-tab rule.
        //
        // Only where the tabs actually abut. A row parted by a channel has no seam to rule: the rule
        // would land in the empty gap, marking a join between two tabs that do not touch - and the
        // engine's own parted row draws nothing there either.
        if (!style.tabBox().isParted()) {
            HorizontalSegmentsRenderer.renderSeamDividers(
                collectBounds(tabs),
                chromeAccent,
                opacity);
        }
    }

    // The laid tab boxes, in row order, for the shared seam-divider pass.
    private static List<Rectangle> collectBounds(List<VanillaTab> tabs) {
        var bounds = new ArrayList<Rectangle>(tabs.size());
        for (var tab : tabs) {
            bounds.add(tab.bounds());
        }
        return bounds;
    }

    // The tab's solid fill as its look gives it. The fill IS the tab's surface: there is no black backdrop
    // underneath, so an unselected tab reads as its solid colour rather than that colour bled over black -
    // and it is the whole of what marks the lit tab, which is why nothing here asks which tab that is. The
    // inter-tab seams and the baseline under the row are drawn once by the caller, not here.
    private static void renderChrome(Rectangle bounds, TabLook look, float opacity) {
        UiFill.renderQuad(bounds, new UiElementPaint(look.fill(), opacity));
    }

    // The line the row stands on: one quad spanning every tab, laid in the row directly beneath them.
    //
    // Outside the tabs rather than inside, which is two fixes in one. A line drawn within a tab is
    // translucent over that tab's own fill, so it took a different shade as the tab faded, pulsed or lit -
    // a rule that moves with the surface it is ruling. And a line drawn per tab is cut wherever the tabs
    // are parted, which is exactly where a row that anchors its tabs needs it whole.
    //
    // Spanning the laid tabs rather than any band the row was given, so it reaches from the leading tab's
    // left edge to the trailing tab's right and no further: the row's own width is what it grounds.
    private static void renderBaseline(List<VanillaTab> tabs, Color chromeAccent, float opacity) {

        var leading = tabs.get(0).bounds();
        var trailing = tabs.get(tabs.size() - 1).bounds();

        UiFill.renderQuad(
            new Rectangle(
                leading.x(),
                leading.y() - VanillaTabStrip.BASELINE_THICKNESS,
                trailing.x() + trailing.width() - leading.x(),
                VanillaTabStrip.BASELINE_THICKNESS),
            new UiElementPaint(
                chromeAccent,
                opacity * HorizontalSegmentsRenderer.DIVIDER_ALPHA_MULT));
    }
}
