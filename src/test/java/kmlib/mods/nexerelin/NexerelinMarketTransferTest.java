package kmlib.mods.nexerelin;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.mods.nexerelin.NexerelinDeclineAssertions.assertDeclined;
import static kmlib.mods.nexerelin.NexerelinDeclineAssertions.assertDeclinedBecauseOf;
import static kmlib.mods.nexerelin.SectorFactionFixture.buildSectorHolding;
import static kmlib.testfixtures.starsector.settings.StubbedModIds.NEXERELIN;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pins every answer that is a decline, which is the whole of what can be checked here. The hand-over
 * itself is the mod's own routine, reached only once the presence gate has passed - it stands up
 * intel, moves standing between factions and re-reads the economy against a live game - so it is
 * verified in play rather than pinned by a case that would have to build the mod a game to run in.
 *
 * <p>That makes the declines the load-bearing half anyway: each one is a hand-over the caller has to
 * be free to compose itself, and a decline that silently succeeded would leave a colony with the
 * owner it was being taken from and nothing reporting why. The mod-absent cases matter most, being
 * what every install without the mod takes, and they additionally pin that nothing reaches for a
 * class that is not there.
 */
final class NexerelinMarketTransferTest {

    @Nested
    class TransferOwnership {

        @Test
        void declinesAHandOverWhileTheModIsAbsent() {
            // What every install without the mod answers, and the answer that keeps the class
            // naming one of its types from being resolved at all.
            var market = buildColonyHeldByAFaction();

            ModStateScopes.runWithModEnabled(NEXERELIN, false, () ->
                assertDeclined(NexerelinMarketTransfer.transferOwnership(
                        buildSectorHolding(Factions.PLAYER),
                        market,
                        Factions.PLAYER)));

            verifyNoInteractions(market);
        }

        @Test
        void declinesAHandOverBeforeTheGameSettingsAreUp() {
            // A read taken outside a running game, which cannot say whether the mod is there - so
            // it answers as an install without it does.
            ModStateScopes.runWithoutGameSettings(() ->
                assertDeclined(NexerelinMarketTransfer.transferOwnership(
                        buildSectorHolding(Factions.PLAYER),
                        buildColonyHeldByAFaction(),
                        Factions.PLAYER)));
        }

        @Test
        void declinesAnIncomingOwnerNoFactionAnswersTo() {
            // The routine reads that mod's settings, tariffs and colours off the faction rather
            // than off an id, so an id the sector does not know is a decline rather than a
            // hand-over that dies partway through one.
            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertDeclinedBecauseOf(
                    NexerelinMarketTransfer.transferOwnership(
                        buildSectorHolding(Factions.PLAYER),
                        buildColonyHeldByAFaction(),
                        "a_faction_this_sector_does_not_have"),
                    "no faction with id 'a_faction_this_sector_does_not_have'"));
        }

        @Test
        void declinesAColonyFlyingNoFlagAtAll() {
            // The outgoing owner reaches the routine as a faction too - it reads the id off it and
            // counts what that faction has left afterwards - so a colony nobody is recorded as
            // holding is a decline and the caller hands it over itself.
            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertDeclined(NexerelinMarketTransfer.transferOwnership(
                        buildSectorHolding(Factions.PLAYER),
                        buildColonyHeldByNobody(),
                        Factions.PLAYER)));
        }

        @Test
        void declinesAHandOverNamingNoIncomingOwner() {

            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertDeclined(NexerelinMarketTransfer.transferOwnership(
                        buildSectorHolding(Factions.PLAYER),
                        buildColonyHeldByAFaction(),
                        null)));
        }

        @Test
        void declinesANullMarket() {

            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertDeclined(NexerelinMarketTransfer.transferOwnership(
                        buildSectorHolding(Factions.PLAYER),
                        null,
                        Factions.PLAYER)));
        }

        @Test
        void declinesAHandOverWithNoSectorToReadTheIncomingOwnerFrom() {

            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertDeclined(NexerelinMarketTransfer.transferOwnership(
                        null,
                        buildColonyHeldByAFaction(),
                        Factions.PLAYER)));
        }
    }

    // A colony still flying the flag of the owner it is being taken from, which is what the mod's
    // routine reads that owner off.
    private static MarketAPI buildColonyHeldByAFaction() {
        return buildColonyHeldBy(mock(FactionAPI.class));
    }

    // A colony no faction is recorded as holding - nothing to hand the place over from.
    private static MarketAPI buildColonyHeldByNobody() {
        return buildColonyHeldBy(null);
    }

    private static MarketAPI buildColonyHeldBy(FactionAPI owner) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(owner);

        return marketMock;
    }
}
