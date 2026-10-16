package kmlib.starsector.commodities;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the price reads: a non-finite price reads as none, an average leaves such a market out of both its sum and its
 * count, and the local read reaches from the place's market to its location's average to the sector's.
 *
 * <p>Markets quote the selling side; the sides' own mapping is {@link PlayerTradeSideTests}'.
 */
final class CommodityMarketPricesTests {

    private static final String COMMODITY_ID = "supplies";

    // A commodity the market does not list prices at 0, a finite answer.
    private static MarketAPI buildMarketSelling(Map<String, Float> pricesByCommodity) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getDemandPrice(anyString(), eq(1.0), eq(true)))
            .thenReturn(0f);

        for (var price : pricesByCommodity.entrySet()) {

            when(marketMock.getDemandPrice(eq(price.getKey()), eq(1.0), eq(true)))
                .thenReturn(price.getValue());
        }

        return marketMock;
    }

    private static MarketAPI buildMarketSelling(float unitPrice) {

        return buildMarketSelling(Map.of(COMMODITY_ID, unitPrice));
    }

    @Nested
    class FindUnitPrice {

        @Test
        void readsTheMarketsFinitePrice() {

            assertThat(CommodityMarketPrices.findUnitPrice(
                    buildMarketSelling(80f),
                    COMMODITY_ID,
                    PlayerTradeSide.SELLING))
                .contains(80f);
        }

        @Test
        void readsNoPriceFromANonFiniteQuote() {

            assertThat(CommodityMarketPrices.findUnitPrice(
                    buildMarketSelling(Float.NaN),
                    COMMODITY_ID,
                    PlayerTradeSide.SELLING))
                .isEmpty();
        }

        @Test
        void readsNoPriceFromANullMarket() {

            assertThat(CommodityMarketPrices.findUnitPrice(null, COMMODITY_ID, PlayerTradeSide.SELLING))
                .isEmpty();
        }
    }
}
