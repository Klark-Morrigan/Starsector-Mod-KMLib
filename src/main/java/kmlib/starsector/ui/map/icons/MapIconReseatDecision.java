package kmlib.starsector.ui.map.icons;

import kmlib.starsector.ui.map.MapIconLayering;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Decides when an entity has to leave its location and come back, which is what moves its icon to
 * the end of the map widget's draw order: out while the widget still shows the icon, back once a
 * frame has rendered without it, and nothing at all while no map the caller cares about is up.
 *
 * <p>It acts on where the icon is rather than on an event that would have moved it, waits for the
 * widget to drop the icon rather than for an advance to pass, and abandons a lift that keeps failing
 * for the rest of the map open. Each of those is a lesson from play; the package README says what
 * taught it and why the alternatives were wrong.
 *
 * <p>Beside the action it keeps what a log needs when the picture is wrong: the readings behind the
 * latest advances, the edges of the map coming and going, and a map that stays up with no icon
 * placeable for the entity - the two reads this is handed disagreeing about what is on screen. All
 * of it is read off state this already keeps, so it is detected here and only worded by the caller.
 *
 * <p>Only the decision is here. Reaching the entity, reading its placement, removing and adding are
 * {@link MapIconReseater}'s, which leaves the state machine - the part with something to get wrong -
 * answerable without a running game.
 */
final class MapIconReseatDecision {

    // How many lifts that fail to clear the nebulae are attempted before this stands down for the
    // rest of the map open. Above one, because the first read after a put-back can legitimately
    // still see the old placement - the icon is re-seeded by a render, not by the add. Low, because
    // a lift that has not taken by then is a build this no longer fits rather than a slow frame.
    static final int MAX_ATTEMPTS = 4;

    // How many advances the entity may spend out of its location waiting for the widget to drop its
    // icon before it is put back regardless. The drop shows on the first advance after the frame
    // that rendered without the entity, so the wait is as many advances as the campaign runs per
    // frame: one ordinarily, its speed-up multiplier under the speed-up toggle. Well above any
    // multiplier a player sets, since a put-back that came too early is a lift that did nothing;
    // low enough that a map which has stopped rendering costs the entity a blink rather than a
    // stretch.
    static final int MAX_ADVANCES_DETACHED = 16;

    // How many advances in a row a showing map may leave the icon unplaceable, with the entity in
    // its location, before that is reported as the two reads disagreeing: one says a map this cares
    // about is up, the other finds no icon for the entity in whatever widget it reached. Well above
    // the handful an open legitimately spends there - the icon is seeded by the widget's first
    // render rather than by the open, and again by the first render after a put-back - since a
    // report that fired on the ordinary case would be noise nobody reads.
    static final int UNPLACEABLE_ADVANCES_BEFORE_DISAGREEMENT = 30;

    // How many of the latest readings are kept for the stand-down report. A lift is at least two
    // advances - out, then back - so this holds every advance of the attempts the bound allows when
    // each drop shows at once, and the tail of them when the waits run longer, which is the end a
    // report wants either way.
    static final int RECENT_READINGS_CAPACITY = 12;

    // The readings behind the latest advances that had a map to read or a put-back to order, oldest
    // first. Kept so a stand-down can say what was seen on the way to it rather than only that it
    // happened; the ordinary frame, with no map up, records nothing.
    private final Deque<ReseatReading> recentReadings = new ArrayDeque<>();

    // Which advance this is, counted from construction. Stamped on each reading so a gap between two
    // of them reads as the frames nothing was recorded on.
    private long advanceCount;

    // Whether the entity is out of its location and owed the put-back. Held for as long as the
    // widget still shows the icon, bounded, since the window exists only so that one frame renders
    // without the icon - which is what drops it from the widget's map - and an advance is not a
    // frame.
    private boolean isEntityDetached;

    // Advances the entity has spent out on the current lift, against the bound above.
    private int advancesDetached;

    // Whether the previous advance had a map showing, from which this advance's edge is read.
    private boolean wasMapShowing;

    // What the current map open has accrued, replaced whole when the map goes down: what one open
    // spent says nothing about the next, which is a fresh widget with a fresh seeding.
    private MapOpenTally openTally = new MapOpenTally();

    // What the latest advance is worth saying, for the caller to read after it has decided.
    private ReseatAdvanceNotes notesOfLastAdvance = ReseatAdvanceNotes.BEFORE_ANY_ADVANCE;

    /**
     * @param isMapShowing whether a map whose icon order matters is on screen this advance
     * @param readIconLayering where the entity's icon currently sits, as a supplier because
     *        answering it costs a walk into the live widget tree - a cost worth paying on the
     *        advances that might act and not on every frame of a campaign
     * @param isEntityPresent whether the entity is currently in a location, a supplier for the same
     *        reason: answering it can cost a walk over everything the location holds
     * @return what this advance owes the location holding the entity
     */
    ReseatAction decideReseatAction(
            boolean isMapShowing,
            Supplier<MapIconLayering> readIconLayering,
            BooleanSupplier isEntityPresent) {

        advanceCount++;

        var mapShowingEdge = resolveMapShowingEdge(isMapShowing);
        var action = decideActionThisAdvance(isMapShowing, readIconLayering, isEntityPresent);

        // Composed after the action, so the tally describes the advance as it left it.
        notesOfLastAdvance = new ReseatAdvanceNotes(
            mapShowingEdge,
            openTally.attemptsSinceLastClear,
            openTally.hasStoodDown,
            openTally.wasIconSeenClear,
            openTally.claimDisagreementReport());

        // After the notes, so the closing line reports the open as it ended and the next open
        // starts from nothing owed.
        if (mapShowingEdge == MapShowingEdge.CLOSED) {
            openTally = new MapOpenTally();
        }
        wasMapShowing = isMapShowing;
        return action;
    }

    /**
     * The readings behind the latest advances that had something to read, oldest first, as one
     * bracketed list for a log line - empty brackets while nothing has been read.
     *
     * @return at most {@link #RECENT_READINGS_CAPACITY} readings, each as its advance, what was found
     *         and what was ordered
     */
    String describeRecentReadings() {
        return recentReadings.stream()
            .map(ReseatReading::describe)
            .collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * @return whether this has abandoned the move for the rest of the current map open, for the
     *         caller to report
     */
    boolean hasStoodDown() {
        return openTally.hasStoodDown;
    }

    /**
     * Whether a later advance will order the put-back, so the caller holding an entity out of its
     * location can tell a removal still in progress from one nothing is coming back for.
     *
     * <p>Published because the two records of the same removal can part company: this one is
     * consumed the moment an advance asks after it, while the caller's - the entity and the location
     * owed it - is held until the move is made. A fault in between leaves the caller holding an
     * entity no later advance has any reason to put back, and only the caller can tell that state
     * from the ordinary advances it spends out waiting for the widget to drop its icon.
     *
     * @return whether a removal is still awaiting its put-back
     */
    boolean isPutBackOwed() {
        return isEntityDetached;
    }

    /**
     * What the latest {@link #decideReseatAction} is worth saying beyond the action it returned.
     *
     * @return the notes of the latest advance; before any advance, the notes of nothing having
     *         happened
     */
    ReseatAdvanceNotes readNotesOfLastAdvance() {
        return notesOfLastAdvance;
    }

    // The rule itself, apart from the bookkeeping around it.
    private ReseatAction decideActionThisAdvance(
            boolean isMapShowing,
            Supplier<MapIconLayering> readIconLayering,
            BooleanSupplier isEntityPresent) {

        if (isEntityDetached) {
            return decidePutBack(isMapShowing, readIconLayering, isEntityPresent);
        }

        if (openTally.hasStoodDown || !isMapShowing) {
            return ReseatAction.NONE;
        }

        var layering = readIconLayering.get();
        if (layering == MapIconLayering.CLEAR_OF_NEBULAE) {
            // The one reading that says a lift worked. Everything else - no map, no icon yet, a
            // tree that cannot be read - leaves the count alone rather than forgiving attempts on
            // the strength of an answer nobody got.
            openTally.attemptsSinceLastClear = 0;
            openTally.wasIconSeenClear = true;
            openTally.unplaceableRun = 0;
            return recordReading(ReseatObservation.ICON_CLEAR, ReseatAction.NONE);
        }
        if (layering == MapIconLayering.UNREADABLE) {
            noteUnplaceableReading(isEntityPresent);
            return recordReading(ReseatObservation.ICON_UNPLACEABLE, ReseatAction.NONE);
        }

        openTally.unplaceableRun = 0;
        if (!isEntityPresent.getAsBoolean()) {
            return recordReading(ReseatObservation.ENTITY_ABSENT, ReseatAction.NONE);
        }

        openTally.attemptsSinceLastClear++;
        if (openTally.attemptsSinceLastClear > MAX_ATTEMPTS) {
            openTally.hasStoodDown = true;
            return recordReading(ReseatObservation.ICON_BURIED, ReseatAction.NONE);
        }
        isEntityDetached = true;
        advancesDetached = 0;
        return recordReading(ReseatObservation.ICON_BURIED, ReseatAction.REMOVE);
    }

    // What an advance with the entity out owes it: the put-back once the widget has dropped the
    // icon, at once if the map is down or the wait has run out, and nothing if something else has
    // already put the entity back.
    //
    // The owed state is spent by the asking and re-armed only by a deliberate wait, so a read that
    // faults here leaves nothing owed - which is what lets the caller holding the entity return it
    // rather than hold it for an order no later advance would give.
    private ReseatAction decidePutBack(
            boolean isMapShowing,
            Supplier<MapIconLayering> readIconLayering,
            BooleanSupplier isEntityPresent) {

        isEntityDetached = false;

        if (isEntityPresent.getAsBoolean()) {
            return recordReading(ReseatObservation.ENTITY_RESTORED_WHILE_OUT, ReseatAction.NONE);
        }
        // Put back regardless of the placement once no map is up. The removal is a means, never a
        // state to leave standing: a map closed mid-sequence would otherwise strand the entity out
        // of its location until whatever put it there runs again.
        if (!isMapShowing) {
            return recordReading(ReseatObservation.PUT_BACK_OWED, ReseatAction.ADD);
        }
        // An icon that can no longer be placed is one the widget has dropped, which is the frame
        // this waited for; a widget that cannot be read at all answers the same and is put back
        // for the same reason, nothing further being learnable from waiting.
        if (readIconLayering.get() == MapIconLayering.UNREADABLE) {
            return recordReading(ReseatObservation.PUT_BACK_OWED, ReseatAction.ADD);
        }

        advancesDetached++;
        if (advancesDetached >= MAX_ADVANCES_DETACHED) {
            return recordReading(ReseatObservation.PUT_BACK_OWED, ReseatAction.ADD);
        }
        isEntityDetached = true;
        return recordReading(ReseatObservation.ICON_NOT_YET_DROPPED, ReseatAction.NONE);
    }

    // Counts an advance with a map up and no icon placeable while the entity is there, which is the
    // two reads disagreeing once it has gone on long enough - and no single advance can tell it
    // from the frame before the widget has seeded the icon. An absent entity ends the count, that
    // being the ordinary state before whatever seeds it has run.
    private void noteUnplaceableReading(BooleanSupplier isEntityPresent) {
        openTally.unplaceableRun = isEntityPresent.getAsBoolean()
            ? openTally.unplaceableRun + 1
            : 0;
    }

    // Keeps the reading behind an action, dropping the oldest once the capacity is reached, and
    // hands the action back so a decision is recorded where it is made.
    private ReseatAction recordReading(ReseatObservation observation, ReseatAction action) {

        recentReadings.addLast(new ReseatReading(advanceCount, observation, action));

        if (recentReadings.size() > RECENT_READINGS_CAPACITY) {
            recentReadings.removeFirst();
        }
        return action;
    }

    // Which way the map moved between the previous advance and this one, if it moved at all.
    private MapShowingEdge resolveMapShowingEdge(boolean isMapShowing) {

        if (isMapShowing == wasMapShowing) {
            return MapShowingEdge.NONE;
        }
        return isMapShowing ? MapShowingEdge.OPENED : MapShowingEdge.CLOSED;
    }

    /**
     * What one advance owes the location holding the entity. Named for the move rather than for the
     * reason behind it, so the caller stays a switch over two engine calls and holds no second copy
     * of the rule that chose them.
     */
    enum ReseatAction {

        /** Take the entity out, so the next rendered frame drops its icon from the widget's map. */
        REMOVE,

        /** Put it back, so its icon re-enters at the tail - after everything seeded on open. */
        ADD,

        /** Leave the location alone. */
        NONE
    }

    // What one open of the map has accrued. One holder rather than five fields so what is per open
    // is a type, and forgetting an open is replacing it.
    private static final class MapOpenTally {

        // Lifts attempted since the icon was last seen clear. Reset by that sighting rather than by
        // a put-back, so what is counted is attempts that achieved nothing.
        private int attemptsSinceLastClear;

        // Set once the attempts run out, so a build the lever no longer fits costs a handful of
        // moves per map open rather than two per frame for as long as one is up.
        private boolean hasStoodDown;

        // Whether the icon has been seen clear since the map opened: an open that never saw it
        // clear is one whose lifts all count as achieving nothing.
        private boolean wasIconSeenClear;

        // Consecutive advances on which a map was showing and the icon could not be placed while
        // the entity was in its location. Ends at any other reading.
        private int unplaceableRun;

        // Whether the disagreement has been claimed for this open, so it is claimed once rather
        // than on every advance past the threshold.
        private boolean hasReportedDisagreement;

        // True on exactly one advance per open: the one the unplaceable run reaches the threshold
        // on. Claimed rather than read, so the report cannot be made twice for one run.
        private boolean claimDisagreementReport() {

            if (hasReportedDisagreement
                    || unplaceableRun < UNPLACEABLE_ADVANCES_BEFORE_DISAGREEMENT) {
                return false;
            }
            hasReportedDisagreement = true;
            return true;
        }
    }
}
