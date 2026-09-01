package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.characters.RelationshipAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static kmlib.starsector.relation.StarsectorPlayerRelationshipFormatter.formatPlayerRelationship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StarsectorPlayerRelationshipFormatterTest {

    private static final Color RED = new Color(200, 50, 50);

    @Nested
    class FormatPlayerRelationship {

        @Test
        void returnsEmptySummaryForNullFaction() {

            var result = formatPlayerRelationship(null);

            assertThat(result.getDescription())
                .isNull();
            assertThat(result.getColour())
                .isNull();
        }

        @Test
        void namesTheLevelAndTheReputationAndCarriesTheStandingColourThrough() {

            // Which tiers the level, the number and the colour are read off is the standing read's
            // own business; this suite only proves the prose built over whatever it answers.
            var relationshipMock = mock(RelationshipAPI.class);

            when(relationshipMock.getLevel())
                .thenReturn(RepLevel.VENGEFUL);
            when(relationshipMock.getRepInt())
                .thenReturn(-100);
            when(relationshipMock.getRelColor())
                .thenReturn(RED);

            var factionMock = mock(FactionAPI.class);

            when(factionMock.getRelToPlayer())
                .thenReturn(relationshipMock);

            var result = formatPlayerRelationship(factionMock);

            assertThat(result.getDescription())
                .isEqualTo("Vengeful (-100 / 100)");
            assertThat(result.getColour())
                .isEqualTo(RED);
        }

        @Test
        void printsAPositiveReputationWithoutASign() {

            var relationshipMock = mock(RelationshipAPI.class);

            when(relationshipMock.getLevel())
                .thenReturn(RepLevel.COOPERATIVE);
            when(relationshipMock.getRepInt())
                .thenReturn(85);
            when(relationshipMock.getRelColor())
                .thenReturn(RED);

            var factionMock = mock(FactionAPI.class);

            when(factionMock.getRelToPlayer())
                .thenReturn(relationshipMock);

            assertThat(formatPlayerRelationship(factionMock).getDescription())
                .isEqualTo("Cooperative (85 / 100)");
        }
    }
}
