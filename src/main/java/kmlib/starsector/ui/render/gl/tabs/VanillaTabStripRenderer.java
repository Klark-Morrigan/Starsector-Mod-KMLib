package kmlib.starsector.ui.render.gl.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.render.gl.controls.HorizontalSegmentsRenderer;
import kmlib.starsector.ui.widgets.tabs.TabLookSource;
import kmlib.starsector.ui.widgets.tabs.TabWashSource;
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

    private static final float BASELINE_THICKNESS = 1f;

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
     * @param looks   where each tab's settled look comes from - selection and the hover fade are already
     *                blended into it here, so this pass only paints it
     * @param washes  where each tab's resolved lift comes from - the interaction is already composed into
     *                a wash here, so this pass only paints it
     * @param style   the strip's look; its palette's chrome accent (see
     *                {@link TabPalette#createMapTabPalette}), its hotkey presentation, and its face are
     *                read here, its band height having been spent laying the tabs out
     * @param opacity overall alpha, 0..1, applied to every quad and every text colour
     */
    public static void render(
            List<VanillaTab> tabs,
            TabLookSource looks,
            TabWashSource washes,
            TabStyle style,
            float opacity) {

        var chromeAccent = style.palette().chromeAccent();
        var textFace = style.face();
        for (var index = 0; index < tabs.size(); index++) {

            var tab = tabs.get(index);

            // The look as painted: the settled shade the tab has faded to, brightened by whatever pulse is
            // still running on it. A tab with none carries a wash that moves it nowhere, so no branch
            // here decides whether a lift applies.
            var look = looks.resolveLookAt(index).computeWashedLook(washes.resolveWashAt(index));

            renderChrome(tab.bounds(), look, chromeAccent, opacity);
            TabLabelRenderer.renderCentredLabel(
                tab.bounds(),
                tab.content(),
                look,
                style.hotkey(),
                textFace,
                opacity);
        }
        // The seams between tabs, ruled once over the laid boxes through the shared segmented-row
        // primitive so this strip and a radio row divide their segments the same way. Drawn after the
        // per-tab chrome (a divider must sit over the backdrops it parts) and clear of the centred
        // labels, so the single pass reads identically to a per-tab rule.
        HorizontalSegmentsRenderer.renderSeamDividers(
            collectBounds(tabs),
            chromeAccent,
            opacity);
    }

    // The laid tab boxes, in row order, for the shared seam-divider pass.
    private static List<Rectangle> collectBounds(List<VanillaTab> tabs) {
        var bounds = new ArrayList<Rectangle>(tabs.size());
        for (var tab : tabs) {
            bounds.add(tab.bounds());
        }
        return bounds;
    }

    // The tab's solid fill as its look gives it, over a faint baseline grounding the row. The fill IS the
    // tab's surface: there is no black backdrop underneath, so an unselected tab reads as its solid colour
    // rather than that colour bled over black - and it is the whole of what marks the lit tab, which is
    // why nothing here asks which tab that is. The inter-tab seams are drawn once by the caller through
    // the shared primitive, not here.
    private static void renderChrome(
            Rectangle bounds,
            TabLook look,
            Color chromeAccent,
            float opacity) {

        UiFill.renderQuad(bounds, new UiElementPaint(look.fill(), opacity));
        UiFill.renderQuad(
            new Rectangle(
                bounds.x(),
                bounds.y(),
                bounds.width(),
                BASELINE_THICKNESS),
            new UiElementPaint(
                chromeAccent,
                opacity * HorizontalSegmentsRenderer.DIVIDER_ALPHA_MULT));
    }
}
