package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;

import kmlib.starsector.factions.StarsectorPlayerFactionResolver.PlayerFactionSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

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

    @AfterEach
    void resetPlaceholderSet() {
        // The placeholder set is process-static; reset to defaults after
        // each test so a `setUnestablishedPlayerFactionNames` call in one
        // case cannot leak into another.
        StarsectorPlayerFactionResolver.setUnestablishedPlayerFactionNames(null);
    }

    @Test
    void establishedIsFalseOnDefaultNameAndNoMarkets() {
        var established = StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                stubSource("Independent", false));

        assertThat(established).isFalse();
    }

    @Test
    void establishedIsTrueWhenDisplayNameHasBeenCustomised() {
        // Nex's custom-faction-at-game-start case: name customised
        // before any colony exists. Either signal alone passes.
        var established = StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                stubSource("Concord", false));

        assertThat(established).isTrue();
    }

    @Test
    void establishedIsTrueWhenPlayerOwnsAMarket() {
        // Vanilla rename-prompt-dismissed case: name stays default,
        // player still owns a colony.
        var established = StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
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
        var established = StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
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
        var factionMock = Mockito.mock(FactionAPI.class);
        Mockito.when(factionMock.getDisplayName()).thenReturn("Hegemony");

        var resolved = StarsectorPlayerFactionResolver.resolveDisplayName(
                factionMock, "Independent");

        assertThat(resolved).isEqualTo("Hegemony");
    }

    @Test
    void resolveDisplayNameFallsBackOnPlaceholderName() {
        var factionMock = Mockito.mock(FactionAPI.class);
        Mockito.when(factionMock.getDisplayName()).thenReturn("player");

        var resolved = StarsectorPlayerFactionResolver.resolveDisplayName(
                factionMock, "Independent");

        assertThat(resolved).isEqualTo("Independent");
    }

    @Test
    void resolveDisplayNameFallsBackOnNullFaction() {
        var resolved = StarsectorPlayerFactionResolver.resolveDisplayName(
                null, "faction leader");

        assertThat(resolved).isEqualTo("faction leader");
    }

    @Test
    void resolveDisplayNameFallsBackOnBlankDisplayName() {
        var factionMock = Mockito.mock(FactionAPI.class);
        Mockito.when(factionMock.getDisplayName()).thenReturn("   ");

        var resolved = StarsectorPlayerFactionResolver.resolveDisplayName(
                factionMock, "Independent");

        assertThat(resolved).isEqualTo("Independent");
    }

    @Test
    void unestablishedSetExtensionTakesEffectOnEstablishedCheck() {
        // A modder pointing a different launcher environment at a new
        // placeholder name extends the live set; the established-check
        // must read the updated set rather than the built-in defaults
        // so the extension actually takes effect.
        StarsectorPlayerFactionResolver.setUnestablishedPlayerFactionNames(
                Set.of("Unaffiliated"));

        assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                stubSource("Unaffiliated", false))).isFalse();
        // The previous defaults are no longer recognised - replace,
        // not merge, is the documented contract.
        assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                stubSource("Independent", false))).isTrue();
    }

    @Test
    void unestablishedSetExtensionTakesEffectOnDisplayNameFallback() {
        // The display-name fallback shares the set with the established
        // check, so an extension must steer the fallback too.
        StarsectorPlayerFactionResolver.setUnestablishedPlayerFactionNames(
                Set.of("Unaffiliated"));
        var factionMock = Mockito.mock(FactionAPI.class);
        Mockito.when(factionMock.getDisplayName()).thenReturn("Unaffiliated");

        var resolved = StarsectorPlayerFactionResolver.resolveDisplayName(
                factionMock, "faction");

        assertThat(resolved).isEqualTo("faction");
    }

    @Test
    void nullPlaceholderSetResetsToDefaults() {
        // Null is the documented reset signal so callers do not need to
        // hold onto a snapshot of the defaults to restore them.
        StarsectorPlayerFactionResolver.setUnestablishedPlayerFactionNames(
                Set.of("Unaffiliated"));
        StarsectorPlayerFactionResolver.setUnestablishedPlayerFactionNames(null);

        assertThat(StarsectorPlayerFactionResolver.getUnestablishedPlayerFactionNames())
                .containsExactlyInAnyOrder("Independent", "player", "Player");
    }

    @Test
    void emptyPlaceholderSetResetsToDefaults() {
        // An empty set would make every displayName register as
        // established, which defeats the fallback prose the resolver
        // exists for; treat it as a reset.
        StarsectorPlayerFactionResolver.setUnestablishedPlayerFactionNames(Set.of());

        assertThat(StarsectorPlayerFactionResolver.getUnestablishedPlayerFactionNames())
                .containsExactlyInAnyOrder("Independent", "player", "Player");
    }

    @Test
    void getterReturnsUnmodifiableView() {
        // Defensive read: callers cannot mutate the live set behind
        // the resolver's back, so an extension has to go through the
        // setter (which copies).
        var live =
                StarsectorPlayerFactionResolver.getUnestablishedPlayerFactionNames();

        try {
            live.add("Unaffiliated");
            assertThat(false).as("expected UnsupportedOperationException").isTrue();
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    void setterCopiesInputSet() {
        // A caller mutating their original collection after the setter
        // returns must not bleed into the resolver's live set.
        var caller = new HashSet<String>(Set.of("Unaffiliated"));
        StarsectorPlayerFactionResolver.setUnestablishedPlayerFactionNames(caller);
        caller.add("StillStrangers");

        assertThat(StarsectorPlayerFactionResolver.getUnestablishedPlayerFactionNames())
                .containsExactly("Unaffiliated");
    }

    private static PlayerFactionSource stubSource(String displayName, boolean ownsMarket) {
        var factionMock = Mockito.mock(FactionAPI.class);
        Mockito.when(factionMock.getDisplayName()).thenReturn(displayName);
        return new PlayerFactionSource() {
            @Override
            public FactionAPI playerFaction() {
                return factionMock;
            }

            @Override
            public boolean ownsAnyMarket() {
                return ownsMarket;
            }
        };
    }
}
