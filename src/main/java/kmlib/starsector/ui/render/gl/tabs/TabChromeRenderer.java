package kmlib.starsector.ui.render.gl.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.widgets.tabs.RaisedButtonTabStrip;
import kmlib.starsector.ui.widgets.tabs.TabPaintSources;
import kmlib.starsector.ui.widgets.tabs.VanillaTab;
import kmlib.starsector.ui.widgets.tabs.style.TabChrome;
import kmlib.starsector.ui.widgets.tabs.style.TabLight;
import kmlib.starsector.ui.widgets.tabs.style.TabPaint;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;

import java.util.List;

/**
 * What every tab chrome can do: paint a laid-out row of tabs at the paint it is handed. The contract
 * exists so the chromes are interchangeable by type rather than by coincidence - two paint passes that
 * happen to take the same arguments today could drift apart on the next change, and a caller choosing
 * between them would then be choosing between two different jobs.
 *
 * <p>It is also the seam a third chrome plugs into. Adding one is a renderer satisfying this and a case in
 * {@link #resolveRendererFor}; nothing that draws a tab row learns of it.
 *
 * <p>The chromes stay static paint passes rather than becoming instances, as every other renderer here is:
 * a chrome holds no state, so a method reference is the whole of what an implementation needs to be.
 */
@FunctionalInterface
public interface TabChromeRenderer {

    /**
     * The chrome a {@link TabChrome} names. The one place the choice is bound to a painter, and it sits
     * here rather than on the enum because the enum is substrate-independent - it travels inside the
     * {@link TabStyle} the layout measures a band against, so it cannot reach a GL renderer. A host names
     * the look it wants without naming the pass that draws it, and this turns the one into the other.
     *
     * @param chrome which surface the row's paint is laid onto
     * @return the pass that paints that chrome
     */
    static TabChromeRenderer resolveRendererFor(TabChrome chrome) {
        return switch (chrome) {
            case STRIP -> VanillaTabStripRenderer::render;
            case RAISED_BUTTON -> RaisedButtonTabStripRenderer::render;
        };
    }

    /**
     * How much of the screen a chrome needs to paint the row laid into {@code band}: the band itself for a
     * chrome that stays within it, and the band grown by whatever a chrome reaches outside it.
     *
     * <p>A caller clipping the row to its band would otherwise crop that reach silently, which is a border
     * that simply is not there rather than an error anything reports. It sits beside the painter lookup
     * because it is the same question asked of the same enum - what a chrome draws, and where - and a
     * chrome added to one without the other would be cropped or over-clipped by exactly the amount it
     * differs by.
     *
     * @param chrome which surface the row's paint is laid onto
     * @param band   the row's laid-out band, in UI coordinates
     * @return the region that chrome may paint into
     */
    static Rectangle computePaintedRegionFor(TabChrome chrome, Rectangle band) {
        return switch (chrome) {
            case STRIP -> band;
            case RAISED_BUTTON -> RaisedButtonTabStrip.computeRowFootprint(band);
        };
    }

    /**
     * Walks the row in order, handing each tab to {@code painter} in the look it is painted at: the settled
     * shade its look source reports, lifted by whatever pulse its wash source reports for it. A tab with no
     * pulse carries a wash that moves it nowhere, so nothing here decides whether a lift applies.
     *
     * <p>Held here rather than at each chrome because the channels resolve in one order only - the look
     * settles, the lift is layered over what it yields, and the light is added over whatever was drawn. A
     * chrome writing that walk itself is a chance to compose them the other way round, which gives a click
     * over a half-faded hover a colour neither channel named. A chrome supplies what it draws per tab and
     * nothing about how the row is read.
     *
     * @param tabs    the laid-out tabs, in row order
     * @param sources where each tab's look, lift, and light come from
     * @param painter what to draw for one tab, given its place in the row and the paint it takes
     */
    static void paintEachTab(
            List<VanillaTab> tabs,
            TabPaintSources sources,
            TabPainter painter) {

        for (var index = 0; index < tabs.size(); index++) {
            painter.paintTab(
                index,
                tabs.get(index),
                new TabPaint(
                    sources.looks().resolveLookAt(index)
                        .computeWashedLook(sources.washes().resolveWashAt(index)),
                    sources.lights().resolveLightAt(index)));
        }
    }

    /**
     * Paints the row. Every channel arrives resolved - a tab has already faded onto its hovered shade,
     * already carries whatever lift is running on it, and is handed the light to finish it with - so an
     * implementation paints what it is given and computes no timing.
     *
     * @param tabs    the laid-out tabs, in row order
     * @param sources where each tab's look, lift, and light come from
     * @param style   the row's look; its band height having been spent laying the tabs out, a chrome reads
     *                only the paint it carries
     * @param opacity overall alpha, 0..1, applied to every quad and every text colour
     */
    void renderTabs(
        List<VanillaTab> tabs,
        TabPaintSources sources,
        TabStyle style,
        float opacity);

    /**
     * What a chrome draws for one tab. The whole of what differs between the chromes: the row's walk and
     * the composition of its channels belong to the contract, so an implementation of this is handed
     * finished paint and decides only what surface to lay it on.
     */
    @FunctionalInterface
    interface TabPainter {

        /**
         * Draws one tab.
         *
         * <p>The position comes with the tab because a chrome's geometry can turn on it - a row of raised
         * buttons parts each button from the one on its left, which is a thing only the leading tab is
         * exempt from. It is handed down from the walk rather than counted by the chrome, so a chrome
         * cannot end up numbering the row differently from the source that reported its looks.
         *
         * @param rowIndex the tab's position in the row, counting from the left
         * @param tab      the laid-out tab - its box, and the label and bound key it shows
         * @param paint    what to paint it with: the surface it settled on, and the light to lay over the
         *                 finished result - {@link TabLight#NONE} where the row's pointer rule brightens by
         *                 shade instead
         */
        void paintTab(int rowIndex, VanillaTab tab, TabPaint paint);
    }
}
