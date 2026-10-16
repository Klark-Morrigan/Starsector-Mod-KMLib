package kmlib.testfixtures.starsector.systems.claims;

import kmlib.starsector.systems.claims.ContestAdmission;
import kmlib.starsector.systems.claims.PresenceOnlyClaimStanding;
import kmlib.starsector.systems.claims.WeighedClaimStanding;

import java.util.List;

/**
 * Builds the standings a claim contest is posed with, for tests about what a contest means
 * rather than about the arithmetic behind it. Published as a fixture variant so both KMLib's and
 * consuming mods' tests state a standing the same way.
 *
 * <p>A standing is a market breakdown plus the faction it belongs to, so a test that cares only
 * about a score would otherwise have to invent a colony to carry it - and every such test would
 * invent a slightly different one. Stating the score directly keeps those cases about the score.
 * Each colony is an ordinary {@link ClaimMarketFixture} market, varied only where a builder says.
 *
 * <p>Both kinds of standing are built here, since a case posing a contest usually wants the
 * distinction between them and nothing else about either - who was weighed, and who was merely
 * there.
 */
public final class ClaimStandingFixture {

    // Whether the player knows of a builder's colony. Known is what a case posing standings by
    // score alone wants; unknown is what the fog-of-war builders vary.
    private static final boolean KNOWN_TO_PLAYER = true;
    private static final boolean UNKNOWN_TO_PLAYER = false;

    // The colonies the three builders stand on. Named apart so a box listing several kinds of
    // standing can be read line by line, and each name gives its colony its own ID.
    private static final String STANDING_MARKET_NAME = "Standing Colony";
    private static final String UNKNOWN_MARKET_NAME = "Unknown Colony";
    private static final String UNWEIGHED_MARKET_NAME = "Unweighed Colony";

    private ClaimStandingFixture() {
    }

    /**
     * A faction standing on a single market worth exactly the given score and holding nothing
     * else in the system.
     *
     * @param factionId     the faction the standing belongs to
     * @param score         what the contest weighs the faction's presence at
     * @param isTerritorial whether the faction may claim a system at all
     * @return the standing
     */
    public static WeighedClaimStanding buildStandingOnOneMarket(
            String factionId,
            int score,
            boolean isTerritorial) {

        return buildStandingOnOneMarket(factionId, score, isTerritorial, KNOWN_TO_PLAYER);
    }

    /**
     * The same standing, over a colony the player knows of or does not.
     *
     * <p>Both readings are ordinary, and the second is the shape a case about a fog-of-war
     * projection over the scored kind has to pose: the mechanic settles a contest over colonies
     * nobody has reached, so a faction can be weighed on a market no display may name. Such a market
     * is held in the open and listed, so what the second reading poses is an undiscovered colony.
     *
     * @param factionId       the faction the standing belongs to
     * @param score           what the contest weighs the faction's presence at
     * @param isTerritorial   whether the faction may claim a system at all
     * @param isKnownToPlayer whether the player knows of the colony the standing rests on
     * @return the standing
     */
    public static WeighedClaimStanding buildStandingOnOneMarket(
            String factionId,
            int score,
            boolean isTerritorial,
            boolean isKnownToPlayer) {

        // The whole score on the size, the shape with the fewest moving parts: no siblings beside
        // it and no garrison bonus folded into it.
        var standingMarket = ClaimMarketFixture
            .startMarket(STANDING_MARKET_NAME)
            .setMarketSize(score)
            .setKnownToPlayer(isKnownToPlayer)
            .buildMarket();

        return new WeighedClaimStanding(factionId, isTerritorial, standingMarket, List.of());
    }

    /**
     * A faction present in a system through one colony the contest never weighed, and so standing
     * at nought however large that colony is.
     *
     * <p>Takes no score, unlike its scored counterpart: a presence-only standing reports a nought
     * of its own, and a builder offering one to state would be offering a number that goes
     * nowhere.
     *
     * @param factionId     the faction the standing belongs to
     * @param isTerritorial whether the faction may claim a system at all - carried, and claiming
     *                      nothing here, since a nought passes no gate either way
     * @return the standing
     */
    public static PresenceOnlyClaimStanding buildPresenceOnlyStanding(
            String factionId,
            boolean isTerritorial) {

        return buildPresenceOnlyStanding(
            factionId,
            isTerritorial,
            UNWEIGHED_MARKET_NAME,
            KNOWN_TO_PLAYER);
    }

    /**
     * The same faction, present through one colony the player knows nothing of - so a presence a
     * display reading the whole contest knows about and may not name.
     *
     * <p>The presence-only standing a fog-of-war filter removes, which is what it is here for. It is
     * not the only standing such a filter reaches: the scored builder above poses a weighed standing
     * over an undiscovered colony, the mechanic weighing colonies nobody has reached. A case about
     * the projection therefore states which kind it is about rather than relying on one being the
     * only shape that can be dropped.
     *
     * @param factionId     the faction the standing belongs to
     * @param isTerritorial whether the faction may claim a system at all - carried, and claiming
     *                      nothing here, since a nought passes no gate either way
     * @return the standing
     */
    public static PresenceOnlyClaimStanding buildUnknownPresenceOnlyStanding(
            String factionId,
            boolean isTerritorial) {

        return buildPresenceOnlyStanding(
            factionId,
            isTerritorial,
            UNKNOWN_MARKET_NAME,
            UNKNOWN_TO_PLAYER);
    }

    // One concealed colony standing for the faction. Concealment rather than an absence from the
    // economy's listing, arbitrarily: the two suppress scoring identically, and a case posing a
    // faction that was never weighed is not about which of them did it. Whether the player has
    // found it is a separate fact - concealment keeps the mechanic from weighing a market, being
    // unknown keeps a display from naming it.
    private static PresenceOnlyClaimStanding buildPresenceOnlyStanding(
            String factionId,
            boolean isTerritorial,
            String marketName,
            boolean isKnownToPlayer) {

        var unweighedMarket = ClaimMarketFixture
            .startMarket(marketName)
            .setAdmission(ContestAdmission.HIDDEN)
            .setKnownToPlayer(isKnownToPlayer)
            .buildMarket();

        return new PresenceOnlyClaimStanding(factionId, isTerritorial, List.of(unweighedMarket));
    }
}
