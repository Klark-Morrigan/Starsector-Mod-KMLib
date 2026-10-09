package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.starsector.WalkCountCapture;
import kmlib.starsector.factions.StarsectorPlayerFactionResolver.PlayerFactionSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the resolver's contracts:
 * <ul>
 *   <li>the established check returns {@code true} on either signal alone (custom display name OR
 *       owns a market), asked of a stub source or of one named sector;</li>
 *   <li>{@link StarsectorPlayerFactionResolver#resolveDisplayName(FactionAPI, String)} returns the
 *       live display name when populated and the caller's fallback when the name lands in the
 *       placeholder set (or is null / blank);</li>
 *   <li>the placeholder set is replaced rather than merged, reset by null or empty, copied on the
 *       way in and read-only on the way out.</li>
 * </ul>
 *
 * <p>Tests inject a stub {@link PlayerFactionSource} via the
 * package-private overload, so no static mocking is required.
 */
class StarsectorPlayerFactionResolverTests {

    @AfterEach
    void resetPlaceholderSet() {

        // The placeholder set is process-static; reset to defaults after
        // each test so a `setUnestablishedPlayerFactionNames` call in one
        // case cannot leak into another.
        StarsectorPlayerFactionResolver.setUnestablishedPlayerFactionNames(null);
    }

    @Nested
    class IsPlayerFactionEstablished {

        @Test
        void establishedIsFalseOnDefaultNameAndNoMarkets() {

            var established = StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                stubSource("Independent", false));

            assertThat(established)
                .isFalse();
        }

        @Test
        void establishedIsTrueWhenDisplayNameHasBeenCustomised() {
            // Nex's custom-faction-at-game-start case: name customised
            // before any colony exists. Either signal alone passes.
            var established = StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                stubSource("Concord", false));

            assertThat(established)
                .isTrue();
        }

        @Test
        void establishedIsTrueWhenPlayerOwnsAMarket() {
            // Vanilla rename-prompt-dismissed case: name stays default,
            // player still owns a colony.
            var established = StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                stubSource("Independent", true));

            assertThat(established)
                .isTrue();
        }

        @Test
        void establishedFalseForBothNexPlayerCasings() {
            // Both the lowercase ID (Nex's stock player.faction) and the
            // capitalised variant must resolve as unestablished.
            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                    stubSource("player", false)))
                .isFalse();

            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                    stubSource("Player", false)))
                .isFalse();
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

            assertThat(established)
                .isFalse();
        }

        @Test
        void establishedIsFalseForNoSector() {
            // The sector-bound form of the same question: nothing to ask
            // holds no player identity, so it answers rather than throws.
            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished((SectorAPI) null))
                .isFalse();
        }

        @Test
        void establishedReadsTheDisplayNameOfTheNamedSector() {

            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                    stubSector("Concord", false)))
                .isTrue();

            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                    stubSector("Independent", false)))
                .isFalse();
        }

        @Test
        void establishedReadsTheMarketsOfTheNamedSector() {
            // Default name, so the market signal is what decides it - and
            // the market walked is that sector's, which is the whole point
            // of the overload: Misc reads the sector the game is running.
            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                    stubSector("Independent", true)))
                .isTrue();
        }

        @Test
        void establishedCountsTheMarketsItWalksInTheNamedSector() {
            // The walk reads the economy's whole listing, so it reports that read like every other
            // shared read of the listing does - a profile row would otherwise hide what it cost.
            var sector = stubSector("Independent", false);
            var counts = WalkCountCapture.captureCountsOf(
                () -> StarsectorPlayerFactionResolver.isPlayerFactionEstablished(sector));

            assertThat(counts.readCount(SectorWalkCounters.MARKETS_READ))
                .isEqualTo(1L);
        }

        @Test
        void establishedIsFalseWhenTheSectorRegistersNoPlayerFaction() {
            // The player-faction guard earns its place: a market whose own
            // faction is null would otherwise match a null player faction
            // and report an identity nothing established.
            var placeholderFactionMock = mock(FactionAPI.class);

            when(placeholderFactionMock.getDisplayName())
                .thenReturn("Independent");

            var economyMock = mock(EconomyAPI.class);

            when(economyMock.getMarketsCopy())
                .thenReturn(List.of(mock(MarketAPI.class)));

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getPlayerFaction())
                .thenReturn(placeholderFactionMock);
            when(sectorMock.getEconomy())
                .thenReturn(economyMock);

            // getFaction("player") is left unstubbed: this sector knows no
            // player faction, and the market it holds names no faction either.

            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(sectorMock))
                .isFalse();
        }

        @Test
        void establishedIsFalseWhenTheSectorHasNoEconomy() {
            // A load in progress: the sector exists, the economy does not.
            var playerFactionMock = mock(FactionAPI.class);

            when(playerFactionMock.getDisplayName())
                .thenReturn("Independent");

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getPlayerFaction())
                .thenReturn(playerFactionMock);
            when(sectorMock.getFaction("player"))
                .thenReturn(playerFactionMock);

            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(sectorMock))
                .isFalse();
        }
    }

    @Nested
    class ResolveDisplayName {

        @Test
        void returnsLiveNameForCustomisedFaction() {

            var factionMock = mock(FactionAPI.class);

            when(factionMock.getDisplayName())
                .thenReturn("Hegemony");

            var resolved = StarsectorPlayerFactionResolver.resolveDisplayName(
                factionMock,
                "Independent");

            assertThat(resolved)
                .isEqualTo("Hegemony");
        }

        @Test
        void fallsBackOnPlaceholderName() {

            var factionMock = mock(FactionAPI.class);

            when(factionMock.getDisplayName())
                .thenReturn("player");

            var resolved = StarsectorPlayerFactionResolver.resolveDisplayName(
                factionMock,
                "Independent");

            assertThat(resolved)
                .isEqualTo("Independent");
        }

        @Test
        void fallsBackOnNullFaction() {

            var resolved = StarsectorPlayerFactionResolver.resolveDisplayName(
                null,
                "faction leader");

            assertThat(resolved)
                .isEqualTo("faction leader");
        }

        @Test
        void fallsBackOnBlankDisplayName() {
            var factionMock = mock(FactionAPI.class);
            when(factionMock.getDisplayName()).thenReturn("   ");

            var resolved = StarsectorPlayerFactionResolver.resolveDisplayName(
                factionMock,
                "Independent");

            assertThat(resolved)
                .isEqualTo("Independent");
        }
    }

    @Nested
    class SetUnestablishedPlayerFactionNames {

        @Test
        void unestablishedSetExtensionTakesEffectOnEstablishedCheck() {
            // A modder pointing a different launcher environment at a new
            // placeholder name extends the live set; the established-check
            // must read the updated set rather than the built-in defaults
            // so the extension actually takes effect.
            StarsectorPlayerFactionResolver.setUnestablishedPlayerFactionNames(
                Set.of("Unaffiliated"));

            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                    stubSource("Unaffiliated", false)))
                .isFalse();

            // The previous defaults are no longer recognised - replace,
            // not merge, is the documented contract.
            assertThat(StarsectorPlayerFactionResolver.isPlayerFactionEstablished(
                    stubSource("Independent", false)))
                .isTrue();
        }

        @Test
        void unestablishedSetExtensionTakesEffectOnDisplayNameFallback() {
            // The display-name fallback shares the set with the established
            // check, so an extension must steer the fallback too.
            StarsectorPlayerFactionResolver.setUnestablishedPlayerFactionNames(
                Set.of("Unaffiliated"));

            var factionMock = mock(FactionAPI.class);

            when(factionMock.getDisplayName())
                .thenReturn("Unaffiliated");

            var resolved = StarsectorPlayerFactionResolver.resolveDisplayName(
                factionMock, "faction");

            assertThat(resolved)
                .isEqualTo("faction");
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
        void setterCopiesInputSet() {
            // A caller mutating their original collection after the setter
            // returns must not bleed into the resolver's live set.
            var caller = new HashSet<String>(Set.of("Unaffiliated"));

            StarsectorPlayerFactionResolver.setUnestablishedPlayerFactionNames(caller);
            caller.add("StillStrangers");

            assertThat(StarsectorPlayerFactionResolver.getUnestablishedPlayerFactionNames())
                .containsExactly("Unaffiliated");
        }
    }

    @Nested
    class GetUnestablishedPlayerFactionNames {

        @Test
        void getterReturnsUnmodifiableView() {
            // Defensive read: callers cannot mutate the live set behind
            // the resolver's back, so an extension has to go through the
            // setter (which copies).
            var live =
                StarsectorPlayerFactionResolver.getUnestablishedPlayerFactionNames();

            assertThatThrownBy(() -> live.add("Unaffiliated"))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    /**
     * A sector holding one market, owned either by its player faction or
     * by somebody else - the two inputs the established-check reads,
     * both taken off the sector rather than off the running game.
     */
    private static SectorAPI stubSector(String displayName, boolean ownsMarket) {

        var playerFactionMock = mock(FactionAPI.class);

        when(playerFactionMock.getDisplayName())
            .thenReturn(displayName);

        var otherFactionMock = mock(FactionAPI.class);
        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(ownsMarket ? playerFactionMock : otherFactionMock);

        var economyMock = mock(EconomyAPI.class);

        when(economyMock.getMarketsCopy())
            .thenReturn(List.of(marketMock));

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getPlayerFaction())
            .thenReturn(playerFactionMock);

        // The literal ID vanilla's own market walk looks the player up by.
        when(sectorMock.getFaction("player"))
            .thenReturn(playerFactionMock);
        when(sectorMock.getEconomy())
            .thenReturn(economyMock);

        return sectorMock;
    }

    private static PlayerFactionSource stubSource(String displayName, boolean ownsMarket) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getDisplayName())
            .thenReturn(displayName);

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
