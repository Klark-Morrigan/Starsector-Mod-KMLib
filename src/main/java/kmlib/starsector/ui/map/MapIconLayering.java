package kmlib.starsector.ui.map;

/**
 * Where an icon sits in the map widget's draw order, relative to the nebulae the widget appends
 * after the entities its location holds.
 *
 * <p>The widget seeds its icon map from the location's entities first and appends its synthetic
 * per-system nebulae after all of them, so an entity that was in its location when the map was
 * seeded is drawn under the fog. This is the whole of what a reader can say about that and the whole
 * of what an actor needs to hear.
 *
 * <p>It sits above the map packages rather than in one of them because two of them share it and
 * neither owns it: the probe that reads a placement and the script that acts on one are a read and a
 * write of the same subject, kept apart on purpose, and a vocabulary type living in either would
 * make one of them depend on the other.
 */
public enum MapIconLayering {

    /** Drawn after every nebula icon, so the map's fog does not paint over it. */
    CLEAR_OF_NEBULAE,

    /** Drawn before at least one nebula icon, so that fog paints over it. */
    BURIED_UNDER_NEBULAE,

    /** No map on screen, no icon for this entity yet, or the widget could not be read. */
    UNREADABLE
}
