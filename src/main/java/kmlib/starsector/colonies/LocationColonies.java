package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.markets.LocationMarkets;
import kmlib.starsector.markets.MarketColocation;
import kmlib.starsector.markets.Markets;

import java.util.ArrayList;
import java.util.List;

/**
 * Selecting the owned colonies in one place, whatever kind of place it is.
 *
 * <p>The search {@link Colonies} and {@link HyperspaceColonies} are each a thin reader
 * over. Nothing about picking colonies out of a place is system-specific - both listings are
 * read, ownership rejects the condition-only markets, and the survivors resolve to one entry
 * per place and owner - so the rule is stated once here and the two kinds of place are named
 * separately above it. A single read taking a kind of place as an argument would instead put
 * the burden on every caller to know which kinds it may pass.
 *
 * <p>Not a reader in its own right. A caller knows whether it is asking about a star system or
 * about hyperspace, and asking through the one that says so is what keeps a hyperspace colony
 * from arriving somewhere only systems were expected.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state.
 */
public final class LocationColonies {

    private LocationColonies() {
        // utility class, no instances.
    }

    /**
     * Reads {@code location}'s colonies out of the sector - the single walk every reader above
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
     * @param sector   the sector whose economy and locations are read; null yields an empty set,
     *                 there being no listing to select from
     * @param location the location to read; null yields an empty set
     * @return the location's colonies, economy-listed ones first in economy order
     */
    public static Colonies readColoniesIn(SectorAPI sector, LocationAPI location) {

        var listedMarkets = LocationMarkets.readMarkets(sector, location);
        var ownedMarkets = new ArrayList<MarketAPI>();

        collectOwnedColonies(listedMarkets, ownedMarkets);
        collectOwnedColonies(
            LocationMarkets.readMarketsUnlistedByEconomy(sector, location),
            ownedMarkets);

        var colonies = new ArrayList<Colony>();

        for (var market : MarketColocation.readLargestMarketsPerFaction(ownedMarkets)) {

            // Kind is resolved off the market that won the place, so a colony superseded by a
            // larger market on its entity is classified as whatever the winner is - not as
            // whatever the loser was.
            colonies.add(new Colony(
                market,
                ColonyKind.resolveKind(market),
                isListedByEconomy(listedMarkets, market)));
        }
        return new Colonies(colonies);
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
