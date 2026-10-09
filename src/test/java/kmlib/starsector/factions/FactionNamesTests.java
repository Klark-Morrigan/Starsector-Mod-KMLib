package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.testfixtures.starsector.memory.StoredMemoryFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins how a faction's two authored names are read: the form picks the name, a null faction reads
 * as null, and the fullest-name read prefers the long name, falls back to the short one only where
 * the long is blank, trims both and never answers blank. A row's label is the long name or, where the
 * faction carries none, the ID it was asked about. The display-name reads show any faction's name, but
 * hold the player faction's back until its sector records it as set up.
 */
final class FactionNamesTests {

    private static final String HEGEMONY_ID = "hegemony";
    private static final String SHORT_NAME = "Hegemony";
    private static final String LONG_NAME = "The Hegemony";
    private static final String PLAYER_FACTION_ID = "player";

    // A faction whose two names are the ones given.
    private static FactionAPI createNamedFaction(String shortName, String longName) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getDisplayName())
            .thenReturn(shortName);
        when(factionMock.getDisplayNameLong())
            .thenReturn(longName);

        return factionMock;
    }

    // The player faction under the given display name.
    private static FactionAPI createPlayerFaction(String displayName) {

        var factionMock = createNamedFaction(displayName, null);

        when(factionMock.getId())
            .thenReturn(PLAYER_FACTION_ID);
        when(factionMock.isPlayerFaction())
            .thenReturn(true);

        return factionMock;
    }

    // A sector whose memory records the faction-naming dialog as shown or not.
    private static SectorAPI createSector(boolean hasShownNamingDialog) {

        var memoryFake = new StoredMemoryFake();

        if (hasShownNamingDialog) {
            memoryFake.storeValue("$shownFactionConfigDialog", true);
        }

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getMemoryWithoutUpdate())
            .thenReturn(memoryFake.getMemory());

        return sectorMock;
    }

    @Nested
    class ResolveDisplayName {

        @Test
        void readsAnotherFactionsNameWhateverTheSectorRecords() {

            var factionMock = createNamedFaction(SHORT_NAME, LONG_NAME);

            assertThat(FactionNames.resolveDisplayName(createSector(false), factionMock, "fallback"))
                .isEqualTo("Hegemony");
        }

        @Test
        void fallsBackForThePlayerFactionBeforeItIsSetUp() {

            var factionMock = createPlayerFaction("Your");

            assertThat(FactionNames.resolveDisplayName(createSector(false), factionMock, "fallback"))
                .isEqualTo("fallback");
        }

        @Test
        void readsThePlayerFactionsNameOnceItIsSetUp() {

            var factionMock = createPlayerFaction("Concord");

            assertThat(FactionNames.resolveDisplayName(createSector(true), factionMock, "fallback"))
                .isEqualTo("Concord");
        }

        @Test
        void fallsBackForThePlayerFactionWithNoSector() {

            var factionMock = createPlayerFaction("Concord");

            assertThat(FactionNames.resolveDisplayName(null, factionMock, "fallback"))
                .isEqualTo("fallback");
        }

        @Test
        void fallsBackForABlankName() {

            var factionMock = createNamedFaction("   ", LONG_NAME);

            assertThat(FactionNames.resolveDisplayName(createSector(true), factionMock, "fallback"))
                .isEqualTo("fallback");
        }

        @Test
        void fallsBackForANullFaction() {

            assertThat(FactionNames.resolveDisplayName(createSector(true), null, "fallback"))
                .isEqualTo("fallback");
        }
    }

    @Nested
    class ResolveDisplayNameOrId {

        @Test
        void readsTheNameWhereThereIsOneToShow() {

            var factionMock = createPlayerFaction("Concord");

            assertThat(FactionNames.resolveDisplayNameOrId(createSector(true), factionMock))
                .isEqualTo("Concord");
        }

        @Test
        void fallsBackToTheIdForThePlayerFactionBeforeItIsSetUp() {

            var factionMock = createPlayerFaction("Your");

            assertThat(FactionNames.resolveDisplayNameOrId(createSector(false), factionMock))
                .isEqualTo("player");
        }

        @Test
        void answersNullForANullFaction() {

            assertThat(FactionNames.resolveDisplayNameOrId(createSector(true), null))
                .isNull();
        }
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

    @Nested
    class ResolveLabel {

        @Test
        void readsTheTrimmedLongName() {

            var factionMock = createNamedFaction(SHORT_NAME, " The Hegemony ");

            assertThat(FactionNames.resolveLabel(factionMock, HEGEMONY_ID))
                .isEqualTo("The Hegemony");
        }

        @Test
        void fallsBackToTheIdWhenTheLongNameIsBlank() {

            var factionMock = createNamedFaction(SHORT_NAME, " ");

            assertThat(FactionNames.resolveLabel(factionMock, HEGEMONY_ID))
                .isEqualTo("hegemony");
        }

        @Test
        void fallsBackToTheIdForANullFaction() {

            assertThat(FactionNames.resolveLabel(null, HEGEMONY_ID))
                .isEqualTo("hegemony");
        }
    }
}
