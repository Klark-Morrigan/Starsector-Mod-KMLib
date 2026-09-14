package kmlib.starsector.ui.sound;

/**
 * What kind of thing the pointer reached, for a look to answer how loud arriving on it is. The one axis a
 * {@link UiSoundScheme} varies an arrival along: which role sounds is the panel's, and how loudly is this.
 *
 * <p>A kind rather than a widget, because how loud an arrival should be is a fact about what the player is
 * doing and not about which class drew the thing. Passing over a panel's chrome, reaching a control with one
 * answer to give, and running down a list of them are three different acts, and the third puts many more
 * things under one sweep of the pointer than the other two do. Two widgets the player reads the same way
 * want the same volume however differently they are painted.
 *
 * <p>That is also what keeps the set closed. A widget added later picks the kind it reads as rather than
 * naming a volume of its own, so the knobs a host offers stay these three however many widgets grow around
 * them - and a panel keeps one balance instead of a per-widget scatter nobody can tune against each other.
 */
public enum PointerArrivalTarget {

    /**
     * The panel's own furniture - a header tab, the collapse handle. Few of them, reached deliberately, and
     * each one a move between whole views rather than a step within one.
     */
    PANEL_CHROME,

    /**
     * A control standing alone in the strip with one answer to give - a checkbox, a toggle. Reached one at a
     * time, so an arrival on one is the player having aimed at it.
     */
    SINGLE_OPTION_CONTROL,

    /**
     * One of many alike - a segment of a radio row, a row of a table or list. The kind a sweep crosses
     * several of without meaning to reach any, which is why it is the kind a look quietens.
     */
    LISTED_ITEM
}
