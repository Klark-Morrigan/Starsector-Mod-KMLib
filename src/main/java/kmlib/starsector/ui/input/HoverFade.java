package kmlib.starsector.ui.input;

import kmlib.animation.EasedFraction;
import kmlib.animation.TraverseDurations;

/**
 * How far one element has travelled onto its hovered look: a fraction aimed at 1 while the pointer is on it
 * and at 0 while it is not, so an element eases onto the look and back off it rather than switching on the
 * frame the pointer arrives. What the fraction <em>means</em> is the consumer's own - a tab blends between
 * two looks, a notch lifts its chrome toward its lit colour - which is why nothing about colour or chrome is
 * here. A shared effect would have to know what each element is made of; a shared fraction knows none of
 * them.
 *
 * <p>The fade is retargeted rather than replayed, so an element the pointer leaves mid-rise falls from where
 * it stands instead of jumping to either end - the {@link EasedFraction} inside stores its position linearly
 * and eases only on read, which is what makes a mid-flight reversal continuous.
 *
 * <p>Transient per-session UI state, held with whatever owns the element's other live state; nothing here is
 * persisted, so a fade needs no migration and a re-opened panel starts from rest.
 */
public final class HoverFade {

    // How long an element takes to let go, in seconds. Short enough that the element reads as answering the
    // input rather than catching up with it - a pointer crossing a row must not leave a trail of half-lit
    // elements behind it - and long enough that the travel is still visible as a travel rather than a
    // switch. Dialled by eye against the vanilla chrome the panel sits among.
    private static final float RELEASE_DURATION_SECONDS = 0.3f;

    // How much quicker the way on is than the way off. A rise answers something the player just did, so it
    // has to arrive under the gesture that asked for it; a fall answers nothing and reads better unhurried.
    // Equal paces make the whole motion feel like the slower half, which is the one nobody asked for.
    private static final float RISE_SPEED_MULTIPLE = 2f;

    /**
     * The pace an element's motion runs at when a consumer offers no control over it: onto the look it is
     * heading for at twice the speed it comes back off it.
     *
     * <p>One pace for every motion a panel makes in answer to input, not the hover's alone: a fade onto a
     * look and a lift over one are the same gesture answered at the same speed, and two paces written beside
     * each other is how one panel ends up with two rhythms. A consumer adding a motion takes this rather
     * than measuring its own. The advance takes the pair per frame, so this is the recommended default
     * rather than the holder's own pace.
     */
    public static final TraverseDurations DEFAULT_DURATIONS = new TraverseDurations(
        RELEASE_DURATION_SECONDS / RISE_SPEED_MULTIPLE,
        RELEASE_DURATION_SECONDS);

    // The two ends the fade travels between, named so a target reads as a destination rather than a bare
    // bound.
    private static final float HOVERED_PROGRESS = 1f;
    private static final float UNHOVERED_PROGRESS = 0f;

    // A reset skips the travel, and with no travel left to scale, the elapsed time it is charged is
    // immaterial.
    private static final float NO_ELAPSED_SECONDS = 0f;

    // Whether the element is pointed at, named so the reset below reads as "aimed off the hovered look"
    // rather than as a bare false.
    private static final boolean NOT_HOVERED = false;

    // Where the element currently stands between those two ends.
    private final EasedFraction hoverProgress = new EasedFraction();

    /**
     * Steps the fade toward the end {@code isHovered} names by a frame's worth of time, at that direction's
     * own pace. A frame spent already at that end leaves the fade unchanged, so a render loop can call this
     * every frame unconditionally.
     *
     * @param isHovered      whether the pointer is on the element this frame
     * @param elapsedSeconds real time since the last frame the consumer drew
     * @param durations      how long travelling onto the hovered look and back off it each take; a
     *                       non-positive one snaps that way
     */
    public void advanceTowardHover(
            boolean isHovered,
            float elapsedSeconds,
            TraverseDurations durations) {

        hoverProgress.advanceTowardTarget(
            resolveTargetProgress(isHovered),
            elapsedSeconds,
            // Which way the fade is heading is what isHovered says, so the pace it travels at follows from
            // the same flag rather than from a second reading of where the fraction currently stands.
            durations.resolveDurationSeconds(isHovered));
    }

    /**
     * @return how far onto its hovered look the element currently is, eased so the travel accelerates off
     *         the start and settles into the end: 0 fully off it, 1 fully on it
     */
    public float getHoverFraction() {
        return hoverProgress.getEasedValue();
    }

    /**
     * Whether the fade has come all the way back off the hovered look, so an owner holding one fade per
     * element can drop a settled one rather than keeping it forever - a fade at rest and a missing fade read
     * identically to a consumer.
     *
     * @return true once the fade sits exactly at its unhovered end
     */
    public boolean hasSettledOffHover() {
        return hoverProgress.hasReachedTarget(UNHOVERED_PROGRESS);
    }

    /**
     * Drops the fade all the way back off the hovered look in one step, for an owner whose element stops
     * showing. A fade left part-way up would otherwise be the first thing the next session paints and then
     * wind down, showing the player the tail of a hover they never saw begin.
     */
    public void resetFade() {
        // Routed through the ordinary advance rather than a second write to the fraction, so there is one
        // rule for where "off the hovered look" is and the reset cannot land somewhere the fade never does.
        advanceTowardHover(NOT_HOVERED, NO_ELAPSED_SECONDS, TraverseDurations.SNAP);
    }

    // The end the fade is heading for this frame. One rule for what being hovered means, so the advance and
    // the settled test cannot disagree about which end that is.
    private static float resolveTargetProgress(boolean isHovered) {
        return isHovered
            ? HOVERED_PROGRESS
            : UNHOVERED_PROGRESS;
    }
}
