package kmlib.starsector.ui.map.icons;

import kmlib.starsector.ui.map.MapIconLayering;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Decides when an entity has to leave its location and come straight back, which is what moves its
 * icon to the end of the map widget's draw order.
 *
 * <p>The widget keeps one icon per entity in an insertion-ordered map, and seeds that map afresh
 * each time a map is opened - from the location's own entities first, and the map's synthetic
 * nebulae appended after all of them. So an entity that was in its location when the map opened is
 * drawn beneath the fog, and there is no published call that says otherwise or moves it.
 *
 * <p>Insertion order is reachable all the same. An icon missing from one rendered frame is dropped
 * from the widget's map, and a re-added entity re-enters at the tail. Removing on one advance and
 * adding back on the next therefore lifts the icon past everything seeded on open - and no further,
 * the widget walking terrain-tagged icons and the rest in separate passes that no insertion order
 * crosses.
 *
 * <p>It acts on where the icon <em>is</em> rather than on an event that would have moved it. The
 * obvious trigger is the edge into "a map is showing", the map being seeded per open - but that is a
 * proxy, and a proxy is only as good as its every occurrence being observed. One missed edge leaves
 * the icon buried for the rest of the session with nothing able to notice, which is exactly what a
 * layering that comes and goes looks like from the outside. Reading the placement instead makes this
 * self-correcting: whatever re-seeded the map, and whatever was missed, the next advance sees a
 * buried icon and lifts it.
 *
 * <p>The map read stays as a scope rather than a trigger. It says which map's ordering the caller
 * cares about, so nothing is moved for a screen the caller has no interest in.
 *
 * <p>Reading the state does mean the move can be attempted against a build where it no longer works,
 * where an event-keyed rule would simply have fired once and stopped. {@link #MAX_ATTEMPTS} is what
 * bounds that: a lift that does not clear the fog is retried a few times and then abandoned for the
 * session, leaving the icon where the widget seeded it. That is the same graceful direction the
 * whole lever fails in, rather than an entity flickering out of its location for as long as the game
 * is running.
 *
 * <p>Beside the action it keeps what a log needs when the picture is wrong and the moves alone do
 * not say why: the {@link ReseatReading readings} behind the latest advances, the
 * {@link MapShowingEdge edges} of the map coming and going with the attempt count carried across
 * them, and a map that stays up with no icon placeable for the entity - the two reads this is handed
 * disagreeing about what is on screen, a state no single reading shows. All of it is read off state
 * this already keeps, which is why it is detected here and only worded by the caller.
 *
 * <p>Only the decision is here. Reaching the entity, reading its placement, removing and adding are
 * {@link MapIconReseater}'s, which leaves the state machine - the part with something to get wrong -
 * answerable without a running game.
 */
final class MapIconReseatDecision {

    // How many lifts that fail to clear the nebulae are attempted before this stands down for the
    // session. Above one, because the first read after a put-back can legitimately still see the old
    // placement - the icon is re-seeded by a render, not by the add. Low, because a lift that has
    // not taken by then is a build this no longer fits rather than a slow frame.
    static final int MAX_ATTEMPTS = 4;

    // How many advances in a row a showing map may leave the icon unplaceable, with the entity in
    // its location, before that is reported as the two reads disagreeing: one says a map this cares
    // about is up, the other finds no icon for the entity in whatever widget it reached. Well above
    // the handful an open legitimately spends there - the icon is seeded by the widget's first
    // render rather than by the open, and again by the first render after a put-back - since a
    // report per open that fired on the ordinary case would be noise nobody reads.
    static final int UNPLACEABLE_ADVANCES_BEFORE_DISAGREEMENT = 30;

    // How many of the latest readings are kept for the stand-down report. A lift is two advances -
    // out, then back - so this holds every advance of the attempts the bound allows, with room for
    // the reads between them.
    static final int RECENT_READINGS_CAPACITY = 12;

    // The readings behind the latest advances that had a map to read or a put-back to order, oldest
    // first. Kept so a stand-down can say what was seen on the way to it rather than only that it
    // happened; the ordinary frame, with no map up, records nothing.
    private final Deque<ReseatReading> recentReadings = new ArrayDeque<>();

    // Which advance this is, counted from construction. Stamped on each reading so a gap between two
    // of them reads as the frames nothing was recorded on.
    private long advanceCount;

    // Whether the previous advance took the entity out and is owed the put-back. One advance is the
    // entire window, and deliberately so: it exists only so that exactly one frame renders without
    // the icon, which is what drops it from the widget's map.
    private boolean isEntityDetached;

    // Lifts attempted since the icon was last seen clear. Reset by that sighting rather than by a
    // put-back, so what is counted is attempts that achieved nothing.
    private int attemptsSinceLastClear;

    // Set once the attempts run out, so a build this no longer fits costs a handful of moves rather
    // than two per frame forever.
    private boolean hasStoodDown;

    // Whether the previous advance had a map showing, from which this advance's edge is read.
    private boolean wasMapShowing;

    // Whether the icon has been seen clear since the map opened. Carried to the closing edge,
    // because an open that never saw it clear is one whose lifts all count as achieving nothing.
    private boolean wasIconSeenClearThisOpen;

    // Consecutive advances of this open on which a map was showing and the icon could not be placed
    // while the entity was in its location. Ends at any other reading.
    private int unplaceableRunThisOpen;

    // Whether the disagreement has been reported for this open, so it is said once per open rather
    // than on every advance past the threshold.
    private boolean hasReportedDisagreementThisOpen;

    // What the latest advance is worth saying, for the caller to read after it has decided.
    private ReseatAdvanceNotes notesOfLastAdvance =
        new ReseatAdvanceNotes(MapShowingEdge.NONE, 0, false, false, false);

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
        if (mapShowingEdge == MapShowingEdge.OPENED) {
            beginOpen();
        }
        var action = decideActionThisAdvance(isMapShowing, readIconLayering, isEntityPresent);

        // Composed after the action, so the count and the flags describe the advance as it left
        // them; on a closed edge the clear sighting still describes the open that just ended, being
        // reset only by the next opened edge.
        notesOfLastAdvance = new ReseatAdvanceNotes(
            mapShowingEdge,
            attemptsSinceLastClear,
            hasStoodDown,
            wasIconSeenClearThisOpen,
            claimDisagreementReport());

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

    /** @return whether this has abandoned the move for the session, for the caller to report once */
    boolean hasStoodDown() {
        return hasStoodDown;
    }

    /**
     * Whether the next advance will order the put-back, so the caller holding an entity out of its
     * location can tell a removal still in progress from one nothing is coming back for.
     *
     * <p>Published because the two records of the same removal can part company: this one is
     * consumed the moment the next advance asks, while the caller's - the entity and the location
     * owed it - is held until the move is made. A fault in between leaves the caller holding an
     * entity no later advance has any reason to put back, and only the caller can tell that state
     * from the ordinary one frame it spends out.
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

    // Forgets what the previous open established, so a clear sighting or a report made on one open
    // says nothing about the next.
    private void beginOpen() {
        wasIconSeenClearThisOpen = false;
        unplaceableRunThisOpen = 0;
        hasReportedDisagreementThisOpen = false;
    }

    // True on exactly one advance per open: the one the unplaceable run reaches the threshold on.
    // Claimed rather than read, so the report cannot be made twice for one run.
    private boolean claimDisagreementReport() {

        if (hasReportedDisagreementThisOpen
                || unplaceableRunThisOpen < UNPLACEABLE_ADVANCES_BEFORE_DISAGREEMENT) {
            return false;
        }
        hasReportedDisagreementThisOpen = true;
        return true;
    }

    // The rule itself, apart from the bookkeeping around it.
    private ReseatAction decideActionThisAdvance(
            boolean isMapShowing,
            Supplier<MapIconLayering> readIconLayering,
            BooleanSupplier isEntityPresent) {

        if (isEntityDetached) {
            isEntityDetached = false;
            // Put back regardless of what the map is doing now. The removal is a means, never a
            // state to leave standing: a map closed mid-sequence would otherwise strand the entity
            // out of its location until whatever put it there runs again.
            return isEntityPresent.getAsBoolean()
                ? recordReading(ReseatObservation.ENTITY_RESTORED_WHILE_OUT, ReseatAction.NONE)
                : recordReading(ReseatObservation.PUT_BACK_OWED, ReseatAction.ADD);
        }

        if (hasStoodDown || !isMapShowing) {
            return ReseatAction.NONE;
        }

        var layering = readIconLayering.get();
        if (layering == MapIconLayering.CLEAR_OF_NEBULAE) {
            // The one reading that says a lift worked. Everything else - no map, no icon yet, a
            // tree that cannot be read - leaves the count alone rather than forgiving attempts on
            // the strength of an answer nobody got.
            attemptsSinceLastClear = 0;
            wasIconSeenClearThisOpen = true;
            unplaceableRunThisOpen = 0;
            return recordReading(ReseatObservation.ICON_CLEAR, ReseatAction.NONE);
        }
        if (layering == MapIconLayering.UNREADABLE) {
            // Not acted on, but counted: an icon that stays unplaceable while the entity is there
            // and a map is up is the two reads disagreeing, which no single advance can tell from
            // the frame before the widget has seeded the icon.
            unplaceableRunThisOpen = isEntityPresent.getAsBoolean()
                ? unplaceableRunThisOpen + 1
                : 0;
            return recordReading(ReseatObservation.ICON_UNPLACEABLE, ReseatAction.NONE);
        }

        unplaceableRunThisOpen = 0;
        if (!isEntityPresent.getAsBoolean()) {
            return recordReading(ReseatObservation.ENTITY_ABSENT, ReseatAction.NONE);
        }

        attemptsSinceLastClear++;
        if (attemptsSinceLastClear > MAX_ATTEMPTS) {
            hasStoodDown = true;
            return recordReading(ReseatObservation.ICON_BURIED, ReseatAction.NONE);
        }
        isEntityDetached = true;
        return recordReading(ReseatObservation.ICON_BURIED, ReseatAction.REMOVE);
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
}
