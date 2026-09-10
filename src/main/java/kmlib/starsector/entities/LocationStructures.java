package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import java.util.ArrayList;
import java.util.List;

/**
 * Finding the structures that sit in one place, whatever kind of place it is.
 *
 * <p>The search under every "what is built here" read. A structure sits in a location - a star
 * system, or hyperspace - and nothing about finding one is system-specific, so the caller decides
 * which kind of place it wants rather than this taking a kind as an argument.
 *
 * <p>Selection is the {@code objective} tag rather than a list of entity ids. The improvised
 * variants carry it, and a mod adding a fourth kind of structure joins the set by tagging its own
 * - which an id list could not admit without being edited for every mod that ships one.
 *
 * <p>Nothing is filtered out here: an undiscovered structure comes back like any other, discovery
 * being a fact a caller reads off {@link Structure} and applies to its own purpose.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and
 * null-defensive like the rest of the library.
 */
public final class LocationStructures {

    private LocationStructures() {
        // utility class, no instances.
    }

    /**
     * The structures standing in {@code location}, in the order the location lists them.
     *
     * <p>Answered off the location's own tag lookup rather than by walking everything present, so
     * asking hyperspace - where the entity list is the sector's largest - costs a handful of
     * relays rather than the whole of it.
     *
     * @param location the location to read; null yields an empty list
     * @return the location's structures; never null
     */
    public static List<Structure> readStructuresIn(LocationAPI location) {

        if (location == null) {
            return List.of();
        }
        var entities = location.getEntitiesWithTag(Tags.OBJECTIVE);

        if (entities == null) {
            return List.of();
        }
        var structures = new ArrayList<Structure>(entities.size());

        for (var entity : entities) {
            // An entity the location lists as null is nothing to stand a structure on, and
            // refusing it here is what keeps the record's own constructor free to insist.
            if (entity != null) {
                structures.add(new Structure(entity));
            }
        }
        return structures;
    }
}
