package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins how a faction is looked up by ID: a known ID reads its faction, and an absent sector, an
 * unknown ID or an ID naming nobody each read as no faction. Pins the listing of every faction's names:
 * both forms, blanks left out, each text once.
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

    @Nested
    class ListFactionNames {

        @Test
        void readsEveryFactionsShortAndLongNameLeavingBlanksOut() {
            // Plenty of modded factions declare no long name; a blank is nothing a caller could draw.
            var sectorMock = createSectorOf(
                createNamedFaction("Hegemony", "The Hegemony"),
                createNamedFaction("Pather", " "));

            assertThat(SectorFactions.listFactionNames(sectorMock))
                .containsExactly("Hegemony", "Pather", "The Hegemony");
        }

        @Test
        void readsANameAuthoredAsBothFormsOnce() {
            // A second copy says nothing the first did not, and would only be read again.
            var sectorMock = createSectorOf(createNamedFaction("Tri-Tachyon", "Tri-Tachyon"));

            assertThat(SectorFactions.listFactionNames(sectorMock))
                .containsExactly("Tri-Tachyon");
        }

        @Test
        void answersNothingForANullSector() {

            assertThat(SectorFactions.listFactionNames(null))
                .isEmpty();
        }
    }

    // A faction whose two names are the ones given.
    private static FactionAPI createNamedFaction(String shortName, String longName) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getDisplayName())
            .thenReturn(shortName);
        when(factionMock.getDisplayNameLong())
            .thenReturn(longName);

        return factionMock;
    }

    // A sector holding the given factions, in that order.
    private static SectorAPI createSectorOf(FactionAPI... factionMocks) {

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getAllFactions())
            .thenReturn(List.of(factionMocks));

        return sectorMock;
    }
}
