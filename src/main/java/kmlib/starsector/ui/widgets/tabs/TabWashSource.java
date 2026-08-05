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
     * Binds a palette's click lift to a row's live pulses: each tab carries that lift scaled to however far
     * through its click cycle it currently is, so a tab with none carries a wash that moves it nowhere. Held
     * here rather than at each caller so the map strip and the raised-button chrome lift a tab identically.
     *
     * <p>This is where the panel's animator and the strip's paint meet, and the split either side of it is
     * the point: the animator counts frames and knows no colour, the palette names a colour and knows no
     * time, and only this binding needs both.
     *
     * @param palette the strip's paint - the peak depth a click lifts a tab by
     * @param pulses  how far through its lift each tab currently is
     * @return a source answering the resolved wash for any index in the row
     */
    static TabWashSource createClickPulsedWashSource(TabPalette palette, TabPulseSource pulses) {

        // Resolved once for the row rather than per tab: the peak is the palette's, so asking it per index
        // would answer the same wash as many times as the row is long.
        var clickPeak = palette.resolveWash(TabWashState.CLICKED);

        return tabIndex -> clickPeak.computeScaledWash(pulses.resolvePulseFractionAt(tabIndex));
    }

    /**
     * The lift the tab at {@code tabIndex} currently carries, composed from whatever is happening to it.
     *
     * @param tabIndex the tab's index in row order
     * @return its resolved wash, of no strength when nothing is happening to it - which target a resting
     *         wash names is immaterial, since a strength of zero blends nowhere
     */
    TabWash resolveWashAt(int tabIndex);
}
