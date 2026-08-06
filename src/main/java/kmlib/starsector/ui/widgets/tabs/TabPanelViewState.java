package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.ranges.Ranges;

/**
 * How far a tab panel is scrolled and folded: the two live values that move while the panel sits on screen,
 * as opposed to the dimensions and controls that describe what it is. Bundled so they travel as one value
 * rather than as two bare floats side by side, where a caller could silently swap a scroll offset for a
 * collapse fraction and get a panel that lays out without complaint.
 *
 * <p>The collapse fraction arrives clamped, so an overshooting animation value cannot invert the geometry
 * downstream. The scroll offset stays raw: how far it may travel depends on the overflow, which is not
 * known until the strip has been measured, so it is the layout that settles it.
 *
 * @param rawScrollOffset  the requested scroll offset for the body's scrolling control, in pixels, before
 *                         it is clamped to the overflow the laid-out strip turns out to have
 * @param collapseFraction how far the body is folded horizontally: 0 lays it out at full width, 1 docks it
 *                         to the border-only rail; clamped to the unit range on construction
 */
public record TabPanelViewState(float rawScrollOffset, float collapseFraction) {
    /**
     * A fully expanded panel scrolled to its top - the state a panel opens in, and the value to pass where
     * a caller tracks neither.
     */
    public static final TabPanelViewState RESTING = new TabPanelViewState(0f, 0f);

    // The fraction a panel resting fully expanded reports; above it the fold is under way.
    private static final float NO_COLLAPSE_FRACTION = 0f;

    /**
     * Clamps the collapse fraction to the unit range, so a fraction past either end behaves as the nearest
     * end rather than inverting the fold.
     */
    public TabPanelViewState {
        collapseFraction = Ranges.clampToUnit(collapseFraction);
    }

    /**
     * Whether the fold is under way at all - the panel part-way to its docked rail, or all the way there.
     * Asked rather than compared, so what counts as folding is stated once beside the fraction it is read
     * from rather than at each place that acts on it.
     *
     * @return whether the body is anywhere but fully expanded
     */
    public boolean isFolding() {
        return collapseFraction > NO_COLLAPSE_FRACTION;
    }
}
