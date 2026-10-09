package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins how a faction is looked up by ID: a known ID reads its faction, and an absent sector, an
 * unknown ID or an ID naming nobody each read as no faction.
 */
final class SectorFactionsTests {

    private static final String HEGEMONY = "hegemony";

    @Nested
    class FindFaction {

        @Test
        void readsTheFactionTheIdNames() {

            var sectorMock = mock(SectorAPI.class);
            var factionMock = mock(FactionAPI.class);

            when(sectorMock.getFaction(HEGEMONY))
                .thenReturn(factionMock);

            assertThat(SectorFactions.findFaction(sectorMock, HEGEMONY))
                .isSameAs(factionMock);
        }

        @Test
        void answersNullForAnIdTheSectorDoesNotKnow() {

            var sectorMock = mock(SectorAPI.class);

            assertThat(SectorFactions.findFaction(sectorMock, "ghost_faction"))
                .isNull();
        }

        @Test
        void answersNullForANullSector() {

            assertThat(SectorFactions.findFaction(null, HEGEMONY))
                .isNull();
        }

        @Test
        void answersNullForANullIdWithoutAskingTheSector() {

            var sectorMock = mock(SectorAPI.class);

            assertThat(SectorFactions.findFaction(sectorMock, null))
                .isNull();
            verify(sectorMock, never())
                .getFaction(any());
        }

        @Test
        void answersNullForABlankIdWithoutAskingTheSector() {

            var sectorMock = mock(SectorAPI.class);

            assertThat(SectorFactions.findFaction(sectorMock, " "))
                .isNull();
            verify(sectorMock, never())
                .getFaction(any());
        }
    }
}
