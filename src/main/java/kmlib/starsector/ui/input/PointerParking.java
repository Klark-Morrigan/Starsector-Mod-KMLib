package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;

import kmlib.starsector.ui.coreui.CoreUiTree;

/**
 * How a panel drawn over another screen takes a pointer event for itself.
 *
 * <p>Consuming is the obvious way and it is wrong for one kind of event. A consumed event is invisible
 * to the screen underneath - that is what consuming is for - but a vanilla control decides it is no
 * longer hovered by hearing a move that is not on it, so a claimed move leaves whatever was lit when
 * the pointer crossed onto the panel lit for as long as the pointer stays there. Consuming is a claim
 * on what an event *does*; it was never meant to be a claim on what the screen may *know*.
 *
 * <p>So a move is claimed by parking the pointer instead: the event is left unconsumed and moved to a
 * position nothing on the screen occupies. Every widget beneath then reaches the same conclusion, which
 * is the true one - the pointer is not on it. A control lit a moment ago lets go, and nothing behind
 * the panel lights up in its place, because nothing is under the pointer any more. One rule covers
 * every control on the screen, including the ones a later game version adds.
 *
 * <p>Moving the real event rather than substituting one is not a preference. The engine hands input
 * listeners a copy of the frame's list and passes the original on to the screen, so replacing an entry
 * changes a list nothing will read; only a change to the event itself reaches both. The screen also
 * casts what it is given to its own event type, so a substitute of any other type would be rejected
 * where it landed.
 *
 * <p>Presses and the wheel are still consumed. Those are acts, and the panel claiming one means the
 * screen must not also act on it - which is exactly what consuming says.
 */
public final class PointerParking {

    // Far off every widget, and the position the game itself writes when an overlay takes the pointer
    // from the screen beneath it. Positive rather than negative, following that same precedent.
    private static final int PARKED_POSITION = 1000000;

    // The setters that move an event. They are on the game's own event type and not on the interface
    // mods compile against, so they are reached by name off the instance in hand - no type is named,
    // and nothing here fails to load if the game's event class is renamed.
    private static final String SET_X_METHOD = "setX";
    private static final String SET_Y_METHOD = "setY";

    private PointerParking() {
    }

    /**
     * Takes an event for the panel: a pointer move by parking the pointer, anything else by consuming.
     *
     * <p>A move that cannot be parked is consumed, which is where this began - the screen beneath keeps
     * a stale hover, and nothing is worse than it was. The order matters and is the safe one: the event
     * is only left unconsumed once it has actually been moved, so a failed reach can never hand the
     * screen a live pointer at its real position.
     *
     * @param event the event to claim, already hit-tested by the caller
     */
    public static void claimEvent(InputEventAPI event) {
        if (event.isMouseMoveEvent() && parkPointerOf(event)) {
            return;
        }
        event.consume();
    }

    // Moves an event to where no widget is, reporting whether it could.
    //
    // The reach is by method name and guarded whole, because the game's event type cannot be named from
    // here and a version that no longer offers these setters has to read as "cannot park" rather than as
    // a crash. A partial move - the first setter taking and the second not - reports failure like any
    // other, and the caller consumes, which puts the half-moved event out of the screen's sight anyway.
    private static boolean parkPointerOf(InputEventAPI event) {
        try {
            CoreUiTree.invokeWithArgs(event, SET_X_METHOD, PARKED_POSITION);
            CoreUiTree.invokeWithArgs(event, SET_Y_METHOD, PARKED_POSITION);
            return true;
        } catch (Throwable cannotParkPointer) {
            return false;
        }
    }
}
