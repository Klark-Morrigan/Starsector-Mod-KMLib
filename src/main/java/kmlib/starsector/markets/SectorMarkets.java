package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.List;

/**
 * Queries over the sector's whole set of markets, the economy's listing rather than one location's -
 * the market counterpart of {@link kmlib.starsector.systems.SectorStarSystems}.
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

        var names = new ArrayList<String>();

        for (var market : markets) {

            var name = market.getName();

            if (KmlibStrings.hasText(name)) {
                names.add(name);
            }
        }
        return names;
    }
}
