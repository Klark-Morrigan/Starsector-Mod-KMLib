package kmlib.starsector.markets;

/**
 * A market's configured patrol strength, split into the three vanilla size tiers.
 *
 * <p>Vanilla names the tiers light, medium, and heavy on the market's dynamic
 * stats; this exposes them as small, medium, and large - the sizes a player reads
 * a garrison in - with light mapping to {@code small} and heavy to {@code large}.
 * The counts are the market's static configuration (what its military industries
 * are set up to field), not the fleets currently spawned, so a caller reads the
 * same value every pass regardless of what is in flight.
 *
 * @param small  the light-tier patrols the market fields
 * @param medium the medium-tier patrols the market fields
 * @param large  the heavy-tier patrols the market fields
 */
public record PatrolCounts(int small, int medium, int large) {

    /** A market with no military industry fields no patrols of any tier. */
    public static final PatrolCounts NONE = new PatrolCounts(0, 0, 0);
}
