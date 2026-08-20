package kmlib.starsector.nexerelin;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.starsector.nexerelin.NexerelinDeclineAssertions.assertDeclined;
import static kmlib.starsector.nexerelin.NexerelinDeclineAssertions.assertDeclinedBecauseOf;
import static kmlib.starsector.nexerelin.SectorFactionFixture.buildSectorHolding;
import static kmlib.testfixtures.starsector.settings.StubbedModIds.NEXERELIN;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pins every answer that is a decline, which is the whole of what can be checked here. The founding
 * itself is the mod's own routine, reached only once the presence gate has passed - it stands up
 * intel, names an administrator and steps the economy against a live game - so it is verified in
 * play rather than pinned by a case that would have to build the mod a game to run in.
 *
 * <p>That makes the declines the load-bearing half anyway: each one is a founding the caller has to
 * be free to compose itself, and a decline that silently succeeded would leave a body uncolonised
 * with nothing reporting why. The mod-absent cases matter most, being what every install without
 * the mod takes, and they additionally pin that nothing reaches for a class that is not there.
 */
final class NexerelinColoniserTest {

    // The owner a founding is posed under whenever the case is not about who holds the colony.
    private static final String FACTION_OWNER_ID = "hegemony";

    // The size a founding is posed at. Every case here is a decline, so it never reaches the mod
    // and the number stands only for a caller having stated one.
    private static final int COLONY_SIZE = 3;

    @Nested
    class EstablishColony {

        @Test
        void declines_a_founding_while_the_mod_is_absent() {
            // What every install without the mod answers, and the answer that keeps the class
            // naming one of its types from being resolved at all.
            var market = buildWorldMarket();

            ModStateScopes.runWithModEnabled(NEXERELIN, false, () ->
                assertDeclined(NexerelinColoniser.establishColony(
                        buildSectorHolding(FACTION_OWNER_ID),
                        market,
                        FACTION_OWNER_ID,
                        COLONY_SIZE)));

            verifyNoInteractions(market);
        }

        @Test
        void declines_a_founding_before_the_game_settings_are_up() {
            // A read taken outside a running game, which cannot say whether the mod is there - so
            // it answers as an install without it does.
            ModStateScopes.runWithoutGameSettings(() ->
                assertDeclined(NexerelinColoniser.establishColony(
                        buildSectorHolding(FACTION_OWNER_ID),
                        buildWorldMarket(),
                        FACTION_OWNER_ID,
                        COLONY_SIZE)));
        }

        @Test
        void declines_a_body_that_is_not_a_planet() {
            // The mod's routine renames a world still carrying its star system's name and reads
            // the system off the planet to do it, so a modded body of another kind is a decline
            // and the caller founds the colony itself.
            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertDeclinedBecauseOf(
                    NexerelinColoniser.establishColony(
                        buildSectorHolding(FACTION_OWNER_ID),
                        buildStationMarket(),
                        FACTION_OWNER_ID,
                        COLONY_SIZE),
                    "is not a planet"));
        }

        @Test
        void declines_a_place_the_game_gave_no_body() {

            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertDeclined(NexerelinColoniser.establishColony(
                        buildSectorHolding(FACTION_OWNER_ID),
                        buildMarketWithoutBody(),
                        FACTION_OWNER_ID,
                        COLONY_SIZE)));
        }

        @Test
        void declines_an_owner_no_faction_answers_to() {
            // The routine reads that mod's settings and tariffs off the faction rather than off an
            // id, so an id the sector does not know is a decline rather than a founding that dies
            // partway through one.
            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertDeclined(NexerelinColoniser.establishColony(
                        buildSectorHolding(FACTION_OWNER_ID),
                        buildWorldMarket(),
                        "a_faction_this_sector_does_not_have",
                        COLONY_SIZE)));
        }

        @Test
        void declines_a_founding_under_no_owner_at_all() {

            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertDeclined(NexerelinColoniser.establishColony(
                        buildSectorHolding(FACTION_OWNER_ID),
                        buildWorldMarket(),
                        null,
                        COLONY_SIZE)));
        }

        @Test
        void declines_a_null_market() {

            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertDeclined(NexerelinColoniser.establishColony(
                        buildSectorHolding(Factions.PLAYER),
                        null,
                        Factions.PLAYER,
                        COLONY_SIZE)));
        }

        @Test
        void declines_a_founding_with_no_sector_to_read_the_owner_from() {

            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertDeclined(NexerelinColoniser.establishColony(
                        null,
                        buildWorldMarket(),
                        Factions.PLAYER,
                        COLONY_SIZE)));
        }
    }

    // A colonisable world: survey data hung on a planet, which is the one body shape the mod's
    // routine can found on.
    private static MarketAPI buildWorldMarket() {
        return buildMarketOn(mock(PlanetAPI.class));
    }

    // A colonisable place whose body is not a planet, as a modded body can be.
    private static MarketAPI buildStationMarket() {
        return buildMarketOn(mock(SectorEntityToken.class));
    }

    // A colonisable place the game gave no body at all - nothing to found a colony on.
    private static MarketAPI buildMarketWithoutBody() {
        return buildMarketOn(null);
    }

    private static MarketAPI buildMarketOn(SectorEntityToken body) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getPrimaryEntity())
            .thenReturn(body);

        return marketMock;
    }
}
