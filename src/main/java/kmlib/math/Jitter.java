package kmlib.math;

/**
 * Uniform random multiplier centred on {@code 1.0}, used by KM*
 * mods to apply month-end "weather" jitter to economy formulas.
 * The single {@code jitterSize} parameter is the half-width of the
 * band: a size of {@code 0.15} produces values in
 * {@code [0.85, 1.15]}.
 *
 * <p>Centralised in KMLib so every KM* mod that rolls a jitter
 * multiplier shares one definition of "what 0.15 means" - a
 * divergence (one mod treating it as half-width, another as
 * full-width) would silently mis-scale every consumer.
 *
 * <p>Negative inputs are normalised via {@link Math#abs(float)}.
 * Callers occasionally read the size from a settings file where a
 * stray minus sign is a typo rather than a request to flip the
 * band, and the band is symmetric around 1 anyway - a "negative
 * jitter" has no separate meaning.
 *
 * <p>Final class with a private constructor: pure-function utility,
 * no instance state. Shape matches
 * {@link kmlib.starsector.time.StarsectorClock}.
 */
public final class Jitter {

    private Jitter() {
        // utility class, no instances.
    }

    /**
     * Returns a uniform random {@code float} in
     * {@code [1 - |jitterSize|, 1 + |jitterSize|]}. Backed by
     * {@link Math#random()} so callers do not need to thread a
     * {@code Random} instance; the cost of grabbing the shared PRNG
     * is negligible at preseed-pass frequencies (a few hundred
     * rolls per in-game month).
     *
     * @param jitterSize half-width of the band; sign is ignored.
     * @return value in the closed interval
     *         {@code [1 - |jitterSize|, 1 + |jitterSize|]}.
     */
    public static float roll(float jitterSize) {
        float size = Math.abs(jitterSize);
        float min = 1f - size;
        float max = 1f + size;
        return min + (max - min) * (float) Math.random();
    }
}
