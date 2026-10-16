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

    @Nested
    class FindAverageUnitPrice {

        @Test
        void takesTheMeanAcrossMarkets() {

            var markets = List.of(buildMarketSelling(100f), buildMarketSelling(200f), buildMarketSelling(300f));

            assertThat(CommodityMarketPrices.findAverageUnitPrice(markets, COMMODITY_ID, PlayerTradeSide.SELLING))
                .contains(200f);
        }

        // (100 + 300) / 2: the NaN counts in neither the sum nor the count.
        @Test
        void skipsANonFinitePriceWithoutPoisoningTheMean() {

            var markets = List.of(
                buildMarketSelling(Float.NaN),
                buildMarketSelling(100f),
                buildMarketSelling(300f));

            assertThat(CommodityMarketPrices.findAverageUnitPrice(markets, COMMODITY_ID, PlayerTradeSide.SELLING))
                .contains(200f);
        }

        @Test
        void answersNothingWithoutMarkets() {

            assertThat(CommodityMarketPrices.findAverageUnitPrice(List.of(), COMMODITY_ID, PlayerTradeSide.SELLING))
                .isEmpty();
        }
    }

    @Nested
    class ComputeAverageUnitPrices {

        @Test
        void pricesEachCommodityAskedFor() {

            var markets = List.of(
                buildMarketSelling(Map.of("ore", 100f, "organics", 60f)),
                buildMarketSelling(Map.of("ore", 300f, "organics", 80f)));

            assertThat(CommodityMarketPrices.computeAverageUnitPrices(
                    markets,
                    Set.of("ore", "organics"),
                    PlayerTradeSide.SELLING))
                .containsOnly(entry("ore", 200f), entry("organics", 70f));
        }

        @Test
        void leavesOutACommodityNoMarketPriced() {

            assertThat(CommodityMarketPrices.computeAverageUnitPrices(
                    List.of(buildMarketSelling(Map.of("ore", Float.NaN))),
                    Set.of("ore"),
                    PlayerTradeSide.SELLING))
                .isEmpty();
        }
    }

    @Nested
    class FindLocalUnitPrice {

        private EconomyAPI economyMock;
        private LocationAPI locationMock;
        private SectorAPI sectorMock;

        @BeforeEach
        void setUp() {

            economyMock = mock(EconomyAPI.class);
            locationMock = mock(LocationAPI.class);
            sectorMock = mock(SectorAPI.class);

            var locationMarkets = List.of(buildMarketSelling(90f), buildMarketSelling(110f));
            var sectorMarkets = List.of(buildMarketSelling(40f), buildMarketSelling(60f));

            when(sectorMock.getEconomy())
                .thenReturn(economyMock);
            when(economyMock.getMarkets(locationMock))
                .thenReturn(locationMarkets);
            when(economyMock.getMarketsCopy())
                .thenReturn(sectorMarkets);
        }

        private MarketAPI buildLocalMarketSelling(float unitPrice, boolean isInEconomy) {

            var marketMock = buildMarketSelling(unitPrice);

            when(marketMock.isInEconomy())
                .thenReturn(isInEconomy);

            return marketMock;
        }

        @Test
        void readsTheLocalMarketTheEconomyRuns() {

            assertThat(CommodityMarketPrices.findLocalUnitPrice(
                    sectorMock,
                    buildLocalMarketSelling(75f, true),
                    locationMock,
                    COMMODITY_ID,
                    PlayerTradeSide.SELLING))
                .contains(new LocalUnitPrice(75f, UnitPriceScope.LOCAL_MARKET));
        }

        @Test
        void averagesTheLocationPastALocalMarketOutsideTheEconomy() {

            assertThat(CommodityMarketPrices.findLocalUnitPrice(
                    sectorMock,
                    buildLocalMarketSelling(75f, false),
                    locationMock,
                    COMMODITY_ID,
                    PlayerTradeSide.SELLING))
                .contains(new LocalUnitPrice(100f, UnitPriceScope.LOCATION_AVERAGE));
        }

        @Test
        void averagesTheLocationPastALocalMarketWithNoFinitePrice() {

            assertThat(CommodityMarketPrices.findLocalUnitPrice(
                    sectorMock,
                    buildLocalMarketSelling(Float.NaN, true),
                    locationMock,
                    COMMODITY_ID,
                    PlayerTradeSide.SELLING))
                .contains(new LocalUnitPrice(100f, UnitPriceScope.LOCATION_AVERAGE));
        }

        @Test
        void averagesTheLocationWithoutALocalMarket() {

            assertThat(CommodityMarketPrices.findLocalUnitPrice(
                    sectorMock,
                    null,
                    locationMock,
                    COMMODITY_ID,
                    PlayerTradeSide.SELLING))
                .contains(new LocalUnitPrice(100f, UnitPriceScope.LOCATION_AVERAGE));
        }

        @Test
        void averagesTheSectorWhereTheLocationHoldsNoMarket() {

            when(economyMock.getMarkets(locationMock))
                .thenReturn(List.of());

            assertThat(CommodityMarketPrices.findLocalUnitPrice(
                    sectorMock,
                    null,
                    locationMock,
                    COMMODITY_ID,
                    PlayerTradeSide.SELLING))
                .contains(new LocalUnitPrice(50f, UnitPriceScope.SECTOR_AVERAGE));
        }

        @Test
        void averagesTheSectorWithoutALocation() {

            assertThat(CommodityMarketPrices.findLocalUnitPrice(
                    sectorMock,
                    null,
                    null,
                    COMMODITY_ID,
                    PlayerTradeSide.SELLING))
                .contains(new LocalUnitPrice(50f, UnitPriceScope.SECTOR_AVERAGE));
        }

        @Test
        void answersNothingWhereNoMarketAnywherePricesIt() {

            when(economyMock.getMarkets(locationMock))
                .thenReturn(List.of());
            when(economyMock.getMarketsCopy())
                .thenReturn(List.of());

            assertThat(CommodityMarketPrices.findLocalUnitPrice(
                    sectorMock,
                    null,
                    locationMock,
                    COMMODITY_ID,
                    PlayerTradeSide.SELLING))
                .isEmpty();
        }

        @Test
        void readsOnlyTheLocalMarketWithoutASector() {

            assertThat(CommodityMarketPrices.findLocalUnitPrice(
                    null,
                    buildLocalMarketSelling(75f, true),
                    locationMock,
                    COMMODITY_ID,
                    PlayerTradeSide.SELLING))
                .contains(new LocalUnitPrice(75f, UnitPriceScope.LOCAL_MARKET));
        }
    }
}
