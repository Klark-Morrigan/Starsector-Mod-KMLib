package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.controls.BodyInteractionSources;

/**
 * What the pointer is doing to a whole tab panel, carried as one value: the channels its header tabs are
 * painted from, and the channels its body cells are. They travel together for
 * the reason the two halves of a tab panel are drawn in one pass - both are resolved by one owner against
 * the one placement being drawn, off one read of the cursor, so a consumer that could hand over one without
 * the other could hand over two readings of different frames.
 *
 * <p>They stay separate fields inside it because the two halves answer the pointer differently: a tab meets
 * a shade its palette names and a body cell washes in the look's own, and only the pace is shared. What is
 * one value here is the reading, not the treatment.
 *
 * @param headerTabs      how far each header tab has travelled onto the hovered shade, and what lift it carries
 * @param bandButtonHover how far the panel's own band button has travelled onto that same shade - one
 *                        fraction rather than a channel pair, the button being a single cell that answers
 *                        the pointer and nothing else
 * @param bodyControls    how far onto its hovered look, and how far through its press lift, each cell of each
 *                        body control stands
 */
public record TabPanelInteractionSources(
    TabInteractionSources headerTabs,
    float bandButtonHover,
    BodyInteractionSources bodyControls) {

    /**
     * A panel with nothing happening to it: no tab hovered or lifted, and no body cell under the pointer or
     * carrying a press, so every part paints the settled look its own state names. What a consumer drawing a
     * tab panel without an animator behind it passes.
     */
    public static final TabPanelInteractionSources RESTING = new TabPanelInteractionSources(
        TabInteractionSources.RESTING,
        TabHoverSource.NOT_HOVERED,
        BodyInteractionSources.RESTING);

    /**
     * The band button's fraction as the channel pair a strip is drawn from: it answers the pointer and
     * carries no lift, so the pulse channel rests. Composed here rather than at the pass that draws it, so
     * "a one-cell control has one channel" is stated where the fraction is carried rather than restated by
     * every consumer painting one.
     *
     * @return the band button's paint channels
     */
    public TabInteractionSources resolveBandButtonSources() {
        return new TabInteractionSources(
            tabIndex -> bandButtonHover,
            TabPulseSource.createRestingPulseSource());
    }
}
