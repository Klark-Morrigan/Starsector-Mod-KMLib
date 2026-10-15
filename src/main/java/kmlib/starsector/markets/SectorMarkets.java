package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.starsector.SectorWalks;
import kmlib.text.KmlibStrings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Queries over the sector's whole set of markets, rather than one location's - the market
 * counterpart of {@link kmlib.starsector.systems.SectorStarSystems}.
 *
 * <p>Each read says which listing it answers from. {@link #listMarketNames} reads the economy's
 * own list; {@link #readMarketIds} walks every place and reads both of its listings through
 * {@link LocationMarkets#readMarketListings}.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and
 * null-defensive like the rest of the library.
 */
public final class SectorMarkets {

    private SectorMarkets() {
        // utility class, no instances.
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
     * The ID of every market the sector holds anywhere - every star system and hyperspace - for a
     * caller asking whether something it once recorded against a market is still about anything
     * at all.
     *
     * <p>Read off both of a place's listings rather than off a resolved colony set. This asks what
     * the sector holds at all, not what may be shown of it: a colony set settles which of the
     * markets on a place is the colony and drops the rest, and a market superseded today can win
     * its place tomorrow. An ID present under any market anywhere is an ID still worth answering
     * for.
     *
     * <p>Hyperspace is asked after the systems whatever the system list held. The two are separate
     * places, so one being unreadable says nothing about the other.
     *
     * @param sector the sector to walk; null yields nothing, there being no listing to read
     * @return the IDs, in no particular order; never null
     */
    public static Set<String> readMarketIds(SectorAPI sector) {

        var marketIds = new HashSet<String>();

        if (sector == null) {
            return marketIds;
        }
        SectorWalks.walkStarSystems(sector, system -> collectMarketIdsIn(sector, system, marketIds));
        collectMarketIdsIn(sector, sector.getHyperspace(), marketIds);

        return marketIds;
    }

    private static void collectMarketIdsIn(
            SectorAPI sector,
            LocationAPI location,
            Set<String> marketIds) {

        for (var market : LocationMarkets.readMarketListings(sector, location).listPresentMarkets()) {

            var marketId = market.getId();

            // A market the game names with nothing can have nothing recorded against it, a record
            // being filed under the very ID this walk is compared against.
            if (marketId != null) {
                marketIds.add(marketId);
            }
        }
    }
}
