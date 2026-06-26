package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.characters.RelationshipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static kmlib.starsector.relation.StarsectorPlayerRelationshipFormatter.formatPlayerRelationship;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StarsectorPlayerRelationshipFormatterTest {
    private static final Color RED = new Color(200, 50, 50);

    @Test
    void returnsEmptySummaryForNullFaction() {
        var result = formatPlayerRelationship(null);

        assertThat(result.getDescription()).isNull();
        assertThat(result.getColor()).isNull();
    }

    @Test
    void formatsDescriptionAndColorViaRelationshipApiPath() {
        var relationshipMock = mock(RelationshipAPI.class);
        when(relationshipMock.getLevel()).thenReturn(RepLevel.VENGEFUL);
        when(relationshipMock.getRepInt()).thenReturn(-100);
        when(relationshipMock.getRelColor()).thenReturn(RED);

        var factionMock = mock(FactionAPI.class);
        when(factionMock.getRelToPlayer()).thenReturn(relationshipMock);

        var result = formatPlayerRelationship(factionMock);

        assertThat(result.getDescription()).isEqualTo("Vengeful (-100 / 100)");
        assertThat(result.getColor()).isEqualTo(RED);
    }

    @Test
    void fallsBackToFactionRelationshipWhenRelToPlayerIsNull() {
        var factionMock = mock(FactionAPI.class);
        // getRelToPlayer defaults to null, triggering the fallback path.
        // Stub the faction-level colour so the resolver does NOT reach the
        // Misc.getRelColor fallback - Misc reads from the static palette
        // (Global.getSettings()) which is not initialised in unit tests.
        when(factionMock.getRelationship(Factions.PLAYER)).thenReturn(0.0f);
        when(factionMock.getRelColor(Factions.PLAYER)).thenReturn(RED);

        var result = formatPlayerRelationship(factionMock);

        // Level name varies by Starsector version; verify only the numeric format.
        assertThat(result.getDescription()).isNotNull().contains("/ 100");
        assertThat(result.getColor()).isEqualTo(RED);
    }
}
