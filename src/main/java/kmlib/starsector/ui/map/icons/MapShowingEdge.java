package kmlib.starsector.ui.map.icons;

/**
 * What one advance changed about whether a map the reseat cares about is showing: the map came up,
 * went down, or stayed as it was.
 *
 * <p>An edge rather than the level, because the level is read every advance and the decision acts on
 * it there; what a log wants is the moments it moved, each carrying the state the decision was in as
 * it did - so a count that ran up across many opens can be read back open by open.
 */
enum MapShowingEdge {

    /** No map was showing on the previous advance and one is now. */
    OPENED,

    /** A map was showing on the previous advance and none is now. */
    CLOSED,

    /** The same as the previous advance, either way. */
    NONE
}
