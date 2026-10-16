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
