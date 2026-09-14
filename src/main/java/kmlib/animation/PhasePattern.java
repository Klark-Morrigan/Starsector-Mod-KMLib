package kmlib.animation;

import kmlib.math.ranges.Ranges;

/**
 * A rhythm a repeating animation runs on: the sequence of sounds and silences one turn of its cycle is made
 * of, read off a phase. A pure function like {@link PhaseEnvelope}, holding nothing and stepping nothing, so
 * what a pattern says at a given phase is the same however long a consumer looked away.
 *
 * <p>A pattern says only when something happens, never what happens or how loud - the envelope shapes each
 * element, and the consumer decides what an element is of. Splitting the two is what lets one rhythm drive a
 * glow, a ring or a colour without any of them knowing about the others.
 *
 * <p>Lengths are held in beat units rather than in seconds, so how fast a pattern runs is the consumer's
 * decision and how it is proportioned is the pattern's. One unit of a rhythm is its shortest sound; every
 * other length is a multiple of that, which is what keeps a rhythm recognisable at any pace.
 *
 * <p>A closed set rather than something a caller composes, because a rhythm is a thing a player chooses
 * between rather than a thing a caller builds. Elements alternate strictly, starting with a sound and ending
 * with a silence, so a turn always closes on a rest and no two sounds can run together.
 */
public enum PhasePattern {

    /**
     * A plain periodic beat: one sound, then a silence as long as it. What an emitter with nothing to say
     * beyond its own presence runs on.
     */
    STEADY_BEAT(
        BeatUnits.SHORT, BeatUnits.GAP),

    /**
     * The distress silhouette - three short, three long, three short, then a rest long enough that the group
     * reads as a message repeating rather than as nine unrelated flashes.
     *
     * <p>Proportioned the way the signal itself is: a long sound is three shorts, the sounds within the group
     * are parted by one short, and the group is parted from its repeat by seven.
     */
    SOS(
        BeatUnits.SHORT, BeatUnits.GAP, BeatUnits.SHORT, BeatUnits.GAP, BeatUnits.SHORT, BeatUnits.GAP,
        BeatUnits.LONG, BeatUnits.GAP, BeatUnits.LONG, BeatUnits.GAP, BeatUnits.LONG, BeatUnits.GAP,
        BeatUnits.SHORT, BeatUnits.GAP, BeatUnits.SHORT, BeatUnits.GAP, BeatUnits.SHORT, BeatUnits.REST);

    // Where an element the phase has only just entered stands, which is also where the turn's very end reads
    // as standing - the first instant of the next turn rather than the last of this one.
    private static final float BEAT_START = 0f;

    // Whether the first element of a pattern is a sound. Every pattern opens on one and alternates from
    // there, so the walk below tracks the state by flipping rather than by storing it per element.
    private static final boolean OPENS_SOUNDING = true;

    // The elements of one turn in beat units, alternating sound and silence from the first. Held as lengths
    // rather than as boundaries so the sequence reads as the rhythm it is.
    private final float[] elementsInUnits;

    // The whole turn's length in beat units, summed once. It converts a pace into a period and divides a
    // phase into positions, so it is worth holding rather than re-summing on every reading.
    private final float cycleUnits;

    PhasePattern(float... elementsInUnits) {

        this.elementsInUnits = elementsInUnits;

        var totalUnits = 0f;
        for (var elementUnits : elementsInUnits) {
            totalUnits += elementUnits;
        }
        this.cycleUnits = totalUnits;
    }

    /**
     * Where in the rhythm a phase stands.
     *
     * @param phase where the cycle currently stands, 0 at the start of a turn and 1 at its end; a phase
     *              outside that is confined to it, and a phase at the very end reads as the opening instant
     *              of the next turn, so a wrapping phase crosses the seam without repeating or skipping an
     *              element
     * @return the element the phase is in and how far through it
     */
    public PatternBeat resolveBeatAt(float phase) {

        var position = Ranges.clampToUnit(phase) * cycleUnits;
        var isSounding = OPENS_SOUNDING;

        for (var elementUnits : elementsInUnits) {

            if (position < elementUnits) {
                return new PatternBeat(isSounding, position / elementUnits);
            }
            position -= elementUnits;
            isSounding = !isSounding;
        }

        // Only a phase standing exactly at the turn's end walks off the sequence. The alternation has flipped
        // once per element and so has come back round to how the turn opens, which is what the next turn is
        // about to do - so the reading carries on rather than reporting an element that does not exist.
        return new PatternBeat(isSounding, BEAT_START);
    }

    /**
     * How long one whole turn of this rhythm takes at a given pace, so a caller drives the clock with a
     * period the rhythm agrees with rather than deriving one from a length it would have to know.
     *
     * @param unitSeconds how long one beat unit - the rhythm's shortest sound - lasts
     * @return the seconds one turn of the whole pattern takes
     */
    public float resolvePeriodSeconds(float unitSeconds) {

        return unitSeconds * cycleUnits;
    }

    // The lengths every rhythm is proportioned in, in beat units. Held in a nested type because an enum
    // constant cannot name a constant of the enum declared after it, and these have to be shared by the
    // constants rather than written out per pattern.
    private static final class BeatUnits {

        private static final float SHORT = 1f;
        private static final float LONG = 3f;
        private static final float GAP = 1f;
        private static final float REST = 7f;

        private BeatUnits() {
        }
    }
}
