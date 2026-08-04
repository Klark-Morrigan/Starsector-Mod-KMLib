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
     * Builds a source lifting only the tab under the pointer, by the given hover wash, with every other
     * tab left at rest - the whole of what a strip drawn straight from cursor position needs.
     *
     * @param hoveredIndex the index the pointer is over, or any value outside the row to lift none
     * @param hoverWash    the lift the hovered tab holds
     * @return a source answering {@code hoverWash} at that index and {@link TabWash#NONE} elsewhere
     */
    static TabWashSource createHoverWashSource(int hoveredIndex, TabWash hoverWash) {
        return tabIndex -> tabIndex == hoveredIndex ? hoverWash : TabWash.NONE;
    }

    /**
     * The lift the tab at {@code tabIndex} currently carries, composed from whatever is happening to it.
     *
     * @param tabIndex the tab's index in row order
     * @return its resolved wash; {@link TabWash#NONE} when nothing is happening to it
     */
    TabWash resolveWashAt(int tabIndex);
}
