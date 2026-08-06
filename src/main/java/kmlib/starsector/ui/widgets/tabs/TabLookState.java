package kmlib.starsector.ui.widgets.tabs;

/**
 * Which of a tab strip's three settled looks a tab is wearing. Each names an absolute {@link TabLook} - a
 * fill and a label colour - rather than a lift over another, which is what parts these from
 * {@link TabWashState}, whose members only ever brighten whichever look a tab already wears.
 *
 * <p>Hovering is a look and not a lift because a hovered tab lands on one shade whatever it was showing
 * before: the resting and the selected tab meet at the same colour under the pointer. A fraction applied
 * to each tab's own fill could not do that - two different starting colours lifted by one fraction stay
 * two different colours - so the hovered shade is named outright.
 *
 * <p>Hovering therefore wins over selection while the pointer is on a tab: a tab under the pointer wears
 * the hovered shade whether or not it is the one being shown. Nothing else marks the shown tab - a strip
 * states selection by fill alone - so how far the hovered shade stands from the selected one is what
 * decides whether a pointed-at tab can be told from the shown one. Derived too close to it, the two read
 * alike while the pointer rests on the row.
 */
public enum TabLookState {

    /** A tab whose content the panel is not showing, with nothing on it - its resting fill and label. */
    UNSELECTED,

    /** The one tab whose content the panel is showing - its lit fill and label. */
    SELECTED,

    /** The tab under the pointer, selected or not - the one shade both of the others meet at. */
    HOVERED
}
