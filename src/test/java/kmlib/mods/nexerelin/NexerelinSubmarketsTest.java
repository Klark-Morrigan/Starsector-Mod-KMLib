package kmlib.mods.nexerelin;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.mods.nexerelin.NexerelinDeclineAssertions.assertDeclined;
import static kmlib.mods.nexerelin.NexerelinDeclineAssertions.assertDeclinedBecauseOf;
import static kmlib.testfixtures.starsector.settings.StubbedModIds.NEXERELIN;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Pins every answer that is a decline, which is the whole of what can be checked here. The rule
 * itself is the mod's own, reached only once the presence gate has passed - it reads that mod's
 * lists of modded markets, opens and closes counters and restocks them against a live economy - so
 * it is verified in play rather than pinned by a case that would have to build the mod a game to
 * run in.
 *
 * <p>That makes the declines the load-bearing half anyway: each one is a set of counters the caller
 * has to be free to decide itself, and a decline that silently succeeded would leave a colony
 * trading over the counters of the owner it just left. The mod-absent cases matter most, being what
 * every install without the mod takes, and they additionally pin that nothing reaches for a class
 * that is not there.
 */
final class NexerelinSubmarketsTest {

    // The owner a colony is posed as leaving whenever the case is not about who held it.
    private static final String FACTION_OWNER_ID = "hegemony";

    @Nested
    class ApplySubmarkets {

        @Test
        void declinesTheCountersWhileTheModIsAbsent() {
            // What every install without the mod answers, and the answer that keeps the class
            // naming one of its types from being resolved at all.
            var marketMock = mock(MarketAPI.class);

            ModStateScopes.runWithModEnabled(NEXERELIN, false, () ->
                assertDeclinedBecauseOf(
                    NexerelinSubmarkets.applySubmarkets(
                        marketMock,
                        FACTION_OWNER_ID,
                        Factions.PLAYER),
                    "Nexerelin is not enabled"));

            verifyNoInteractions(marketMock);
        }

        @Test
        void declinesTheCountersBeforeTheGameSettingsAreUp() {
            // A read taken outside a running game, which cannot say whether the mod is there - so
            // it answers as an install without it does.
            ModStateScopes.runWithoutGameSettings(() ->
                assertDeclined(NexerelinSubmarkets.applySubmarkets(
                        mock(MarketAPI.class),
                        FACTION_OWNER_ID,
                        Factions.PLAYER)));
        }

        @Test
        void declinesAChangeNamingNoIncomingOwner() {
            // The mod's routine reads every one of its verdicts off the incoming ID, so a change
            // that names nobody is a decline rather than a set of counters decided against nothing.
            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertDeclined(NexerelinSubmarkets.applySubmarkets(
                        mock(MarketAPI.class),
                        FACTION_OWNER_ID,
                        null)));
        }

        @Test
        void declinesANullMarket() {

            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertDeclined(NexerelinSubmarkets.applySubmarkets(
                        null,
                        FACTION_OWNER_ID,
                        Factions.PLAYER)));
        }
    }
}
