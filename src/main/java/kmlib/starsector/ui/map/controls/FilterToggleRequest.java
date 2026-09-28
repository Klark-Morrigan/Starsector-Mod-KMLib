package kmlib.starsector.ui.map.controls;

import kmlib.starsector.compatibility.GameReachReporter;

/**
 * What a caller asks of a toggle on a map's filter row: the words it wears, what a click runs, and
 * where a row no longer the game's usual shape is filed.
 *
 * <p>One value because all three are the caller's and none is the row's. They travel from the
 * public append, through the build, to the handle that goes on to announce a key in the words, and
 * held apart they were three more arguments on every step of that way, beside the row and the size
 * that are measured rather than asked for.
 *
 * @param label     the words on the button, which are also how its own label is found again
 * @param onToggled what to run when it is clicked, called after the button has flipped its own state
 * @param reporter  where a refusal that says the row changed is filed, for the mod standing the toggle
 */
record FilterToggleRequest(
    String label,
    Runnable onToggled,
    GameReachReporter reporter) {
}
