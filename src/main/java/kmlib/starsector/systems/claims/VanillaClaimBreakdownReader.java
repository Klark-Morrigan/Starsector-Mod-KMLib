package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;

import kmlib.starsector.factions.FactionFlags;
import kmlib.starsector.markets.Markets;
import kmlib.starsector.systems.StarSystems;
import kmlib.starsector.systems.SystemColonies;
import kmlib.starsector.systems.SystemColoniesIndex;
import kmlib.starsector.systems.SystemColony;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

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
 * <p>A hidden market carries no score of its own, as in vanilla, and that exclusion is not the
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
 * <p>A faction holding nothing but such markets still takes a standing - a
 * {@link PresenceOnlyClaimStanding} at nought - rather than dropping out of the contest for want
 * of anything to stand on. That is the last step of the same widening: a station drawn on the map
 * in a faction's colours has to reach the account of who is in the system, and the faction it
 * belongs to is what carries it there. The nought is what keeps the widening off the mechanic,
 * the lead changing only on a score strictly greater than nought.
 *
 * <p>The markets themselves come from {@link SystemColonies} rather than from a walk of this
 * class's own, so what counts as a colony here is what counts as one everywhere else reading the
 * same system - and the condition-only market every uninhabited planet carries, which a widening
 * to off-economy markets would otherwise admit on every surveyed rock, is excluded by that shared
 * rule rather than by a test repeated here.
 *
 * <p>That read walks the whole system, so it is the expensive half of the two reads below; the
 * override is a bare memory read and stays cheap. A caller that walks a sector reads several
 * surfaces off each system and would pay that walk once per surface, so a reader built for such a
 * pass takes the pass's {@link SystemColoniesIndex} and shares the one walk with everything else
 * the pass reads.
 *
 * <p>A reader built without one walks afresh on every ask, and that is the case the second
 * constructor exists for rather than an oversight. An index is a snapshot of the sector the pass
 * that opened it saw; a reader outliving any one pass - the shared instance a hover box holds, for
 * one - would go on answering off a sector that has since moved on. Paying the walk is the honest
 * price of having no pass to belong to.
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

    // The pass's shared colony walk, or null for a reader no pass owns. Nullable rather than
    // split into two types because the two readers differ in nothing a caller can see: same
    // contest, same claimant, same standings, off a walk that is either shared or repeated.
    private final SystemColoniesIndex coloniesIndex;

    /**
     * A reader with no pass behind it, walking each system afresh on every ask.
     *
     * <p>What a long-lived reader has to take: an instance kept past the pass that built it
     * would answer off a snapshot nothing refreshes, so one that cannot be discarded with a
     * pass must not hold one.
     */
    public VanillaClaimBreakdownReader() {
        this(null);
    }

    /**
     * A reader sharing one pass's colony walk, so a system this pass has already read costs
     * nothing to read again.
     *
     * @param coloniesIndex the pass's colony index, discarded with the pass that opened it;
     *                      null reads each system afresh, as the no-index reader does
     */
    public VanillaClaimBreakdownReader(SystemColoniesIndex coloniesIndex) {
        this.coloniesIndex = coloniesIndex;
    }

    @Override
    public SystemClaimBreakdown readBreakdown(StarSystemAPI system) {

        if (system == null) {
            return SystemClaimBreakdown.NONE;
        }
        var overrideFactionId = readCoreFactionId(system);
        var claimedMarkets = readClaimedMarkets(readColonies(system));

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

    // The system's colonies as the contest meets them: the shared set, each numbered by where it
    // falls in it, paired with the faction that owns it.
    //
    // The set already answers what the walk used to work out for itself - which colonies belong to
    // somebody, which of two markets on one entity speaks for the place, and the economy's own
    // order ahead of what the economy does not list - so all that is left here is the fact the set
    // carries but does not apply: only the economy's own markets may feed an arithmetic vanilla
    // runs off the economy, which is why the sibling count below is handed the narrower list.
    // Widening that count instead would raise a real colony's score over what the game scores it
    // at, and could hand the system to a different faction - a mechanic change wearing a display
    // fix's clothes.
    //
    // The whole set is read rather than the known projection over it, because vanilla scores
    // colonies the player has never found: fogging the input here would resolve a claimant the
    // game itself would not report. Whether the player knows of a colony rides each market instead,
    // for a display to withhold.
    private static List<ClaimedMarket> readClaimedMarkets(SystemColonies systemColonies) {

        var colonies = systemColonies.colonies();
        var economyMarkets = selectEconomyListedMarkets(colonies);
        var claimedMarkets = new ArrayList<ClaimedMarket>(colonies.size());

        for (var colony : colonies) {
            // The place a colony takes counts from one across the whole set, since the contest is
            // settled on relative order alone and a run with no gaps in it leaves the player
            // nothing to wonder about when the number is stated back to them.
            claimedMarkets.add(new ClaimedMarket(
                colony.market().getFaction(),
                computeMarketClaim(
                    colony,
                    economyMarkets,
                    claimedMarkets.size() + FIRST_LISTED)));
        }
        return claimedMarkets;
    }

    // The half of the set the economy itself lists - the only markets vanilla's sibling term is
    // counted over. Taken off the set rather than read from the economy a second time, so the
    // markets counted are exactly the ones numbered above them.
    private static List<MarketAPI> selectEconomyListedMarkets(List<SystemColony> colonies) {

        var economyMarkets = new ArrayList<MarketAPI>(colonies.size());

        for (var colony : colonies) {
            if (colony.isListedByEconomy()) {
                economyMarkets.add(colony.market());
            }
        }
        return economyMarkets;
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

    // Every faction present, weighed ones first: one standing per faction holding a market scored
    // on its own account, in the order those markets first appear - which is the order a tie
    // between two equal standings is settled in, so the ranking agrees with the claimant resolved
    // above it - then one apiece for the factions no market was scored for.
    //
    // The two folds are kept in that order because the ranking below sorts stably on the score
    // alone: a presence-only standing is worth nought, so putting them after the weighed ones is
    // what settles them at the foot without the comparison having to know the kinds apart.
    private static List<FactionClaimStanding> collectStandings(List<ClaimedMarket> claimedMarkets) {

        var standingByFactionId = new LinkedHashMap<String, ClaimedMarket>();

        for (var claimedMarket : claimedMarkets) {
            if (claimedMarket.claim().isScoredOnItsOwnAccount()) {
                recordBestStanding(standingByFactionId, claimedMarket);
            }
        }
        var standings = new ArrayList<FactionClaimStanding>();

        for (var standing : standingByFactionId.values()) {
            standings.add(new WeighedClaimStanding(
                standing.faction().getId(),
                isEligibleToClaim(standing.faction()),
                standing.claim(),
                selectOtherMarketClaims(claimedMarkets, standing)));
        }
        standings.addAll(
            collectPresenceOnlyStandings(claimedMarkets, standingByFactionId.keySet()));

        return standings;
    }

    // One standing per faction the fold above found no scored market for: present in the system
    // through concealed or unregistered colonies alone, which the mechanic carries without ever
    // weighing. Such a faction has nothing that could stand for it, so it is listed for what it
    // holds - a station the map draws in its colours has to reach the account of the system.
    //
    // Grouped in listing order, so the markets beneath a standing read in the order the contest
    // walked them and two such factions settle between themselves the way every other tie does.
    private static List<PresenceOnlyClaimStanding> collectPresenceOnlyStandings(
            List<ClaimedMarket> claimedMarkets,
            Set<String> weighedFactionIds) {

        var marketsByFactionId = new LinkedHashMap<String, List<ClaimedMarket>>();

        for (var claimedMarket : claimedMarkets) {
            var factionId = claimedMarket.faction().getId();

            if (!weighedFactionIds.contains(factionId)) {
                marketsByFactionId
                    .computeIfAbsent(factionId, id -> new ArrayList<>())
                    .add(claimedMarket);
            }
        }
        var standings = new ArrayList<PresenceOnlyClaimStanding>(marketsByFactionId.size());

        for (var factionMarkets : marketsByFactionId.values()) {
            // Read off the first of the faction's markets rather than carried alongside the group:
            // the game holds one faction instance per id, so every market in a group names the
            // same object and a second source for it could only ever be a way to disagree.
            var faction = factionMarkets.get(0).faction();

            standings.add(new PresenceOnlyClaimStanding(
                faction.getId(),
                isEligibleToClaim(faction),
                factionMarkets.stream().map(ClaimedMarket::claim).toList()));
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
    private static List<FactionClaimStanding> rankStandings(
            List<FactionClaimStanding> standings) {

        var ranked = new ArrayList<>(standings);

        ranked.sort(Comparator
            .comparingInt(FactionClaimStanding::score)
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
    //
    // How the colony is identified - its name and the glyph the map marks its entity with - is
    // recorded here rather than looked up by whatever lists it: reading the pair on the walk that
    // met the market is what stops a second lookup answering for a different one.
    private static MarketClaimBreakdown computeMarketClaim(
            SystemColony colony,
            List<MarketAPI> economyMarkets,
            int listingPosition) {

        var market = colony.market();
        var siblingMarketCount = 0;

        for (var other : economyMarkets) {
            if (other != market && other.getFaction() == market.getFaction()) {
                siblingMarketCount++;
            }
        }
        return new MarketClaimBreakdown(
            Markets.readNameplate(market),
            listingPosition,
            colony.isKnownToPlayer(),
            new ContestAdmission(colony.isHidden(), !colony.isListedByEconomy()),
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

    // The system's colonies, off the pass's index where a pass owns this reader and by a walk of
    // its own where none does. The whole set either way, so which of the two answered it can
    // change nothing but what the answer cost: an index memoises the very read below it.
    private SystemColonies readColonies(StarSystemAPI system) {

        if (coloniesIndex == null) {
            return SystemColonies.readColoniesIn(Global.getSector(), system);
        }
        return coloniesIndex.readColoniesIn(system);
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
