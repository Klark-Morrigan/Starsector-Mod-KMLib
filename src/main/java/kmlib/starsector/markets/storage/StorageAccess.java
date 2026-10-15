package kmlib.starsector.markets.storage;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;

import kmlib.starsector.markets.Markets;

/**
 * Opening a market's storage without vanilla's one-time access fee.
 *
 * <p>Vanilla charges that fee at the counter for storage the player did not build. A caller that has decided the
 * player should not pay it - a colony the player now holds, a storage a mod builds for the player - marks the storage
 * as paid instead, through the one reach here.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and null-defensive like the
 * rest of the library.
 */
public final class StorageAccess {

    private StorageAccess() {
    }

    /**
     * Marks a market's storage counter as paid to unlock, so clicking it opens the storage rather than vanilla's
     * access-fee dialog.
     *
     * <p>Does nothing where the market has no such counter, or where the plugin keeping it is not vanilla's
     * {@link StoragePlugin}: a mod is free to replace that plugin, and one it replaced is not this library's to drive.
     * Vanilla never clears the mark, so repeating it is harmless.
     *
     * @param market      the market holding the storage; null does nothing
     * @param submarketId the storage counter's submarket ID
     */
    public static void unlockStorage(MarketAPI market, String submarketId) {

        var storagePlugin = Markets.readSubmarketPlugin(market, submarketId, StoragePlugin.class);
        if (storagePlugin != null) {
            storagePlugin.setPlayerPaidToUnlock(true);
        }
    }
}
