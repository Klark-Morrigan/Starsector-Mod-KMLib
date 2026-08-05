package kmlib.animation;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link PulseEnvelope}s of a set of like elements, keyed so a consumer can trigger any one of them and
 * ask any one of them how far up its lift it currently is. Advanced in one pass, so every running pulse is
 * charged the same frame's time however many of them there are.
 *
 * <p>A lone element holds a {@link PulseEnvelope} directly; a set of them holds one of these. The difference
 * is only whether there is more than one element to tell apart, so nothing about what a fraction lifts lives
 * here either.
 *
 * <p>Unlike a set of held fades, only the triggered key is touched on a trigger: a pulse is an event on one
 * element rather than a position the whole set shares, so triggering one says nothing about the others and
 * several can be running at once.
 *
 * <p>A key whose cycle has run out is dropped, since a missing envelope and a spent one answer the same
 * fraction. That keeps the set bounded for a set whose keys churn, and costs nothing for one whose keys are
 * stable.
 *
 * @param <K> what tells one element of the set from another - an index for a row that keeps its order, an
 *            identity of its own for a set that can be rebuilt between triggers
 */
public final class PulseEnvelopes<K> {

    // What a key with no envelope of its own reads as: an element nothing has triggered stands at rest,
    // which is the same answer a spent envelope gives - so a dropped envelope changes no consumer's reading.
    private static final float NOT_PULSING = 0f;

    // One envelope per element with a pulse still running on it, keyed by the consumer's own identity for an
    // element. Grown on a trigger and pruned once a cycle runs out, so the set holds only what is in motion.
    private final Map<K, PulseEnvelope> envelopesByKey = new HashMap<>();

    /**
     * Steps every running pulse by a frame's worth of time and drops the ones that have run out.
     *
     * @param elapsedSeconds real time since the last frame the consumer drew
     * @param durations      how long the rise and the fall each take; a non-positive one snaps that way
     */
    public void advanceByElapsedTime(float elapsedSeconds, TraverseDurations durations) {

        var entries = envelopesByKey.entrySet().iterator();
        while (entries.hasNext()) {

            var envelope = entries.next().getValue();
            envelope.advanceByElapsedTime(elapsedSeconds, durations);

            if (envelope.hasSettled()) {
                entries.remove();
            }
        }
    }

    /**
     * Drops every pulse, for a consumer whose elements stop showing - so a lift left part-way through its
     * cycle cannot reappear on whatever is drawn next, decaying from a peak the player never saw rise.
     */
    public void resetPulses() {
        envelopesByKey.clear();
    }

    /**
     * @param key the element being asked about
     * @return how far up its lift that element currently is, 0 for one with no pulse running
     */
    public float resolvePulseFractionAt(K key) {
        var envelope = envelopesByKey.get(key);
        return envelope == null
            ? NOT_PULSING
            : envelope.getPulseFraction();
    }

    /**
     * Starts (or restarts) the pulse on one element, minting its envelope if this is the first trigger it
     * has taken since the last reset. A retrigger aims the running lift back at its peak rather than adding
     * a second envelope beside it, so a repeated trigger cannot stack past the peak.
     *
     * @param key the element the event landed on
     */
    public void startPulseAt(K key) {
        envelopesByKey
            .computeIfAbsent(key, envelopeKey -> new PulseEnvelope())
            .startPulse();
    }
}
