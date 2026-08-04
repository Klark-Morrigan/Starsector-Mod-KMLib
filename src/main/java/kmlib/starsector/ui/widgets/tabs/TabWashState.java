package kmlib.starsector.ui.widgets.tabs;

/**
 * What is momentarily happening to a tab: the pointer resting over it, a click landing on it, or the key
 * it is bound to being pressed. Each names a {@link TabWash} - a lift layered over whichever
 * {@link TabBaseState} look the tab already wears, never a look of its own - so a hovered selected tab
 * and a hovered unselected tab each read as their own base brightened rather than as one shared
 * "hovered" colour.
 *
 * <p>These are not exclusive and do not name a tab's current condition: a click lands on the tab the
 * pointer is already over, and a bound key can fire for it at the same moment. A tab's rendered lift is
 * therefore composed from however many are active, not read off a single state.
 */
public enum TabWashState {

    /** The pointer is resting over the tab - a lift the tab holds for as long as the pointer stays. */
    HOVERED,

    /** A click has just landed on the tab - a lift that peaks and decays. */
    CLICKED,

    /** The key the tab is bound to has just been pressed - a lift that peaks and decays. */
    HOTKEYED
}
