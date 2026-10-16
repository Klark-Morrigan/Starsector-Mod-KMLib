package kmlib.starsector.commodities;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.markets.LocationMarkets;
import kmlib.starsector.markets.SectorMarkets;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What markets ask and pay for a commodity: one market's price, an average across markets, and the price at a place
 * reached outward until some market answers.
 *
 * <p>A non-finite price is read as no price. A market with nothing sensible to quote - a demand overflow, a market the
 * economy never ran - can answer one, and a single such answer would poison an average.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and null-defensive like the
 * rest of the library.
 */
public final class CommodityMarketPrices {

    private CommodityMarketPrices() {
    }

    /**
     * The commodity's average unit price across {@code markets}, traded {@code side}.
     *
     * @param markets     the markets to average over
     * @param commodityId the commodity
     * @param side        which way the player trades
     * @return the average, or empty where no market priced the commodity
     */
    public static Optional<Float> findAverageUnitPrice(
            List<MarketAPI> markets,
            String commodityId,
            PlayerTradeSide side) {

        var sum = 0.0;
        var count = 0;

        for (var market : markets) {

            var unitPrice = findUnitPrice(market, commodityId, side);
            if (unitPrice.isPresent()) {
                sum += unitPrice.get();
                count++;
            }
        }

        return count == 0
            ? Optional.empty()
            : Optional.of((float) (sum / count));
    }

    /**
     * The commodity's unit price at a place, reached outward until a market answers: the place's own market while the
     * economy runs it, else the average across its location's markets, else the average across the sector's.
     *
     * <p>The place's own market counts only while the economy lists it. A market the economy never ran - a bare world's
     * placeholder, a market a mod keeps outside the economy - quotes prices no supply or demand stands behind.
     *
     * @param sector      the sector whose economy is read; null skips the location and sector averages
     * @param localMarket the market at the place; null where none stands there
     * @param location    the place's location, such as its star system; null skips the location average
     * @param commodityId the commodity
     * @param side        which way the player trades
     * @return the price and how far the read reached, or empty where no market anywhere priced the commodity
     */
    public static Optional<LocalUnitPrice> findLocalUnitPrice(
            SectorAPI sector,
            MarketAPI localMarket,
            LocationAPI location,
            String commodityId,
            PlayerTradeSide side) {

        if (localMarket != null && localMarket.isInEconomy()) {

            var localPrice = findUnitPrice(localMarket, commodityId, side);
            if (localPrice.isPresent()) {
                return Optional.of(new LocalUnitPrice(localPrice.get(), UnitPriceScope.LOCAL_MARKET));
            }
        }

        var locationAverage = findAverageUnitPrice(
            LocationMarkets.readMarkets(sector, location),
            commodityId,
            side);

        if (locationAverage.isPresent()) {
            return Optional.of(new LocalUnitPrice(locationAverage.get(), UnitPriceScope.LOCATION_AVERAGE));
        }

        return findAverageUnitPrice(SectorMarkets.readMarkets(sector), commodityId, side)
            .map(sectorAverage -> new LocalUnitPrice(sectorAverage, UnitPriceScope.SECTOR_AVERAGE));
    }

    /**
     * One market's unit price for the commodity, traded {@code side}.
     *
     * @param market      the market quoting; null yields empty
     * @param commodityId the commodity
     * @param side        which way the player trades
     * @return the unit price, or empty where the market quotes none that is finite
     */
    public static Optional<Float> findUnitPrice(MarketAPI market, String commodityId, PlayerTradeSide side) {

        if (market == null) {
            return Optional.empty();
        }

        var unitPrice = side.readUnitPrice(market, commodityId);

        return Float.isFinite(unitPrice)
            ? Optional.of(unitPrice)
            : Optional.empty();
    }
}
