package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.starsector.WalkCountCapture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class SectorMarketsTests {

    private static MarketAPI createNamedMarket(String name) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getName())
            .thenReturn(name);

        return marketMock;
    }

    private static SectorAPI createSectorListing(List<MarketAPI> markets) {

        var sectorMock = mock(SectorAPI.class);
        var economyMock = mock(EconomyAPI.class);

        when(sectorMock.getEconomy())
            .thenReturn(economyMock);
        when(economyMock.getMarketsCopy())
            .thenReturn(markets);

        return sectorMock;
    }

    @Nested
    class ListMarketNames {

        @Test
        void readsEveryNamedMarketLeavingBlanksOut() {

            var markets = List.of(createNamedMarket("Jangala"), createNamedMarket(""), createNamedMarket("Sindria"));

            assertThat(SectorMarkets.listMarketNames(createSectorListing(markets)))
                .containsExactly("Jangala", "Sindria");
        }

        @Test
        void answersNothingForASectorWithNoEconomyYet() {
            assertThat(SectorMarkets.listMarketNames(mock(SectorAPI.class)))
                .isEmpty();
        }

        @Test
        void answersNothingForANullSector() {
            assertThat(SectorMarkets.listMarketNames(null))
                .isEmpty();
        }

        @Test
        void countsEveryMarketItReadOnTheOpenSection() {

            var markets = List.of(createNamedMarket("Jangala"), createNamedMarket("Sindria"));
            var sectorMock = createSectorListing(markets);

            var counts = WalkCountCapture.captureCountsOf(() -> SectorMarkets.listMarketNames(sectorMock));

            assertThat(counts.readCount(SectorWalkCounters.MARKETS_READ))
                .isEqualTo(2L);
        }
    }
}
