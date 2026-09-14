package kmlib.math.ranges;

/**
 * Clamps values into a range. The SSOT for confining a value to bounds, so a fraction that drives an
 * interpolation, an animation, or a scroll position is squeezed into its legal range one way rather than
 * each call site re-inlining the same {@code min}/{@code max} pair. Pure arithmetic, free of any UI or
 * engine coupling.
 */
public final class Ranges {
    private Ranges() {
    }

    /**
     * Confines {@code value} to the unit range: below 0 clamps to 0, above 1 clamps to 1, and a value
     * already within stays unchanged - so a fraction from an animation or a pointer mapping never runs
     * past either end.
     *
     * @param value the value to confine
     * @return {@code value} squeezed into [0, 1]
     */
    public static float clampToUnit(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    /**
     * Confines {@code value} between two bounds, with an EMPTY range collapsing to whichever
     * of its ends is nearer rather than to a reversed interval.
     *
     * <p>The empty case is the reason this is not an inlined {@code min}/{@code max} pair.
     * Where the floor and the ceiling come from two constraints that do not overlap - each
     * reasonable on its own - the naive nesting hands back the ceiling whatever was asked
     * for, silently preferring one constraint outright. Giving the nearer end instead means
     * a caller that cannot have what it wants still gets the closest thing to it.
     *
     * @param value   the value to confine
     * @param lowest  the floor
     * @param highest the ceiling
     * @return the nearest allowed value
     */
    public static double clampInto(double value, double lowest, double highest) {

        if (lowest > highest) {
            return Math.abs(value - lowest) <= Math.abs(value - highest) ? lowest : highest;
        }
        return Math.min(highest, Math.max(lowest, value));
    }
}
