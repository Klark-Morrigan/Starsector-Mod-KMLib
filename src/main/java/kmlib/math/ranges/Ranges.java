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
}
