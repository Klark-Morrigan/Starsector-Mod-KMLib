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
 * Pins the set-up rule: the player faction counts as established exactly when the handed sector's memory
 * holds the key the faction-naming dialog sets, whatever its display name says, and an absent sector or
 * memory reads as not established.
 */
final class StarsectorPlayerFactionResolverTests {

    private static final String FACTION_CONFIG_SHOWN_KEY = "$shownFactionConfigDialog";
    private static final String PLAYER_FACTION_ID = "player";

    // The vanilla player faction's unnamed spec name, and its Chinese core localisation's
    // translation: "you" (U+4F60).
    private static final String VANILLA_UNNAMED_NAME = "Your";
    private static final String CHINESE_UNNAMED_NAME = "你";

    @Nested
    class IsPlayerFactionEstablished {

        @Test
        void answersTrueWhenTheSectorRecordsTheNamingDialog() {

            var sector = createSector(VANILLA_UNNAMED_NAME, true);

            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(sector))
                .isTrue();
        }

        @Test
        void answersFalseForTheVanillaUnnamedNameWithoutTheKey() {

            var sector = createSector(VANILLA_UNNAMED_NAME, false);

            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(sector))
                .isFalse();
        }

        @Test
        void answersFalseForTheChineseUnnamedNameWithoutTheKey() {

            var sector = createSector(CHINESE_UNNAMED_NAME, false);

            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(sector))
                .isFalse();
        }

        @Test
        void answersFalseForACustomNameWithoutTheKey() {
            // The name is not evidence either way: only the key is.
            var sector = createSector("Concord", false);

            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(sector))
                .isFalse();
        }

        @Test
        void answersFalseForANullSector() {

            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(null))
                .isFalse();
        }

        @Test
        void answersFalseForASectorWithNoMemoryYet() {

            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(mock(SectorAPI.class)))
                .isFalse();
        }
    }

    @Nested
    class FindEstablishedPlayerFactionId {

        @Test
        void readsThePlayerFactionIdOnceEstablished() {

            var sector = createSector(VANILLA_UNNAMED_NAME, true);

            assertThat(StarsectorPlayerFactionResolver.findEstablishedPlayerFactionId(sector))
                .isEqualTo("player");
        }

        @Test
        void answersNullBeforeTheSectorRecordsTheNamingDialog() {

            var sector = createSector(VANILLA_UNNAMED_NAME, false);

            assertThat(StarsectorPlayerFactionResolver.findEstablishedPlayerFactionId(sector))
                .isNull();
        }

        @Test
        void answersNullForANullSector() {

            assertThat(StarsectorPlayerFactionResolver.findEstablishedPlayerFactionId(null))
                .isNull();
        }

        @Test
        void answersNullForABlankPlayerFactionId() {

            var sector = createSector(VANILLA_UNNAMED_NAME, true);

            when(sector.getPlayerFaction().getId())
                .thenReturn("  ");

            assertThat(StarsectorPlayerFactionResolver.findEstablishedPlayerFactionId(sector))
                .isNull();
        }

        @Test
        void answersNullWhenTheSectorHoldsNoPlayerFaction() {

            var memoryFake = new StoredMemoryFake();
            var sectorMock = mock(SectorAPI.class);

            memoryFake.storeValue(FACTION_CONFIG_SHOWN_KEY, true);
            when(sectorMock.getMemoryWithoutUpdate())
                .thenReturn(memoryFake.getMemory());

            assertThat(StarsectorPlayerFactionResolver.findEstablishedPlayerFactionId(sectorMock))
                .isNull();
        }
    }

    // A sector whose player faction carries the given display name, and whose memory records the
    // naming dialog as shown or not.
    private static SectorAPI createSector(String playerFactionName, boolean hasShownNamingDialog) {

        var memoryFake = new StoredMemoryFake();

        if (hasShownNamingDialog) {
            memoryFake.storeValue(FACTION_CONFIG_SHOWN_KEY, true);
        }

        var playerFactionMock = mock(FactionAPI.class);

        when(playerFactionMock.getId())
            .thenReturn(PLAYER_FACTION_ID);
        when(playerFactionMock.getDisplayName())
            .thenReturn(playerFactionName);

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getMemoryWithoutUpdate())
            .thenReturn(memoryFake.getMemory());
        when(sectorMock.getPlayerFaction())
            .thenReturn(playerFactionMock);

        return sectorMock;
    }
}
