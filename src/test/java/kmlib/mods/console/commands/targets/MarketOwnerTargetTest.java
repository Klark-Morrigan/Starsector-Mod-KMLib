package kmlib.mods.console.commands.targets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins how the owner of a resolved target is named in something the player reads: by the faction's
 * own name where it has one, and by its ID where the name is a placeholder standing in for an
 * identity the faction has not been given yet.
 *
 * <p>Cases live under a {@link Nested} group named for the method under test.
 */
final class MarketOwnerTargetTest {

    private static final String PLAYER_FACTION_ID = "player";

    @Nested
    class ReadOwnerName {

        @Test
        void namesAFactionByItsOwnDisplayName() {

            var target = buildTargetOwnedBy("hegemony", "Hegemony");

            assertThat(target.readOwnerName())
                .isEqualTo("Hegemony");
        }

        @Test
        void namesThePlayerFactionByIdWhileItReportsTheVanillaPlaceholder() {
            // "Independent" is what the player's faction reports before its first colony, and in
            // a sentence about who now holds a place it reads as somebody else entirely.
            var target = buildTargetOwnedBy(PLAYER_FACTION_ID, "Independent");

            assertThat(target.readOwnerName())
                .isEqualTo(PLAYER_FACTION_ID);
        }

        @Test
        void namesAFactionByIdWhenItReportsNoNameAtAll() {

            var target = buildTargetOwnedBy("some_mod_faction", "");

            assertThat(target.readOwnerName())
                .isEqualTo("some_mod_faction");
        }
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
}
