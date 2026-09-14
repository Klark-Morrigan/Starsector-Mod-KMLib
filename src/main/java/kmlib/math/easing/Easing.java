package kmlib.math.easing;

import kmlib.math.ranges.Ranges;

/**
 * Remaps a unit-range progress onto an eased curve, so an animation that steps its progress linearly in
 * time still reads as smoothly accelerating rather than moving at a constant rate. The SSOT for the ease
 * shape, kept apart from whatever holds the progress: the state machine advances a plain linear parameter
 * and asks here for its eased value, so the curve is one pure function to reason about and pin rather than
 * arithmetic threaded through a stateful advance. Free of any UI or engine coupling.
 */
public final class Easing {
    // Coefficients of the smoothstep polynomial 3t^2 - 2t^3, factored below as t^2*(3 - 2t). This pair is
    // the unique one giving value 0 and 1 at the ends with zero slope at both - what makes the ease flatten
    // into a stop - so they are named rather than left as bare literals in the expression.
    private static final float SQUARE_TERM_COEFFICIENT = 3f;
    private static final float CUBIC_TERM_COEFFICIENT = 2f;

    private Easing() {
    }

    /**
     * Eases {@code progress} with the smoothstep S-curve: it starts and ends flat (zero slope at both
     * ends) and is steepest in the middle, so a value driven linearly through [0, 1] accelerates out of
     * the start and decelerates into the end. The curve is symmetric about its midpoint - {@code
     * easeInOut(t) + easeInOut(1 - t) == 1} - so a forward and a reversed run trace the same shape.
     * Progress is clamped to the unit range first, so an overshooting value never runs the polynomial past
     * the ends where it would fold back.
     *
     * @param progress how far through the animation, 0 at the start and 1 at the end; clamped to [0, 1]
     * @return the eased position in [0, 1]: 0 at the start, 1 at the end, 0.5 at the midpoint
     */
    public static float easeInOut(float progress) {
        var clamped = Ranges.clampToUnit(progress);
        return clamped * clamped * (SQUARE_TERM_COEFFICIENT - CUBIC_TERM_COEFFICIENT * clamped);
    }
}
