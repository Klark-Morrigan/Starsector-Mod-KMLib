package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;

import kmlib.starsector.factions.FactionFlags;
import kmlib.starsector.markets.Markets;
import kmlib.starsector.systems.StarSystems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * {@link ClaimBreakdownReader} binding that recomputes vanilla's claim mechanic
 * ({@link Misc#getClaimingFaction}) in the open, keeping every intermediate value the static
 * throws away.
 *
 * <p>Recomputing a mechanic the base game already implements is a liability, so the terms are
 * deliberate: the claimant mirrors {@code Misc.getClaimingFaction} step for step - the same
 * score, the same territoriality gate, and above all the same strictly-greater comparison that
 * leaves a tied contest with whichever market the economy lists first. A winner reported here
 * is the winner the game itself would report, which is what lets it stand as the single source
 * for both a map fill and the text explaining it.
 *
 * <p>What is mirrored is that <em>result</em>, not the market walk that reaches it, and the
 * standings go one market wider than vanilla scores: the player's colonies are scored, forced
 * non-territorial. The mechanic bars the player from claiming at all, so a standing that can
 * never be territorial cannot move the winner - while dropping it, as vanilla does, would
 * report a system the player holds a colony in as one they have no presence in.
 *
 * <p>A hidden market gets no standing of its own, as in vanilla, and that exclusion is not the
 * same call: hidden markets already enter the contest through the sibling count, so scoring one
 * separately would count it twice, and since a faction stands on its strongest market alone a
 * large hidden base would displace the visible colony actually contesting the system. Note that
 * this is not about whether the player has found it - a market's hiddenness and its entity's
 * discovery are independent, and the mechanic reads only the former.
 *
 * <p>Scoring walks the whole economy of a system, so it is the expensive read of the two; the
 * override is a bare memory read and stays cheap.
 */
public final class VanillaClaimBreakdownReader implements ClaimBreakdownReader {

    // A military market weighs far more than any colony's size can reach on its own, which is
    // how a garrison world claims a system its neighbours out-populate. Vanilla's flat bonus.
    private static final int MILITARY_MARKET_BONUS = 10;

    @Override
    public SystemClaimBreakdown readBreakdown(StarSystemAPI system) {

        if (system == null) {
            return SystemClaimBreakdown.NONE;
        }
        var overrideFactionId = readCoreFactionId(system);
        var markets = StarSystems.readMarkets(Global.getSector(), system);
        var bestScoreByFactionId = new LinkedHashMap<String, FactionClaimScore>();

        String topTerritorialFactionId = null;

        // The running maximum starts at 0 and only ever rises on a strictly greater
        // territorial score. Two consequences are load-bearing and mirrored on purpose: a tie
        // leaves the market the economy listed first in front, and a score of zero can never
        // take a system.
        var topScore = 0;

        for (var market : markets) {
            var faction = market == null ? null : market.getFaction();

            // A hidden market is counted through its siblings rather than on its own account,
            // so scoring it here would count it a second time.
            if (faction == null || market.isHidden()) {
                continue;
            }
            // The player is present but ineligible: the mechanic never lets a player colony
            // claim a system, so its standing is territorial by neither configuration nor
            // accident. Read off the faction rather than its .faction file, which is free to
            // declare a territoriality the mechanic will not honour.
            var isTerritorial =
                !faction.isPlayerFaction() && FactionFlags.isTerritorial(faction);

            var standing = new FactionClaimScore(
                faction.getId(),
                computeMarketScore(market, markets),
                isTerritorial);

            recordBestScore(bestScoreByFactionId, standing);

            if (standing.isTerritorial() && standing.score() > topScore) {
                topScore = standing.score();
                topTerritorialFactionId = standing.factionId();
            }
        }
        // An override answers the question before any market is weighed, so it stands as the
        // claimant even where the scores point elsewhere; those scores stay on as context.
        var claimantFactionId =
            overrideFactionId != null
                ? overrideFactionId
                : topTerritorialFactionId;

        return new SystemClaimBreakdown(
            overrideFactionId,
            claimantFactionId,
            rankScores(bestScoreByFactionId.values()));
    }

    @Override
    public String readCoreFactionId(StarSystemAPI system) {
        return StarSystems.readFactionClaimOverride(system);
    }

    // A faction stands on its strongest market alone: the mechanic never sums a faction's
    // holdings, it picks a single winning market, so the best score is the faction's standing.
    // Keeping the first entry's position means the map preserves first-appearance order, which
    // is the order ties have to resolve in.
    private static void recordBestScore(
            LinkedHashMap<String, FactionClaimScore> bestByFactionId,
            FactionClaimScore standing) {

        var best = bestByFactionId.get(standing.factionId());
        if (best == null || standing.score() > best.score()) {
            bestByFactionId.put(
                standing.factionId(),
                standing);
        }
    }

    // Strongest first, which is the order any reader wants to see a contest in. The sort is
    // stable, so equally-scored factions stay in economy order and the ranking agrees with the
    // tie rule that picked the winner.
    private static List<FactionClaimScore> rankScores(Collection<FactionClaimScore> scores) {

        var ranked = new ArrayList<>(scores);

        ranked.sort(Comparator
            .comparingInt(FactionClaimScore::score)
            .reversed());

        return ranked;
    }

    // A market's weight in the contest: its size, a point for every other market its faction
    // holds in the same system, and the military bonus. The sibling count runs over every
    // market present - hidden and player-owned ones included - because sheer presence is what
    // it measures, not who is eligible to claim. Faction identity is compared by reference,
    // as the mechanic compares it; the game holds one instance per faction.
    private static int computeMarketScore(MarketAPI market, List<MarketAPI> systemMarkets) {

        var score = market.getSize();
        
        for (var other : systemMarkets) {
            if (other != null && other != market && other.getFaction() == market.getFaction()) {
                score++;
            }
        }
        return Markets.isMilitary(market)
            ? score + MILITARY_MARKET_BONUS
            : score;
    }
}
