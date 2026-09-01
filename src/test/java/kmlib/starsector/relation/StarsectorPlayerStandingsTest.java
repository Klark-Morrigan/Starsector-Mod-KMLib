package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.characters.RelationshipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;

import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.awt.Color;

import static kmlib.starsector.relation.StarsectorPlayerStandings.readPlayerStanding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StarsectorPlayerStandingsTest {

    private static final Color BLUE = new Color(50, 90, 200);
    private static final Color GREEN = new Color(60, 180, 60);
    private static final Color RED = new Color(200, 50, 50);

    @Nested
    class ReadPlayerStanding {

        @Test
        void returnsNoStandingForNullFaction() {

            assertThat(readPlayerStanding(null))
                .isEmpty();
        }

        @Test
        void readsLevelReputationAndColourFromTheRelationshipObject() {

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

            assertThat(readPlayerStanding(factionMock))
                .contains(new PlayerStanding(RepLevel.VENGEFUL, -100, RED));
        }

        @Test
        void fallsBackToTheFactionColourWhenTheRelationshipPaintsNothing() {

            var relationshipMock = mock(RelationshipAPI.class);

            when(relationshipMock.getLevel())
                .thenReturn(RepLevel.FRIENDLY);
            when(relationshipMock.getRepInt())
                .thenReturn(60);

            // getRelColor defaults to null, which is what sends the walk on to the faction tier.
            var factionMock = mock(FactionAPI.class);

            when(factionMock.getRelToPlayer())
                .thenReturn(relationshipMock);
            when(factionMock.getRelColor(Factions.PLAYER))
                .thenReturn(GREEN);

            assertThat(readPlayerStanding(factionMock))
                .contains(new PlayerStanding(RepLevel.FRIENDLY, 60, GREEN));
        }

        @Test
        void reconstructsTheStandingFromTheRawReputationWhenThereIsNoRelationshipObject() {

            var factionMock = mock(FactionAPI.class);

            // getRelToPlayer defaults to null, which is what sends the read down the raw-reputation
            // path; the faction tier then paints it, so the stateless palette is never reached.
            when(factionMock.getRelationship(Factions.PLAYER))
                .thenReturn(0.6f);
            when(factionMock.getRelColor(Factions.PLAYER))
                .thenReturn(BLUE);

            assertThat(readPlayerStanding(factionMock))
                .contains(new PlayerStanding(RepLevel.FRIENDLY, 60, BLUE));
        }

        @Test
        void fallsBackToTheStatelessPaletteWhenNeitherTierPaintsAnything() {

            var relationshipMock = mock(RelationshipAPI.class);

            when(relationshipMock.getLevel())
                .thenReturn(RepLevel.SUSPICIOUS);
            when(relationshipMock.getRepInt())
                .thenReturn(-20);
            when(relationshipMock.getRel())
                .thenReturn(-0.2f);

            var factionMock = mock(FactionAPI.class);

            when(factionMock.getRelToPlayer())
                .thenReturn(relationshipMock);

            // Misc.<clinit> reads from Global.getSettings(), so a no-op SettingsAPI proxy must be in
            // place before Mockito instruments the class - otherwise instrumentation triggers class
            // init and explodes on an NPE deep inside Misc's static fields.
            StarsectorSettingsFake.installSettings();
            try (var miscMock = Mockito.mockStatic(Misc.class)) {

                miscMock.when(() -> Misc.getRelColor(-0.2f))
                    .thenReturn(RED);

                assertThat(readPlayerStanding(factionMock))
                    .contains(new PlayerStanding(RepLevel.SUSPICIOUS, -20, RED));
            } finally {
                StarsectorSettingsFake.clearSettings();
            }
        }

        @Test
        void readsTheNeutralStandingAsAStandingRatherThanAsAbsence() {

            var factionMock = mock(FactionAPI.class);

            // A faction the player has no history with reports nought, which is a standing on the
            // scale - only a faction that cannot be looked up at all answers no standing.
            when(factionMock.getRelationship(Factions.PLAYER))
                .thenReturn(0.0f);
            when(factionMock.getRelColor(Factions.PLAYER))
                .thenReturn(GREEN);

            assertThat(readPlayerStanding(factionMock))
                .contains(new PlayerStanding(RepLevel.NEUTRAL, 0, GREEN));
        }
    }
}
