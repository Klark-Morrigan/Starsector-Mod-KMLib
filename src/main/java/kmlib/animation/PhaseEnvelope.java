package kmlib.animation;

import kmlib.math.easing.Easing;
import kmlib.math.ranges.Ranges;

/**
 * The shape one turn of a repeating animation takes: a value that climbs from rest to its peak over a leading
 * share of the turn and falls back over the remainder. A pure function of a phase - hand it where the cycle
 * stands and it answers how high the value is there - so it holds nothing, steps nothing, and two consumers
 * reading the same phase in different passes of one frame necessarily agree.
 *
 * <p>The split between the two halves is a value rather than a constant because it carries what the animation
 * reads as. A rise taking most of the turn swells; one taking a sliver of it strikes and decays, which is what
 * an alarm or a ping looks like. The same arithmetic serves both, and only the share moves.
 *
 * <p>Named for the phase it is read off rather than for the pulse it shapes, because {@link PulseEnvelope}
 * is a different thing under the name this would otherwise take: a lift something triggers, which times its
 * own rise and fall from that moment and has to be stepped every frame. The two are not alternatives - a
 * triggered lift answers an event, and this answers a cycle that was already turning.
 *
 * <p>Eased on read, on the same curve every other animation in this package settles along, so a value driven
 * by a phase advancing at a constant rate still reads as movement rather than as a ramp.
 *
 * <p>Arithmetic over time and nothing else - no colour, no shape, no element - so whatever holds one decides
 * for itself what its amplitude drives.
 *
 * @param riseFraction the share of the turn spent climbing to the peak, the rest of it spent falling back;
 *                     confined to [0, 1], where 0 stands at the peak from the turn's first instant and decays
 *                     across the whole of it, and 1 climbs across the whole of it and never falls
 */
public record PhaseEnvelope(
    float riseFraction) {

    // The two ends the amplitude travels between, named so the peak reads as a destination rather than as a
    // bare bound.
    private static final float PEAK_AMPLITUDE = 1f;

    // One whole turn of the cycle, which is what a phase of 1 stands at: the share left for the fall is
    // whatever the rise did not take of it.
    private static final float WHOLE_TURN = 1f;

    public PhaseEnvelope {

        // Confined here rather than at every reading, so an out-of-range share cannot divide the rise by a
        // negative span or leave the fall no span at all.
        riseFraction = Ranges.clampToUnit(riseFraction);
    }

    /**
     * How high the value stands at this point in the cycle.
     *
     * <p>Rest at both ends of the turn, so a phase that wraps carries the amplitude round with it: the value
     * a whole turn reaches is the value the next turn opens at, and nothing jumps at the seam.
     *
     * @param phase where the cycle currently stands, 0 at the start of a turn and 1 at its end; a phase
     *              outside that is confined to it, so a caller need not guard a reading of its own
     * @return the amplitude in [0, 1]: 0 at rest, 1 at the peak
     */
    public float resolveAmplitude(float phase) {

        return Easing.easeInOut(resolveLinearAmplitude(Ranges.clampToUnit(phase)));
    }

    // The un-eased ramp, split at the rise fraction. Kept apart from the ease so the split is one piece of
    // arithmetic to follow, and the curve stays the single shape the package eases everything along.
    private float resolveLinearAmplitude(float turnPhase) {

        // A phase below the split is inside the rise, which means the share it is a fraction of is above it
        // and so above zero - that ordering is what makes the divide safe without a guard of its own.
        if (turnPhase < riseFraction) {
            return turnPhase / riseFraction;
        }
        var fallSpan = WHOLE_TURN - riseFraction;

        // A rise taking the whole turn leaves nothing to fall over, so the turn ends at the peak rather than
        // dividing the fall by a span of nothing.
        if (fallSpan <= 0f) {
            return PEAK_AMPLITUDE;
        }
        return (WHOLE_TURN - turnPhase) / fallSpan;
    }
}
