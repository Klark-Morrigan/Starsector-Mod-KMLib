package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.characters.RelationshipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.Locale;

/**
 * Formats a faction's relationship-to-the-player as a vanilla-styled
 * description + matching colour pair. Mirrors what the engine renders
 * in colony tooltips and intel rows: {@code "<RepLevel name> (<rep> /
 * 100)"}, e.g. {@code "Friendly (35 / 100)"}.
 *
 * <p>The colour resolver walks a three-tier fallback to pick up
 * whichever shade Starsector itself would use for this faction pair
 * right now:</p>
 * <ol>
 *   <li>{@link RelationshipAPI#getRelColor()} on the live relationship
 *       object - the most authoritative source, because it picks up
 *       any per-relationship overrides Starsector applies (tutorial
 *       pinning, scripted events, etc.).</li>
 *   <li>{@link FactionAPI#getRelColor(String)} against
 *       {@link Factions#PLAYER} - the next-best read, used when the
 *       live relationship object is unavailable.</li>
 *   <li>{@link Misc#getRelColor(float)} on the raw float - the
 *       stateless palette fallback.</li>
 * </ol>
 *
 * <p>Reads are bare: any {@link RuntimeException} from a modded
 * {@link FactionAPI} or {@link RelationshipAPI} propagates to the
 * caller rather than degrading silently. Lives in KMLib so every mod
 * that surfaces a faction's player-relation renders it the same way
 * and pulls the same colour from the same fallback chain. Stateless
 * - the single entry point is a static method, no instance needed.</p>
 */
public final class StarsectorPlayerRelationshipFormatter {

    private static final int MAX_RELATIONSHIP_REPUTATION = 100;
    private static final String PLAYER_RELATIONSHIP_DESCRIPTION_FORMAT = "%s (%d / %d)";

    private StarsectorPlayerRelationshipFormatter() {
    }

    public static RelationshipSummary formatPlayerRelationship(FactionAPI faction) {

        if (faction == null) {
            return RelationshipSummary.createEmptySummary();
        }

        var relationship = faction.getRelToPlayer();

        if (relationship != null) {

            var level = relationship.getLevel();

            if (level != null) {

                return new RelationshipSummary(
                    formatRelationshipDescription(level, relationship.getRepInt()),
                    relationship.getRelColor());
            }
        }

        var rel = faction.getRelationship(Factions.PLAYER);
        var level = RepLevel.getLevelFor(rel);

        if (level == null) {
            return RelationshipSummary.createEmptySummary();
        }
        var repInt = RepLevel.getRepInt(rel);
        var colour = faction.getRelColor(Factions.PLAYER);

        if (colour == null) {
            colour = Misc.getRelColor(rel);
        }
        return new RelationshipSummary(
            formatRelationshipDescription(level, repInt),
            colour);
    }

    private static String formatRelationshipDescription(RepLevel level, int repInt) {

        var levelName = level.getDisplayName();

        if (levelName == null || levelName.trim().isEmpty()) {

            // Enum-name fallback when the display name is missing (very
            // old saves, modded RepLevels with empty display strings).
            levelName = level.name();
        }
        return String.format(
            Locale.ROOT,
            PLAYER_RELATIONSHIP_DESCRIPTION_FORMAT,
            levelName,
            repInt,
            MAX_RELATIONSHIP_REPUTATION);
    }

    public static final class RelationshipSummary {

        private final String description;
        private final Color colour;

        private RelationshipSummary(String description, Color colour) {
            this.description = description;
            this.colour = colour;
        }

        public String getDescription() {
            return description;
        }

        public Color getColour() {
            return colour;
        }

        private static RelationshipSummary createEmptySummary() {
            return new RelationshipSummary(null, null);
        }
    }
}
