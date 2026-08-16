package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.markets.Markets;

import java.util.ArrayList;
import java.util.List;

/**
 * Every owned colony in one star system, selected by one rule, one entry per place and owner.
 *
 * <p>The set exists because "what colonies are in this system" was being answered
 * independently by every reader that asked it - one walking the economy, one walking the
 * entities, one doing both and concatenating - so two surfaces drawn from the same system
 * could disagree about who is present in it. Selecting once and handing the result down means
 * a disagreement is no longer expressible.
 *
 * <p>Both listings are read: the economy's, then the markets hung on the system's entities
 * that it does not list. Economy order is preserved ahead of the rest, because a caller
 * mirroring vanilla's claim mechanic settles a tied contest on whichever market the economy
 * reaches first, and an order imposed here would resolve a different winner.
 *
 * <p>The condition-only market every uninhabited planet carries is what the ownership rule
 * exists to reject. Widening to off-economy markets without it would admit one per surveyed
 * rock in the system, each owned by nobody, and every reader below would report the system as
 * settled by a faction that is not there.
 *
 * <p>The set is unfogged: it holds every owned colony present, found or not. That is
 * deliberate, because a mechanic mirrored from vanilla has to see what vanilla sees - claim
 * scoring weighs colonies the player has never found, and a fogged input would resolve a
 * different claimant. What the player may be shown is {@link #readKnownColonies}, one named
 * projection over the set rather than a filter each display reader applies for itself.
 */
public record SystemColonies(
    List<SystemColony> colonies) {

    /** A system with nobody in it, and the answer for a system that cannot be read at all. */
    public static final SystemColonies NONE = new SystemColonies(List.of());

    /**
     * Takes an immutable copy of the colonies, and reads a null list as an empty one, so a set
     * handed around a render pass cannot change under its readers.
     */
    public SystemColonies {
        colonies = colonies == null ? List.of() : List.copyOf(colonies);
    }

    /**
     * Reads {@code system}'s colonies out of the sector - the single walk every reader below
     * is meant to share rather than repeat.
     *
     * <p>Both listings are filtered for ownership before they are resolved per place, so a
     * planet's condition-only market can never win a place from the real colony sharing its
     * entity by being the larger of the two.
     *
     * <p>The two listings are resolved together in one pass rather than each on its own. The
     * unlisted read already excludes anything naming a place the economy lists, so the two
     * cannot collide today; resolving jointly makes the "one entry per place and owner"
     * guarantee this read's own rather than one inherited from that exclusion holding.
     *
     * @param sector the sector whose economy and systems are read; null yields an empty set,
     *               there being no listing to select from
     * @param system the system to read; null yields an empty set
     * @return the system's colonies, economy-listed ones first in economy order
     */
    public static SystemColonies readColoniesIn(SectorAPI sector, StarSystemAPI system) {

        var listedMarkets = StarSystems.readMarkets(sector, system);
        var ownedMarkets = new ArrayList<MarketAPI>();

        collectOwnedColonies(listedMarkets, ownedMarkets);
        collectOwnedColonies(
            StarSystems.readMarketsUnlistedByEconomy(sector, system),
            ownedMarkets);

        var colonies = new ArrayList<SystemColony>();

        for (var market : Markets.readLargestMarketsPerFaction(ownedMarkets)) {

            colonies.add(new SystemColony(market, isListedByEconomy(listedMarkets, market)));
        }
        return new SystemColonies(colonies);
    }

    /**
     * The colonies the player may be shown - the fogged projection of the set, and the one
     * every display reader is meant to take rather than filtering the whole set itself.
     *
     * <p>Named once here so "what counts on the map" cannot drift between the cell that paints
     * a system, the ribbon that counts in it and the box that names its factions. The dev
     * reveal is a parameter rather than a settings read, so the projection stays free of any
     * one mod's knobs and a pass resolves its reveal once.
     *
     * @param shouldIncludeUndiscoveredMarkets whether an undiscovered colony still counts (the
     *                                         "show all factions" dev reveal); false applies
     *                                         the normal known-to-player filter
     * @return the colonies passing {@link Markets#isCountedAsColony}, in the set's own order
     */
    public List<SystemColony> readKnownColonies(boolean shouldIncludeUndiscoveredMarkets) {

        var knownColonies = new ArrayList<SystemColony>();

        for (var colony : colonies) {
            
            if (Markets.isCountedAsColony(colony.market(), shouldIncludeUndiscoveredMarkets)) {
                knownColonies.add(colony);
            }
        }
        return List.copyOf(knownColonies);
    }

    // Appends the owned colonies among a listing, in the order the listing gives them. Kept as
    // one pass over both listings so the ownership rule is stated once rather than per listing.
    private static void collectOwnedColonies(
            List<MarketAPI> markets,
            List<MarketAPI> ownedMarkets) {

        for (var market : markets) {
            if (Markets.isOwnedColony(market)) {
                ownedMarkets.add(market);
            }
        }
    }

    // Whether this very market object is one the economy listed. Identity rather than equality:
    // a mod's supplementary market on a listed station is a different colony candidate, not the
    // listed one, and a market overriding equals must not be able to claim the economy's place.
    private static boolean isListedByEconomy(List<MarketAPI> listedMarkets, MarketAPI market) {

        for (var listedMarket : listedMarkets) {
            if (listedMarket == market) {
                return true;
            }
        }
        return false;
    }
}
