package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import kmlib.starsector.factions.StarsectorPlayerFactionResolver.PlayerFactionSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the resolver's two contracts:
 * <ul>
 *   <li>{@link StarsectorPlayerFactionResolver#isPlayerFactionEstablished(PlayerFactionSource)}
 *       returns {@code true} on either signal alone (custom display
 *       name OR owns a market);</li>
 *   <li>{@link StarsectorPlayerFactionResolver#resolveDisplayName(FactionAPI, String)}
 *       returns the live display name when populated and the caller's
 *       fallback when the name lands in the unestablished placeholder
 *       set (or is null / blank).</li>
 * </ul>
 *
 * <p>Tests inject a stub {@link PlayerFactionSource} via the
 * package-private overload, so no static mocking is required.
 */
class StarsectorPlayerFactionResolverTest {

    @Test
    void establishedIsFalseOnDefaultNameAndNoMarkets() {
        boolean established = StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                stubSource("Independent", false));

        assertThat(established).isFalse();
    }

    @Test
    void establishedIsTrueWhenDisplayNameHasBeenCustomised() {
        // Nex's custom-faction-at-game-start case: name customised
        // before any colony exists. Either signal alone passes.
        boolean established = StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                stubSource("Concord", false));

        assertThat(established).isTrue();
    }

    @Test
    void establishedIsTrueWhenPlayerOwnsAMarket() {
        // Vanilla rename-prompt-dismissed case: name stays default,
        // player still owns a colony.
        boolean established = StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                stubSource("Independent", true));

        assertThat(established).isTrue();
    }

    @Test
    void establishedFalseForBothNexPlayerCasings() {
        // Both the lowercase id (Nex's stock player.faction) and the
        // capitalised variant must resolve as unestablished.
        assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                stubSource("player", false))).isFalse();
        assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                stubSource("Player", false))).isFalse();
    }

    @Test
    void establishedIsFalseWhenPlayerFactionIsNull() {
        // Defensive: a null player faction (no-sector / pre-game-load)
        // resolves as unestablished rather than throwing.
        boolean established = StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                new PlayerFactionSource() {
                    @Override
                    public FactionAPI playerFaction() {
                        return null;
                    }

                    @Override
                    public boolean ownsAnyMarket() {
                        return false;
                    }
                });

        assertThat(established).isFalse();
    }

    @Test
    void resolveDisplayNameReturnsLiveNameForCustomisedFaction() {
        FactionAPI faction = Mockito.mock(FactionAPI.class);
        Mockito.when(faction.getDisplayName()).thenReturn("Hegemony");

        String resolved = StarsectorPlayerFactionResolver.resolveDisplayName(
                faction, "Independent");

        assertThat(resolved).isEqualTo("Hegemony");
    }

    @Test
    void resolveDisplayNameFallsBackOnPlaceholderName() {
        FactionAPI faction = Mockito.mock(FactionAPI.class);
        Mockito.when(faction.getDisplayName()).thenReturn("player");

        String resolved = StarsectorPlayerFactionResolver.resolveDisplayName(
                faction, "Independent");

        assertThat(resolved).isEqualTo("Independent");
    }

    @Test
    void resolveDisplayNameFallsBackOnNullFaction() {
        String resolved = StarsectorPlayerFactionResolver.resolveDisplayName(
                null, "faction leader");

        assertThat(resolved).isEqualTo("faction leader");
    }

    @Test
    void resolveDisplayNameFallsBackOnBlankDisplayName() {
        FactionAPI faction = Mockito.mock(FactionAPI.class);
        Mockito.when(faction.getDisplayName()).thenReturn("   ");

        String resolved = StarsectorPlayerFactionResolver.resolveDisplayName(
                faction, "Independent");

        assertThat(resolved).isEqualTo("Independent");
    }

    private static PlayerFactionSource stubSource(String displayName, boolean ownsMarket) {
        FactionAPI faction = Mockito.mock(FactionAPI.class);
        Mockito.when(faction.getDisplayName()).thenReturn(displayName);
        return new PlayerFactionSource() {
            @Override
            public FactionAPI playerFaction() {
                return faction;
            }

            @Override
            public boolean ownsAnyMarket() {
                return ownsMarket;
            }
        };
    }
}
