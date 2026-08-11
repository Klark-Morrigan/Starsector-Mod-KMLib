package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.SectorEntityToken;

/**
 * Reads how an entity is identified to a reader - the name it goes by, paired with the glyph the
 * sector map marks it with.
 *
 * <p>The pairing is the whole of what this exists for. Read apart, the name and the glyph are two
 * calls a caller has to keep pointed at the same entity, which is a convention every surface has to
 * honour rather than something it can rely on; taken together they are a state no caller can get
 * wrong. A list of entities is where that matters, one entity's name drawn beside another's mark
 * being a mistake that reads as a fact.
 *
 * <p>The glyph half is {@link EntityMapIcons}'s and is read through it rather than rebuilt here, so
 * there is still one answer to where vanilla keeps an entity's icon. What this adds is only which
 * name goes with it.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state.
 */
public final class EntityNameplates {

    private EntityNameplates() {
        // utility class, no instances.
    }

    /**
     * Reads an entity's nameplate - both halves off the one token.
     *
     * @param entity the entity to identify; null yields {@link EntityNameplate#BLANK}, so a caller
     *               reading an entity the game turned out not to hold gets something it can draw
     *               rather than a throw at the draw
     * @return the entity's nameplate
     */
    public static EntityNameplate readNameplate(SectorEntityToken entity) {
        if (entity == null) {
            return EntityNameplate.BLANK;
        }
        return new EntityNameplate(
            entity.getName(),
            EntityMapIcons.resolveMapIcon(entity));
    }
}
