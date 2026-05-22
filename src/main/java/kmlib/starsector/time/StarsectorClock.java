package kmlib.starsector.time;

/**
 * Calendar constants for Starsector's in-game clock that vanilla
 * does not expose through any public API. Centralised in KMLib so
 * every KM* mod that needs them references the same single number -
 * a divergence between mods (one assuming 30, another assuming 31)
 * would silently desync month-end timing across the modset.
 *
 * <p>Today the only constant carried here is
 * {@link #DAYS_PER_MONTH}, but the class is named for the general
 * domain ({@code StarsectorClock}) rather than the specific
 * constant so future additions (months per cycle, hours per day,
 * seconds per game tick if vanilla ever exposes one) slot in
 * without a rename.
 *
 * <p>Final class with a private constructor: no instance state,
 * pure-constant utility. Matches
 * {@link kmlib.starsector.factions.StarsectorPlayerFactionResolver}'s
 * shape.
 */
public final class StarsectorClock {

    private StarsectorClock() {
        // utility class, no instances.
    }

    /**
     * Number of in-game days in a Starsector month. Vanilla pins
     * this at 30 deep inside the engine - no
     * {@code CampaignClockAPI} method exposes it, so every mod
     * that wants to compute "fraction of the current month
     * elapsed" or "trigger N days before month-end" has to
     * hard-code the value.
     *
     * <p>Sourcing from this single constant keeps the KM* mods
     * aligned with each other if vanilla ever changes the value
     * (extremely unlikely - the in-game calendar is load-bearing
     * for vanilla's own economy / event timing).
     */
    public static final int DAYS_PER_MONTH = 30;
}
