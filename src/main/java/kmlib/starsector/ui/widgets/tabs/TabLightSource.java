package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.widgets.tabs.style.TabLight;
import kmlib.starsector.ui.widgets.tabs.style.TabPalette;

/**
 * Where a tab strip's paint pass gets the light to lay over each finished tab from: asked per tab index,
 * one already resolved {@link TabLight} per answer. The third of the paint seams, beside
 * {@link TabLookSource} and {@link TabWashSource}, and the only one whose answer is drawn *after* the tab
 * rather than being part of it.
 *
 * <p>Apart from the look channel because the two reach the screen differently: a look is the tab's own
 * surface and must be resolved before anything is drawn, where light lands on whatever that surface turned
 * out to be - including whatever shows through an interior a chrome left unpainted. A chrome whose pointer
 * rule is a shade gets {@link TabLight#NONE} here and draws no extra pass at all.
 */
@FunctionalInterface
public interface TabLightSource {

    /**
     * A source answering no light for any tab - what a row with no animator behind it takes, and what
     * every chrome whose palette brightens by shade resolves to anyway.
     */
    static TabLightSource createUnlitSource() {
        return tabIndex -> TabLight.NONE;
    }

    /**
     * Binds a palette to a row's hover progress: each tab takes the light its palette's pointer rule adds,
     * scaled by however far that tab's fade has run.
     *
     * <p>Selection is not read, where the look source reads it: light is added over whichever shade a tab
     * settled on, so which one that was has already been spent by the time it lands.
     *
     * @param palette the strip's paint, which says whether its pointer rule adds light at all
     * @param hovers  how far each tab has travelled into being pointed at
     * @return a source answering the resolved light for any index in the row
     */
    static TabLightSource createHoverLitSource(TabPalette palette, TabHoverSource hovers) {
        return tabIndex -> palette.resolveLightAtHoverFraction(
            hovers.resolveHoverFractionAt(tabIndex));
    }

    /**
     * The light to lay over the finished tab at {@code tabIndex}.
     *
     * @param tabIndex the tab's index in row order
     * @return its resolved light, or {@link TabLight#NONE}
     */
    TabLight resolveLightAt(int tabIndex);
}
