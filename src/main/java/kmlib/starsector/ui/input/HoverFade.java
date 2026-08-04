package kmlib.starsector.ui.input;

import kmlib.animation.EasedFraction;

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
    /**
     * The pace one element's fade runs at, in seconds, when a consumer offers no control over it. Sized so a
     * round trip onto the hovered look and back takes about as long as a click pulse, which is what keeps the
     * two motions reading as one interaction vocabulary rather than two. The advance takes the duration per
     * frame, so this is the recommended default rather than the holder's own pace.
     */
    public static final float DEFAULT_DURATION_SECONDS = 0.35f;

    // The two ends the fade travels between, named so a target reads as a destination rather than a bare
    // bound.
    private static final float HOVERED_PROGRESS = 1f;
    private static final float UNHOVERED_PROGRESS = 0f;

    // Where the element currently stands between those two ends.
    private final EasedFraction hoverProgress = new EasedFraction();

    /**
     * Steps the fade toward the end {@code isHovered} names by a frame's worth of time. A frame spent already
     * at that end leaves the fade unchanged, so a render loop can call this every frame unconditionally.
     *
     * @param isHovered       whether the pointer is on the element this frame
     * @param elapsedSeconds  real time since the last frame the consumer drew
     * @param durationSeconds how long a full traverse should take; zero or less snaps instantly
     */
    public void advanceTowardHover(boolean isHovered, float elapsedSeconds, float durationSeconds) {
        hoverProgress.advanceTowardTarget(
            resolveTargetProgress(isHovered),
            elapsedSeconds,
            durationSeconds);
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

    // The end the fade is heading for this frame. One rule for what being hovered means, so the advance and
    // the settled test cannot disagree about which end that is.
    private static float resolveTargetProgress(boolean isHovered) {
        return isHovered
            ? HOVERED_PROGRESS
            : UNHOVERED_PROGRESS;
    }
}
