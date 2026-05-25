package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.characters.RelationshipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import kmlib.starsector.relation.StarsectorPlayerRelationshipFormatter.RelationshipSummary;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StarsectorPlayerRelationshipFormatterTest {
    private static final Color RED = new Color(200, 50, 50);
    private final StarsectorPlayerRelationshipFormatter formatter =
            new StarsectorPlayerRelationshipFormatter();

    @Test
    void returnsEmptySummaryForNullFaction() {
        RelationshipSummary result = formatter.formatPlayerRelationship(null);

        assertThat(result.getDescription()).isNull();
        assertThat(result.getColor()).isNull();
    }

    @Test
    void formatsDescriptionAndColorViaRelationshipApiPath() {
        RelationshipAPI relationship = mock(RelationshipAPI.class);
        when(relationship.getLevel()).thenReturn(RepLevel.VENGEFUL);
        when(relationship.getRepInt()).thenReturn(-100);
        when(relationship.getRelColor()).thenReturn(RED);

        FactionAPI faction = mock(FactionAPI.class);
        when(faction.getRelToPlayer()).thenReturn(relationship);

        RelationshipSummary result = formatter.formatPlayerRelationship(faction);

        assertThat(result.getDescription()).isEqualTo("Vengeful (-100 / 100)");
        assertThat(result.getColor()).isEqualTo(RED);
    }

    @Test
    void fallsBackToFactionRelationshipWhenRelToPlayerIsNull() {
        FactionAPI faction = mock(FactionAPI.class);
        // getRelToPlayer defaults to null, triggering the fallback path.
        // Stub the faction-level colour so the resolver does NOT reach the
        // Misc.getRelColor fallback - Misc reads from the static palette
        // (Global.getSettings()) which is not initialised in unit tests.
        when(faction.getRelationship(Factions.PLAYER)).thenReturn(0.0f);
        when(faction.getRelColor(Factions.PLAYER)).thenReturn(RED);

        RelationshipSummary result = formatter.formatPlayerRelationship(faction);

        // Level name varies by Starsector version; verify only the numeric format.
        assertThat(result.getDescription()).isNotNull().contains("/ 100");
        assertThat(result.getColor()).isEqualTo(RED);
    }
}
