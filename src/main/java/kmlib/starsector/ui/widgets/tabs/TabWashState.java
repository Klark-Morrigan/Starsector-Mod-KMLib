package kmlib.starsector.ui.widgets.tabs;

/**
 * What is momentarily lifting a tab above the look it has settled on. It names a {@link TabWash} - a brief
 * lift over whichever {@link TabLookState} shade the tab is wearing, never a shade of its own - so a click on
 * a resting tab and a click on the tab under the pointer each brighten from where that tab already was.
 *
 * <p>A lift is an event with a life of its own, which is what separates it from the looks: a tab holds a look
 * for as long as it is selected or under the pointer, but carries one of these only while its pulse runs.
 *
 * <p>One member, because a bound key's press is not one of them: its blink travels onto the hovered shade
 * rather than past it, so it rides the look channel and composes with a held hover by the greater of the two
 * - which is what makes a key pressed for the tab already under the pointer show nothing. An enum of one
 * still earns its place: it is what a wash lookup is asked for, so a second lift added later is a member
 * here rather than a second lookup beside it.
 */
public enum TabWashState {

    /** A click has just landed on the tab - a lift that peaks and decays. */
    CLICKED
}
