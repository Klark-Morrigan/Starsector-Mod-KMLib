package kmlib.starsector.ui.widgets.tabs.style;

/**
 * Which settled look a tab is wearing - the shade it rests at with no pointer on it. Each names an
 * absolute {@link TabLook} - a fill and a label colour - rather than a lift over another, which is what
 * parts these from {@link TabWashState}, whose members only ever brighten whichever look a tab already
 * wears.
 *
 * <p>Being pointed at is not among them, and cannot be: it is a departure from a settled look rather than
 * a look of its own, and what the departure is differs by chrome - a strip's tabs travel to one shared
 * shade, a row of buttons brightens each from where it stands. {@link TabHover} is where that lives, and
 * leaving it out here is what stops a caller asking a palette for "the hovered look" of a chrome that has
 * no single one.
 */
public enum TabLookState {

    /** A tab whose content the panel is not showing, with nothing on it - its resting fill and label. */
    UNSELECTED,

    /** The one tab whose content the panel is showing - its lit fill and label. */
    SELECTED
}
