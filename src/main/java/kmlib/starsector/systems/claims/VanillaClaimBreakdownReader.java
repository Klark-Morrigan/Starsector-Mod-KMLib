package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;

import kmlib.starsector.factions.FactionFlags;
import kmlib.starsector.markets.Markets;
import kmlib.starsector.markets.colonies.Colonies;
import kmlib.starsector.markets.colonies.Colony;
import kmlib.starsector.markets.colonies.KnownColonyReader;
import kmlib.starsector.markets.colonies.SystemColonies;
import kmlib.starsector.systems.SectorPassIndex;
import kmlib.starsector.systems.StarSystems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Vanilla's claim mechanic ({@link Misc#getClaimingFaction}) recomputed in the open, keeping every
 * intermediate value the static throws away - and answering both claim ports off that one
 * computation.
 *
 * <p>Recomputing a mechanic the base game already implements is a liability, so the terms are
 * deliberate: the claimant mirrors {@code Misc.getClaimingFaction} step for step - the same
 * score, the same territoriality gate, and above all the same strictly-greater comparison that
 * leaves a tied contest with whichever market the economy lists first. A winner reported here
 * is the winner the game itself would report, which is what lets it stand as the single source
 * for both a map fill and the text explaining it.
 *
 * <p>{@link ClaimReader} is answered here rather than by a binding of its own: it asks the same
 * question of the same walk, and a class holding this one to put it would state no rule the two
 * ports do not already share.
 *
 * <p>It does not go through {@link #readBreakdown} to do it. The claimant is one faction ID off a
 * walk whose every other product exists to be read by a display - the nameplates, the standings and
 * the order they rank in, what each market reports about the player's knowledge of it - and a caller
 * resolving who claims each system of a sector would build all of that once per system and keep none
 * of it. The two ports therefore share the walk and the comparison that settles it, and part company
 * after: the same split that keeps {@link #readCoreFactionId} from paying for the scoring.
 *
 * <p>What is mirrored is that <em>result</em>, not the market walk reaching it: the standings
 * carry three kinds of market vanilla's own walk drops, none of which can move the winner.
 *
 * <ul>
 *   <li>The player's colonies are scored, forced non-territorial - the mechanic bars the player
 *       from claiming at all, so the standing can never lead, while dropping it as vanilla does
 *       would report a system the player holds a colony in as one they have no presence in.
 *   <li>A hidden market carries no score of its own, as in vanilla, but not for vanilla's reason:
 *       it already enters the contest through the sibling count, so scoring it separately would
 *       count it twice, and since a faction stands on its strongest market alone a large hidden
 *       base would displace the visible colony actually contesting the system. Hiddenness and the
 *       entity's discovery are independent - the mechanic reads only the former.
 *   <li>A colony the economy does not list is carried too, treated exactly as a hidden market is.
 *       Vanilla builds Galatia Academy that way deliberately, and a system account never
 *       mentioning that station in its faction's colours tells a reader less than the map already
 *       shows them. It stays out of the sibling count for the reason it is never scored: admitting
 *       it would raise a real colony's score above the one the game scores it at.
 * </ul>
 *
 * <p>A faction holding nothing but such markets still takes a standing - a
 * {@link PresenceOnlyClaimStanding} at nought - rather than dropping out of the contest for want
 * of anything to stand on, since a station drawn on the map in a faction's colours has to reach
 * the account of who is in the system. The nought is what keeps the widening off the mechanic,
 * the lead changing only on a score strictly greater than nought.
 *
 * <p>The markets themselves come from {@link Colonies} rather than from a walk of this class's
 * own, so what counts as a colony here is what counts as one everywhere else reading the same
 * system - and the condition-only market every uninhabited planet carries, which the widening to
 * off-economy markets would otherwise admit on every surveyed rock, is excluded by that shared
 * rule rather than by a check repeated here. The one such market the shared rule does admit, a
 * decivilised world, reaches the contest as a presence and nothing more: it is unowned and
 * off-economy, so it is never weighed, never counted as a sibling and can move no claimant.
 *
 * <p>A knowledge port is taken alongside and reaches the contest nowhere: it decides only what
 * each market's breakdown reports about the player's knowledge of it, which a display uses to
 * withhold a name. Taken as a port rather than resolved here because knowledge is no fact of the
 * sector's - a derelict or a concealed colony is known only where somebody has seen it, and which
 * shapes are held to that at all is the caller's judgement.
 *
 * <p>That colony read walks the whole system, so a caller reading several surfaces off each system
 * of a sector would pay the walk once per surface. A reader built for such a pass therefore takes
 * the pass's {@link SectorPassIndex} and shares one walk with everything else the pass reads.
 * A reader built without one walks afresh on every ask, which is what a reader outliving any pass
 * has to do: an index is a snapshot of the sector the pass that opened it saw, so the shared
 * instance a hover box holds would otherwise answer off a sector that has since moved on.
 */
public final class VanillaClaimBreakdownReader implements ClaimBreakdownReader, ClaimReader {

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
    private final SectorPassIndex sectorIndex;

    // What the player may be told about the colonies met on the walk. Applied to no part of the
    // contest - see readClaimedMarkets - and spent only so each market's breakdown can say
    // whether a display naming it would be telling the player something they have no way of
    // knowing. An absent port names nothing, rather than a reader silently answering under a rule
    // nobody stated and reporting a derelict as known on a surface built to hide exactly that.
    private final KnownColonyReader knownColonyReader;

    /**
     * A reader with no pass behind it, walking each system afresh on every ask.
     *
     * @param knownColonyReader what this reader asks which of the colonies it meets the player may
     *                          be told about, carried onto each market's breakdown; null names none
     */
    public VanillaClaimBreakdownReader(KnownColonyReader knownColonyReader) {
        this(knownColonyReader, null);
    }

    /**
     * A reader sharing one pass's colony walk, so a system this pass has already read costs
     * nothing to read again.
     *
     * @param knownColonyReader what this reader asks which of the colonies it meets the player may
     *                          be told about, carried onto each market's breakdown; null names none
     * @param sectorIndex       the pass's colony index, discarded with the pass that opened it;
     *                          null reads each system afresh, as the no-index reader does
     */
    public VanillaClaimBreakdownReader(
            KnownColonyReader knownColonyReader,
            SectorPassIndex sectorIndex) {

        this.knownColonyReader = knownColonyReader == null
            ? KnownColonyReader.NOTHING_KNOWN
            : knownColonyReader;

        this.sectorIndex = sectorIndex;
    }

    @Override
    public SystemClaimBreakdown readBreakdown(StarSystemAPI system) {

        if (system == null) {
            return SystemClaimBreakdown.NONE;
        }
        var overrideFactionId = readCoreFactionId(system);
        var claimedMarkets = readClaimedMarkets(readColonies(system), knownColonyReader);

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
    public String readClaimingFactionId(StarSystemAPI system) {

        if (system == null) {
            return SystemClaimBreakdown.NONE.claimantFactionId();
        }
        var overrideFactionId = readCoreFactionId(system);

        // A decree answers before a market is weighed, so nothing beneath it is walked at all.
        if (overrideFactionId != null) {
            return overrideFactionId;
        }
        // Walked under no knowledge at all, which changes no answer: the claimant is resolved off
        // the unfogged set - the mechanic scores colonies nobody has found - and the port reaches
        // only what a market's breakdown reports about the player, which nothing here reads. Asking
        // it anyway would project every colony in the system to build a set this walk never opens.
        return resolveTopTerritorialFactionId(
            readClaimedMarkets(readColonies(system), KnownColonyReader.NOTHING_KNOWN));
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
    //
    // That knowledge is asked of the port over the whole place rather than re-derived per market,
    // because it is no fact a market carries: an abandoned station or a concealed colony is known
    // only where somebody has seen it standing, which is a question about the system. The two
    // reads therefore run over one walk - the contest off the set, the fog off the port's
    // selection from it - so a market can never be scored as present and named as unknown for
    // reasons that disagree.
    private static List<ClaimedMarket> readClaimedMarkets(
            Colonies systemColonies,
            KnownColonyReader knownColonyReader) {

        var colonies = systemColonies.colonies();
        var economyMarkets = selectEconomyListedMarkets(colonies);
        var knownColonies = collectKnownColonies(systemColonies, knownColonyReader);
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
                    knownColonies.contains(colony),
                    claimedMarkets.size() + FIRST_LISTED)));
        }
        return claimedMarkets;
    }

    // The port's selection, held for membership tests rather than walked per market - the walk
    // below asks it once per colony, and a list scan would make that quadratic in a system's
    // markets.
    //
    // Identity rather than equality, as every other market comparison in the contest is: two
    // indistinguishable twin colonies are distinct entries the set kept apart, and an equality
    // test would let one answer the fog question on the other's behalf.
    private static Set<Colony> collectKnownColonies(
            Colonies systemColonies,
            KnownColonyReader knownColonyReader) {

        Set<Colony> knownColonies = Collections.newSetFromMap(new IdentityHashMap<>());

        knownColonies.addAll(knownColonyReader.readKnownColonies(systemColonies));

        return knownColonies;
    }

    // The half of the set the economy itself lists - the only markets vanilla's sibling term is
    // counted over. Taken off the set rather than read from the economy a second time, so the
    // markets counted are exactly the ones numbered above them.
    private static List<MarketAPI> selectEconomyListedMarkets(List<Colony> colonies) {

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
            // the game holds one faction instance per ID, so every market in a group names the
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
    // a box that would rather not name a colony the player knows nothing of reads the flag
    // instead. It is handed in rather than read off the colony because it is the system's answer,
    // not the market's.
    //
    // How the colony is identified - its name, the glyph the map marks its entity with, and the
    // ID that says which colony it actually is - is recorded here rather than looked up by
    // whatever lists it: reading them on the walk that met the market is what stops a second
    // lookup answering for a different one. The ID reaches no term of the arithmetic; it is what
    // lets a display pair this row with whatever else it knows about the same colony, which a
    // display name cannot do - vanilla names a station colony and its defending station alike.
    private static MarketClaimBreakdown computeMarketClaim(
            Colony colony,
            List<MarketAPI> economyMarkets,
            boolean isKnownToPlayer,
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
            market.getId(),
            listingPosition,
            isKnownToPlayer,
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
    private Colonies readColonies(StarSystemAPI system) {

        if (sectorIndex == null) {
            return SystemColonies.readColoniesIn(Global.getSector(), system);
        }
        return sectorIndex.readColoniesIn(system);
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
