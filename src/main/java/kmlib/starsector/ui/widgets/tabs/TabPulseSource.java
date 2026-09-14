package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.widgets.tabs.style.TabWash;

/**
 * Where a tab strip's lift channel gets its progress from: asked per tab index, one fraction per answer
 * saying how far through its momentary lift that tab currently is. The seam exists so a renderer holds no
 * timing - a click is an event, and how long ago it landed belongs with whatever saw it, not with a pass
 * that runs once a frame and remembers nothing between them.
 *
 * <p>A fraction rather than a finished {@link TabWash} for the same reason {@link TabHoverSource} answers
 * one: how far a lift has run is live panel state, while what that lift is made of is the strip's paint. Sent
 * as a fraction, the two meet at the pass that has both - which is what keeps the panel's animator free of
 * any colour at all.
 *
 * <p>Asked per index rather than handed as a list running alongside the tabs, so a row and its fractions
 * cannot fall out of step, and a set where at most a few tabs are lifted at all costs no entry per tab.
 */
@FunctionalInterface
public interface TabPulseSource {

    /** The fraction a tab with nothing happening to it reads: no lift at all, so it wears its own look. */
    float NOT_PULSING = 0f;

    /**
     * Builds a source lifting no tab at all - what a strip drawn without an animator behind it reports, every
     * tab painting the settled look its state names. Named rather than left to each caller's own empty
     * lambda, so a strip with no pulses running says so in one recognisable way.
     *
     * @return a source answering {@link #NOT_PULSING} for every index
     */
    static TabPulseSource createRestingPulseSource() {
        return tabIndex -> NOT_PULSING;
    }

    /**
     * How far through its momentary lift the tab at {@code tabIndex} currently is.
     *
     * @param tabIndex the tab's index in row order
     * @return its pulse fraction, 0 with nothing running on it and 1 at the lift's peak
     */
    float resolvePulseFractionAt(int tabIndex);
}
