package kmlib.console.markets;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.markets.MarketPlacementFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link MarketTargetResolver#resolveTargetMarket}: which market a command
 * ends up acting on, and - where it ends up acting on none - what the player is told and why.
 * The cases live in a {@link Nested} group so the suite reports as a per-method tree; the shared
 * mock builders stay on the outer class.
 *
 * <p>The reach of each of the two ways to name a place is pinned here as much as the outcome is:
 * an id resolves across the whole sector, hyperspace included, while the nearest search is
 * confined to the system the fleet is in and refuses outright when there is none.
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
            var jangala = MarketPlacementFixture.buildMarketOnBodyAt("jangala", 500, 0);
            var sectorMock = buildSectorAround(systemMock, jangala);

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    sectorMock,
                    "jangala",
                    ANY_MARKET))
                .isEqualTo(new ResolvedMarketTarget(jangala));
        }

        @Test
        void resolves_a_named_entity_the_economy_does_not_list() {
            // The named place is found on the entity itself, so a body carrying only survey data
            // - which the economy never lists - can still be pointed at by id.
            var sectorMock = buildSectorAround(buildSystemNamed(SYSTEM_NAME));
            var surveyData = MarketPlacementFixture.buildMarketOnBodyAt("corvus_iii", 500, 0);

            answerForEntityById(sectorMock, surveyData.getPrimaryEntity());

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    sectorMock,
                    "corvus_iii",
                    ANY_MARKET))
                .isEqualTo(new ResolvedMarketTarget(surveyData));
        }

        @Test
        void resolves_a_named_place_from_outside_every_star_system() {
            // An id names one place in the whole sector, so it is reachable from hyperspace,
            // where there is no system to be scoped to at all. Nothing about naming a place
            // depends on where the player is standing - only "nearest" does.
            var sectorMock = buildSectorInHyperspace();
            var jangala = MarketPlacementFixture.buildMarketOnBodyAt("jangala", 500, 0);

            answerForEntityById(sectorMock, jangala.getPrimaryEntity());

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    sectorMock,
                    "jangala",
                    ANY_MARKET))
                .isEqualTo(new ResolvedMarketTarget(jangala));
        }

        @Test
        void takes_the_named_place_over_a_nearer_one() {
            // Naming a place is the player overriding the search, not narrowing it: the nearest
            // qualifying place is what a bare invocation means, and this invocation is not bare.
            var systemMock = buildSystemNamed(SYSTEM_NAME);
            var named = MarketPlacementFixture.buildMarketOnBodyAt("jangala", 5000, 0);
            var nearer = MarketPlacementFixture.buildMarketOnBodyAt("gilead", 100, 0);
            var sectorMock = buildSectorAround(systemMock, named, nearer);

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    sectorMock,
                    "jangala",
                    ANY_MARKET))
                .isEqualTo(new ResolvedMarketTarget(named));
        }

        @Test
        void refuses_an_id_no_entity_in_the_sector_carries() {
            // Named rather than searched: the player asked for one place, so the answer is that
            // there is no such place, not the nearest thing that happens to qualify.
            var sectorMock = buildSectorAround(buildSystemNamed(SYSTEM_NAME));

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    sectorMock,
                    "jangala",
                    ANY_MARKET))
                .isEqualTo(new UnresolvedMarketTarget(
                    "No entity with id 'jangala' in the sector."));
        }

        @Test
        void refuses_an_entity_with_no_market_on_it() {

            var sectorMock = buildSectorAround(buildSystemNamed(SYSTEM_NAME));

            answerForEntityById(
                sectorMock,
                MarketPlacementFixture.buildBodyAt("corvus_gate", 0, 0));

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    sectorMock,
                    "corvus_gate",
                    ANY_MARKET))
                .isEqualTo(new UnresolvedMarketTarget("Entity 'corvus_gate' has no market."));
        }

        @Test
        void refuses_a_named_market_of_the_wrong_kind() {
            // Told apart from "no such place" on purpose: the place exists and the player pointed
            // at it, so the correction is a different one.
            var systemMock = buildSystemNamed(SYSTEM_NAME);
            var jangala = MarketPlacementFixture.buildMarketOnBodyAt("jangala", 500, 0);

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    buildSectorAround(systemMock, jangala),
                    "jangala",
                    NO_MARKET))
                .isEqualTo(new UnresolvedMarketTarget(
                    "The market on 'jangala' is not a suitable target."));
        }

        @Test
        void takes_the_nearest_qualifying_market_when_no_id_is_given() {

            var systemMock = buildSystemNamed(SYSTEM_NAME);
            var far = MarketPlacementFixture.buildMarketOnBodyAt("far", 5000, 0);
            var near = MarketPlacementFixture.buildMarketOnBodyAt("near", 100, 0);
            var sectorMock = buildSectorAround(systemMock, far, near);

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    sectorMock,
                    null,
                    ANY_MARKET))
                .isEqualTo(new ResolvedMarketTarget(near));
        }

        @Test
        void treats_a_blank_id_as_no_id_at_all() {
            // The console hands over whatever the player typed, and a run of spaces is a bare
            // invocation - searching for an entity named by them would find nothing at all.
            var systemMock = buildSystemNamed(SYSTEM_NAME);
            var jangala = MarketPlacementFixture.buildMarketOnBodyAt("jangala", 500, 0);

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    buildSectorAround(systemMock, jangala),
                    "   ",
                    ANY_MARKET))
                .isEqualTo(new ResolvedMarketTarget(jangala));
        }

        @Test
        void refuses_a_search_that_turns_nothing_up() {

            var systemMock = buildSystemNamed(SYSTEM_NAME);
            var jangala = MarketPlacementFixture.buildMarketOnBodyAt("jangala", 500, 0);

            assertThat(MarketTargetResolver.resolveTargetMarket(
                    buildSectorAround(systemMock, jangala),
                    null,
                    NO_MARKET))
                .isEqualTo(new UnresolvedMarketTarget("Nothing in Corvus is a suitable target."));
        }

        @Test
        void refuses_a_bare_run_made_from_outside_every_star_system() {
            // A fleet in hyperspace has nowhere to measure "nearest" from, so the refusal names
            // the way out rather than only the obstacle: an id reaches a place from here.
            assertThat(MarketTargetResolver.resolveTargetMarket(
                    buildSectorInHyperspace(),
                    null,
                    ANY_MARKET))
                .isEqualTo(new UnresolvedMarketTarget(
                    "Not in a star system - name an entity id to point at a place directly."));
        }

        @Test
        void refuses_a_run_made_without_a_sector() {
            // Nothing to look an id up in and nowhere to search from, so neither way of naming a
            // place is open.
            assertThat(MarketTargetResolver.resolveTargetMarket(null, "jangala", ANY_MARKET))
                .isEqualTo(new UnresolvedMarketTarget("No sector to search."));
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

    // The sector a run is made against: an economy listing the given markets in the system, the
    // player's fleet sitting in that system at the origin - which is both where the search
    // measures from and how the resolution learns which system "here" is - and the sector
    // answering for each listed market's body by id, which is the other way a resolution reaches
    // one.
    private static SectorAPI buildSectorAround(StarSystemAPI systemMock, MarketAPI... markets) {

        // Each collaborator finishes its own stubbing before the sector's opens, so the calls do
        // not nest into an unfinished-stubbing error.
        var economyMock = mock(EconomyAPI.class);

        MarketPlacementFixture.listMarketsIn(economyMock, systemMock, markets);

        var fleetMock = MarketPlacementFixture.buildFleetAt(0, 0);

        when(fleetMock.getStarSystem())
            .thenReturn(systemMock);

        var sectorMock = buildSectorWithFleet(fleetMock);

        when(sectorMock.getEconomy())
            .thenReturn(economyMock);

        for (var market : markets) {
            answerForEntityById(sectorMock, market.getPrimaryEntity());
        }
        return sectorMock;
    }

    // A sector whose fleet is in no star system - hyperspace, where an id still names a place and
    // a nearest search has nowhere to start.
    private static SectorAPI buildSectorInHyperspace() {
        return buildSectorWithFleet(MarketPlacementFixture.buildFleetAt(0, 0));
    }

    private static SectorAPI buildSectorWithFleet(CampaignFleetAPI fleetMock) {

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getPlayerFleet())
            .thenReturn(fleetMock);

        return sectorMock;
    }

    // Makes the sector answer for the entity under its id, which is what an id-named resolution
    // looks one up through - the sector rather than a system, ids being unique sector-wide.
    private static void answerForEntityById(SectorAPI sectorMock, SectorEntityToken entityMock) {

        // The id is read off the entity before the sector's stubbing opens, so calling one mock
        // does not land inside a stubbing in progress on another.
        var entityId = entityMock.getId();

        when(sectorMock.getEntityById(entityId))
            .thenReturn(entityMock);
    }
}
