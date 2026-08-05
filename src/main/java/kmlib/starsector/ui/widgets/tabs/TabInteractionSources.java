package kmlib.starsector.ui.widgets.tabs;

/**
 * The two channels a tab's paint is resolved from, carried together from the panel's live state to the pass
 * that draws a strip: how far each tab has travelled onto the hovered shade, and how far through its
 * momentary lift each one is. They travel as one value because a renderer needs both to paint a tab and
 * neither on its own, so a consumer wires the pair up once rather than threading two seams through every
 * layer between the panel's live state and its chrome.
 *
 * <p>They stay separate fields inside it for the reason they are separate seams: a look settles first and a
 * lift is layered over whatever it yields, so the pair is an ordered composition rather than one blended
 * value.
 *
 * <p>Both are bare fractions rather than resolved paint, so whatever holds a panel's animations holds no
 * colour at all: the palette turns each fraction into a look or a wash at the pass that has both.
 *
 * @param hoverSource how far each tab has travelled onto the hovered shade
 * @param pulseSource how far through its momentary lift each tab is
 */
public record TabInteractionSources(
    TabHoverSource hoverSource,
    TabPulseSource pulseSource) {

    /**
     * A strip with nothing happening to it: no tab hovered and none lifted, so every tab paints the settled
     * look its selection names. What a consumer drawing a tab row without an animator behind it passes, and
     * what a body control - which has no tabs at all - is drawn with.
     */
    public static final TabInteractionSources RESTING = new TabInteractionSources(
        TabHoverSource.createRestingHoverSource(),
        TabPulseSource.createRestingPulseSource());
}
