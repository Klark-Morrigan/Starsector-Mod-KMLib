package kmlib.animation;

/**
 * How far through a two-ended traverse something currently stands: a fraction aimed at the far end for as
 * long as a flag says it is heading there and back at rest while it does not, so whatever holds one eases
 * between the two ends rather than switching on the frame the flag turns over.
 *
 * <p>What the fraction <em>means</em> is the holder's own - an element blends between two looks, a notch
 * lifts its chrome toward its lit colour, a dialog stands further onto the screen - which is why nothing
 * about colour, chrome or pointers is here. A shared effect would have to know what each of those is made
 * of; a shared fraction knows none of them.
 *
 * <p>Where the target is a per-call flag rather than held state, for the reason {@link EasedFraction}'s is
 * a per-call value: the holder already knows which way it is heading this frame, and passing it each time
 * is what lets a turn take effect on the very next step. A holder whose direction is itself worth reading
 * keeps that flag and hands it in.
 *
 * <p>Retargeted rather than replayed, so something turned round mid-travel carries on from where it stands
 * instead of jumping to either end - the {@link EasedFraction} inside stores its position linearly and
 * eases only on read, which is what makes a mid-flight reversal continuous.
 *
 * <p>Transient by nature, being only where a motion currently sits, so nothing here is persisted and a
 * fresh one starts at rest.
 */
public final class TraverseFraction {

    // The two ends the fraction travels between, named so a target reads as a destination rather than a
    // bare bound.
    private static final float FAR_END = 1f;
    private static final float REST = 0f;

    // A drop covers the whole way in one step, so the time it is charged is immaterial.
    private static final float NO_ELAPSED_SECONDS = 0f;

    // Heading back to rest, named so the drop below reads as "aimed at rest" rather than as a bare false.
    private static final boolean HEADING_BACK = false;

    // Where the motion currently stands between those two ends.
    private final EasedFraction progress = new EasedFraction();

    /**
     * Steps the fraction toward the end {@code isRising} names by a frame's worth of time, at that
     * direction's own pace. A frame spent already at that end leaves it unchanged, so a render loop can
     * call this every frame unconditionally.
     *
     * @param isRising       whether the motion is heading for the far end rather than back to rest
     * @param elapsedSeconds real time since the last frame the holder drew
     * @param durations      how long travelling out to the far end and back to rest each take; a
     *                       non-positive one snaps that way
     */
    public void advanceTowardEnd(
            boolean isRising,
            float elapsedSeconds,
            TraverseDurations durations) {

        progress.advanceTowardTarget(
            resolveEndValue(isRising),
            elapsedSeconds,
            // Which way the motion is heading is what the flag says, so the pace it travels at follows
            // from the same flag rather than from a second reading of where the fraction stands.
            durations.resolveDurationSeconds(isRising));
    }

    /**
     * Drops the fraction all the way back to rest in one step, for a holder whose motion stops showing. A
     * fraction left part-way out would otherwise be the first thing the next appearance paints and then
     * wind down, showing the viewer the tail of a travel they never saw begin.
     */
    public void dropToRest() {
        // Routed through the ordinary advance rather than a second write to the fraction, so there is one
        // rule for where rest is and a drop cannot land somewhere the travel never does.
        advanceTowardEnd(HEADING_BACK, NO_ELAPSED_SECONDS, TraverseDurations.SNAP);
    }

    /**
     * @return how far out the motion currently stands, eased so the travel accelerates off the start and
     *         settles into the end: 0 at rest, 1 fully out
     */
    public float getEasedValue() {
        return progress.getEasedValue();
    }

    /**
     * Whether the travel has come all the way back, so a holder keeping one fraction per element can drop
     * a settled one rather than keeping it forever - a fraction at rest and a missing one read identically
     * to whatever asks.
     *
     * @return true once the fraction sits exactly at rest
     */
    public boolean hasSettledAtRest() {
        return progress.hasReachedTarget(REST);
    }

    // The end the travel is heading for this frame. One rule for what the flag means, so the advance and
    // the settled test cannot disagree about which end that is.
    private static float resolveEndValue(boolean isRising) {
        return isRising
            ? FAR_END
            : REST;
    }
}
