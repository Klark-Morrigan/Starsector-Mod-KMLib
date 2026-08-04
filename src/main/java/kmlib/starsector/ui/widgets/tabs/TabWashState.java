package kmlib.starsector.ui.widgets.tabs;

/**
 * What is momentarily happening to a tab: a click landing on it, or the key it is bound to being pressed.
 * Each names a {@link TabWash} - a brief lift over whichever {@link TabLookState} shade the tab is
 * wearing, never a shade of its own - so a click on a resting tab and a click on the tab under the
 * pointer each brighten from where that tab already was.
 *
 * <p>Both are events with a life of their own, which is what separates them from the looks: a tab holds a
 * look for as long as it is selected or under the pointer, but carries one of these only while its pulse
 * runs. They are not exclusive, so a tab's rendered lift is composed from however many are still running.
 */
public enum TabWashState {

    /** A click has just landed on the tab - a lift that peaks and decays. */
    CLICKED,

    /** The key the tab is bound to has just been pressed - a lift that peaks and decays. */
    HOTKEYED
}
