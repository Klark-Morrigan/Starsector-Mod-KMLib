package kmlib.starsector.markets.storage;

import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pins the storage unlock's branches: a vanilla storage is marked paid, and a missing market, a missing counter and a
 * counter another plugin keeps are each left alone.
 */
final class StorageAccessTests {

    private static final String SUBMARKET_ID = "storage";

    private static MarketAPI buildMarketHolding(SubmarketPlugin pluginMock) {

        var submarketMock = mock(SubmarketAPI.class);

        when(submarketMock.getPlugin())
            .thenReturn(pluginMock);

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getSubmarket(SUBMARKET_ID))
            .thenReturn(submarketMock);

        return marketMock;
    }

    @Nested
    class UnlockStorage {

        @Test
        void marksAVanillaStorageAsPaidToUnlock() {

            var pluginMock = mock(StoragePlugin.class);

            StorageAccess.unlockStorage(buildMarketHolding(pluginMock), SUBMARKET_ID);

            verify(pluginMock)
                .setPlayerPaidToUnlock(true);
        }

        // An unstubbed market answers no counter, as a market without storage does.
        @Test
        void leavesAMarketWithoutTheCounterAlone() {

            var marketMock = mock(MarketAPI.class);

            StorageAccess.unlockStorage(marketMock, SUBMARKET_ID);

            verify(marketMock)
                .getSubmarket(SUBMARKET_ID);
        }

        @Test
        void leavesACounterAnotherPluginKeepsAlone() {

            var pluginMock = mock(SubmarketPlugin.class);

            StorageAccess.unlockStorage(buildMarketHolding(pluginMock), SUBMARKET_ID);

            verifyNoInteractions(pluginMock);
        }

        @Test
        void ignoresANullMarket() {

            assertThatCode(() -> StorageAccess.unlockStorage(null, SUBMARKET_ID))
                .doesNotThrowAnyException();
        }
    }
}
