package kmlib.animation;

import kmlib.math.easing.Easing;
import kmlib.time.Timings;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * How far through a two-ended traverse something stands at the instant it is asked, derived from how long
 * ago it last turned rather than advanced by a per-frame tick. The clocked counterpart of
 * {@link TraverseFraction}: the same two ends, the same flag naming the one it heads for, and the same pace
 * each way.
 *
 * <p>Read rather than stepped. Nothing has to be advanced in the right pass, or at all: two readings taken
 * in different passes of one frame agree because they evaluate the same function of the same clock, a
 * settled traverse costs nothing between turns, and a frame nobody draws on cannot strand it halfway. What
 * it holds is only the moment of its last turn and where the traverse stood then.
 *
 * <p>Real time rather than simulation time, off a monotonic source, for the reasons the README of
 * {@code kmlib.animation.cycles} gives for a phase: a paused simulation would freeze the traverse on the very
 * screen it plays on.
 *
 * <p>A turn is measured from where the previous traverse had reached rather than from an end of it, so one
 * reversed part-way turns around from where the eye currently sees it instead of jumping to the far end and
 * travelling back over a stretch it has already covered.
 *
 * <p>The traverse is held linear and eased only when read, which is what keeps that reversal continuous: a
 * turn records the linear position, and putting the curve over an already-curved value would compound it
 * and leave the way back running at the wrong rate.
 *
 * <p>Which end the traverse heads for is asked at every reading rather than held, so a holder whose own
 * state says where it is going states it each time, and a traverse read toward an end it was never turned
 * to simply travels there from where its last turn left it.
 */
public final class ClockedTraverseFraction {

    // The monotonic clock the traverse is measured against. Only differences are read, so a wall clock
    // stepping backwards cannot send a traverse the wrong way.
    private final LongSupplier readElapsedNanos;

    // How long each way takes, read at each reading rather than captured at the turn, so a knob moved
    // mid-traverse is answered by it rather than on the next one.
    private final Supplier<TraverseDurations> readDurations;

    // Where the traverse set off from and when, recorded at each turn.
    private boolean hasRecordedTurn;
    private float progressAtLastTurn;
    private long turnedAtNanos;

    /**
     * Travels off the JVM's monotonic clock - what a running game gets.
     *
     * @param readDurations how long travelling out to the far end and back to rest each take, asked at every
     *                      reading; a non-positive one makes that way a cut
     */
    public ClockedTraverseFraction(Supplier<TraverseDurations> readDurations) {
        this(System::nanoTime, readDurations);
    }

    /**
     * @param readElapsedNanos the monotonic clock the traverse is measured against
     * @param readDurations    how long each way takes; a non-positive one makes that way a cut
     */
    ClockedTraverseFraction(LongSupplier readElapsedNanos, Supplier<TraverseDurations> readDurations) {

        this.readElapsedNanos = readElapsedNanos;
        this.readDurations = readDurations;
    }

    /**
     * Whether the traverse has come all the way back, so a holder keeping one per element can drop a settled
     * one rather than keeping it forever.
     *
     * @param isRising whether the traverse heads for the far end rather than back to rest
     * @return true while it sits exactly at rest
     */
    public boolean hasSettledAtRest(boolean isRising) {
        return resolveLinearProgress(isRising, readElapsedNanos.getAsLong()) == LinearTravel.REST;
    }

    /**
     * How far out the traverse stands, eased so the travel accelerates off one end and settles into the
     * other.
     *
     * @param isRising whether the traverse heads for the far end rather than back to rest
     * @return 0 at rest, 1 fully out; with no turn recorded, the end it heads for
     */
    public float resolveEasedValue(boolean isRising) {
        return Easing.easeInOut(resolveLinearProgress(isRising, readElapsedNanos.getAsLong()));
    }

    /**
     * Turns the traverse toward one end, setting off from wherever it stands now.
     *
     * <p>A turn reverses a traverse, so where it stands is read as heading for the other end: that is the
     * traverse being turned round, and what the eye currently sees.
     *
     * @param isRising whether the traverse now heads for the far end rather than back to rest
     */
    public void turnTowardEnd(boolean isRising) {

        var nowNanos = readElapsedNanos.getAsLong();

        progressAtLastTurn = resolveLinearProgress(!isRising, nowNanos);
        turnedAtNanos = nowNanos;
        hasRecordedTurn = true;
    }

    // How far the traverse stands between the two ends before the curve is put over it. Kept apart from the
    // eased reading because a turn records this rather than the eased value: the curve laid over a value
    // already curved would compound, and the way back would leave at the wrong rate.
    private float resolveLinearProgress(boolean isRising, long nowNanos) {

        var endValue = LinearTravel.resolveEndValue(isRising);

        // Nothing has turned, so there is no traverse to be part-way through: a holder starting at either end
        // reads that end on its first frame rather than travelling to it.
        if (!hasRecordedTurn) {
            return endValue;
        }
        return LinearTravel.resolveProgressAfter(
            progressAtLastTurn,
            endValue,
            Timings.convertNanosToSeconds(nowNanos - turnedAtNanos),
            readDurations.get().resolveDurationSeconds(isRising));
    }
}
