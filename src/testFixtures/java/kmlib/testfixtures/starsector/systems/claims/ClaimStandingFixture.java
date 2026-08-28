package kmlib.testfixtures.starsector.systems.claims;

import kmlib.starsector.entities.EntityNameplate;
import kmlib.starsector.systems.claims.ContestAdmission;
import kmlib.starsector.systems.claims.MarketClaimBreakdown;
import kmlib.starsector.systems.claims.PresenceOnlyClaimStanding;
import kmlib.starsector.systems.claims.WeighedClaimStanding;

import java.util.List;
import java.util.OptionalInt;

/**
 * Builds the standings a claim contest is posed with, for tests about what a contest means
 * rather than about the arithmetic behind it. Shipped from KMLib so both KMLib's and consuming
 * mods' tests state a standing the same way.
 *
 * <p>A standing is a market breakdown plus the faction it belongs to, so a test that cares only
 * about a score would otherwise have to invent a colony to carry it - and every such test would
 * invent a slightly different one. Stating the score directly keeps those cases about the score.
 *
 * <p>Both kinds of standing are built here, since a case posing a contest usually wants the
 * distinction between them and nothing else about either - who was weighed, and who was merely
 * there.
 */
public final class ClaimStandingFixture {

    // The one colony a plainly-scored standing rests on. Named rather than left blank so a box
    // rendering the market list has something to print, and marked with nothing: a test posing
    // standings by score alone is not about how a market line is identified, and a glyph here would
    // put a mark on every box built from this fixture.
    private static final EntityNameplate STANDING_MARKET =
        EntityNameplate.createUnmarkedNameplate("Standing Colony");

    // The one colony a presence-only standing is present through, named apart from the scored one
    // so a box listing both kinds can be read line by line.
    private static final EntityNameplate UNWEIGHED_MARKET =
        EntityNameplate.createUnmarkedNameplate("Unweighed Colony");

    // What a colony the contest never weighed is worth. Its size is never read - the standing
    // reports a nought of its own - so it is stated once here rather than invented per case.
    private static final int UNWEIGHED_MARKET_SIZE = 3;

    // The market carries the whole score on its size, which is the shape with the fewest moving
    // parts: no siblings beside it and no garrison bonus folded into it.
    private static final int NO_SIBLING_MARKETS = 0;

    // Where the market falls in the system's listing. A test posing standings by score alone is
    // not posing a tie, so every standing built here takes the head of the list.
    private static final int FIRST_LISTED = 1;

    // The one colony an unfound presence stands on, named apart again so a case about the fog can
    // be read against the found kinds above it.
    private static final EntityNameplate UNFOUND_MARKET =
        EntityNameplate.createUnmarkedNameplate("Undiscovered Colony");

    // Whether the player has found the colony. Found is what a case posing standings by score alone
    // wants; unfound is the one thing the fog-of-war builder below varies.
    private static final boolean IS_KNOWN_TO_PLAYER = true;
    private static final boolean IS_UNFOUND_BY_PLAYER = false;

    // Which colony each standing rests on. A case about what a contest means is not a case about
    // pairing a row with anything else, so each builder's market is identified once here rather
    // than offered for a caller to vary - distinct per builder, so a box listing several kinds of
    // standing still has one id per line.
    private static final String STANDING_MARKET_ID = "standing_colony";
    private static final String UNWEIGHED_MARKET_ID = "unweighed_colony";
    private static final String UNFOUND_MARKET_ID = "undiscovered_colony";

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

        return buildStandingOnOneMarket(factionId, score, isTerritorial, IS_KNOWN_TO_PLAYER);
    }

    /**
     * The same standing, over a colony the player has or has not found.
     *
     * <p>Both readings are ordinary, and the unfound one is the shape a case about a fog-of-war
     * projection over the scored kind has to pose: the mechanic settles a contest over colonies
     * nobody has reached, so a faction can be weighed on a market no display may name.
     *
     * @param factionId       the faction the standing belongs to
     * @param score           what the contest weighs the faction's presence at
     * @param isTerritorial   whether the faction may claim a system at all
     * @param isKnownToPlayer whether the player has found the colony the standing rests on
     * @return the standing
     */
    public static WeighedClaimStanding buildStandingOnOneMarket(
            String factionId,
            int score,
            boolean isTerritorial,
            boolean isKnownToPlayer) {

        return new WeighedClaimStanding(
            factionId,
            isTerritorial,
            new MarketClaimBreakdown(
                STANDING_MARKET,
                STANDING_MARKET_ID,
                FIRST_LISTED,
                isKnownToPlayer,
                // Held in the open and listed by the economy, which a standing market could not be
                // otherwise in any case: the mechanic lets neither kind stand for a faction.
                // Whether the player has found it is a separate question, and the two do not move
                // together - concealment is what the mechanic skips a market for, being found is
                // what a display may name it on.
                ContestAdmission.WEIGHED,
                score,
                NO_SIBLING_MARKETS,
                OptionalInt.empty()),
            List.of());
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

        return new PresenceOnlyClaimStanding(
            factionId,
            isTerritorial,
            List.of(new MarketClaimBreakdown(
                UNWEIGHED_MARKET,
                UNWEIGHED_MARKET_ID,
                FIRST_LISTED,
                IS_KNOWN_TO_PLAYER,
                // Concealment rather than an absence from the economy's listing, arbitrarily: the
                // two suppress scoring identically, and a test posing a faction that was never
                // weighed is not about which of them did it.
                ContestAdmission.HIDDEN,
                UNWEIGHED_MARKET_SIZE,
                NO_SIBLING_MARKETS,
                OptionalInt.empty())));
    }

    /**
     * The same faction, present through one colony the player has not found - so a presence a
     * display reading the whole contest knows about and may not name.
     *
     * <p>The presence-only standing a fog-of-war filter removes, which is what it is here for. It is
     * not the only standing such a filter reaches: the scored builder above poses a weighed standing
     * the player has not found, the mechanic weighing colonies nobody has reached. A case about the
     * projection therefore states which kind it is about rather than relying on one being the only
     * shape that can be dropped.
     *
     * @param factionId     the faction the standing belongs to
     * @param isTerritorial whether the faction may claim a system at all - carried, and claiming
     *                      nothing here, since a nought passes no gate either way
     * @return the standing
     */
    public static PresenceOnlyClaimStanding buildUnfoundPresenceOnlyStanding(
            String factionId,
            boolean isTerritorial) {

        return new PresenceOnlyClaimStanding(
            factionId,
            isTerritorial,
            List.of(new MarketClaimBreakdown(
                UNFOUND_MARKET,
                UNFOUND_MARKET_ID,
                FIRST_LISTED,
                IS_UNFOUND_BY_PLAYER,
                // Concealed as well as unfound, which is one pairing of two independent facts:
                // concealment is what keeps the mechanic from weighing a market, being unfound is
                // what keeps a display from naming it. Both are posed here because this standing
                // needs the first to be presence-only and the case needs the second.
                ContestAdmission.HIDDEN,
                UNWEIGHED_MARKET_SIZE,
                NO_SIBLING_MARKETS,
                OptionalInt.empty())));
    }
}
