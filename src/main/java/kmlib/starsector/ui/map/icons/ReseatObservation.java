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

    /** The previous advance took the entity out, and it is not in its location. */
    PUT_BACK_OWED,

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
