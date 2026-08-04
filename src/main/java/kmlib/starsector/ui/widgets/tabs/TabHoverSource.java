package kmlib.starsector.ui.widgets.tabs;

/**
 * Where a tab strip's paint pass gets each tab's hover progress from: asked per tab index, one fraction per
 * answer saying how far that tab has travelled onto the hovered shade. The seam exists so a renderer never
 * reads the cursor: a tab is hovered because whatever owns the panel's live state says so, resolved against
 * the placement the panel was actually drawn at, not because a paint pass hit-tested a mouse position it
 * would have to fetch from the engine mid-draw.
 *
 * <p>A fraction rather than a boolean because a tab eases onto the hovered shade and back off it; the
 * boolean is what the fraction was advanced toward, and it has already been spent by the time a strip is
 * painted.
 *
 * <p>Asked per index rather than handed as a list running alongside the tabs, so a row and its fractions
 * cannot fall out of step, matching how {@link TabWashSource} answers for the other channel.
 */
@FunctionalInterface
public interface TabHoverSource {

    /** The fraction a tab with no hover on it reads: fully off the hovered shade, wearing its own look. */
    float NOT_HOVERED = 0f;

    /**
     * Builds a source hovering no tab at all - what a strip drawn without an animator behind it reports,
     * every tab painting the settled look its selection names. Named rather than left to each caller's own
     * empty lambda, so a strip with no hover running says so in one recognisable way.
     *
     * @return a source answering {@link #NOT_HOVERED} for every index
     */
    static TabHoverSource createRestingHoverSource() {
        return tabIndex -> NOT_HOVERED;
    }

    /**
     * How far the tab at {@code tabIndex} has travelled onto the hovered shade.
     *
     * @param tabIndex the tab's index in row order
     * @return its hover fraction, 0 fully off the hovered shade and 1 fully on it
     */
    float resolveHoverFractionAt(int tabIndex);
}
