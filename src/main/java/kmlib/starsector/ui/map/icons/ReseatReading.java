package kmlib.starsector.ui.map.icons;

import kmlib.starsector.ui.map.icons.MapIconReseatDecision.ReseatAction;

/**
 * One advance the decision had something to read: which advance it was, what it found, and what it
 * ordered. The unit a stand-down report is made of.
 *
 * @param advanceOrdinal which advance since the decision was made, so a gap between two readings
 *                       reads as the frames nothing was recorded on - a closed map between two
 *                       opens, or a stand-down that ended the recording
 * @param observation    the fact the action followed from
 * @param action         what the decision ordered on that advance
 */
record ReseatReading(
    long advanceOrdinal,
    ReseatObservation observation,
    ReseatAction action) {

    /** @return the reading as one short term for a log line: the advance, the fact, the action */
    String describe() {
        return "#" + advanceOrdinal + " " + observation + " -> " + action;
    }
}
