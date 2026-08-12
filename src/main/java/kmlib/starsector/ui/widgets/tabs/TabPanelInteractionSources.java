package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.controls.BodyHoverSource;

/**
 * What the pointer is doing to a whole tab panel, carried as one value: the channels its header tabs are
 * painted from, and how far onto its hovered look each cell of its body stands. They travel together for
 * the reason the two halves of a tab panel are drawn in one pass - both are resolved by one owner against
 * the one placement being drawn, off one read of the cursor, so a consumer that could hand over one without
 * the other could hand over two readings of different frames.
 *
 * <p>They stay separate fields inside it because the two halves answer the pointer differently: a tab meets
 * a shade its palette names and a body cell washes in the look's own, and only the pace is shared. What is
 * one value here is the reading, not the treatment.
 *
 * @param headerTabs    how far each header tab has travelled onto the hovered shade, and what lift it carries
 * @param bodyControls  how far onto its hovered look each cell of each body control stands
 */
public record TabPanelInteractionSources(
    TabInteractionSources headerTabs,
    BodyHoverSource bodyControls) {

    /**
     * A panel with nothing happening to it: no tab hovered or lifted and no body cell under the pointer, so
     * every part paints the settled look its own state names. What a consumer drawing a tab panel without an
     * animator behind it passes.
     */
    public static final TabPanelInteractionSources RESTING = new TabPanelInteractionSources(
        TabInteractionSources.RESTING,
        BodyHoverSource.createRestingHoverSource());
}
