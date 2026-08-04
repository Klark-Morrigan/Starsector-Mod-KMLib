package kmlib.starsector.ui.widgets.tabs;

/**
 * The two channels a tab's paint is resolved from, carried together to the pass that draws a strip: how far
 * each tab has travelled onto the hovered shade, and what momentary lift each is carrying. They travel as one
 * value because a renderer needs both to paint a tab and neither on its own, so a consumer wires the pair up
 * once rather than threading two seams through every layer between the panel's live state and its chrome.
 *
 * <p>They stay separate fields inside it for the reason they are separate seams: a look settles first and a
 * lift is layered over whatever it yields, so the pair is an ordered composition rather than one blended
 * value.
 *
 * @param hoverSource how far each tab has travelled onto the hovered shade
 * @param washSource  the momentary lift each tab is carrying
 */
public record TabInteractionSources(
    TabHoverSource hoverSource,
    TabWashSource washSource) {

    /**
     * A strip with nothing happening to it: no tab hovered and none lifted, so every tab paints the settled
     * look its selection names. What a consumer drawing a tab row without an animator behind it passes, and
     * what a body control - which has no tabs at all - is drawn with.
     */
    public static final TabInteractionSources RESTING = new TabInteractionSources(
        TabHoverSource.createRestingHoverSource(),
        TabWashSource.createRestingWashSource());
}
