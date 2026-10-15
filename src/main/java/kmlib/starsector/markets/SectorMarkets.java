package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.text.KmlibStrings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Queries over the sector's whole set of markets, the economy's listing rather than one location's -
 * the market counterpart of {@link kmlib.starsector.systems.SectorStarSystems}.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and
 * null-defensive like the rest of the library.
 */
public final class SectorMarkets {

    // Reads only; never instantiated.
    private SectorMarkets() {
    }

    /**
     * Every market's name, blanks left out - for a caller that has to know what a colony may be called
     * before it names one, such as settling the face a colony's name is drawn in.
     *
     * @param sector the sector to read; null, or one with no economy yet, yields an empty list
     * @return one name per named market, in the economy's order
     */
    public static List<String> listMarketNames(SectorAPI sector) {

        if (sector == null || sector.getEconomy() == null) {
            return List.of();
        }

        var markets = sector.getEconomy().getMarketsCopy();

        SectorWalkCounters.countMarketsRead(markets.size());

        return KmlibStrings.collectTexts(markets, MarketAPI::getName);
    }

    /**
     * The ID of every market the sector holds anywhere, every star system first and hyperspace
     * last - for a caller asking whether something it once recorded against a market is still
     * about anything at all.
     *
     * <p>Read off both of a place's listings ({@link LocationMarkets#readMarkets} and
     * {@link LocationMarkets#readMarketsUnlistedByEconomy}) rather than off a resolved colony set.
     * This asks what the sector holds at all, not what may be shown of it: a colony set settles
     * which of the markets on a place is the colony and drops the rest, and a market superseded
     * today can win its place tomorrow. An ID present under any market anywhere is an ID still
     * worth answering for.
     *
     * <p>A sector listing no systems is still asked for its hyperspace. The two are separate places,
     * so one being unreadable says nothing about the other.
     *
     * @param sector the sector to walk; null yields nothing, there being no listing to read
     * @return the IDs, wherever each market stands; never null
     */
    public static Set<String> readMarketIds(SectorAPI sector) {

        var marketIds = new HashSet<String>();

        if (sector == null) {
            return marketIds;
        }

        var systems = sector.getStarSystems();
        if (systems != null) {

            for (var system : systems) {
                collectMarketIdsIn(sector, system, marketIds);
            }

            // The walk goes over the system list itself, so it is counted here or nowhere. The
            // markets it yields are not: each listing counts its own read.
            SectorWalkCounters.countSectorWalk(systems.size());
        }

        collectMarketIdsIn(sector, sector.getHyperspace(), marketIds);

        return marketIds;
    }

    private static void collectMarketIdsIn(
            SectorAPI sector,
            LocationAPI location,
            Set<String> marketIds) {

        collectMarketIds(LocationMarkets.readMarkets(sector, location), marketIds);
        collectMarketIds(LocationMarkets.readMarketsUnlistedByEconomy(sector, location), marketIds);
    }

    private static void collectMarketIds(List<MarketAPI> markets, Set<String> marketIds) {

        for (var market : markets) {

            var marketId = market.getId();

            // A market the game names with nothing can have nothing recorded against it, a record
            // being filed under the very ID this walk is compared against.
            if (marketId != null) {
                marketIds.add(marketId);
            }
        }
    }
}
