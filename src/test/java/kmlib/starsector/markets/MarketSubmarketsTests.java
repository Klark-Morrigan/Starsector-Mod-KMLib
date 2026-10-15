package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link MarketSubmarkets#findSubmarketCargo} and
 * {@link MarketSubmarkets#findSubmarketPlugin}, one {@link Nested} group per method.
 */
final class MarketSubmarketsTests {

    @Nested
    class FindSubmarketCargo {

        @Test
        void findsTheCountersCargo() {

            var cargoMock = mock(CargoAPI.class);
            var submarketMock = mock(SubmarketAPI.class);
            var marketMock = mock(MarketAPI.class);

            when(submarketMock.getCargo())
                .thenReturn(cargoMock);
            when(marketMock.getSubmarket("storage"))
                .thenReturn(submarketMock);

            assertThat(MarketSubmarkets.findSubmarketCargo(marketMock, "storage"))
                .containsSame(cargoMock);
        }

        @Test
        void findsNothingWhereTheMarketKeepsNoSuchCounter() {

            assertThat(MarketSubmarkets.findSubmarketCargo(mock(MarketAPI.class), "storage"))
                .isEmpty();
        }

        @Test
        void findsNothingForANullMarket() {

            assertThat(MarketSubmarkets.findSubmarketCargo(null, "storage"))
                .isEmpty();
        }
    }

    // The colonies here are MarketOwnershipFixture's rather than this suite's own builders: what
    // is being read is a counter and the plugin behind it, and that fixture is where this package
    // states what a colony's counters look like - including which of them are kept by a plugin
    // worth speaking to.
    @Nested
    class FindSubmarketPlugin {

        @Test
        void findsThePluginKeepingTheCounter() {

            var market = MarketOwnershipFixture.buildColonyTradingThrough(
                "player",
                "storage");

            assertThat(MarketSubmarkets.findSubmarketPlugin(market, "storage", StoragePlugin.class))
                .containsSame((StoragePlugin) market.getSubmarket("storage").getPlugin());
        }

        @Test
        void findsNothingWhereTheCounterIsKeptByAnotherKindOfPlugin() {
            // The case the kind is asked for at all: a mod may put its own plugin behind a counter
            // vanilla defines, and an operation that cast it outright would fail on that install
            // rather than pass the counter over.
            var market = MarketOwnershipFixture.buildColonyTradingThrough(
                "player",
                "local_resources");

            assertThat(MarketSubmarkets.findSubmarketPlugin(market, "local_resources", StoragePlugin.class))
                .isEmpty();
        }

        @Test
        void findsNothingWhereTheMarketKeepsNoSuchCounter() {

            var market = MarketOwnershipFixture.buildColonyHeldBy("hegemony");

            assertThat(MarketSubmarkets.findSubmarketPlugin(market, "storage", StoragePlugin.class))
                .isEmpty();
        }

        @Test
        void findsNothingForANullMarket() {

            assertThat(MarketSubmarkets.findSubmarketPlugin(null, "storage", StoragePlugin.class))
                .isEmpty();
        }
    }
}
