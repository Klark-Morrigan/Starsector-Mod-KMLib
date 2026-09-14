package kmlib.math.hashing;

/**
 * The share of the unit range a named thing holds, derived from the name alone. The same name reads the same
 * fraction in every session and on every machine, so a set of like things can be spread over one range
 * without any of them carrying a number of its own and without anything being stored between runs.
 *
 * <p>Spreading is what it is for. Like things timed off one clock all sit at the same point in it, which
 * reads as a single thing repeating rather than as many; giving each its own share of the range is what
 * separates them. Drawing a random share instead would move every session, and holding one per thing would
 * be state to persist and migrate for something derivable from a name that is already there.
 *
 * <p>Built on {@link String#hashCode()}, which the JDK specifies exactly and so answers the same everywhere,
 * then spread through {@link Avalanche} because raw string hashes of names differing in one character land
 * next to each other - and neighbouring shares are the one outcome the spread exists to avoid.
 */
public final class StableFractions {

    // A float carries 24 mantissa bits, so 24 bits of hash is all that survives the divide: taking exactly
    // that many lands the share on an exact multiple of 1/2^24, which can never round up to a whole.
    private static final int FRACTION_BITS = 24;
    private static final int DISCARDED_BITS = Integer.SIZE - FRACTION_BITS;
    private static final float FRACTION_SCALE = 1 << FRACTION_BITS;

    private StableFractions() {
    }

    /**
     * The share of the unit range the key holds.
     *
     * <p>A key whose hash is zero - the empty string among them - resolves to the start of the range, since
     * zero is the avalanche's one fixed point. Harmless here, where the start is as good a share as any
     * other; it only matters to a caller that needs zero to stay observable through a combine.
     *
     * @param key the name of the thing being spread; its stability is what makes the share stable
     * @return a fraction in [0, 1), the same one for a given key every time it is asked
     */
    public static float resolveFraction(String key) {

        // Unsigned so a negative hash keeps its bits rather than sign-extending into the shift, and the top
        // bits are taken because the avalanche has spread the input across all of them equally.
        return (Avalanche.mixBits(key.hashCode()) >>> DISCARDED_BITS) / FRACTION_SCALE;
    }
}
