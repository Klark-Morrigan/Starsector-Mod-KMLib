package kmlib.animation;

import kmlib.math.easing.Easing;
import kmlib.math.ranges.Ranges;
import kmlib.time.Timings;

import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

/**
 * How far a ramp between two ends stands at the instant it is asked, derived from how long ago it last
 * turned rather than advanced by a per-frame tick.
 *
 * <p>Read rather than stepped. Nothing has to be advanced in the right pass, or at all: two readings taken
 * in different passes of one frame agree because they evaluate the same function of the same clock, a
 * settled ramp costs nothing between turns, and a frame nobody draws on cannot strand it halfway. What it
 * holds is only the moment of its last turn and where the ramp stood then.
 *
 * <p>Real time rather than simulation time, off a monotonic source, for the reasons this package's README
 * gives for a phase: a paused simulation would freeze the ramp on the very screen it plays on.
 *
 * <p>A turn is measured from where the previous ramp had reached rather than from an end of it, so a ramp
 * reversed part-way turns around from where the eye currently sees it instead of jumping to the far end
 * and travelling back over a stretch it has already covered.
 *
 * <p>The ramp is held linear and eased only when read, which is what keeps that reversal continuous: a
 * turn records the linear position, and putting the curve over an already-curved value would compound it
 * and leave the way back running at the wrong rate.
 *
 * <p>Which end the ramp heads for is asked at every reading rather than held, so a holder whose own state
 * says where it is going states it each time, and a ramp read toward an end it was never turned to simply
 * travels there from where its last turn left it.
 */
public final class ClockedEasedFraction {

    // The two ends of the ramp. The curve leaves both where they are, so they read the same before and
    // after easing.
    private static final float AT_REST = 0f;
    private static final float AT_FULL = 1f;

    // The monotonic clock the ramp is measured against. Only differences are read, so a wall clock
    // stepping backwards cannot send a ramp the wrong way.
    private final LongSupplier readElapsedNanos;

    // How long a whole ramp takes, read at each reading rather than captured at the turn, so a knob moved
    // mid-ramp is answered by it rather than on the next one.
    private final DoubleSupplier readRampSeconds;

    // Where the ramp set off from and when, recorded at each turn.
    private boolean hasRecordedTurn;
    private float progressAtLastTurn;
    private long turnedAtNanos;

    /**
     * Ramps off the JVM's monotonic clock - what a running game gets.
     *
     * @param readRampSeconds how long a whole ramp takes, in seconds, asked at every reading; zero or less
     *                        makes it a cut
     */
    public ClockedEasedFraction(DoubleSupplier readRampSeconds) {
        this(System::nanoTime, readRampSeconds);
    }

    /**
     * @param readElapsedNanos the monotonic clock the ramp is measured against
     * @param readRampSeconds  how long a whole ramp takes, in seconds; zero or less makes it a cut
     */
    ClockedEasedFraction(LongSupplier readElapsedNanos, DoubleSupplier readRampSeconds) {

        this.readElapsedNanos = readElapsedNanos;
        this.readRampSeconds = readRampSeconds;
    }

    /**
     * Turns the ramp toward one end, setting off from wherever it stands now.
     *
     * <p>A turn reverses a ramp, so where it stands is read as heading for the other end: that is the ramp
     * being turned round, and what the eye currently sees.
     *
     * @param isTowardFull whether the ramp now heads for the full end rather than back to rest
     */
    public void headToward(boolean isTowardFull) {

        var nowNanos = readElapsedNanos.getAsLong();

        progressAtLastTurn = resolveLinearProgress(!isTowardFull, nowNanos);
        turnedAtNanos = nowNanos;
        hasRecordedTurn = true;
    }

    /**
     * How far the ramp stands, eased so the travel accelerates off one end and settles into the other.
     *
     * @param isTowardFull whether the ramp heads for the full end rather than back to rest
     * @return 0 at rest, 1 at full; with no turn recorded, or a ramp of no time at all, the end it heads for
     */
    public float resolveEasedValueToward(boolean isTowardFull) {
        return Easing.easeInOut(resolveLinearProgress(isTowardFull, readElapsedNanos.getAsLong()));
    }

    // How far the ramp stands between the two ends before the curve is put over it. Kept apart from the
    // reading above because a turn records this rather than the eased value: the curve laid over a value
    // already curved would compound, and the way back would leave at the wrong rate.
    private float resolveLinearProgress(boolean isTowardFull, long nowNanos) {

        // Nothing has turned, so there is no ramp to be part-way through: a holder starting at either end
        // reads that end on its first frame rather than travelling to it.
        if (!hasRecordedTurn) {
            return resolveSettledProgress(isTowardFull);
        }

        var rampSeconds = readRampSeconds.getAsDouble();

        // A ramp over no time at all is a cut. Answered ahead of the division rather than by it.
        if (rampSeconds <= 0) {
            return resolveSettledProgress(isTowardFull);
        }

        var elapsedNanos = nowNanos - turnedAtNanos;
        var travelled = (float) (Timings.convertNanosToSeconds(elapsedNanos) / rampSeconds);

        // Clamped here rather than left to the curve, which clamps for its own reading only: a turn
        // records this value, and an unclamped one would put the next ramp's start beyond the end it is
        // travelling from and leave the reversal to cover a stretch that is not on screen.
        return Ranges.clampToUnit(isTowardFull
            ? progressAtLastTurn + travelled
            : progressAtLastTurn - travelled);
    }

    // Where the ramp rests when it is travelling nowhere: the end it heads for.
    private static float resolveSettledProgress(boolean isTowardFull) {
        return isTowardFull ? AT_FULL : AT_REST;
    }
}
