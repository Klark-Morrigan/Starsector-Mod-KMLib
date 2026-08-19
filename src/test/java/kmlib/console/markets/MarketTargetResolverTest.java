package kmlib.console.markets;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link MarketTargetResolver#resolveTargetMarket}: which market a command
 * ends up acting on, and - where it ends up acting on none - what the player is told and why.
 * The cases live in a {@link Nested} group so the suite reports as a per-method tree; the shared
 * mock builders stay on the outer class.
 *
 * <p>Posed against a requirement of the suite's own rather than one the console ships, so the
 * cases are about resolution - naming, searching, refusing - and not about what makes a market
 * colonisable or transferable. Those two rules are {@link MarketTargetRequirementTest}'s.
 */
final class MarketTargetResolverTest {

    // The system every case is posed in, named the same way vanilla names one whose display name
    // needs no repair, so a message quoting it reads as the player would see it.
    private static final String SYSTEM_NAME = "Corvus";

    private static final String TARGET_PHRASE = "a suitable target";

    private static final MarketTargetRequirement ANY_MARKET =
        new MarketTargetRequirement(market -> true, TARGET_PHRASE);

    private static final MarketTargetRequirement NO_MARKET =
        new MarketTargetRequirement(market -> false, TARGET_PHRASE);

    @Nested
    class ResolveTargetMarket {

        @Test
        void resolves_the_market_on_the_entity_the_id_names() {

            var systemMock = buildSystemNamed(SYSTEM_NAME);
            var jangala = buildMarketOnBodyIn(systemMock, "jangala", 500, 0);
            var sectorMock = buildSectorAround(systemMock, jangala);

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    sectorMock,
                    systemMock,
                    "jangala",
                    ANY_MARKET))
                .isEqualTo(new ResolvedMarketTarget(jangala));
        }

        @Test
        void resolves_a_named_entity_the_economy_does_not_list() {
            // The named place is found on the entity itself, so a body carrying only survey data
            // - which the economy never lists - can still be pointed at by id.
            var systemMock = buildSystemNamed(SYSTEM_NAME);
            var surveyData = buildMarketOnBodyIn(systemMock, "corvus_iii", 500, 0);
            var sectorMock = buildSectorAround(systemMock);

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    sectorMock,
                    systemMock,
                    "corvus_iii",
                    ANY_MARKET))
                .isEqualTo(new ResolvedMarketTarget(surveyData));
        }

        @Test
        void refuses_an_id_no_entity_here_carries() {
            // Named rather than searched: the player asked for one place, so the answer is that
            // it is not here, not the nearest thing that happens to qualify.
            var systemMock = buildSystemNamed(SYSTEM_NAME);
            var sectorMock = buildSectorAround(systemMock);

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    sectorMock,
                    systemMock,
                    "jangala",
                    ANY_MARKET))
                .isEqualTo(new UnresolvedMarketTarget(
                    "No entity with id 'jangala' in Corvus."));
        }

        @Test
        void refuses_an_entity_with_no_market_on_it() {

            var systemMock = buildSystemNamed(SYSTEM_NAME);

            buildBodyWithoutMarketIn(systemMock, "corvus_gate");

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    buildSectorAround(systemMock),
                    systemMock,
                    "corvus_gate",
                    ANY_MARKET))
                .isEqualTo(new UnresolvedMarketTarget("Entity 'corvus_gate' has no market."));
        }

        @Test
        void refuses_a_named_market_of_the_wrong_kind() {
            // Told apart from "not here" on purpose: the place exists and the player pointed at
            // it, so the correction is a different one.
            var systemMock = buildSystemNamed(SYSTEM_NAME);
            var jangala = buildMarketOnBodyIn(systemMock, "jangala", 500, 0);

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    buildSectorAround(systemMock, jangala),
                    systemMock,
                    "jangala",
                    NO_MARKET))
                .isEqualTo(new UnresolvedMarketTarget(
                    "The market on 'jangala' is not a suitable target."));
        }

        @Test
        void takes_the_nearest_qualifying_market_when_no_id_is_given() {

            var systemMock = buildSystemNamed(SYSTEM_NAME);
            var far = buildMarketOnBodyIn(systemMock, "far", 5000, 0);
            var near = buildMarketOnBodyIn(systemMock, "near", 100, 0);
            var sectorMock = buildSectorAround(systemMock, far, near);

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    sectorMock,
                    systemMock,
                    null,
                    ANY_MARKET))
                .isEqualTo(new ResolvedMarketTarget(near));
        }

        @Test
        void treats_a_blank_id_as_no_id_at_all() {
            // The console hands over whatever the player typed, and a run of spaces is a bare
            // invocation - searching for an entity named by them would find nothing at all.
            var systemMock = buildSystemNamed(SYSTEM_NAME);
            var jangala = buildMarketOnBodyIn(systemMock, "jangala", 500, 0);

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    buildSectorAround(systemMock, jangala),
                    systemMock,
                    "   ",
                    ANY_MARKET))
                .isEqualTo(new ResolvedMarketTarget(jangala));
        }

        @Test
        void refuses_a_search_that_turns_nothing_up() {

            var systemMock = buildSystemNamed(SYSTEM_NAME);
            var jangala = buildMarketOnBodyIn(systemMock, "jangala", 500, 0);

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    buildSectorAround(systemMock, jangala),
                    systemMock,
                    null,
                    NO_MARKET))
                .isEqualTo(new UnresolvedMarketTarget("Nothing in Corvus is a suitable target."));
        }

        @Test
        void refuses_a_run_with_no_system_to_search() {
            // The context guard turns a hyperspace invocation away before this is reached, so
            // this is the answer to a caller that skipped it - not a sector-wide search.
            assertThat(MarketTargetResolver.resolveTargetMarket(
                    mock(SectorAPI.class),
                    null,
                    "jangala",
                    ANY_MARKET))
                .isEqualTo(new UnresolvedMarketTarget("No star system to search."));
        }
    }

    // A system named the way vanilla names one, with its base name under the composed one, so a
    // message quoting it is quoting what the display read yields.
    private static StarSystemAPI buildSystemNamed(String name) {

        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getName())
            .thenReturn(name);
        when(systemMock.getNameWithNoType())
            .thenReturn(name);

        return systemMock;
    }

    // The sector a run is made against: an economy listing the given markets in the system, and
    // a fleet at the origin for a search to measure from.
    private static SectorAPI buildSectorAround(StarSystemAPI systemMock, MarketAPI... markets) {

        // Each collaborator finishes its own stubbing before the sector's opens, so the calls do
        // not nest into an unfinished-stubbing error.
        var economyMock = mock(EconomyAPI.class);

        when(economyMock.getMarkets(systemMock))
            .thenReturn(List.of(markets));

        var fleetMock = mock(CampaignFleetAPI.class);

        when(fleetMock.getLocation())
            .thenReturn(new Vector2f(0, 0));

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getEconomy())
            .thenReturn(economyMock);
        when(sectorMock.getPlayerFleet())
            .thenReturn(fleetMock);

        return sectorMock;
    }

    // A market on a body of its own in the system, reachable both ways a resolution reaches one:
    // by the id the body answers to, and by the distance a search ranks it at.
    private static MarketAPI buildMarketOnBodyIn(
            StarSystemAPI systemMock,
            String bodyId,
            float x,
            float y) {

        var bodyMock = buildBodyWithoutMarketIn(systemMock, bodyId);

        when(bodyMock.getLocation())
            .thenReturn(new Vector2f(x, y));

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getPrimaryEntity())
            .thenReturn(bodyMock);
        when(bodyMock.getMarket())
            .thenReturn(marketMock);

        return marketMock;
    }

    // A body the system answers for by id with nothing on it - a gate, a beacon, a bare rock.
    private static SectorEntityToken buildBodyWithoutMarketIn(
            StarSystemAPI systemMock,
            String bodyId) {

        var bodyMock = mock(SectorEntityToken.class);

        when(bodyMock.getId())
            .thenReturn(bodyId);
        when(systemMock.getEntityById(bodyId))
            .thenReturn(bodyMock);

        return bodyMock;
    }
}
