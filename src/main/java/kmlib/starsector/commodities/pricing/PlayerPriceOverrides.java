package kmlib.starsector.commodities.pricing;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.commodities.CommodityMarketPrices;
import kmlib.starsector.commodities.PlayerTradeSide;

import java.util.Optional;

/**
 * Setting the price a market quotes the player for a commodity to a chosen figure, whatever the market would quote on
 * its own.
 *
 * <p>The only lever is the market's player price modifier, which bends vanilla's own price rather than replacing it.
 * So the override reads that price with its own entry removed, then writes the entry that carries it to the target. A
 * flat entry of the target alone would add it to vanilla's price instead.
 *
 * <p>The figure holds for a trade of one unit. Vanilla bends a larger trade's price along its own curve, and the
 * override moves that curve rather than flattening it.
 *
 * <p>Each override is filed under the caller's source key, so a caller clears its own and leaves every other mod's
 * entries standing.
 */
public final class PlayerPriceOverrides {

    private PlayerPriceOverrides() {
    }

    /**
     * Sets the market's unit price for the commodity, traded {@code side}, to {@code targetUnitPrice}. Calling it again
     * replaces the earlier override rather than stacking on it.
     *
     * <p>Does nothing, and answers empty, where the market holds no data for the commodity, quotes it no finite price,
     * or multiplies the player's price to nothing - none of which an added amount can move.
     *
     * @param market          the market quoting; null does nothing
     * @param commodityId     the commodity
     * @param side            which way the player trades
     * @param sourceKey       the caller's key for the modifier entry
     * @param targetUnitPrice the unit price to quote
     * @return the unit price the market quoted on its own, or empty where nothing was overridden
     */
    public static Optional<Float> overrideUnitPrice(
            MarketAPI market,
            String commodityId,
            PlayerTradeSide side,
            String sourceKey,
            float targetUnitPrice) {

        if (market == null || market.getCommodityData(commodityId) == null) {
            return Optional.empty();
        }

        var priceMod = side.readPlayerPriceMod(market.getCommodityData(commodityId));

        priceMod.unmodifyFlat(sourceKey);

        var ownUnitPrice = CommodityMarketPrices.findUnitPrice(market, commodityId, side);
        var mult = priceMod.getMult();

        if (ownUnitPrice.isEmpty() || mult == 0f) {
            return Optional.empty();
        }

        // Vanilla multiplies after adding, so the entry is the gap divided by the multiplier.
        priceMod.modifyFlat(sourceKey, (targetUnitPrice - ownUnitPrice.get()) / mult);

        return ownUnitPrice;
    }

    /**
     * Removes the caller's override, so the market quotes its own price again.
     *
     * @param market      the market quoting; null does nothing
     * @param commodityId the commodity
     * @param side        which way the player trades
     * @param sourceKey   the key the override was filed under
     */
    public static void clearUnitPriceOverride(
            MarketAPI market,
            String commodityId,
            PlayerTradeSide side,
            String sourceKey) {

        if (market == null || market.getCommodityData(commodityId) == null) {
            return;
        }

        side.readPlayerPriceMod(market.getCommodityData(commodityId))
            .unmodifyFlat(sourceKey);
    }
}
