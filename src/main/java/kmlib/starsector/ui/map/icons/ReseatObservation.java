package kmlib.starsector.ui.map.icons;

/**
 * What the decision found on an advance it had something to read - the one fact its action followed
 * from, kept beside that action so a run of them says why each move was or was not made.
 *
 * <p>Coarser than the readings themselves on purpose: the placement and the presence are asked
 * lazily and in an order that stops at the first answer settling the advance, so a reading holds
 * only the answer that settled it rather than a full set nobody asked for.
 */
enum ReseatObservation {

    /**
     * The entity is out of its location and no icon can be placed for it: the widget has dropped the
     * icon, or cannot be read at all. Either way the wait is over and the entity goes back.
     */
    ICON_DROPPED,

    /** The entity is out of its location and no map is up, so it goes back without a placement read. */
    MAP_DOWN,

    /**
     * The entity is out of its location and the widget still shows its icon, but the wait has run to its
     * bound, so it goes back regardless. A lift that ends here is one no rendered frame took.
     */
    WAIT_EXPIRED,

    /** The entity is out of its location and the widget still shows its icon: no frame rendered without it yet. */
    ICON_NOT_YET_DROPPED,

    /** The previous advance took the entity out, and something else has already put it back. */
    ENTITY_RESTORED_WHILE_OUT,

    /** The icon is drawn after every nebula icon. */
    ICON_CLEAR,

    /** The icon is drawn before at least one nebula icon. */
    ICON_BURIED,

    /** The icon could not be placed: no widget read, or no icon for the entity in the one read. */
    ICON_UNPLACEABLE,

    /** The icon is buried, but the entity is not in any location to be moved. */
    ENTITY_ABSENT
}
