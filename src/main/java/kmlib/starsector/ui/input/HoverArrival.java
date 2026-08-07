package kmlib.starsector.ui.input;

/**
 * Whether the pointer has just reached one element, told apart from the frames it spends resting there
 * afterwards. The discrete sibling of {@link HoverFade}: the fade says how far onto its hovered look an
 * element stands and holds at the top for as long as the pointer stays, and this says the one frame it
 * got there.
 *
 * <p>The two are apart rather than one object because they answer different kinds of question and are
 * read by different passes - a fraction is a position the paint pass reads every frame, an arrival is a
 * moment something answers once. Anything that has to happen on reaching an element rather than while on
 * it reads this: a sound, a tooltip's delay starting, a probe firing. Reading it off the fade instead
 * would turn a tick into a tone, since a fraction parked at 1 says "on it", never "just got here".
 *
 * <p>Leaving is not an arrival and reports nothing - nothing was reached. What the moment is <em>for</em>
 * is the consumer's own, exactly as with the fade: nothing here plays, draws, or delays anything.
 *
 * <p>Transient per-session UI state. A reset forgets that the element was ever reached, so an element the
 * pointer is already parked on when its panel comes back announces itself afresh - it is an arrival to
 * the player, the element not having been there a moment ago, however still the pointer was.
 */
public final class HoverArrival {

    // Whether the element has already been reported as reached. Latched rather than derived, an arrival
    // being a change and not a state: what the pointer is on this frame cannot say on its own whether the
    // player has just got there.
    private boolean hasArrived;

    /**
     * Reports whether the pointer reached the element this frame, and records where it is for the next
     * one. Called once per frame with what the hit-test found, so the latch tracks the pointer whether or
     * not the consumer acts on the answer.
     *
     * @param isHovered whether the pointer is on the element this frame
     * @return true only on the frame the pointer arrives; false while it rests there and while it is away
     */
    public boolean detectArrival(boolean isHovered) {

        var isArriving = isHovered && !hasArrived;
        hasArrived = isHovered;
        return isArriving;
    }

    /**
     * Forgets that the element was ever reached, for a consumer whose element stops showing - so the next
     * session announces an arrival the player will read as one rather than staying silent because the
     * pointer happens not to have moved since.
     */
    public void resetArrival() {
        hasArrived = false;
    }
}
