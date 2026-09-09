package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.RelationshipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import java.awt.Color;
import java.util.Optional;

/**
 * Reads where a faction stands with the player, as the player-fixed binding of
 * {@link StarsectorFactionRelations#readRelation}.
 *
 * <p>Kept as its own entry point because the player pair is the one Starsector exposes a live
 * {@link RelationshipAPI} for, and that object is the most authoritative source there is: it picks
 * up per-relationship overrides the engine applies (tutorial pinning, scripted events, and the
 * like) that neither the faction's palette nor the raw number reports. So this class holds that one
 * extra tier and falls through to the general read for everything below it, rather than the general
 * read carrying a branch about the player.
 *
 * <p>Reads are bare: any {@link RuntimeException} from a modded {@link FactionAPI} or
 * {@link RelationshipAPI} propagates to the caller rather than degrading silently. Stateless - the
 * single entry point is a static method, no instance needed.
 */
public final class StarsectorPlayerRelations {

    private StarsectorPlayerRelations() {
    }

    /**
     * Resolves a faction's relation with the player.
     *
     * @param faction the faction whose relation is read; nothing is read of no faction, so it holds
     *                none
     * @return the relation, or none where there is no faction to read one from
     */
    public static Optional<FactionRelation> readPlayerRelation(FactionAPI faction) {

        if (faction == null) {
            return Optional.empty();
        }
        var relationship = faction.getRelToPlayer();

        if (relationship != null) {

            var level = relationship.getLevel();

            if (level != null) {

                return Optional.of(new FactionRelation(
                    level,
                    relationship.getRepInt(),
                    resolvePlayerRelationColour(faction, relationship)));
            }
        }

        // Without a relationship object naming a level there is nothing the player tier can add, so
        // the relation comes off the general read against the player id - the same faction and raw
        // number the object would have been carrying.
        return StarsectorFactionRelations.readRelation(faction, Factions.PLAYER);
    }

    /**
     * The live relationship's own shade, falling through to the general colour walk where it paints
     * nothing.
     *
     * <p>Falls through on the relationship object's own number rather than on the faction's, since
     * the object is the tier that outranks it: a pair the engine has pinned reports one relationship
     * there and another on the faction, and painting the shade of the number this relation did not
     * take would put its two facets at odds.
     */
    private static Color resolvePlayerRelationColour(
            FactionAPI faction, RelationshipAPI relationship) {

        var relationshipColour = relationship.getRelColor();

        return relationshipColour == null
            ? StarsectorRelationColours.resolveRelationColour(
                faction, Factions.PLAYER, relationship.getRel())
            : relationshipColour;
    }
}
