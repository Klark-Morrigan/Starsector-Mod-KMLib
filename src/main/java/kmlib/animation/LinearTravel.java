package kmlib.animation;

/**
 * The rules every two-ended motion in this package travels by, held once so the stepped and the clocked
 * types cannot drift apart.
 *
 * <p>The travel is linear, with the curve put over it only when read. Covering a stretch in one step lands
 * where covering it in many smaller ones does, because the travel stops on its end rather than past it. That
 * is what lets a stepped type apply the rule to each frame's time and a clocked type apply it once to the
 * time since its last turn.
 */
final class LinearTravel {

    // The far end of the travel. The curve leaves both ends where they are, so they read the same before and
    // after easing.
    private static final float FAR_END = 1f;

    // The near end, where a fresh stepped motion sits.
    static final float REST = 0f;

    private LinearTravel() {
    }

    /**
     * Where a travel stands after covering some time toward an end.
     *
     * @param startProgress   where the travel set off from
     * @param endValue        the end it heads for
     * @param elapsedSeconds  how long it has travelled
     * @param durationSeconds how long a whole traverse of the range takes; zero or less covers any distance
     *                        at once
     * @return the linear position reached, never past the end
     */
    static float resolveProgressAfter(
            float startProgress,
            float endValue,
            double elapsedSeconds,
            double durationSeconds) {

        // A traverse over no time at all is a cut. Answered ahead of the division rather than by it.
        if (durationSeconds <= 0) {
            return endValue;
        }

        var travelled = (float) (elapsedSeconds / durationSeconds);

        // Clamping against the end rather than the range end is what makes a settled travel land on its end
        // exactly, whichever side it approached from - so an arrival is an equality, not a proximity.
        return startProgress < endValue
            ? Math.min(endValue, startProgress + travelled)
            : Math.max(endValue, startProgress - travelled);
    }

    /**
     * @param isRising whether the travel heads for the far end rather than back to rest
     * @return the end it heads for
     */
    static float resolveEndValue(boolean isRising) {

        return isRising
            ? FAR_END
            : REST;
    }
}
