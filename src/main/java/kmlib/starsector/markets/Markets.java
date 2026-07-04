package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

/**
 * Queries over a market's owned entities.
 *
 * <p>Centralises reads a market only answers by walking its connected entities,
 * so KM* mods (and any external caller) share one implementation of a check like
 * "does this colony have a station" rather than re-deriving the entity scan each
 * time.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance
 * state. Matches {@link kmlib.starsector.systems.StarSystems}'s shape, and is
 * null-market defensive like the rest of the library.
 */
public final class Markets {

    // Vanilla's opt-out tag for a "station"-tagged entity that must not be treated
    // as a market's orbital station. No Tags constant exists for it, so the literal
    // is vanilla's own contract - OrbitalStation's station scan tests it the same way.
    private static final String NO_ORBITAL_STATION_TAG = "NO_ORBITAL_STATION";

    private Markets() {
        // utility class, no instances.
    }

    /**
     * Whether a market owns an attached defensive station.
     *
     * <p>Reads the market's connected entities - the ownership link the game
     * maintains, so a station found here is this market's own rather than a rival's
     * or an abandoned hulk sharing the orbit - and mirrors the scan
     * {@code OrbitalStation} itself runs: a {@code "station"}-tagged entity not
     * opted out via {@code NO_ORBITAL_STATION}. Keying on the entity tag captures
     * vanilla and modded stations alike, so no industry ids are read.
     *
     * @param market the market to inspect; null (or one with no connected entities)
     *               yields false
     * @return true when one of the market's connected entities is its orbital station
     */
    public static boolean hasAttachedStation(MarketAPI market) {
        if (market == null || market.getConnectedEntities() == null) {
            return false;
        }
        for (var entity : market.getConnectedEntities()) {
            if (entity.hasTag(Tags.STATION) && !entity.hasTag(NO_ORBITAL_STATION_TAG)) {
                return true;
            }
        }
        return false;
    }
}
