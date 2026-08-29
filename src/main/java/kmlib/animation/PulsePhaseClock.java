package kmlib.animation;

import kmlib.math.hashing.StableFractions;
import kmlib.profiling.Timings;

import java.util.function.LongSupplier;

/**
 * Where a repeating animation stands in its cycle at the moment it is asked, phased off real time.
 *
 * <p>Real time rather than simulation time, because the two disagree exactly where an ambient animation has
 * to carry on regardless. A paused simulation stops advancing at all, and a compressed one advances many
 * seconds per frame, so an animation phased off it would freeze in the first case and strobe in the second -
 * neither of which is anything the player did to the thing being animated.
 *
 * <p>Read rather than stepped. A phase is a function of the instant it is asked at, not an integration of the
 * frames since it started, so there is no per-frame call to place and none to forget: two consumers drawing
 * in different passes of one frame agree because they evaluate the same function of the same clock, not
 * because something advanced them together. It also means a consumer that stops drawing for a while comes
 * back to a cycle that kept turning, which is what makes the animation ambient rather than a thing that
 * starts when it is looked at.
 *
 * <p>Elapsed time is measured from the moment the clock was made and off a monotonic source, so only
 * differences are ever read and a wall clock stepping backwards cannot send a phase back around.
 *
 * <p>Arithmetic over time and nothing else - no colour, no shape, no element - so whatever holds one decides
 * for itself what a phase drives. Holds nothing worth persisting: a fresh clock starts its cycles over, which
 * is invisible in animation that repeats anyway.
 */
public final class PulsePhaseClock {

    // What a period of zero or less reads as. The start of the cycle, so an animation asked to repeat over no
    // time at all stands still rather than dividing by nothing.
    private static final float STILL_PHASE = 0f;

    // The share of a turn a phase asked for without a subject is moved along by: none of it, since there is
    // nothing to spread it apart from.
    private static final float UNSTAGGERED_OFFSET = 0f;

    // One whole turn of a cycle. Phase is the fractional part of how many turns have elapsed, so the wrap is
    // a remainder against a turn rather than against the period in seconds.
    private static final double WHOLE_TURN = 1.0;

    private final LongSupplier readElapsedNanos;

    // The reading the clock was made at, subtracted from every later one so elapsed time starts at zero. The
    // source's own origin is arbitrary, and a phase taken against it directly would start mid-cycle.
    private final long startedAtNanos;

    /** Phases off the JVM's monotonic clock - what a running game gets. */
    public PulsePhaseClock() {
        this(System::nanoTime);
    }

    /**
     * @param readElapsedNanos the elapsed-time clock phases are taken against, which is a monotonic one
     *                         rather than a wall clock: only differences are read, and a wall clock stepping
     *                         back would run a cycle backwards
     */
    PulsePhaseClock(LongSupplier readElapsedNanos) {

        this.readElapsedNanos = readElapsedNanos;
        this.startedAtNanos = readElapsedNanos.getAsLong();
    }

    /**
     * @return real seconds since this clock was made, for a consumer pacing something that does not repeat
     *         and so has no period for the readings below to take
     */
    public double readElapsedSeconds() {

        return Timings.convertNanosToSeconds(readElapsedNanos.getAsLong() - startedAtNanos);
    }

    /**
     * How far through a cycle of the stated length the clock currently stands, wrapping at every turn.
     *
     * @param periodSeconds how long one whole turn of the cycle takes; zero or less stands still at the start
     *                      of it, so a period coming from a knob a player can wind down to nothing yields a
     *                      still animation rather than an undefined one
     * @return a phase in [0, 1)
     */
    public float resolvePhase(float periodSeconds) {

        return resolveOffsetPhase(periodSeconds, UNSTAGGERED_OFFSET);
    }

    /**
     * The same phase, moved along by the subject's own fixed share of a turn, so like emitters sit apart in
     * the cycle instead of every one of them peaking on the same frame - which reads as one thing happening
     * rather than as many sources.
     *
     * <p>The share is derived from the subject's name rather than drawn or stored, so a subject sits at the
     * same point in every session and nothing has to be persisted for it to.
     *
     * @param periodSeconds how long one whole turn of the cycle takes, as above
     * @param subjectId     what tells this emitter from the others; its stability across sessions is what
     *                      makes the subject's share of the turn stable
     * @return a phase in [0, 1)
     */
    public float resolvePhaseForSubject(float periodSeconds, String subjectId) {

        return resolveOffsetPhase(periodSeconds, StableFractions.resolveFraction(subjectId));
    }

    // The one arithmetic path both readings take, so a staggered phase cannot wrap or stand still differently
    // from a plain one. Turns elapsed are counted whole and fractional together and the whole part discarded,
    // which is what makes the offset a shift along the same cycle rather than a second cycle of its own.
    private float resolveOffsetPhase(float periodSeconds, float offsetFraction) {

        if (periodSeconds <= 0f) {
            return STILL_PHASE;
        }
        var turnsElapsed = readElapsedSeconds() / periodSeconds + offsetFraction;

        return (float) (turnsElapsed % WHOLE_TURN);
    }
}
