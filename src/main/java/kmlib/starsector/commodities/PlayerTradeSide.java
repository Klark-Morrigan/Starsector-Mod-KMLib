package kmlib.starsector.commodities;

import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.StatBonus;

/**
 * Which way the player trades a commodity with a market, and the price the market quotes that way.
 *
 * <p>Vanilla names the two prices from the market's side: its supply price is what it asks when the player buys, and
 * its demand price is what it pays when the player sells. Naming the side from the player's end keeps a caller from
 * reaching for the market's word and getting the opposite trade.
 */
public enum PlayerTradeSide {

    /** The player buys from the market, at its supply price. */
    BUYING {

        @Override
        public float readUnitPrice(MarketAPI market, String commodityId) {

            return market.getSupplyPrice(commodityId, ONE_UNIT, true);
        }

        @Override
        public StatBonus readPlayerPriceMod(CommodityOnMarketAPI commodity) {

            return commodity.getPlayerSupplyPriceMod();
        }
    },

    /** The player sells to the market, at its demand price. */
    SELLING {

        @Override
        public float readUnitPrice(MarketAPI market, String commodityId) {

            return market.getDemandPrice(commodityId, ONE_UNIT, true);
        }

        @Override
        public StatBonus readPlayerPriceMod(CommodityOnMarketAPI commodity) {

            return commodity.getPlayerDemandPriceMod();
        }
    };

    // Vanilla's price for a trade bends with its size; one unit is the price a single trade starts at.
    private static final double ONE_UNIT = 1.0;

    /**
     * The market's price for one unit traded this way, the player's price modifiers included and the counter's tariff
     * not, since the tariff belongs to the submarket the trade goes through.
     *
     * @param market      the market quoting
     * @param commodityId the commodity
     * @return the unit price; vanilla's own answer, which may be non-finite on a market with nothing sensible to quote
     */
    public abstract float readUnitPrice(MarketAPI market, String commodityId);

    /**
     * The modifier vanilla applies to the player's price this way, the one a mod adjusts to move that price.
     *
     * @param commodity the commodity on a market
     * @return the player's price modifier for this side
     */
    public abstract StatBonus readPlayerPriceMod(CommodityOnMarketAPI commodity);
}
