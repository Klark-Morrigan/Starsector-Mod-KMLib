package kmlib.starsector.commodities;

import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.StatBonus;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins each side to the market's price and modifier for that trade: the player buys at the supply price and sells at
 * the demand price, both for one unit with the player's modifiers included.
 */
final class PlayerTradeSideTests {

    @Nested
    class ReadUnitPrice {

        @Test
        void readsTheSupplyPriceWhenBuying() {

            var marketMock = mock(MarketAPI.class);

            when(marketMock.getSupplyPrice("supplies", 1.0, true))
                .thenReturn(120f);

            assertThat(PlayerTradeSide.BUYING.readUnitPrice(marketMock, "supplies"))
                .isEqualTo(120f);
        }

        @Test
        void readsTheDemandPriceWhenSelling() {

            var marketMock = mock(MarketAPI.class);

            when(marketMock.getDemandPrice("supplies", 1.0, true))
                .thenReturn(80f);

            assertThat(PlayerTradeSide.SELLING.readUnitPrice(marketMock, "supplies"))
                .isEqualTo(80f);
        }
    }

    @Nested
    class ReadPlayerPriceMod {

        @Test
        void readsTheSupplyModifierWhenBuying() {

            var commodityMock = mock(CommodityOnMarketAPI.class);
            var supplyPriceMod = new StatBonus();

            when(commodityMock.getPlayerSupplyPriceMod())
                .thenReturn(supplyPriceMod);

            assertThat(PlayerTradeSide.BUYING.readPlayerPriceMod(commodityMock))
                .isSameAs(supplyPriceMod);
        }

        @Test
        void readsTheDemandModifierWhenSelling() {

            var commodityMock = mock(CommodityOnMarketAPI.class);
            var demandPriceMod = new StatBonus();

            when(commodityMock.getPlayerDemandPriceMod())
                .thenReturn(demandPriceMod);

            assertThat(PlayerTradeSide.SELLING.readPlayerPriceMod(commodityMock))
                .isSameAs(demandPriceMod);
        }
    }
}
