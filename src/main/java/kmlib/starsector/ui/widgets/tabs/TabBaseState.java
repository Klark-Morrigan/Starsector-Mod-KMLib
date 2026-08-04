package kmlib.starsector.ui.widgets.tabs;

/**
 * What a tab <em>is</em> while nothing is happening to it: the resting condition it holds between one
 * interaction and the next. A base state names an absolute look ({@link TabBaseLook}) - a fill and a
 * label colour - because there is nothing underneath it to lift from; that is what parts it from
 * {@link TabWashState}, whose members only ever wash whichever base a tab is already wearing.
 *
 * <p>Kept apart from the momentary states rather than gathered with them into one set of every look a
 * tab can wear, so a caller asking for a resting look cannot name a momentary one, and the two kinds of
 * style cannot be handed to each other's consumer.
 */
public enum TabBaseState {

    /** A tab whose content the panel is not showing - its resting fill and label. */
    UNSELECTED,

    /** The one tab whose content the panel is showing - its lit fill and label. */
    SELECTED
}
