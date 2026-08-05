package kmlib.starsector.ui.map.icons;

import java.util.function.BooleanSupplier;

/**
 * Decides when an entity has to leave its location and come straight back, which is what moves its
 * icon to the end of the map widget's draw order.
 *
 * <p>The widget keeps one icon per entity in an insertion-ordered map, and seeds that map afresh
 * each time a map is opened - from the location's own entities first, and the starfield's synthetic
 * nebulae appended after all of them. So an entity that was in its location when the map opened is
 * drawn beneath the fog, and there is no published call that says otherwise or moves it.
 *
 * <p>Insertion order is reachable all the same. An icon missing from one rendered frame is dropped
 * from the widget's map, and a re-added entity re-enters at the tail. Removing on one advance and
 * adding back on the next therefore lifts the icon past everything seeded on open - and no further,
 * the widget walking terrain-tagged icons and the rest in separate passes that no insertion order
 * crosses.
 *
 * <p>The map is seeded per open, so the move is owed per open rather than per frame a map is up.
 * That is what makes this a latch over two readings rather than a rule about the current one: the
 * edge into "a map is showing" is the trigger, and the advance after it is the put-back.
 *
 * <p>Only the decision is here. Reaching the entity, removing and adding are
 * {@link MapIconReseater}'s, which leaves the state machine - the part with something to get wrong -
 * answerable without a running game.
 *
 * <p>The lever is an artefact of how the widget seeds itself, not something the engine promises. A
 * game build that seeds differently simply leaves the icon where it is drawn today, with nothing
 * else disturbed; {@code MapIconOrderTrace} is what says so from a running game.
 */
final class MapIconReseatDecision {

    // The previous advance's reading, which is what turns a steady "a map is showing" into the edge
    // this arms on.
    private boolean wasMapShowing;

    // Whether the previous advance took the entity out and is owed the put-back. One advance is the
    // entire window, and deliberately so: it exists only so that exactly one frame renders without
    // the icon, which is what drops it from the widget's map.
    private boolean isEntityDetached;

    /**
     * @param isMapShowing whether a map whose icon order matters is on screen this advance
     * @param isEntityPresent whether the entity is currently in a location, as a supplier because
     *        answering it can cost a walk over everything the location holds - a cost worth paying
     *        on the few advances that act and not on every frame of a campaign
     * @return what this advance owes the location holding the entity
     */
    ReseatAction decideReseatAction(boolean isMapShowing, BooleanSupplier isEntityPresent) {

        var hasMapJustOpened = isMapShowing && !wasMapShowing;
        wasMapShowing = isMapShowing;

        if (isEntityDetached) {
            isEntityDetached = false;
            // Put back regardless of what the map is doing now. The removal is a means, never a
            // state to leave standing: a map closed mid-sequence would otherwise strand the entity
            // out of its location until the next load, and a save written then would not hold it.
            return isEntityPresent.getAsBoolean()
                ? ReseatAction.NONE
                : ReseatAction.ADD;
        }

        // Nothing to move unless a map has just been opened and there is an entity to move. Absent
        // is the ordinary case before whatever puts the entity there has run, and adding one from
        // here would be seeding an entity this has never been told how to build.
        if (!hasMapJustOpened || !isEntityPresent.getAsBoolean()) {
            return ReseatAction.NONE;
        }

        isEntityDetached = true;
        return ReseatAction.REMOVE;
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
