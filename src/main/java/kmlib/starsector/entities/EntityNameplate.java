package kmlib.starsector.entities;

import java.util.Optional;

/**
 * How a surface identifies one entity to a reader: the name it prints, and the glyph the sector
 * map marks that entity with.
 *
 * <p>The two travel together for the reason the glyph's own path and colour do - neither
 * identifies the entity on its own, and pairing them at the surface rather than at the read is
 * what lets one entity's name end up beside another's glyph. Read once from the entity, the
 * mismatch is not a rule to honour but a state that cannot be built.
 *
 * <p>Both halves are wanted together wherever a list of entities is printed, so the pair is one
 * value rather than two fields every such record spells out again - and a caller drawing a line
 * takes the subject rather than two parts it has to keep in step.
 *
 * <p>An entity the map marks with nothing is an ordinary nameplate with an empty glyph, not an
 * absent one: the name still identifies it, and a surface leading with the name alone is the
 * unmarked case rather than a failure.
 *
 * @param displayName the name a surface prints the entity under
 * @param mapIcon     the glyph the sector map marks the entity with, or empty where it carries
 *                    none
 */
public record EntityNameplate(
    String displayName,
    Optional<EntityMapIcon> mapIcon) {

    /**
     * Reads an unstated glyph as no glyph, so a hand-built nameplate cannot fail late on the half a
     * caller had nothing to say about.
     */
    public EntityNameplate {
        mapIcon = mapIcon == null ? Optional.empty() : mapIcon;
    }

    /**
     * Builds the nameplate of an entity the map marks with nothing, which is every entity carrying
     * no authored icon spec.
     *
     * @param displayName the name a surface prints the entity under
     * @return the nameplate, its glyph empty
     */
    public static EntityNameplate createUnmarkedNameplate(String displayName) {
        return new EntityNameplate(displayName, Optional.empty());
    }
}
