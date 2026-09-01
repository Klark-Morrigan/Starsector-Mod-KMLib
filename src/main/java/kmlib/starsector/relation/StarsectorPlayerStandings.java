package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.characters.RelationshipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.Optional;

/**
 * Reads where a faction stands with the player as a single {@link PlayerStanding}.
 *
 * <p>The signed number is what a surface ranking factions by disposition orders on, and the colour
 * is what it draws that number in. Both are answered from one read here rather than left to each
 * caller, so a tooltip describing a standing and a row ranking by it cannot end up quoting
 * different walks of the same fallback chain.
 *
 * <p>The colour walks a three-tier fallback, so whichever shade Starsector would use for this
 * faction pair right now is the one that comes back:
 * <ol>
 *   <li>{@link RelationshipAPI#getRelColor()} on the live relationship object - the most
 *       authoritative source, because it picks up any per-relationship overrides Starsector applies
 *       (tutorial pinning, scripted events, and the like).</li>
 *   <li>{@link FactionAPI#getRelColor(String)} against {@link Factions#PLAYER} - the next-best
 *       read, used where the live relationship object is unavailable or paints nothing.</li>
 *   <li>{@link Misc#getRelColor(float)} on the raw reputation - the stateless palette fallback.</li>
 * </ol>
 *
 * <p>Absence is carried by handing back no standing rather than by a reputation of nought: a
 * faction nobody can look up and a faction the player is indifferent to are the same number and
 * opposite facts, and a caller folding standings across several factions has to be able to skip the
 * first without dragging the scale's centre into its answer.
 *
 * <p>Reads are bare: any {@link RuntimeException} from a modded {@link FactionAPI} or
 * {@link RelationshipAPI} propagates to the caller rather than degrading silently. Stateless - the
 * single entry point is a static method, no instance needed.
 */
public final class StarsectorPlayerStandings {

    private StarsectorPlayerStandings() {
    }

    /**
     * Resolves a faction's standing with the player.
     *
     * @param faction the faction whose standing is read; nothing is read of no faction, so it holds
     *                no standing
     * @return the standing, or no standing where there is no faction to read one from
     */
    public static Optional<PlayerStanding> readPlayerStanding(FactionAPI faction) {

        if (faction == null) {
            return Optional.empty();
        }
        var relationship = faction.getRelToPlayer();

        if (relationship != null) {

            var level = relationship.getLevel();

            if (level != null) {

                return Optional.of(new PlayerStanding(
                    level,
                    relationship.getRepInt(),
                    resolveStandingColour(faction, relationship, relationship.getRel())));
            }
        }

        // Without a relationship object naming a level, the standing is reconstructed from the raw
        // reputation the faction reports - the same number the object would have been carrying.
        // The scale covers the whole float range, so this path always names a level.
        var reputation = faction.getRelationship(Factions.PLAYER);

        return Optional.of(new PlayerStanding(
            RepLevel.getLevelFor(reputation),
            RepLevel.getRepInt(reputation),
            resolveStandingColour(faction, null, reputation)));
    }

    /**
     * Walks the three colour tiers in order, taking the first that paints something.
     *
     * @param relationship the live relationship object, or none where the caller has none to offer
     * @param reputation   the raw reputation the stateless palette is asked about
     */
    private static Color resolveStandingColour(
            FactionAPI faction, RelationshipAPI relationship, float reputation) {

        if (relationship != null) {

            var relationshipColour = relationship.getRelColor();

            if (relationshipColour != null) {
                return relationshipColour;
            }
        }
        var factionColour = faction.getRelColor(Factions.PLAYER);

        if (factionColour != null) {
            return factionColour;
        }
        return Misc.getRelColor(reputation);
    }
}
