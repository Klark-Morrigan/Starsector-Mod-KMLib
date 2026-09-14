package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import kmlib.text.KmlibStrings;

import java.awt.Color;
import java.util.Optional;

/**
 * Reads the glyph the sector map marks an entity with, confining the two unrelated places vanilla
 * stores it to one lookup so no two surfaces can end up showing different icons for one entity.
 *
 * <p>Sibling to {@link kmlib.starsector.factions.FactionCrests}, which confines the faction crest
 * lookup: this reads an entity's map icon. It holds to the same blank-is-absent rule - a null
 * entity, a spec the entity does not have, or a blank sprite path all resolve to empty - so every
 * caller agrees on when an icon is present rather than each re-deciding what a blank path means.
 *
 * <p>The glyph alone, and nothing about the entity beside it: a caller printing an entity in a list
 * wants its name in the same breath, and that pairing is {@link EntityNameplates}'s, which reads
 * this for the half it covers. Answering both here would make an icon lookup the place a name comes
 * from too.
 *
 * <p>Vanilla splits the icon across two specs that share no accessor: a planet's is on its planet
 * spec, and everything else - stations, relays, buoys, arrays - carries its own on a custom-entity
 * spec. Branching on which of the two applies is exactly the kind of split a caller drawing a name
 * should not have to know about.
 *
 * <p>The map's own {@code isShowIconOnMap()} clutter rule is deliberately not consulted. That flag
 * answers whether the map should draw a mark of its own; a caller here is naming the entity in a
 * list, where an entity the map declines to mark still has to be told apart from its neighbours.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state.
 */
public final class EntityMapIcons {

    private EntityMapIcons() {
        // utility class, no instances.
    }

    /**
     * Resolves the map glyph an entity is marked with - its sprite path and the colour that sprite
     * is drawn in - or empty when the entity carries no authored icon.
     *
     * <p>Path and colour are answered as one value rather than through two reads, so one entity's
     * glyph can never be paired with another's tint.
     *
     * @param entity the entity whose map icon is read; null yields empty
     * @return the entity's map icon, or empty when it carries no authored one
     */
    public static Optional<EntityMapIcon> resolveMapIcon(SectorEntityToken entity) {
        if (entity == null) {
            return Optional.empty();
        }
        if (entity instanceof PlanetAPI planet) {
            var planetSpec = planet.getSpec();
            return planetSpec == null
                ? Optional.empty()
                : buildIcon(planetSpec.getIconTexture(), planetSpec.getIconColor());
        }
        var entitySpec = entity.getCustomEntitySpec();
        return entitySpec == null
            ? Optional.empty()
            : buildIcon(entitySpec.getIconName(), entitySpec.getIconColor());
    }

    /**
     * Pairs a spec's authored path and colour into an icon, treating a blank path as no icon at
     * all. The path is trimmed, so a present icon always carries a non-blank sprite path.
     *
     * <p>An absent colour is carried rather than rejected: a glyph with colour in its own pixels
     * has nothing to tint by, and the draw already reads a null tint as "as authored". Only the
     * path decides whether there is an icon here.
     */
    private static Optional<EntityMapIcon> buildIcon(String spritePath, Color iconColour) {
        return KmlibStrings.hasText(spritePath)
            ? Optional.of(new EntityMapIcon(spritePath.trim(), iconColour))
            : Optional.empty();
    }
}
