package kmlib.starsector.ui.map.icons;

/**
 * What the latest advance is worth saying beyond the move it ordered, read off the decision after it
 * has decided. One value rather than a read per fact, so the script wording them asks once per
 * advance and cannot describe two advances as one.
 *
 * @param mapShowingEdge           whether a map came up or went down on this advance
 * @param attemptsSinceLastClear   lifts made since the icon was last seen clear, as the advance
 *                                 left it - carried on both edges, because a count that runs up
 *                                 across opens is the thing a log has to show running up
 * @param hasStoodDown             whether the lift has been abandoned for the session
 * @param wasIconSeenClearThisOpen whether the icon was read clear at least once since the map
 *                                 opened; on a closed edge this describes the open that just ended
 * @param isDisagreementToReport   whether this advance is the one to report the map read and the
 *                                 widget read disagreeing - true once per open, on the advance the
 *                                 run of unplaceable readings reaches the threshold
 */
record ReseatAdvanceNotes(
    MapShowingEdge mapShowingEdge,
    int attemptsSinceLastClear,
    boolean hasStoodDown,
    boolean wasIconSeenClearThisOpen,
    boolean isDisagreementToReport) {
}
