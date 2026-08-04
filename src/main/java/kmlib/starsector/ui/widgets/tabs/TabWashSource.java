package kmlib.starsector.ui.widgets.tabs;

/**
 * Where a tab strip's paint pass gets each tab's resolved lift from: asked per tab index, one already
 * composed {@link TabWash} per answer. The seam exists so a renderer stays a chrome pass - it reads a
 * finished wash and paints it, never learning which interactions produced it or how long any of them has
 * been running.
 *
 * <p>Asked per index rather than handed as a list running alongside the tabs, so a row and its washes
 * cannot fall out of step with each other, and a caller that resolves a lift on demand need not build one
 * entry for every tab in a row where at most a few are lifted at all.
 */
@FunctionalInterface
public interface TabWashSource {

    /**
     * Builds a source lifting no tab at all - what a strip drawn without an animator behind it reports,
     * every tab painting the settled look its state names. Named rather than left to each caller's own
     * empty lambda, so a strip with no pulses running says so in one recognisable way.
     *
     * @return a source answering {@link TabWash#NONE} for every index
     */
    static TabWashSource createRestingWashSource() {
        return tabIndex -> TabWash.NONE;
    }

    /**
     * The lift the tab at {@code tabIndex} currently carries, composed from whatever is happening to it.
     *
     * @param tabIndex the tab's index in row order
     * @return its resolved wash; {@link TabWash#NONE} when nothing is happening to it
     */
    TabWash resolveWashAt(int tabIndex);
}
