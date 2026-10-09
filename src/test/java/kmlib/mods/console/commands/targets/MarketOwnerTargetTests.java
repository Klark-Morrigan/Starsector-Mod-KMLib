package kmlib.mods.console.commands.targets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.testfixtures.starsector.memory.StoredMemoryFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins how the owner of a resolved target is named in something the player reads: by the faction's
 * own name where it has one, and by its ID where it has none or is the player faction before the sector
 * records it as set up.
 *
 * <p>Cases live under a {@link Nested} group named for the method under test.
 */
final class MarketOwnerTargetTests {

    private static final String PLAYER_FACTION_ID = "player";

    @Nested
    class ReadOwnerName {

        @Test
        void namesAFactionByItsOwnDisplayName() {

            var target = buildTargetOwnedBy("hegemony", "Hegemony");

            assertThat(target.readOwnerName(createSector(false)))
                .isEqualTo("Hegemony");
        }

        @Test
        void namesThePlayerFactionByIdBeforeItIsSetUp() {
            // "Your" is the unnamed player faction's spec name, and in a sentence about who now holds
            // a place it reads as somebody else entirely.
            var target = buildPlayerTarget("Your");

            assertThat(target.readOwnerName(createSector(false)))
                .isEqualTo(PLAYER_FACTION_ID);
        }

        @Test
        void namesThePlayerFactionByItsNameOnceItIsSetUp() {

            var target = buildPlayerTarget("Concord");

            assertThat(target.readOwnerName(createSector(true)))
                .isEqualTo("Concord");
        }

        @Test
        void namesAFactionByIdWhenItReportsNoNameAtAll() {

            var target = buildTargetOwnedBy("some_mod_faction", "");

            assertThat(target.readOwnerName(createSector(false)))
                .isEqualTo("some_mod_faction");
        }
    }

    // A target held by the player faction under the given display name.
    private static MarketOwnerTarget buildPlayerTarget(String displayName) {

        var target = buildTargetOwnedBy(PLAYER_FACTION_ID, displayName);

        when(target.owner().isPlayerFaction())
            .thenReturn(true);

        return target;
    }

    // A target held by a faction under the given ID and display name, the market being beside the
    // point for every case here.
    private static MarketOwnerTarget buildTargetOwnedBy(String factionId, String displayName) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(factionId);
        when(factionMock.getDisplayName())
            .thenReturn(displayName);

        return new MarketOwnerTarget(mock(MarketAPI.class), factionMock);
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
}
