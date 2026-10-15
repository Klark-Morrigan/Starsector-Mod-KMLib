package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;

import java.util.Optional;

/**
 * Reads of a market's trading counters, its submarkets.
 *
 * <p>Held apart from {@link Markets}, which reads what a market is, because these read how a market
 * is put together. On a modded market any counter may be missing, and any mod may replace the plugin
 * keeping one, so each read here answers empty for those cases rather than failing.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and
 * null-market defensive like the rest of the library.
 */
public final class MarketSubmarkets {

    private MarketSubmarkets() {
    }

    /**
     * The goods held at one of a market's trading counters.
     *
     * <p>A counter a mod adds exists only once that mod has wired it onto the market, so an absent
     * counter is an ordinary state rather than a fault.
     *
     * @param market      the market whose counter is read; null yields empty
     * @param submarketId the counter's submarket ID
     * @return the counter's cargo, or empty where the market has no such counter
     */
    public static Optional<CargoAPI> findSubmarketCargo(MarketAPI market, String submarketId) {

        return findSubmarket(market, submarketId)
            .map(SubmarketAPI::getCargo);
    }

    /**
     * The plugin keeping one of a market's trading counters, as the type the caller is about to
     * speak to.
     *
     * <p>The type is asked for rather than assumed because any mod may replace the plugin behind a
     * counter vanilla defines. A caller naming vanilla's own plugin class gets nothing on an install
     * that replaced it. A caller naming only the interface it needs is answered by whatever keeps the
     * counter.
     *
     * @param market      the market whose counter is reached; null yields empty
     * @param submarketId the counter's submarket ID
     * @param pluginType  the type of plugin the caller is about to speak to
     * @param <T>         that type
     * @return the counter's plugin as that type, or empty where the market has no such counter or
     *         another type of plugin keeps it
     */
    public static <T> Optional<T> findSubmarketPlugin(
            MarketAPI market,
            String submarketId,
            Class<T> pluginType) {

        return findSubmarket(market, submarketId)
            .map(SubmarketAPI::getPlugin)
            .filter(pluginType::isInstance)
            .map(pluginType::cast);
    }

    private static Optional<SubmarketAPI> findSubmarket(MarketAPI market, String submarketId) {

        return market == null
            ? Optional.empty()
            : Optional.ofNullable(market.getSubmarket(submarketId));
    }
}
