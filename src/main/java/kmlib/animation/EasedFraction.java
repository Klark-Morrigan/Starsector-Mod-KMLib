package kmlib.animation;

import kmlib.math.easing.Easing;
import kmlib.math.ranges.Ranges;

/**
 * How far along the way between two ends something currently is: a linear progress parameter in [0, 1],
 * advanced toward a target by the elapsed fraction of a duration and eased only when read. It is arithmetic
 * over time and nothing else - no colour, no element, no pointer - so any animation that is a position
 * between two ends can hold one and decide for itself what its fraction means. That narrowness is the point:
 * a shared fraction serves every such animation, where a shared effect would have to know what each one is
 * made of.
 *
 * <p>Storing the parameter linearly and easing it on read is what keeps a retargeted animation continuous.
 * Aiming a fraction at the other end mid-flight moves it on from where it stands rather than replaying a
 * curve from zero, so an animation reversed part-way neither snaps nor doubles back at the wrong rate;
 * easing on step would instead compound the curve on itself.
 *
 * <p>Transient by nature - a fraction is only where an animation currently sits - so it is never persisted
 * and needs no migration. A fresh one sits at 0; {@link #createAtValue} seeds one that starts settled
 * somewhere else.
 */
public final class EasedFraction {
    // Linear animation parameter in [0, 1], stepped by elapsed time and eased only on read so a reversal
    // mid-flight carries on from the current eased value rather than restarting a curve.
    private float progress;

    /**
     * A fraction already settled at {@code value}, for an animation that opens at an end other than 0 - the
     * end state a full run would reach, reached without stepping through the run.
     *
     * @param value where the fraction starts, confined to [0, 1]
     * @return a fraction seeded at that value and idle there
     */
    public static EasedFraction createAtValue(float value) {
        var fraction = new EasedFraction();

        fraction.progress = Ranges.clampToUnit(value);

        return fraction;
    }

    /**
     * Steps the fraction toward {@code targetValue} by the elapsed fraction of {@code durationSeconds},
     * stopping exactly on the target rather than overshooting it. A frame spent already at the target leaves
     * the fraction unchanged, so a render loop can call this every frame unconditionally. The target is a
     * per-call input rather than stored state because the caller already knows which end it is heading for,
     * and passing it each frame is what lets a retarget take effect on the very next step. A non-positive
     * duration means "no animation" and covers the whole way in this one step, which also guards the divide
     * against a zero denominator.
     *
     * @param targetValue     the end being headed for, confined to [0, 1]
     * @param elapsedSeconds  real time since the last frame; a full {@code durationSeconds} covers the whole
     *                        range in one step
     * @param durationSeconds how long a full traverse of the range should take; zero or less snaps instantly
     */
    public void advanceTowardTarget(float targetValue, float elapsedSeconds, float durationSeconds) {
        var end = resolveEndValue(targetValue);

        var step = durationSeconds > 0f
            ? elapsedSeconds / durationSeconds
            : 1f;

        // Clamping against the target rather than the range end is what makes a settled fraction land on its
        // target exactly, whichever side it approached from - so an arrival is an equality, not a proximity.
        progress = progress < end
            ? Math.min(end, progress + step)
            : Math.max(end, progress - step);
    }

    /**
     * @return the fraction eased so the motion accelerates off the start and settles into the end, which is
     *         what turns a constant-rate advance into one that reads as movement rather than a ramp: 0 at
     *         one end of the range, 1 at the other
     */
    public float getEasedValue() {
        return Easing.easeInOut(progress);
    }

    /**
     * Whether the fraction sits at an end, so a caller can tell a still animation from a moving one - to
     * rest a frame pump, or to read an end state off the animation rather than tracking it alongside.
     *
     * <p>The test is exact rather than approximate because {@link #advanceTowardTarget} lands on its target
     * by clamping to it: a settled fraction equals its end value instead of merely approaching it.
     *
     * @param targetValue the end to test against, resolved exactly as the advance resolves it
     * @return true once the fraction has arrived at that end
     */
    public boolean hasReachedTarget(float targetValue) {
        return progress == resolveEndValue(targetValue);
    }

    /**
     * @return the target confined to the range the fraction lives in. One rule for what a caller's target
     *         means, because the advance and the arrival test must resolve it identically - resolved apart,
     *         an out-of-range target would be stepped toward one value and tested against another, and the
     *         fraction would read as travelling forever
     */
    private static float resolveEndValue(float targetValue) {
        return Ranges.clampToUnit(targetValue);
    }
}
