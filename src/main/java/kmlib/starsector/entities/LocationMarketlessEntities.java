package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.LocationAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * Finding the custom entities that sit in one place with no market on them, whatever kind of place
 * it is.
 *
 * <p>The search under every "what stands here that no colony accounts for" read. Such an entity
 * sits in a location - a star system, or hyperspace - and nothing about finding one is
 * system-specific, so the caller decides which kind of place it wants rather than this taking a
 * kind as an argument.
 *
 * <p>Selection is the whole of the location's custom entities less the ones a market hangs on, and
 * no narrower. The set therefore runs from stations and habitats down to cargo pods, and picking
 * out the man-made places worth listing is the caller's judgement: one map wants derelict stations
 * and not pods, another wants the relays this hands back beside them, and a line drawn here would
 * be one of those callers' judgement written into a search shared by all of them.
 *
 * <p>Nothing else is filtered out either: an undiscovered entity comes back like any other,
 * discovery being a fact a caller reads off {@link MarketlessEntity} and applies to its own
 * purpose.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and
 * null-defensive like the rest of the library.
 */
public final class LocationMarketlessEntities {

    private LocationMarketlessEntities() {
        // utility class, no instances.
    }

    /**
     * The market-less custom entities standing in {@code location}, in the order the location
     * lists them.
     *
     * <p>Answered off the location's own custom-entity listing rather than by walking everything
     * present, so the fleets and planets a system holds are never touched.
     *
     * @param location the location to read; null yields an empty list
     * @return the location's market-less custom entities; never null
     */
    public static List<MarketlessEntity> readMarketlessEntitiesIn(LocationAPI location) {

        if (location == null) {
            return List.of();
        }
        var entities = location.getCustomEntities();

        if (entities == null) {
            return List.of();
        }
        var marketlessEntities = new ArrayList<MarketlessEntity>(entities.size());

        for (var entity : entities) {
            // An entity the location lists as null has no facts to read, and one carrying a market
            // is a colony's. Dropping both here is what keeps the record's own constructor free to
            // insist on each rather than having to absorb them.
            if (entity != null && entity.getMarket() == null) {
                marketlessEntities.add(new MarketlessEntity(entity));
            }
        }
        return marketlessEntities;
    }
}
