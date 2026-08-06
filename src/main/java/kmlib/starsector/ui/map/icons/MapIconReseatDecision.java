package kmlib.starsector.ui.map.icons;

import kmlib.starsector.ui.map.probes.MapIconLayeringProbe.Layering;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

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
            Supplier<Layering> readIconLayering,
            BooleanSupplier isEntityPresent) {

        if (isEntityDetached) {
            isEntityDetached = false;
            // Put back regardless of what the map is doing now. The removal is a means, never a
            // state to leave standing: a map closed mid-sequence would otherwise strand the entity
            // out of its location until whatever put it there runs again.
            return isEntityPresent.getAsBoolean()
                ? ReseatAction.NONE
                : ReseatAction.ADD;
        }

        if (hasStoodDown || !isMapShowing) {
            return ReseatAction.NONE;
        }

        var layering = readIconLayering.get();
        if (layering == Layering.CLEAR_OF_NEBULAE) {
            // The one reading that says a lift worked. Everything else - no map, no icon yet, a
            // tree that cannot be read - leaves the count alone rather than forgiving attempts on
            // the strength of an answer nobody got.
            attemptsSinceLastClear = 0;
            return ReseatAction.NONE;
        }
        if (layering != Layering.BURIED_UNDER_NEBULAE || !isEntityPresent.getAsBoolean()) {
            return ReseatAction.NONE;
        }

        attemptsSinceLastClear++;
        if (attemptsSinceLastClear > MAX_ATTEMPTS) {
            hasStoodDown = true;
            return ReseatAction.NONE;
        }
        isEntityDetached = true;
        return ReseatAction.REMOVE;
    }

    /** @return whether this has abandoned the move for the session, for the caller to report once */
    boolean hasStoodDown() {
        return hasStoodDown;
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
