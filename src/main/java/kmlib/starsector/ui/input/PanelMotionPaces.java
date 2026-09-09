package kmlib.starsector.ui.input;

import kmlib.animation.TraverseDurations;

/**
 * The pace a panel's motions run at when whatever holds one offers no control over it.
 *
 * <p>One pace for every motion a panel makes <em>in answer to input</em>, not for any one of them: a fade
 * onto a look and a lift over one are the same gesture answered at the same speed, and two pairs written
 * beside each other is how one panel ends up with two rhythms. A motion added to the toolkit takes this
 * rather than measuring its own.
 *
 * <p>Apart from any of the motions it paces, and that is the whole reason it is here rather than on one of
 * them: a constant living on the hover would read as the hover's own, leaving the next motion free to pick
 * a second number that nothing said was wrong.
 *
 * <p>Nothing about how long a motion of the game's own takes belongs here. This is the toolkit's pace for
 * what it draws itself; something matching a pace the game sets states that pace where it matches it, so
 * the two cannot drift into each other.
 */
public final class PanelMotionPaces {

    // How long a motion takes to let go, in seconds. Short enough that the element reads as answering the
    // input rather than catching up with it - a pointer crossing a row must not leave a trail of half-lit
    // elements behind it - and long enough that the travel is still visible as a travel rather than a
    // switch. Dialled by eye against the vanilla chrome a panel sits among.
    private static final float RELEASE_DURATION_SECONDS = 0.3f;

    // How much quicker the way out is than the way back. A rise answers something the viewer just did, so
    // it has to arrive under the gesture that asked for it; a fall answers nothing and reads better
    // unhurried. Equal paces make the whole motion feel like the slower half, which is the one nobody
    // asked for.
    private static final float RISE_SPEED_MULTIPLE = 2f;

    /**
     * Out to the far end at twice the speed it comes back, which is the pace every motion on a panel takes
     * unless its holder states another.
     */
    public static final TraverseDurations DEFAULT_DURATIONS = new TraverseDurations(
        RELEASE_DURATION_SECONDS / RISE_SPEED_MULTIPLE,
        RELEASE_DURATION_SECONDS);

    private PanelMotionPaces() {
    }
}
