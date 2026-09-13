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
 * <p>The same bar is published without a market too, for a caller holding somebody's word that the
 * world is standing there rather than a survey of it: {@link #SIGHTING_SURVEY_LEVEL} is what that
 * word is worth, and {@link #isMetBySighting} compares it on the same ordering.
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

    /**
     * What somebody's word that the world is standing there is worth as a survey - it having been
     * laid eyes on, and nothing beyond that.
     *
     * <p>{@link MarketAPI.SurveyLevel#SEEN} exactly, because a sighting is the whole of what the
     * level means: the place was looked at. The levels above it name survey data - readings taken
     * off the world itself - which no amount of somebody being nearby produces, so a sighting
     * reaches those neither in part nor by degree.
     */
    public static final MarketAPI.SurveyLevel SIGHTING_SURVEY_LEVEL = MarketAPI.SurveyLevel.SEEN;

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
     * Whether a bare sighting of a world reaches the survey a caller asks for - the question a
     * caller holding somebody's word rather than the player's own survey has to answer before it
     * may repeat it.
     *
     * <p>Takes no market, because what a sighting is worth is a property of sightings and not of
     * the world sighted. Whether this particular world has actually been surveyed that far is
     * {@link #isRevealedDecivilised}'s question, and the two are separate routes to the same bar
     * rather than one narrowing the other.
     *
     * @param requiredSurveyLevel the survey the caller asks for; null reads as
     *                            {@link #DEFAULT_SURVEY_LEVEL}, since an unstated bar must not be
     *                            read as no bar at all
     * @return true when being seen is survey enough for the level asked for
     */
    public static boolean isMetBySighting(MarketAPI.SurveyLevel requiredSurveyLevel) {
        return hasReachedLevel(SIGHTING_SURVEY_LEVEL, requiredSurveyLevel);
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

        if (market == null || !hasReachedLevel(market.getSurveyLevel(), requiredSurveyLevel)) {
            return false;
        }
        // getFirstCondition, not getSpecificCondition: the latter matches on a
        // condition's plugin-modification ID (the bare ID plus a "_<unique>"
        // suffix), so a bare condition ID never matches it and it always returns
        // null. getFirstCondition keys off the plain ID, the same ID the engine
        // and the colony UI show.
        var condition = market.getFirstCondition(Conditions.DECIVILIZED);
        return condition != null
            && (!condition.requiresSurveying() || condition.isSurveyed());
    }

    // Whether a level reaches at least as far as the caller asks - the one place the ordering is
    // read, so every bar in this class means the same thing whatever produced the level.
    //
    // Compared on the enum's declared order, which runs from never seen to fully surveyed: the one
    // ordering vanilla states, and the only thing that makes "at least this far" mean anything.
    //
    // The two nulls differ. A bar nobody stated is read as the fog's own rather than as no bar at
    // all, because the direction a missing argument may not take is the widening one; a level
    // nobody reached clears nothing, not even a bar of NONE.
    private static boolean hasReachedLevel(
            MarketAPI.SurveyLevel reachedSurveyLevel,
            MarketAPI.SurveyLevel requiredSurveyLevel) {

        var requiredLevel = requiredSurveyLevel == null
            ? DEFAULT_SURVEY_LEVEL
            : requiredSurveyLevel;

        return reachedSurveyLevel != null && reachedSurveyLevel.compareTo(requiredLevel) >= 0;
    }
}
