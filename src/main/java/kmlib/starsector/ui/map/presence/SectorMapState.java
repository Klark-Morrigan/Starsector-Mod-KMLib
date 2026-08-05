package kmlib.starsector.ui.map.presence;

/**
 * One frame's worth of sector-map state: whether the sector map is the view on screen and, when it
 * is, how its Starscape filter is set. One enumerated answer rather than a flag per signal because
 * the signals are not independent axes - a map that is not showing has no filter state at all - so a
 * flag pair could hold combinations that mean nothing while this cannot.
 */
public enum SectorMapState {

    /**
     * The sector map is not the view on screen: another core tab, a star-system sub-view, or UI data
     * the reading port cannot make sense of at all - which fails closed to the same answer, since an
     * unreadable sub-view is no evidence the sector map is up.
     */
    NOT_SHOWING,

    /**
     * The sector map is showing but its filter object is missing, so the mode is unknown. Neither
     * Starscape on nor off, which is why it is a state and not folded into either: both mode reads
     * decline it instead of one answering true by default.
     *
     * <p>The game field-initialises that object and exposes no setter, so this arises only from save
     * data written before the field existed being rehydrated without it. The guard earns its place
     * regardless, since the alternative is dereferencing that null on the frame an old save first
     * opens its map.
     */
    SHOWING_WITH_UNREADABLE_FILTER,

    /** The sector map is showing and painting the stylised starfield. */
    SHOWING_IN_STARSCAPE_MODE,

    /** The sector map is showing and painting the ordinary schematic. */
    SHOWING_WITH_STARSCAPE_OFF;

    /**
     * @return whether the sector map is on screen at all, whatever its filter is doing - the one
     *         question every state but {@link #NOT_SHOWING} answers the same way, kept here so no
     *         caller has to spell out which states those are
     */
    public boolean isShowing() {
        return this != NOT_SHOWING;
    }
}
