package kmlib.starsector.ui.controls;

/**
 * The two channels one control's cells are painted from, carried together from the panel's live state to
 * the pass that draws that control: how far each cell has travelled onto its hovered look, and how far
 * through its press lift each one stands. They travel as one value because a widget needs both to paint a
 * cell and neither on its own, so a consumer wires the pair up once rather than threading two seams
 * through every layer between the panel's live state and its chrome.
 *
 * <p>They stay separate fields inside it for the reason they are separate seams: the hover settles first
 * and the press is layered over whatever it yields, so the pair is an ordered composition rather than one
 * blended value.
 *
 * <p>Both are bare fractions rather than resolved paint, so whatever holds a panel's animations holds no
 * colour at all: the look turns each fraction into a wash or a light at the pass that has both.
 */
public record ControlInteractionSources(
    ControlHoverSource hovers,
    ControlPressSource presses) {

    /**
     * A control with nothing happening to it: no cell hovered and none pressed, so every cell paints the
     * settled look its own state names. What a consumer drawing a control without an animator behind it
     * passes, and what a tabs row - whose cells answer the pointer through their own palette instead - is
     * drawn with.
     */
    public static final ControlInteractionSources RESTING = new ControlInteractionSources(
        ControlHoverSource.createRestingHoverSource(),
        ControlPressSource.createRestingPressSource());
}
