package kmlib.math.solving;

import java.util.function.ToDoubleFunction;

/**
 * Keeps the better of two candidates by a numeric key, tolerating nulls - the "best so
 * far" companion {@link Bisection} needs: a search that sweeps many candidates (a
 * highest-scoring placement, a tallest fitted box) folds them one at a time into a
 * running incumbent rather than collecting them all first to find a maximum.
 *
 * <p>One generic here rather than a typed helper per quantity: the comparison is the
 * same each time (a higher key wins, the first seen holds a tie, a null candidate never
 * displaces the incumbent), only the key differs, so the key is the parameter.
 */
public final class Picks {

    private Picks() {
    }

    /**
     * The higher of {@code current} and {@code candidate} by {@code key}. A null
     * candidate leaves the incumbent; against a null incumbent the candidate wins; a
     * tie keeps the incumbent, so the first candidate seen at a given key holds -
     * deterministic across repeated searches over the same input.
     *
     * @param current   the incumbent best, or null when none has been seen yet
     * @param candidate the challenger, or null to skip
     * @param key       the quantity to maximise
     * @param <T>       the candidate type
     * @return whichever scores higher on {@code key}
     */
    public static <T> T pickHigher(T current, T candidate, ToDoubleFunction<T> key) {
        if (candidate == null) {
            return current;
        }
        return current == null || key.applyAsDouble(candidate) > key.applyAsDouble(current)
                ? candidate : current;
    }
}
