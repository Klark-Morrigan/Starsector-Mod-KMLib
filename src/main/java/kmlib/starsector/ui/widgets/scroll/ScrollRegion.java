package kmlib.starsector.ui.widgets.scroll;

import kmlib.math.geometry.Rectangle;

/**
 * A scrollable region: a {@code viewport} of visible content that overruns it by {@code overflow},
 * scrolled by {@code offset}, sitting inside a {@code container} whose right gutter holds the scrollbar.
 * It is exactly what a {@link Scrollbar} needs to size and place itself - the container edge for the
 * track's gutter, the viewport for its extent, and the offset/overflow for the thumb - independent of
 * what is scrolling. So a scrolling list inside a panel and any other scrolled content describe
 * themselves the same way, and the scrollbar stays scoped to a scrollable region rather than to any one
 * host.
 *
 * <p>UI coordinates throughout (origin bottom-left, y grows up).
 *
 * @param container the framing rect whose right gutter holds the scrollbar track
 * @param viewport  the visible region the content scrolls within
 * @param offset    how far the content is scrolled, 0..overflow
 * @param overflow  how far the content overruns the viewport, 0 when it fits
 */
public record ScrollRegion(
    Rectangle container,
    Rectangle viewport,
    float offset,
    float overflow) {
    
    /**
     * @return the full height of the scrolled content: the visible viewport plus how far it overruns
     */
    public float computeContentHeight() {
        return viewport.height() + overflow;
    }
}
