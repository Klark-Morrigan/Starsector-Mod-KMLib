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
class FactionNamesTests {

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

    @Nested
    class ResolveName {

        @Test
        void readsTheShortNameForTheShortForm() {

            var faction = createNamedFaction(SHORT_NAME, LONG_NAME);

            assertThat(FactionNames.resolveName(faction, FactionNameForm.SHORT))
                .isEqualTo("Hegemony");
        }

        @Test
        void readsTheLongNameForTheLongForm() {

            var faction = createNamedFaction(SHORT_NAME, LONG_NAME);

            assertThat(FactionNames.resolveName(faction, FactionNameForm.LONG))
                .isEqualTo("The Hegemony");
        }

        @Test
        void keepsABlankNameAsAuthored() {

            var faction = createNamedFaction(SHORT_NAME, " ");

            assertThat(FactionNames.resolveName(faction, FactionNameForm.LONG))
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
            var sectorMock = mock(SectorAPI.class);
            var factions = List.of(
                createNamedFaction(SHORT_NAME, LONG_NAME),
                createNamedFaction("Pather", " "));

            when(sectorMock.getAllFactions())
                .thenReturn(factions);

            assertThat(FactionNames.listEveryName(sectorMock))
                .containsExactly("Hegemony", "The Hegemony", "Pather");
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

            var faction = createNamedFaction(SHORT_NAME, LONG_NAME);

            assertThat(FactionNames.resolveFullestName(faction))
                .isEqualTo("The Hegemony");
        }

        @Test
        void fallsBackToTheShortNameWhenTheLongIsBlank() {

            var faction = createNamedFaction(SHORT_NAME, "  ");

            assertThat(FactionNames.resolveFullestName(faction))
                .isEqualTo("Hegemony");
        }

        @Test
        void trimsTheNameItReads() {

            var faction = createNamedFaction(null, "  The Hegemony ");

            assertThat(FactionNames.resolveFullestName(faction))
                .isEqualTo("The Hegemony");
        }

        @Test
        void answersNullWhenNeitherNameHasText() {

            var faction = createNamedFaction("", null);

            assertThat(FactionNames.resolveFullestName(faction))
                .isNull();
        }

        @Test
        void answersNullForANullFaction() {

            assertThat(FactionNames.resolveFullestName(null))
                .isNull();
        }
    }
}
