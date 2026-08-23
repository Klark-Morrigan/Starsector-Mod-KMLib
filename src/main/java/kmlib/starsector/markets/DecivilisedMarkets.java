package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;

/**
 * What a decivilised world is, and whether the player can see that it is one - a former colony,
 * now factionless ruins.
 *
 * <p>Two questions, and keeping them apart is the whole of this class. Whether a market
 * <em>is</em> a dead world is a fact about the place, asked wherever the sector's colonies are
 * selected; whether the player may be told so is the fog over it, asked wherever a colony is
 * shown. One read answering both would put a fog inside a selection that is deliberately unfogged.
 *
 * <p>A dead world is condition-only, which is what parts it from every other colony and is why it
 * needs naming at all. Vanilla strips the market of its owner, its industries and its economy
 * listing when a colony dies, leaving the same shell every uninhabited planet carries - so the
 * ownership read that admits a colony refuses this one, and the decivilised condition is the only
 * thing left saying people were once here.
 *
 * <p>The reveal mirrors vanilla's own visibility rather than second-guessing it: the player must
 * have encountered the planet (its market is past {@link MarketAPI.SurveyLevel#NONE}), and the
 * {@code decivilized} condition must be visible under its own survey rule - immediately when it
 * does not require surveying, otherwise once surveyed. A colony that decivilises during play has
 * its condition force-revealed by the engine, so it surfaces at once; a procgen dead world follows
 * whatever survey bar the condition carries.
 *
 * <p>That reveal is a survey rather than a discovery, which is why it is stated here rather than
 * left to the entity's discovery flag: a planet the player has flown past is discovered whether or
 * not anybody looked closely enough to see that its ruins are ruins.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state. Matches
 * {@link Markets}'s shape, and is null-market defensive like the rest of the library.
 */
public final class DecivilisedMarkets {

    private DecivilisedMarkets() {
        // utility class, no instances.
    }

    /**
     * Whether a market stands for a world somebody used to live on - the admission that lets a
     * condition-only market into a colony set at all.
     *
     * <p>Narrow on purpose. Every uninhabited planet in the sector carries a condition-only market
     * to hold its hazard and atmosphere, and admitting those would have a map report somebody
     * present in every system anybody ever surveyed. The decivilised condition is exactly what
     * narrows it: it is the one shape that is condition-only and yet somewhere people
     * demonstrably were.
     *
     * <p>Condition-only is required rather than assumed, so a colony that carries the condition
     * while still being held - a world resettled over its own ruins - reads as the living colony
     * it is rather than as the ruin it was.
     *
     * <p>An owner is required for the reason {@link Markets#isOwnedColony} requires one: a dead
     * world is handed to the neutral faction as it dies, and a reader that groups or names
     * colonies by owner has nothing to read off one that has none.
     *
     * <p>Says nothing about the fog. Whether the player can see that the ruins are ruins is
     * {@link #isRevealedDecivilised}'s question, and the two are asked at different layers.
     *
     * @param market the market to test; null yields false
     * @return true when the market is a condition-only shell carrying the decivilised condition
     */
    public static boolean isDecivilisedWorld(MarketAPI market) {
        return market != null
            && market.getFaction() != null
            && market.isPlanetConditionMarketOnly()
            && market.hasCondition(Conditions.DECIVILIZED);
    }

    /**
     * Whether the player can already see that this market is a dead world - the base fog for a
     * ruin, standing where discovery stands for every other kind of colony.
     *
     * <p>Two independent gates, both vanilla's own: the planet has been encountered at all (a
     * never-visited world sits at {@link MarketAPI.SurveyLevel#NONE}), and the decivilised
     * condition is visible under the per-condition survey rule.
     *
     * <p>Whether the market is a dead world in the first place is not re-asked here. A caller
     * holding a market that carries no such condition is answered false by the condition read
     * below, which is the same answer a kind test would have given it.
     *
     * @param market the market to test; null yields false
     * @return true when the player has surveyed enough to know the world is dead
     */
    public static boolean isRevealedDecivilised(MarketAPI market) {
        if (market == null || market.getSurveyLevel() == MarketAPI.SurveyLevel.NONE) {
            return false;
        }
        // getFirstCondition, not getSpecificCondition: the latter matches on a
        // condition's plugin-modification id (the bare id plus a "_<unique>"
        // suffix), so a bare condition id never matches it and it always returns
        // null. getFirstCondition keys off the plain id, the same id the engine
        // and the colony UI show.
        var condition = market.getFirstCondition(Conditions.DECIVILIZED);
        return condition != null
            && (!condition.requiresSurveying() || condition.isSurveyed());
    }
}
