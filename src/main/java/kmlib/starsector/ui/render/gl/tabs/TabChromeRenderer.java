package kmlib.starsector.ui.render.gl.tabs;

import kmlib.starsector.ui.widgets.tabs.TabLookSource;
import kmlib.starsector.ui.widgets.tabs.TabWashSource;
import kmlib.starsector.ui.widgets.tabs.VanillaTab;
import kmlib.starsector.ui.widgets.tabs.style.TabChrome;
import kmlib.starsector.ui.widgets.tabs.style.TabLook;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;

import java.util.List;

/**
 * What every tab chrome can do: paint a laid-out row of tabs at the looks and lifts it is handed. The
 * contract exists so the chromes are interchangeable by type rather than by coincidence - two paint passes
 * that happen to take the same five arguments today could drift apart on the next change, and a caller
 * choosing between them would then be choosing between two different jobs.
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
     * Walks the row in order, handing each tab to {@code painter} in the look it is painted at: the settled
     * shade its look source reports, lifted by whatever pulse its wash source reports for it. A tab with no
     * pulse carries a wash that moves it nowhere, so nothing here decides whether a lift applies.
     *
     * <p>Held here rather than at each chrome because the two channels resolve in one order only - the look
     * settles, and the lift is layered over what it yields. A chrome writing that walk itself is a chance to
     * compose them the other way round, which gives a click over a half-faded hover a colour neither channel
     * named. A chrome supplies what it draws per tab and nothing about how the row is read.
     *
     * @param tabs    the laid-out tabs, in row order
     * @param looks   where each tab's settled look comes from
     * @param washes  where each tab's resolved lift comes from
     * @param painter what to draw for one tab, given the look it is painted at
     */
    static void paintEachTab(
            List<VanillaTab> tabs,
            TabLookSource looks,
            TabWashSource washes,
            TabPainter painter) {

        for (var index = 0; index < tabs.size(); index++) {
            painter.paintTab(
                tabs.get(index),
                looks.resolveLookAt(index).computeWashedLook(washes.resolveWashAt(index)));
        }
    }

    /**
     * Paints the row. Both channels arrive resolved - a tab has already faded onto its hovered shade and
     * already carries whatever lift is running on it - so an implementation paints what it is handed and
     * computes no timing.
     *
     * @param tabs    the laid-out tabs, in row order
     * @param looks   where each tab's settled look comes from
     * @param washes  where each tab's resolved lift comes from
     * @param style   the row's look; its band height having been spent laying the tabs out, a chrome reads
     *                only the paint it carries
     * @param opacity overall alpha, 0..1, applied to every quad and every text colour
     */
    void renderTabs(
        List<VanillaTab> tabs,
        TabLookSource looks,
        TabWashSource washes,
        TabStyle style,
        float opacity);

    /**
     * What a chrome draws for one tab. The whole of what differs between the chromes: the row's walk and
     * the composition of its two channels belong to the contract, so an implementation of this is handed a
     * finished look and decides only what surface to lay it on.
     */
    @FunctionalInterface
    interface TabPainter {

        /**
         * Draws one tab.
         *
         * @param tab  the laid-out tab - its box, and the label and bound key it shows
         * @param look the look it is painted at, its lift included
         */
        void paintTab(VanillaTab tab, TabLook look);
    }
}
