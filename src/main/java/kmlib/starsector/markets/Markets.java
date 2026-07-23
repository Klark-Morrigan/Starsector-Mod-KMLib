package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.DynamicStatsAPI;

/**
 * Queries over a single market's state.
 *
 * <p>Centralises reads a market only answers indirectly - walking its connected
 * entities, or normalising a raw stat against its vanilla band - so KM* mods (and
 * any external caller) share one implementation of a check like "does this colony
 * have a station" or "how stable is it, as a fraction" rather than re-deriving the
 * scan or the band arithmetic each time.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance
 * state. Matches {@link kmlib.starsector.systems.StarSystems}'s shape, and is
 * null-market defensive like the rest of the library.
 */
public final class Markets {

    // Vanilla's opt-out tag for a "station"-tagged entity that must not be treated
    // as a market's orbital station. No Tags constant exists for it, so the literal
    // is vanilla's own contract - OrbitalStation's station scan tests it the same way.
    private static final String NO_ORBITAL_STATION_TAG = "NO_ORBITAL_STATION";

    // Stability's vanilla 0..10 band, the denominator of the stability fraction. A
    // modded market can report a value outside it, so the fraction clamps: an
    // out-of-band reading scales as "none" or "full" rather than overshooting.
    private static final float MAX_STABILITY_VALUE = 10.0f;

    private Markets() {
        // utility class, no instances.
    }

    /**
     * Whether a market is a colony a faction owns, rather than a bare planet's
     * placeholder.
     *
     * <p>The "counts as a colony" filter map and territory logic share: a faction
     * must own it, and it must not be the condition-only market every uninhabited
     * planet carries to hold its hazard and atmosphere conditions. This is
     * ownership alone - it says nothing about whether the player has found the
     * colony yet; compose it with {@link #isKnownToPlayer} when visibility matters.
     *
     * @param market the market to test; null (or one with no owning faction) yields
     *               false
     * @return true when a faction owns the market and it is not condition-only
     */
    public static boolean isOwnedColony(MarketAPI market) {
        return market != null
                && market.getFaction() != null
                && !market.isPlanetConditionMarketOnly();
    }

    /**
     * Whether a market owns an attached defensive station.
     *
     * <p>Reads the market's connected entities - the ownership link the game
     * maintains, so a station found here is this market's own rather than a rival's
     * or an abandoned hulk sharing the orbit - and mirrors the scan
     * {@code OrbitalStation} itself runs: a {@code "station"}-tagged entity not
     * opted out via {@code NO_ORBITAL_STATION}. Keying on the entity tag captures
     * vanilla and modded stations alike, so no industry ids are read.
     *
     * @param market the market to inspect; null (or one with no connected entities)
     *               yields false
     * @return true when one of the market's connected entities is its orbital station
     */
    public static boolean hasAttachedStation(MarketAPI market) {
        if (market == null || market.getConnectedEntities() == null) {
            return false;
        }
        for (var entity : market.getConnectedEntities()) {
            if (entity.hasTag(Tags.STATION) && !entity.hasTag(NO_ORBITAL_STATION_TAG)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A market's stability as a fraction of its vanilla 0..10 band, clamped to
     * [0, 1].
     *
     * <p>The reusable half of any "scale something by how stable this colony is"
     * read: 0 at no stability, 1 at full, linear between. The clamp keeps a modded
     * market that reports outside the band from scaling past "none" or "full", so a
     * caller multiplying by the fraction never overshoots or flips sign.
     *
     * @param market the market to read; null yields 0 (no stability)
     * @return the market's stability in [0, 1]
     */
    public static double getStabilityFraction(MarketAPI market) {
        if (market == null) {
            return 0.0;
        }
        var fraction = market.getStabilityValue() / MAX_STABILITY_VALUE;
        return Math.min(Math.max(fraction, 0.0f), 1.0f);
    }

    /**
     * Whether the player knows this market exists.
     *
     * <p>Known has two arms: the market's entity has been discovered (no longer
     * flagged discoverable) OR the market has been un-hidden, surfaced into the
     * open by a story reveal. Neither arm reads the owner or the intel directory,
     * so a faction hidden from the directory is not barred, and a concealed base on
     * an always-visible entity (Galatia-Academy style) still reads known via the
     * discovery arm.
     *
     * <p>The un-hidden arm catches a colony surfaced ahead of its entity being
     * physically found - un-hidden on first entry yet still {@code setDiscoverable(true)}
     * until a fleet closes to sensor range. It is public knowledge in that window,
     * listed on the star's map tooltip, so it reads known at once. A still-concealed
     * station (a hidden market on a discoverable entity) fails both arms and stays
     * unknown until found.
     *
     * @param market the market to test; null yields false
     * @return true when the player knows the market exists
     */
    public static boolean isKnownToPlayer(MarketAPI market) {
        if (market == null) {
            return false;
        }
        var entity = market.getPrimaryEntity();
        var isEntityDiscovered = entity == null || !entity.isDiscoverable();
        return isEntityDiscovered || !market.isHidden();
    }

    /**
     * Whether a market counts as a known owned colony - the "counts on the map" filter a
     * presence or dominance read admits a market by.
     *
     * <p>Composes {@link #isOwnedColony} with {@link #isKnownToPlayer} so "counts as a known
     * colony" means one thing across every caller rather than each re-deriving the pair and
     * drifting. The dev reveal drops the visibility arm, admitting a colony the player has not
     * yet found so an undiscovered faction still paints under it.
     *
     * @param market                           the market to test; null yields false
     * @param shouldIncludeUndiscoveredMarkets whether an undiscovered colony still counts (the
     *                                         "show all factions" dev reveal); false applies the
     *                                         normal known-to-player filter
     * @return true when a faction owns the market and it is either known or the reveal is on
     */
    public static boolean isCountedAsColony(
            MarketAPI market, boolean shouldIncludeUndiscoveredMarkets) {
        if (!isOwnedColony(market)) {
            return false;
        }
        return shouldIncludeUndiscoveredMarkets || isKnownToPlayer(market);
    }

    /**
     * A market's configured patrol strength, read from the economy as the three
     * vanilla size-tier counts.
     *
     * <p>The reusable half of any "how much military does this colony field" read:
     * the light, medium, and heavy patrol counts vanilla's military industries
     * write onto the market's dynamic stats. This reads that static configuration -
     * what the colony is set up to field - rather than the fleets currently in
     * flight, so it is stable across a pass. The read mirrors vanilla's own
     * {@code MilitaryBase.getMaxPatrols}: each tier's effective mod truncated to an
     * integer count, so this reads the same numbers the game would spawn against.
     *
     * @param market the market to read; null (or one with no stats) yields
     *               {@link PatrolCounts#NONE}
     * @return the market's small, medium, and large patrol counts
     */
    public static PatrolCounts readPatrolCounts(MarketAPI market) {
        if (market == null || market.getStats() == null
                || market.getStats().getDynamic() == null) {
            return PatrolCounts.NONE;
        }
        var dynamic = market.getStats().getDynamic();
        return new PatrolCounts(
                readPatrolTierCount(dynamic, Stats.PATROL_NUM_LIGHT_MOD),
                readPatrolTierCount(dynamic, Stats.PATROL_NUM_MEDIUM_MOD),
                readPatrolTierCount(dynamic, Stats.PATROL_NUM_HEAVY_MOD));
    }

    // One patrol tier's count off the dynamic stats, mirroring vanilla's truncation
    // of the effective mod to an int. A missing mod (no military industry) or a
    // negative reading floors to zero, so a count is never negative.
    private static int readPatrolTierCount(DynamicStatsAPI dynamic, String modKey) {
        var mod = dynamic.getMod(modKey);
        if (mod == null) {
            return 0;
        }
        return Math.max(0, (int) mod.computeEffective(0.0f));
    }
}
