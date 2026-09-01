package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;

import java.awt.Color;
import java.util.Locale;

/**
 * Formats a faction's relationship-to-the-player as a vanilla-styled description + matching colour
 * pair. Mirrors what the engine renders in colony tooltips and intel rows:
 * {@code "<RepLevel name> (<rep> / 100)"}, e.g. {@code "Friendly (35 / 100)"}.
 *
 * <p>Prose only: what the standing is, and what colour it is drawn in, are read by
 * {@link StarsectorPlayerStandings}, so a tooltip describing a standing and a surface ranking by it
 * quote one read of the game rather than two walks of the same fallback chain.
 *
 * <p>Lives in KMLib so every mod that surfaces a faction's player-relation renders it the same way.
 * Stateless - the single entry point is a static method, no instance needed.
 */
public final class StarsectorPlayerRelationshipFormatter {

    private static final int MAX_RELATIONSHIP_REPUTATION = 100;
    private static final String PLAYER_RELATIONSHIP_DESCRIPTION_FORMAT = "%s (%d / %d)";

    private StarsectorPlayerRelationshipFormatter() {
    }

    public static RelationshipSummary formatPlayerRelationship(FactionAPI faction) {

        return StarsectorPlayerStandings.readPlayerStanding(faction)
            .map(standing -> new RelationshipSummary(
                formatRelationshipDescription(standing.level(), standing.reputation()),
                standing.colour()))
            .orElseGet(RelationshipSummary::createEmptySummary);
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
