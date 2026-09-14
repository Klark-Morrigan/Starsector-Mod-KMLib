package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.SectorEntityToken;

/**
 * The facts any campaign entity answers about itself, whatever is standing on it.
 *
 * <p>Here rather than on each kind of thing that wraps an entity, because both are readings a
 * reader would otherwise restate. Discovery in particular is stated backwards in the game's own
 * words - an entity carries {@code discoverable} and loses it once found, so every reader has to
 * invert it - and a rule that has to be inverted to be used is one a second reader eventually
 * inverts differently. The absent entity is settled once here for the same reason: nothing left to
 * find is nothing to withhold.
 *
 * <p>Says nothing about what may be shown. These are the entity's own facts, and the fog a caller
 * draws over them is its own.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and
 * null-defensive like the rest of the library.
 */
public final class Entities {

    private Entities() {
        // utility class, no instances.
    }

    /**
     * Which faction the entity names, as a faction id.
     *
     * <p>Absorbs a faction nobody can name rather than refusing it: the answer is compared
     * against whatever a caller compares factions for, and an absent one has nothing to fail on.
     *
     * @param entity the entity to read; null yields null
     * @return the faction's ID, or null where the entity names none
     */
    public static String readFactionId(SectorEntityToken entity) {

        var faction = entity == null
            ? null
            : entity.getFaction();

        return faction == null
            ? null
            : faction.getId();
    }

    /**
     * Whether the player has found this entity - the base every rule about showing one starts
     * from.
     *
     * <p>There is no {@code isDiscovered} to read: an entity stops being discoverable once found,
     * so the answer is the negation of that flag.
     *
     * @param entity the entity to test; null reads found, there being nothing left to find
     * @return true when the entity has been found
     */
    public static boolean isDiscoveredByPlayer(SectorEntityToken entity) {
        return entity == null || !entity.isDiscoverable();
    }
}
