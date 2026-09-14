package kmlib.math.hashing;

import java.util.function.IntSupplier;

/**
 * Folds several ints into one order-sensitive fingerprint, so a consumer that must react when any of
 * a set of inputs changes reads a single int rather than tracking each source. The motivating case is
 * cache invalidation: each source is a monotonic revision counter (a producer bumps it when its data
 * moves), and {@link #compute} folds their current values so the fingerprint shifts whenever any one
 * advances - but nothing here is revision-specific; any ints fold the same way.
 *
 * <p>The point is composition: a consumer whose inputs grow over time adds one more source to the
 * {@link #compute} call rather than widening a bespoke fold, so the number of inputs is free to grow
 * without stretching any one method's meaning. A consumer with a single static input passes no
 * sources and gets a stable constant.
 */
public final class Fingerprints {
    // The 31-multiply polynomial seed, matching Objects.hash, so the fold spreads its inputs the same
    // familiar way and an empty fold is a fixed non-zero constant.
    private static final int SEED = 1;
    private static final int MULTIPLIER = 31;

    private Fingerprints() {
    }

    /**
     * Folds the current value of each source into one fingerprint. Order-sensitive, so two sources
     * that swap values still produce different results; a change in any source changes the fold. With
     * no sources it returns a fixed constant, the natural "nothing to track" value for a static input.
     *
     * @param sources the int sources to fold, in a fixed order
     * @return a fingerprint that shifts whenever any source's value changes
     */
    public static int compute(IntSupplier... sources) {
        var result = SEED;
        for (var source : sources) {
            result = MULTIPLIER * result + source.getAsInt();
        }
        return result;
    }
}
