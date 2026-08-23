package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;

/**
 * What a decivilised world is, and how far the player has to have surveyed one before being told
 * so - a colony whose government collapsed, its people still on it and nobody running it.
 *
 * <p>Two questions, and keeping them apart is the whole of this class. Whether a market
 * <em>is</em> decivilised is a fact about the place, asked wherever the sector's colonies are
 * selected; whether the player may be told so is the fog over it, asked wherever a colony is
 * shown. One read answering both would put a fog inside a selection that is deliberately unfogged.
 *
 * <p>A decivilised world is condition-only, which is what parts it from every other colony and is
 * why it needs naming at all. Vanilla strips the market of its owner, its industries and its
 * economy listing when a colony collapses, leaving the same shell every uninhabited planet carries
 * - so the ownership read that admits a colony refuses this one, and the decivilised condition is
 * the only thing left saying anybody is down there.
 *
 * <p>The reveal mirrors vanilla's own visibility rather than second-guessing it, on two arms. The
 * world must have been surveyed at least as far as the caller asks - vanilla shows a condition from
 * {@link MarketAPI.SurveyLevel#SEEN} up, which is what a caller stating no level of its own gets -
 * and the {@code decivilized} condition must be visible under its own survey rule, immediately when
 * it does not require surveying and otherwise once surveyed. A colony that decivilises during play
 * has its condition force-revealed by the engine, so it surfaces at once; a procgen one follows
 * whatever bar the condition carries.
 *
 * <p>The level is the caller's because it is the one half a player has any business moving: vanilla
 * decides when a condition may be read, and the map decides how much of a survey it wants before
 * repeating it. The condition's own rule stays where it is, so raising the bar can only ever
 * withhold more than vanilla would and never show what vanilla would not.
 *
 * <p>That reveal is a survey rather than a discovery, which is why it is stated here rather than
 * left to the entity's discovery flag: a planet the player has flown past is discovered whether or
 * not anybody looked closely enough to see what became of the colony on it.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state. Matches
 * {@link Markets}'s shape, and is null-market defensive like the rest of the library.
 */
public final class DecivilisedMarkets {

    /**
     * The survey a world has to have had before the fog will say its colony collapsed, unless a
     * caller asks for more.
     *
     * <p>The lowest level that is not "never seen", which is what vanilla's own visibility asks of
     * every condition it shows - so an unstated bar mirrors the game rather than picking one of
     * its own. A caller wanting the map to say less asks for a higher level; one wanting it to say
     * everything asks for {@link MarketAPI.SurveyLevel#NONE}.
     */
    public static final MarketAPI.SurveyLevel DEFAULT_SURVEY_LEVEL = MarketAPI.SurveyLevel.SEEN;

    private DecivilisedMarkets() {
        // utility class, no instances.
    }

    /**
     * Whether a market stands for a world whose colony collapsed - the admission that lets a
     * condition-only market into a colony set at all.
     *
     * <p>Narrow on purpose. Every uninhabited planet in the sector carries a condition-only market
     * to hold its hazard and atmosphere, and admitting those would have a map report somebody
     * present in every system anybody ever surveyed. The decivilised condition is exactly what
     * narrows it: it is the one shape that is condition-only and yet demonstrably populated.
     *
     * <p>Condition-only is required rather than assumed, so a world resettled over its own ruins
     * reads as the governed colony it is now rather than as the collapse it came out of.
     *
     * <p>An owner is required for the reason {@link Markets#isOwnedColony} requires one: such a
     * market is handed to the neutral faction as the colony collapses, and a reader that groups or
     * names colonies by owner has nothing to read off one that has none.
     *
     * <p>Says nothing about the fog. How far the player has to have surveyed the world before
     * being told is {@link #isRevealedDecivilised}'s question, and the two are asked at different
     * layers.
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
     * Whether the player has surveyed this world far enough to be told its colony collapsed - the
     * base fog for a collapsed colony, standing where discovery stands for every other kind.
     *
     * <p>Two independent gates. The survey the caller asks for is one, compared against the
     * market's own level by {@link MarketAPI.SurveyLevel}'s declared order, so a world sitting at
     * {@link MarketAPI.SurveyLevel#NONE} passes only where the caller asks for nothing at all. The
     * decivilised condition being visible under vanilla's per-condition survey rule is the other,
     * and it is vanilla's to decide rather than the caller's - so a level asked for here can
     * withhold what the game would show and never show what the game would not.
     *
     * <p>Whether the market is decivilised in the first place is not re-asked here. A caller
     * holding a market that carries no such condition is answered false by the condition read
     * below, which is the same answer a kind test would have given it.
     *
     * @param market              the market to test; null yields false
     * @param requiredSurveyLevel how far the world must have been surveyed; null reads as
     *                            {@link #DEFAULT_SURVEY_LEVEL}, since an unstated bar must not be
     *                            read as no bar at all
     * @return true when the player may be told the colony here has collapsed
     */
    public static boolean isRevealedDecivilised(
            MarketAPI market,
            MarketAPI.SurveyLevel requiredSurveyLevel) {

        if (market == null || !hasReachedSurveyLevel(market, requiredSurveyLevel)) {
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

    // Whether the world has been surveyed at least as far as the caller asks.
    //
    // Compared on the enum's declared order, which runs from never seen to fully surveyed - the
    // one ordering vanilla states, and the only thing that makes "at least this far" mean
    // anything. A level nobody stated is read as the fog's own rather than as no bar at all,
    // because the direction a missing argument may not take is the widening one.
    private static boolean hasReachedSurveyLevel(
            MarketAPI market,
            MarketAPI.SurveyLevel requiredSurveyLevel) {

        var requiredLevel = requiredSurveyLevel == null
            ? DEFAULT_SURVEY_LEVEL
            : requiredSurveyLevel;

        var surveyLevel = market.getSurveyLevel();

        return surveyLevel != null && surveyLevel.compareTo(requiredLevel) >= 0;
    }
}
