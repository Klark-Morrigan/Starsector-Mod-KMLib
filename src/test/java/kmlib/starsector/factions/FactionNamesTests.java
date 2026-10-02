package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins how a faction's two authored names are read: the form picks the name, a null faction reads
 * as null, and the fullest-name read prefers the long name, falls back to the short one only where
 * the long is blank, trims both and never answers blank.
 */
final class FactionNamesTests {

    private static final String SHORT_NAME = "Hegemony";
    private static final String LONG_NAME = "The Hegemony";

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

    @Nested
    class ResolveName {

        @Test
        void readsTheShortNameForTheShortForm() {

            var factionMock = createNamedFaction(SHORT_NAME, LONG_NAME);

            assertThat(FactionNames.resolveName(factionMock, FactionNameForm.SHORT))
                .isEqualTo("Hegemony");
        }

        @Test
        void readsTheLongNameForTheLongForm() {

            var factionMock = createNamedFaction(SHORT_NAME, LONG_NAME);

            assertThat(FactionNames.resolveName(factionMock, FactionNameForm.LONG))
                .isEqualTo("The Hegemony");
        }

        @Test
        void keepsABlankNameAsAuthored() {

            var factionMock = createNamedFaction(SHORT_NAME, " ");

            assertThat(FactionNames.resolveName(factionMock, FactionNameForm.LONG))
                .isEqualTo(" ");
        }

        @Test
        void answersNullForANullFaction() {

            assertThat(FactionNames.resolveName(null, FactionNameForm.SHORT))
                .isNull();
        }
    }

    @Nested
    class ListEveryName {

        @Test
        void readsEveryFactionsShortAndLongNameLeavingBlanksOut() {
            // Plenty of modded factions declare no long name; a blank is nothing a caller could draw.
            var sectorMock = createSectorOf(
                createNamedFaction(SHORT_NAME, LONG_NAME),
                createNamedFaction("Pather", " "));

            assertThat(FactionNames.listEveryName(sectorMock))
                .containsExactly("Hegemony", "Pather", "The Hegemony");
        }

        @Test
        void readsANameAuthoredAsBothFormsOnce() {
            // A second copy says nothing the first did not, and would only be read again.
            var sectorMock = createSectorOf(createNamedFaction("Tri-Tachyon", "Tri-Tachyon"));

            assertThat(FactionNames.listEveryName(sectorMock))
                .containsExactly("Tri-Tachyon");
        }

        @Test
        void answersNothingForANullSector() {

            assertThat(FactionNames.listEveryName(null))
                .isEmpty();
        }
    }

    @Nested
    class ResolveFullestName {

        @Test
        void readsTheLongNameWhenItHasText() {

            var factionMock = createNamedFaction(SHORT_NAME, LONG_NAME);

            assertThat(FactionNames.resolveFullestName(factionMock))
                .isEqualTo("The Hegemony");
        }

        @Test
        void fallsBackToTheShortNameWhenTheLongIsBlank() {

            var factionMock = createNamedFaction(SHORT_NAME, "  ");

            assertThat(FactionNames.resolveFullestName(factionMock))
                .isEqualTo("Hegemony");
        }

        @Test
        void trimsTheNameItReads() {

            var factionMock = createNamedFaction(null, "  The Hegemony ");

            assertThat(FactionNames.resolveFullestName(factionMock))
                .isEqualTo("The Hegemony");
        }

        @Test
        void answersNullWhenNeitherNameHasText() {

            var factionMock = createNamedFaction("", null);

            assertThat(FactionNames.resolveFullestName(factionMock))
                .isNull();
        }

        @Test
        void answersNullForANullFaction() {

            assertThat(FactionNames.resolveFullestName(null))
                .isNull();
        }
    }
}
