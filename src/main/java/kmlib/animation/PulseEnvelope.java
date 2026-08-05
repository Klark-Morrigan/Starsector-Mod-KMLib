package kmlib.animation;

/**
 * A momentary lift that rises to its peak and falls back of its own accord: one trigger yields a whole
 * in-and-out cycle, which is what separates it from an {@link EasedFraction} aimed at an end and held there.
 * Arithmetic over time and nothing else - no colour, no element, no pointer - so whatever holds one decides
 * for itself what its fraction lifts, exactly as the fraction inside it leaves what a position between two
 * ends means to its own holder.
 *
 * <p>A fresh envelope sits settled at rest, so an untriggered one reads as nothing happening rather than as a
 * pulse that has already run its course.
 *
 * <p>Retriggering aims the lift back at its peak from wherever it currently stands rather than replaying the
 * curve from zero. That is what keeps a repeated trigger from either stacking past the peak or dropping the
 * lift to nothing before rebuilding it - the second reads as a dip in the opposite direction to the one the
 * trigger asked for, which is worse than the stack it was meant to avoid.
 *
 * <p>Transient by nature - an envelope is only where a lift currently sits - so it is never persisted and
 * needs no migration.
 */
public final class PulseEnvelope {

    // The two ends the lift travels between: the peak a trigger aims it at, and the rest it falls back to
    // once it arrives. Named so a target reads as a destination rather than as a bare bound.
    private static final float PEAK_PROGRESS = 1f;
    private static final float RESTED_PROGRESS = 0f;

    // Where the lift currently stands between those two ends. An eased fraction rather than a raw counter,
    // so a pulse accelerates and settles on the same curve every other animation on the same surface does.
    private final EasedFraction pulseProgress = new EasedFraction();

    // Which end it is heading for: true climbing to the peak, false falling back to rest. Flipped on arrival
    // at the peak rather than by a caller, since a pulse turns around by itself - that turn is the whole
    // difference between this and a fraction held at the end it was sent to.
    private boolean isRising;

    /**
     * Steps the lift toward whichever end it is heading for by a frame's worth of time, turning it around
     * once it reaches the peak. A frame spent already settled leaves it unchanged, so a render loop can call
     * this every frame unconditionally.
     *
     * <p>The two halves of the cycle are timed separately, so a lift can snap to its peak and ease back down
     * over longer - which is what makes it read as a strike rather than a swell. Taking the pair per frame
     * rather than storing it is what lets one caller pace every motion its surface makes from a single value.
     *
     * @param elapsedSeconds real time since the last frame the consumer drew
     * @param durations      how long the rise and the fall each take; a non-positive one snaps that way
     */
    public void advanceByElapsedTime(float elapsedSeconds, TraverseDurations durations) {
        pulseProgress.advanceTowardTarget(
            resolveTargetProgress(),
            elapsedSeconds,
            durations.resolveDurationSeconds(isRising));

        // The peak is a turning point rather than an end: reaching it is what sends the lift back down, so
        // one trigger produces the whole cycle and nothing outside has to time the fall. A frame long enough
        // to overrun the peak spends its remainder there rather than carrying it into the fall, which costs
        // at most one frame of hold and only on a frame that stalled.
        if (isRising && pulseProgress.hasReachedTarget(PEAK_PROGRESS)) {
            isRising = false;
        }
    }

    /**
     * @return how far up its peak the lift currently is, eased so it accelerates off each end and settles
     *         into the other: 0 at rest, 1 at the peak
     */
    public float getPulseFraction() {
        return pulseProgress.getEasedValue();
    }

    /**
     * Whether the cycle has run all the way out, so an owner holding one envelope per element can drop a
     * spent one rather than keeping it forever - a settled envelope and a missing one read identically to a
     * consumer. Both halves are asked: an envelope aimed at the peak has not run out however low it still
     * stands, since it is on its way up rather than back down.
     *
     * @return true once the lift has fallen all the way back to rest and is not climbing again
     */
    public boolean hasSettled() {
        return !isRising && pulseProgress.hasReachedTarget(RESTED_PROGRESS);
    }

    /**
     * Aims the lift at its peak, for whatever saw the event being confirmed. The fall needs no second call -
     * the advance turns the envelope around once it arrives - so a trigger is the only thing a caller has to
     * report.
     */
    public void startPulse() {
        isRising = true;
    }

    // The end the lift is heading for this frame. One rule for which end that is, so the advance and the
    // settled test cannot disagree about whether the cycle has run out.
    private float resolveTargetProgress() {
        return isRising
            ? PEAK_PROGRESS
            : RESTED_PROGRESS;
    }
}
