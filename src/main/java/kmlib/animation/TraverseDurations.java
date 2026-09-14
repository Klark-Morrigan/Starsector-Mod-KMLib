package kmlib.animation;

/**
 * How long an animation takes to run each way: one duration for the way up and one for the way back. The two
 * travel together because they describe one motion, and a consumer handed only the direction it is heading
 * this frame would otherwise have to be handed the matching duration by whoever knows the other one too.
 *
 * <p>They are separate values because a motion answering input does not read right when they are equal: a
 * rise confirms what the player just did and wants to arrive, while a fall is the element letting go and can
 * take its time. Held as a pair rather than as a base and a ratio, so what each direction takes is read
 * rather than derived, and a retune of one does not silently move the other.
 *
 * <p>Arithmetic over time and nothing else - no element, no colour, no pointer - like the {@link
 * EasedFraction} and {@link PulseEnvelope} it paces, so whatever holds a motion decides for itself what the
 * motion is of.
 *
 * @param riseSeconds how long a full traverse toward the far end takes; zero or less snaps that way
 * @param fallSeconds how long a full traverse back to rest takes; zero or less snaps that way
 */
public record TraverseDurations(
    float riseSeconds,
    float fallSeconds) {

    /**
     * No travel either way: both directions cover the whole range in one step. What a caller dropping an
     * animation to an end passes, rather than each such caller naming its own pair of zeroes.
     */
    public static final TraverseDurations SNAP = new TraverseDurations(0f, 0f);

    /**
     * The same pace both ways, for a motion whose two directions are the same gesture - a fold reversed by
     * the handle that started it, where the way back is the way out undone rather than a letting go.
     *
     * @param durationSeconds how long a traverse takes, whichever way it runs
     * @return the symmetric pair
     */
    public static TraverseDurations createSymmetric(float durationSeconds) {
        return new TraverseDurations(durationSeconds, durationSeconds);
    }

    /**
     * The duration that applies to the direction a motion is currently heading.
     *
     * @param isRising whether the motion is heading for the far end rather than back to rest
     * @return that direction's duration
     */
    public float resolveDurationSeconds(boolean isRising) {
        return isRising
            ? riseSeconds
            : fallSeconds;
    }
}
