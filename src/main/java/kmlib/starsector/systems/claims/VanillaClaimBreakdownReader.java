package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
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
import java.util.Map;
import java.util.OptionalInt;

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
 * <p>The standings go one market wider again, in the other direction: a colony the economy does
 * not list at all is carried too, treated exactly as a hidden market is - present in its faction's
 * holdings, never scored, never a claimant. Vanilla builds Galatia Academy that way deliberately,
 * and a system account that never mentions the station on screen in a faction's colours is telling
 * a reader less than the map already shows them. It takes no part because vanilla's own walk never
 * reaches it, which is the same reason it is kept out of the sibling count: admitting it there
 * would raise a real colony's score above the one the game scores it at.
 *
 * <p>Scoring walks every market present in a system, so it is the expensive read of the two; the
 * override is a bare memory read and stays cheap.
 */
public final class VanillaClaimBreakdownReader implements ClaimBreakdownReader {

    // A military market weighs far more than any colony's size can reach on its own, which is
    // how a garrison world claims a system its neighbours out-populate. Vanilla's flat bonus.
    private static final int MILITARY_MARKET_BONUS = 10;

    // A contest nobody has yet led. The running maximum starts here and only ever rises on a
    // strictly greater score, so a score of zero can never take a system.
    private static final int NO_LEADING_SCORE = 0;

    // The place the first market in a system's listing takes. Counted from one because the number
    // is stated to the player, who reads a list of markets as first, second, third.
    private static final int FIRST_LISTED = 1;

    // Which of the two reads a run of markets came out of. Named at the call sites rather than
    // worked out per market, the read that answered a market being the whole of what settles it.
    private static final boolean IS_LISTED_BY_ECONOMY = false;
    private static final boolean IS_OFF_ECONOMY = true;

    @Override
    public SystemClaimBreakdown readBreakdown(StarSystemAPI system) {

        if (system == null) {
            return SystemClaimBreakdown.NONE;
        }
        var overrideFactionId = readCoreFactionId(system);
        var claimedMarkets = readClaimedMarkets(system);

        // An override answers the question before any market is weighed, so it stands as the
        // claimant even where the scores point elsewhere; those scores stay on as context.
        var claimantFactionId =
            overrideFactionId != null
                ? overrideFactionId
                : resolveTopTerritorialFactionId(claimedMarkets);

        return new SystemClaimBreakdown(
            overrideFactionId,
            claimantFactionId,
            rankStandings(collectStandings(claimedMarkets)));
    }

    @Override
    public String readCoreFactionId(StarSystemAPI system) {
        return StarSystems.readFactionClaimOverride(system);
    }

    // Every market in the system that belongs to somebody, scored, in the order the economy
    // lists them - the order a tied contest is settled in, so it is the order kept throughout -
    // followed by the markets the economy does not list at all.
    //
    // Two reads rather than one, because the two answer different questions. Every market present
    // is walked, so the account names a colony the player can see on the map; but only the
    // economy's own may feed an arithmetic vanilla runs off the economy, which is why the sibling
    // count below is handed the narrower list at both calls. Widening that count instead would
    // raise a real colony's score over what the game scores it at, and could hand the system to a
    // different faction - a mechanic change wearing a display fix's clothes.
    private static List<ClaimedMarket> readClaimedMarkets(StarSystemAPI system) {

        var sector = Global.getSector();
        var economyMarkets = StarSystems.readMarkets(sector, system);
        var claimedMarkets = new ArrayList<ClaimedMarket>();

        appendClaimedMarkets(claimedMarkets, economyMarkets, economyMarkets, IS_LISTED_BY_ECONOMY);
        appendClaimedMarkets(
            claimedMarkets,
            StarSystems.readMarketsUnlistedByEconomy(sector, system),
            economyMarkets,
            IS_OFF_ECONOMY);

        return claimedMarkets;
    }

    // One walk's worth of markets folded onto the end of the run, each numbered by where it lands
    // in it. Called once per read rather than over a merged list, so which of the two a market came
    // from is settled by the read that answered it - a merged list would have to be compared back
    // against the economy's to recover the same fact, which is that comparison written twice.
    //
    // An unowned market belongs to nobody's standing and the mechanic would throw on one, so it is
    // dropped here rather than guarded against at each later read.
    private static void appendClaimedMarkets(
            List<ClaimedMarket> claimedMarkets,
            List<MarketAPI> markets,
            List<MarketAPI> economyMarkets,
            boolean isOffEconomyMarket) {

        for (var market : markets) {
            var faction = market == null ? null : market.getFaction();

            if (faction == null) {
                continue;
            }
            // The place a market takes is its place among the owned ones, counting from one, rather
            // than its raw index in the listing. The two order every market identically - dropping
            // the unowned ones takes nothing out of order - and the contest is settled on relative
            // order alone, so numbering the markets that take part leaves a run with no gaps in it
            // for a reader to wonder about.
            claimedMarkets.add(new ClaimedMarket(
                faction,
                computeMarketClaim(
                    market,
                    economyMarkets,
                    claimedMarkets.size() + FIRST_LISTED,
                    isOffEconomyMarket)));
        }
    }

    // Vanilla's own pass, market by market in economy order: the running maximum only ever
    // rises on a strictly greater territorial score, which leaves a tie with whichever market
    // the economy listed first.
    //
    // Walked per market rather than over the factions' finished standings, because the two part
    // company - a faction whose strongest market comes late in the listing loses a tie to one
    // that reached the same score earlier, and the mechanic settles it that way round.
    private static String resolveTopTerritorialFactionId(List<ClaimedMarket> claimedMarkets) {

        String topFactionId = null;
        var topScore = NO_LEADING_SCORE;

        for (var claimedMarket : claimedMarkets) {
            if (!claimedMarket.claim().isScoredOnItsOwnAccount()
                    || !isEligibleToClaim(claimedMarket.faction())) {
                continue;
            }
            var score = claimedMarket.claim().computeTotalScore();

            if (score > topScore) {
                topScore = score;
                topFactionId = claimedMarket.faction().getId();
            }
        }
        return topFactionId;
    }

    // One standing per faction holding a market scored on its own account, in the order those
    // markets first appear - which is the order a tie between two equal standings is settled
    // in, so the ranking agrees with the claimant resolved above it.
    private static List<FactionClaimScore> collectStandings(List<ClaimedMarket> claimedMarkets) {

        var standingByFactionId = new LinkedHashMap<String, ClaimedMarket>();

        for (var claimedMarket : claimedMarkets) {
            if (claimedMarket.claim().isScoredOnItsOwnAccount()) {
                recordBestStanding(standingByFactionId, claimedMarket);
            }
        }
        var standings = new ArrayList<FactionClaimScore>(standingByFactionId.size());

        for (var standing : standingByFactionId.values()) {
            standings.add(new FactionClaimScore(
                standing.faction().getId(),
                isEligibleToClaim(standing.faction()),
                standing.claim(),
                selectOtherMarketClaims(claimedMarkets, standing)));
        }
        return standings;
    }

    // A faction stands on its strongest market alone: the mechanic never sums a faction's
    // holdings, it picks a single winning market, so the best score is the faction's standing.
    // Replaced only on a strictly greater score, so a tie among a faction's own markets keeps
    // the one the economy listed first - the rule the contest between factions turns on too.
    private static void recordBestStanding(
            Map<String, ClaimedMarket> standingByFactionId,
            ClaimedMarket claimedMarket) {

        var factionId = claimedMarket.faction().getId();
        var best = standingByFactionId.get(factionId);

        if (best == null
                || claimedMarket.claim().computeTotalScore() > best.claim().computeTotalScore()) {

            standingByFactionId.put(factionId, claimedMarket);
        }
    }

    // The rest of what the faction holds in the system, its standing market aside - hidden
    // markets included, since they are present for the sibling count, and off-economy ones
    // included, since they are present on the map whatever the mechanic made of them. Matched on
    // the same faction identity the sibling count runs on, so an account of the count has the
    // markets it counts listed beneath it.
    //
    // The standing itself is told apart by identity rather than by value, since the walk holds
    // one of these per market: two indistinguishable twin colonies are then each other's
    // sibling, where an equality test would drop both and leave the count unaccounted for.
    private static List<MarketClaimBreakdown> selectOtherMarketClaims(
            List<ClaimedMarket> claimedMarkets,
            ClaimedMarket standing) {

        var otherClaims = new ArrayList<MarketClaimBreakdown>();

        for (var claimedMarket : claimedMarkets) {
            if (claimedMarket != standing && claimedMarket.faction() == standing.faction()) {
                otherClaims.add(claimedMarket.claim());
            }
        }
        return otherClaims;
    }

    // Strongest first, which is the order any reader wants to see a contest in. The sort is
    // stable, so equally-scored factions stay in economy order and the ranking agrees with the
    // tie rule that picked the winner.
    private static List<FactionClaimScore> rankStandings(Collection<FactionClaimScore> standings) {

        var ranked = new ArrayList<>(standings);

        ranked.sort(Comparator
            .comparingInt(FactionClaimScore::score)
            .reversed());

        return ranked;
    }

    // A market's weight in the contest: its size, a point for every other market its faction
    // holds in the same system, and the military bonus. The sibling count runs over the economy's
    // markets alone - hidden and player-owned ones included, because sheer presence is what it
    // measures, but nothing the economy does not list, because that is the set vanilla counts
    // over. Faction identity is compared by reference, as the mechanic compares it; the game
    // holds one instance per faction.
    //
    // Whether the player knows the market exists is recorded beside all that and applied to
    // none of it. The mechanic scores what is there rather than what has been found, so a
    // fog-of-war filter here would resolve a different claimant from the one the game reports;
    // a box that would rather not name an unfound colony reads the flag instead.
    private static MarketClaimBreakdown computeMarketClaim(
            MarketAPI market,
            List<MarketAPI> economyMarkets,
            int listingPosition,
            boolean isOffEconomyMarket) {

        var siblingMarketCount = 0;

        for (var other : economyMarkets) {
            if (other != null && other != market && other.getFaction() == market.getFaction()) {
                siblingMarketCount++;
            }
        }
        return new MarketClaimBreakdown(
            market.getName(),
            listingPosition,
            Markets.isKnownToPlayer(market),
            new ContestAdmission(market.isHidden(), isOffEconomyMarket),
            market.getSize(),
            siblingMarketCount,
            Markets.isMilitary(market)
                ? OptionalInt.of(MILITARY_MARKET_BONUS)
                : OptionalInt.empty());
    }

    // The player is present but ineligible: the mechanic never lets a player colony claim a
    // system, so its standing is territorial by neither configuration nor accident. Read off
    // the faction rather than its .faction file, which is free to declare a territoriality the
    // mechanic will not honour.
    private static boolean isEligibleToClaim(FactionAPI faction) {
        return !faction.isPlayerFaction() && FactionFlags.isTerritorial(faction);
    }

    /**
     * One scored market of the system's economy, kept beside the faction that owns it - the one
     * fact the claim itself cannot carry, being a plain value with no Starsector types in it.
     * Holding the whole walk as these lets the claimant and the standings be resolved from one
     * pass of the economy under their two different rules, without scoring any market twice.
     */
    private record ClaimedMarket(
        FactionAPI faction,
        MarketClaimBreakdown claim) {
    }
}
