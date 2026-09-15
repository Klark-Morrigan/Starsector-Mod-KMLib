package kmlib.starsector.ui.intel;

/**
 * One frame's worth of map-visor state: whether the intel screen's embedded map preview is lit and,
 * when it is, how its Starscape filter is set. One enumerated answer rather than a flag per signal
 * because the signals are not independent axes - a visor that is not showing has no filter state at
 * all - so a flag pair could hold combinations that mean nothing while this cannot.
 *
 * <p>It is also what lets a caller wanting both signals pay for one reach into the live widget tree
 * instead of two. Asked as separate reads, each walks from the core UI down to the intel panel on
 * its own, and a caller on a per-frame path takes that walk twice to describe one frame.
 *
 * <p>No counterpart to the sector map's unreadable-filter state, because there is no filter object
 * to be missing here: the visor's mode is read off the map widget itself, and a widget that cannot
 * be reached is not a lit visor in the first place.
 */
public enum MapVisorState {

    /**
     * There is no lit visor: the intel tab is not the one showing, one of the sibling sub-tabs that
     * share it is up instead, the panel is not laid out yet, or a large-description item has blanked
     * the preview. An unreadable reach fails closed to this, an intel screen that cannot be walked
     * being no evidence that a visor is on it.
     */
    NOT_SHOWING,

    /** The visor is lit and painting the stylised Starscape look. */
    SHOWING_IN_STARSCAPE_MODE,

    /** The visor is lit and painting the ordinary schematic. */
    SHOWING_WITH_STARSCAPE_OFF;

    /**
     * @return whether a lit visor is on screen at all, whatever its filter is doing - the one
     *         question every state but {@link #NOT_SHOWING} answers the same way, kept here so no
     *         caller has to spell out which states those are
     */
    public boolean isShowing() {
        return this != NOT_SHOWING;
    }
}
