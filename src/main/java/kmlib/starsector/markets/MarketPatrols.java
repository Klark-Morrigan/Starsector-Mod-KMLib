package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.DynamicStatsAPI;

/**
 * Reads of the patrols a market is set up to field.
 *
 * <p>Held apart from {@link Markets} because these are the only market reads that go
 * through the dynamic stats rather than the market's own accessors, and because the
 * two questions they answer - whether a patrol industry is running, and how much it
 * fields - are always asked together and must not drift apart. {@link PatrolCounts},
 * the value they yield, has its owning class here.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state.
 * Matches {@link Markets}'s shape, and is null-market defensive like the rest of the
 * library.
 */
public final class MarketPatrols {

    private MarketPatrols() {
        // utility class, no instances.
    }

    /**
     * Whether a functional patrol HQ garrisons this market - the "does a patrol
     * industry actually field patrols here" gate.
     *
     * <p>Reads vanilla's {@link MemFlags#MARKET_PATROL} (the {@code $patrol} flag) a
     * functional patrol industry - a Patrol HQ, Military Base, or High Command, or a
     * Lion's Guard HQ - sets on its market and clears when it stops, the same flag
     * vanilla's patrol-spawn AI gates on. This is deliberately narrower than a
     * non-zero {@link #readPatrolCounts}: the patrol-count stats are also written by
     * hidden pirate and Luddic Path bases that never set the flag, so a caller
     * asking "does a patrol HQ field patrols here" must read the flag, not the counts.
     *
     * <p>Narrower again than {@link Markets#isMilitary}, which asks what a market
     * <em>is</em> rather than what is currently running on it.
     *
     * @param market the market to test; null (or one with no memory) yields false
     * @return true when a functional patrol industry fields patrols at this market
     */
    public static boolean fieldsPatrols(MarketAPI market) {
        if (market == null) {
            return false;
        }
        var memory = market.getMemoryWithoutUpdate();
        return memory != null && memory.getBoolean(MemFlags.MARKET_PATROL);
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
     * <p>Non-zero counts do not imply a patrol HQ: hidden pirate and Luddic Path
     * bases write these stats too. Gate on {@link #fieldsPatrols} first when the
     * question is whether a functional patrol industry garrisons the market.
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
