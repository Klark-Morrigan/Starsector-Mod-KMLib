package kmlib.starsector.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;

import java.util.List;

import assortment_of_things.abyss.entities.hyper.AbyssalFracture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link StarSystems#getStars},
 * {@link StarSystems#getCentremostStar},
 * {@link StarSystems#isReachable},
 * {@link StarSystems#find}, {@link StarSystems#readMarkets},
 * {@link StarSystems#readMarketsUnlistedByEconomy}, {@link StarSystems#findNearestMarket},
 * {@link StarSystems#readDisplayName} and
 * {@link StarSystems#readFactionClaimOverride}. Each method's cases live in a
 * {@link Nested} group so
 * the suite reports as a per-method tree; the shared mock builders stay on the
 * outer class.
 */
final class StarSystemsTest {

    // The vanilla memory key a decreed claim is set under, spelled out rather than read from
    // MemFlags so a read pointed at another key fails here.
    private static final String CLAIMING_FACTION_FLAG = "$claimingFaction";

    @Nested
    class GetStars {

        @Test
        void keeps_only_the_stars_in_system_order() {

            var starA = buildPlanet(true);
            var gasGiant = buildPlanet(false);
            var starB = buildPlanet(true);
            var systemMock = mock(StarSystemAPI.class);

            when(systemMock.getPlanets())
                .thenReturn(List.of(starA, gasGiant, starB));

            assertThat(StarSystems.getStars(systemMock))
                .containsExactly(starA, starB);
        }

        @Test
        void returns_empty_for_a_null_system() {
            assertThat(StarSystems.getStars(null))
                .isEmpty();
        }
    }

    @Nested
    class GetCentremostStar {

        @Test
        void returns_the_only_star_in_a_single_star_system() {

            var star = buildStarWithLocation(0, 0);
            var system = buildSystemWithCentreAndStars(star, star);

            assertThat(StarSystems.getCentremostStar(system))
                .isSameAs(star);
        }

        @Test
        void returns_the_star_nearest_the_centre_in_a_multi_star_system() {

            var centreMock = mock(SectorEntityToken.class);

            when(centreMock.getLocation())
                .thenReturn(new Vector2f(0, 0));

            var nearStar = buildStarWithLocation(0, 0);
            var farStar = buildStarWithLocation(5000, 0);

            // Listed far-first so the pick is shown to come from distance, not list order.
            var system = buildSystemWithCentreAndStars(centreMock, farStar, nearStar);

            assertThat(StarSystems.getCentremostStar(system))
                .isSameAs(nearStar);
        }

        @Test
        void breaks_an_equal_distance_tie_by_lowest_star_id() {

            var centreMock = mock(SectorEntityToken.class);

            when(centreMock.getLocation())
                .thenReturn(new Vector2f(0, 0));

            var starBeta = buildStarWithIdAt("beta", 0, 100);
            var starAlpha = buildStarWithIdAt("alpha", 0, -100);

            // Both stars sit the same distance from the centre; listed high-id first so the
            // lower id is shown to be the deterministic pick rather than the planet-list order.
            var system = buildSystemWithCentreAndStars(centreMock, starBeta, starAlpha);

            assertThat(StarSystems.getCentremostStar(system))
                .isSameAs(starAlpha);
        }

        @Test
        void falls_back_to_the_centre_token_when_the_system_has_no_star() {

            var centreMock = mock(SectorEntityToken.class);
            var system = buildSystemWithCentreAndStars(centreMock);

            assertThat(StarSystems.getCentremostStar(system))
                .isSameAs(centreMock);
        }

        @Test
        void returns_null_for_a_null_system() {
            assertThat(StarSystems.getCentremostStar(null))
                .isNull();
        }
    }

    @Nested
    class ReadMarkets {

        @Test
        void returns_the_systems_markets_in_economy_order() {

            var first = buildVisibleColony();
            var second = buildConditionOnlyMarket();
            var sector = buildSectorWithMarkets(first, second);

            // Order is the economy's, unfiltered: a caller mirroring vanilla's tie rule
            // resolves on which market comes first, so the traversal must not reorder.
            assertThat(StarSystems.readMarkets(sector, buildOnlySystem(sector)))
                .containsExactly(first, second);
        }

        @Test
        void returns_empty_for_a_system_with_no_markets() {

            var sector = buildSectorWithMarkets();

            assertThat(StarSystems.readMarkets(sector, buildOnlySystem(sector)))
                .isEmpty();
        }

        @Test
        void returns_empty_for_a_null_sector() {
            assertThat(StarSystems.readMarkets(null, mock(StarSystemAPI.class)))
                .isEmpty();
        }

        @Test
        void returns_empty_for_a_null_system() {
            assertThat(StarSystems.readMarkets(mock(SectorAPI.class), null))
                .isEmpty();
        }

        @Test
        void returns_empty_when_the_sector_has_no_economy() {

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getEconomy())
                .thenReturn(null);

            assertThat(StarSystems.readMarkets(sectorMock, mock(StarSystemAPI.class)))
                .isEmpty();
        }

        @Test
        void returns_empty_when_the_economy_reports_no_market_list() {

            var economyMock = mock(EconomyAPI.class);
            var systemMock = mock(StarSystemAPI.class);

            when(economyMock.getMarkets(systemMock))
                .thenReturn(null);

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getEconomy())
                .thenReturn(economyMock);

            assertThat(StarSystems.readMarkets(sectorMock, systemMock))
                .isEmpty();
        }
    }

    @Nested
    class ReadMarketsUnlistedByEconomy {

        @Test
        void yields_the_market_the_economy_does_not_list_and_not_the_ones_it_does() {
            // Vanilla builds Galatia Academy as a real market on a real station and deliberately
            // never registers it, so a read of the economy alone reports the station as nobody's.
            // Only that market comes back: a caller wanting the listed ones has already read them,
            // and handing them over again would leave it comparing the two lists to tell them apart.
            var listed = buildVisibleColony();
            var academy = buildVisibleColony();
            var sector = buildSectorWithMarkets(listed);

            placeEntitiesInOnlySystem(sector, buildEntityCarrying(listed), buildEntityCarrying(academy));

            assertThat(StarSystems.readMarketsUnlistedByEconomy(sector, buildOnlySystem(sector)))
                .containsExactly(academy);
        }

        @Test
        void yields_nothing_for_a_system_the_economy_lists_whole() {
            // The ordinary system: every market on an entity is one the economy already hands over,
            // so the read that exists to find what it left out finds nothing to add.
            var first = buildVisibleColony();
            var second = buildVisibleColony();
            var sector = buildSectorWithMarkets(first, second);

            placeEntitiesInOnlySystem(
                sector,
                buildEntityCarrying(first),
                buildEntityCarrying(second));

            assertThat(StarSystems.readMarketsUnlistedByEconomy(sector, buildOnlySystem(sector)))
                .isEmpty();
        }

        @Test
        void yields_unlisted_markets_in_entity_order() {

            var first = buildVisibleColony();
            var second = buildVisibleColony();
            var sector = buildSectorWithMarkets();

            placeEntitiesInOnlySystem(
                sector,
                buildEntityCarrying(first),
                buildEntityCarrying(second));

            assertThat(StarSystems.readMarketsUnlistedByEconomy(sector, buildOnlySystem(sector)))
                .containsExactly(first, second);
        }

        @Test
        void yields_a_market_the_economy_already_lists_no_second_time() {

            var listed = buildVisibleColony();
            var sector = buildSectorWithMarkets(listed);

            placeEntitiesInOnlySystem(sector, buildEntityCarrying(listed));

            assertThat(StarSystems.readMarketsUnlistedByEconomy(sector, buildOnlySystem(sector)))
                .isEmpty();
        }

        @Test
        void yields_one_unlisted_market_once_though_two_entities_carry_it() {
            // Vanilla hangs a station's market on the station and on what it orbits alike, so one
            // colony can be reached twice down the entity walk.
            var academy = buildVisibleColony();
            var sector = buildSectorWithMarkets();

            placeEntitiesInOnlySystem(
                sector,
                buildEntityCarrying(academy),
                buildEntityCarrying(academy));

            assertThat(StarSystems.readMarketsUnlistedByEconomy(sector, buildOnlySystem(sector)))
                .containsExactly(academy);
        }

        @Test
        void resolves_two_markets_on_one_entity_under_one_owner_to_one() {
            // A mod supersedes a market by adding rather than replacing, so the station ends up
            // carrying two market objects for the one colony - counted twice, it would list a
            // faction's foothold as two separate holdings.
            var station = buildDiscoveredEntity();
            var independentMock = mock(FactionAPI.class);

            when(independentMock.getId())
                .thenReturn("independent");

            var listed = buildColonyAtPlace(station, independentMock);
            var supplementary = buildColonyAtPlace(station, independentMock);
            var sector = buildSectorWithMarkets(listed);

            when(station.getMarket())
                .thenReturn(supplementary);

            placeEntitiesInOnlySystem(sector, station);

            assertThat(StarSystems.readMarketsUnlistedByEconomy(sector, buildOnlySystem(sector)))
                .isEmpty();
        }

        @Test
        void ignores_an_entity_carrying_no_market() {

            var sector = buildSectorWithMarkets(buildVisibleColony());

            placeEntitiesInOnlySystem(sector, buildDiscoveredEntity());

            assertThat(StarSystems.readMarketsUnlistedByEconomy(sector, buildOnlySystem(sector)))
                .isEmpty();
        }

        @Test
        void returns_empty_for_a_null_sector() {
            assertThat(StarSystems.readMarketsUnlistedByEconomy(null, mock(StarSystemAPI.class)))
                .isEmpty();
        }

        @Test
        void returns_empty_for_a_null_system() {
            assertThat(StarSystems.readMarketsUnlistedByEconomy(mock(SectorAPI.class), null))
                .isEmpty();
        }

        @Test
        void returns_empty_when_the_sector_has_no_economy() {
            // With no listing to compare against there is no telling a listed market from an
            // unlisted one, so the read reports nothing rather than every market it can reach.
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getEconomy())
                .thenReturn(null);

            assertThat(StarSystems.readMarketsUnlistedByEconomy(sectorMock, mock(StarSystemAPI.class)))
                .isEmpty();
        }
    }

    @Nested
    class FindNearestMarket {

        @Test
        void returns_the_admitted_market_whose_body_sits_closest() {

            var near = buildMarketOnBodyAt("near", 100, 0);
            var far = buildMarketOnBodyAt("far", 5000, 0);

            // Listed far-first so the pick is shown to come from distance, not economy order.
            var sector = buildSectorWithMarkets(far, near);

            assertThat(StarSystems.findNearestMarket(
                    sector,
                    buildOnlySystem(sector),
                    buildFleetAt(0, 0),
                    market -> true))
                .contains(near);
        }

        @Test
        void passes_over_a_closer_market_the_eligibility_rejects() {
            // What a caller is looking for decides, not proximity alone: the nearest place is
            // routinely the wrong kind of place, which is the whole reason for the predicate.
            var wanted = buildMarketOnBodyAt("wanted", 500, 0);
            var unwanted = buildMarketOnBodyAt("unwanted", 100, 0);
            var sector = buildSectorWithMarkets(unwanted, wanted);

            assertThat(StarSystems.findNearestMarket(
                    sector,
                    buildOnlySystem(sector),
                    buildFleetAt(0, 0),
                    market -> market == wanted))
                .contains(wanted);
        }

        @Test
        void reaches_a_market_the_economy_does_not_list() {
            // A body still carrying only survey data is never registered, so a search of the
            // economy's own listing could not find one to colonise at all.
            var surveyData = buildMarketOnBodyAt("planet", 100, 0);
            var sector = buildSectorWithMarkets();

            placeEntitiesInOnlySystem(sector, surveyData.getPrimaryEntity());

            assertThat(StarSystems.findNearestMarket(
                    sector,
                    buildOnlySystem(sector),
                    buildFleetAt(0, 0),
                    market -> true))
                .contains(surveyData);
        }

        @Test
        void settles_an_equal_distance_on_the_lowest_body_id() {
            // Listed high-id first, so the lower id is shown to be the deterministic pick
            // rather than the order the economy happened to list them in.
            var beta = buildMarketOnBodyAt("beta", 0, 100);
            var alpha = buildMarketOnBodyAt("alpha", 0, -100);
            var sector = buildSectorWithMarkets(beta, alpha);

            assertThat(StarSystems.findNearestMarket(
                    sector,
                    buildOnlySystem(sector),
                    buildFleetAt(0, 0),
                    market -> true))
                .contains(alpha);
        }

        @Test
        void ignores_a_market_standing_for_no_place() {
            // Nothing to measure to, so it cannot be nearest - only nearest by default, which
            // would have a command act on a market the player cannot point at.
            var sector = buildSectorWithMarkets(mock(MarketAPI.class));

            assertThat(StarSystems.findNearestMarket(
                    sector,
                    buildOnlySystem(sector),
                    buildFleetAt(0, 0),
                    market -> true))
                .isEmpty();
        }

        @Test
        void returns_empty_when_nothing_in_the_system_is_admitted() {

            var sector = buildSectorWithMarkets(buildMarketOnBodyAt("planet", 100, 0));

            assertThat(StarSystems.findNearestMarket(
                    sector,
                    buildOnlySystem(sector),
                    buildFleetAt(0, 0),
                    market -> false))
                .isEmpty();
        }

        @Test
        void returns_empty_without_anything_to_measure_from() {

            var sector = buildSectorWithMarkets(buildMarketOnBodyAt("planet", 100, 0));

            assertThat(StarSystems.findNearestMarket(
                    sector,
                    buildOnlySystem(sector),
                    null,
                    market -> true))
                .isEmpty();
        }

        @Test
        void returns_empty_without_an_eligibility_rule() {
            // No rule is not "every market": a caller that forgot to say what it wants must
            // not be handed the nearest place of any kind to act on.
            var sector = buildSectorWithMarkets(buildMarketOnBodyAt("planet", 100, 0));

            assertThat(StarSystems.findNearestMarket(
                    sector,
                    buildOnlySystem(sector),
                    buildFleetAt(0, 0),
                    null))
                .isEmpty();
        }

        @Test
        void returns_empty_for_a_null_sector() {
            assertThat(StarSystems.findNearestMarket(
                    null,
                    mock(StarSystemAPI.class),
                    buildFleetAt(0, 0),
                    market -> true))
                .isEmpty();
        }

        @Test
        void returns_empty_for_a_null_system() {
            assertThat(StarSystems.findNearestMarket(
                    mock(SectorAPI.class),
                    null,
                    buildFleetAt(0, 0),
                    market -> true))
                .isEmpty();
        }
    }

    @Nested
    class ReadDisplayName {

        @Test
        void drops_a_type_word_the_name_already_ends_on() {
            // Vanilla composes the name as the base name plus the type, so a system named after its
            // star stutters: the reading the whole method exists for.
            assertThat(StarSystems.readDisplayName(
                    buildSystemNamed("Penelope's Star Star System", "Penelope's Star")))
                .isEqualTo("Penelope's Star System");
        }

        @Test
        void answers_a_name_with_no_repetition_unchanged() {
            // The ordinary system: the type word is nothing the base name ended on, so the composed
            // name already reads as a person would have written it.
            assertThat(StarSystems.readDisplayName(
                    buildSystemNamed("Galatia Star System", "Galatia")))
                .isEqualTo("Galatia Star System");
        }

        @Test
        void keeps_a_repetition_inside_the_name_proper() {
            // The case the protection exists for: the repeat is the name's own, so dropping it would
            // answer "Ko Star System" - a system nobody named.
            assertThat(StarSystems.readDisplayName(
                    buildSystemNamed("Ko Ko Star System", "Ko Ko")))
                .isEqualTo("Ko Ko Star System");
        }

        @Test
        void answers_the_name_untouched_where_the_name_proper_is_not_what_it_opens_with() {
            // getNameWithNoType strips its type word globally, so a base name carrying that word in
            // its middle comes back as something the name does not begin with - and a word count
            // taken from it would protect the wrong words.
            assertThat(StarSystems.readDisplayName(
                    buildSystemNamed("Nebula Ridge Nebula", "Ridge")))
                .isEqualTo("Nebula Ridge Nebula");
        }

        @Test
        void answers_a_name_that_is_its_own_name_proper_untouched() {
            // Nothing was appended, so every word is protected and there is nothing to weigh.
            assertThat(StarSystems.readDisplayName(buildSystemNamed("Galatia", "Galatia")))
                .isEqualTo("Galatia");
        }

        @Test
        void answers_the_name_untouched_where_the_name_proper_is_blank() {
            // With no base name to protect, a general scan could cut a word out of the name itself.
            assertThat(StarSystems.readDisplayName(buildSystemNamed("Ko Ko System", " ")))
                .isEqualTo("Ko Ko System");
        }

        @Test
        void matches_the_repeat_ignoring_case() {
            assertThat(StarSystems.readDisplayName(
                    buildSystemNamed("Penelope's Star STAR System", "Penelope's Star")))
                .isEqualTo("Penelope's Star System");
        }

        @Test
        void answers_an_unchanged_name_as_the_very_string_it_was_given() {
            // A name that lost nothing is handed back rather than rejoined: rebuilding it would
            // normalise whatever spacing it was authored with, changing a name for no gain.
            var name = "Galatia  Star System";

            assertThat(StarSystems.readDisplayName(buildSystemNamed(name, "Galatia")))
                .isSameAs(name);
        }

        @Test
        void drops_the_repeat_out_of_a_type_of_more_than_two_words() {
            // The protection is a word count off the name proper rather than a rule about
            // where the repeat may fall, so a longer composed type is in reach whole.
            assertThat(StarSystems.readDisplayName(
                    buildSystemNamed("Penelope's Star Star System Cluster", "Penelope's Star")))
                .isEqualTo("Penelope's Star System Cluster");
        }

        @Test
        void returns_blank_for_a_system_with_no_name() {
            assertThat(StarSystems.readDisplayName(buildSystemNamed(null, "Penelope's Star")))
                .isEmpty();
        }

        @Test
        void returns_blank_for_a_null_system() {
            assertThat(StarSystems.readDisplayName(null))
                .isEmpty();
        }
    }

    @Nested
    class ReadFactionClaimOverride {

        @Test
        void returns_the_decreed_faction_id() {
            assertThat(StarSystems.readFactionClaimOverride(buildSystemClaimedBy("luddic_church")))
                .isEqualTo("luddic_church");
        }

        @Test
        void returns_null_when_no_claim_is_imposed() {
            // The ordinary case: vanilla scores markets for an unflagged system, so most
            // claimed systems carry no flag at all.
            assertThat(StarSystems.readFactionClaimOverride(buildSystemClaimedBy(null)))
                .isNull();
        }

        @Test
        void returns_null_for_a_system_with_no_memory() {

            var systemMock = mock(StarSystemAPI.class);

            when(systemMock.getMemoryWithoutUpdate())
                .thenReturn(null);

            assertThat(StarSystems.readFactionClaimOverride(systemMock))
                .isNull();
        }

        @Test
        void returns_null_for_a_null_system() {
            assertThat(StarSystems.readFactionClaimOverride(null))
                .isNull();
        }
    }

    @Nested
    class IsReachable {

        @Test
        void returns_true_for_a_system_with_a_jump_point() {
            assertThat(StarSystems.isReachable(buildSystemNotCutOff("a")))
                .isTrue();
        }

        @Test
        void returns_false_for_a_transverse_only_system_with_an_inactive_gate() {
            // The hidden-system case: not cut off (the engine never tags a
            // nascent-well system), no jump point, only an unlit gate.
            var system = buildTransverseOnlySystem("a", buildGateWithPlugin(buildGatePlugin(false)));

            assertThat(StarSystems.isReachable(system))
                .isFalse();
        }

        @Test
        void returns_false_for_a_cut_off_system_with_no_gate() {
            assertThat(StarSystems.isReachable(cutOffSystem("a")))
                .isFalse();
        }

        @Test
        void returns_false_for_a_cut_off_system_with_only_an_inactive_gate() {

            var system = cutOffSystem("a", buildGateWithPlugin(buildGatePlugin(false)));

            assertThat(StarSystems.isReachable(system))
                .isFalse();
        }

        @Test
        void returns_true_for_a_cut_off_system_with_an_active_gate() {

            var system = cutOffSystem("a", buildGateWithPlugin(buildGatePlugin(true)));

            assertThat(StarSystems.isReachable(system))
                .isTrue();
        }

        @Test
        void returns_true_for_a_cut_off_system_with_a_fracture_when_rat_enabled() {
            // A fracture ferries fleets in past the disabled jump points, so it
            // overrides the cut-off flag the way an active gate does.
            var system = cutOffSystemWithEntities("a", buildFractureEntity());

            try (var globalMock = mockStatic(Global.class)) {

                stubRatEnabled(globalMock, true);

                assertThat(StarSystems.isReachable(system))
                    .isTrue();
            }
        }

        @Test
        void returns_false_for_a_cut_off_system_with_a_fracture_when_rat_disabled() {
            // The optional dependency is off, so the matcher cannot see the
            // fracture and the system reads as the cut-off system it is.
            var system = cutOffSystemWithEntities("a", buildFractureEntity());

            try (var globalMock = mockStatic(Global.class)) {

                stubRatEnabled(globalMock, false);

                assertThat(StarSystems.isReachable(system))
                    .isFalse();
            }
        }
    }

    @Nested
    class Find {
        
        @Test
        void returns_the_tagged_entity_whose_id_matches() {

            var gate = buildEntity("alpha-gate");
            var other = buildEntity("beta-gate");
            var systemMock = mock(StarSystemAPI.class);

            when(systemMock.getEntitiesWithTag("gate"))
                .thenReturn(List.of(other, gate));

            assertThat(StarSystems.find(systemMock, "gate", "alpha-gate"))
                .isSameAs(gate);
        }

        @Test
        void returns_null_when_no_tagged_entity_has_the_id() {

            var other = buildEntity("beta-gate");
            var systemMock = mock(StarSystemAPI.class);

            when(systemMock.getEntitiesWithTag("gate"))
                .thenReturn(List.of(other));

            assertThat(StarSystems.find(systemMock, "gate", "alpha-gate"))
                .isNull();
        }

        @Test
        void returns_null_for_a_null_system() {
            assertThat(StarSystems.find(null, "gate", "alpha-gate"))
                .isNull();
        }

        @Test
        void returns_null_for_a_blank_id() {

            var systemMock = mock(StarSystemAPI.class);

            assertThat(StarSystems.find(systemMock, "gate", " "))
                .isNull();
        }

        @Test
        void returns_null_for_a_null_id() {

            var systemMock = mock(StarSystemAPI.class);

            assertThat(StarSystems.find(systemMock, "gate", null))
                .isNull();
        }
    }

    private static StarSystemAPI buildOnlySystem(SectorAPI sector) {
        return sector.getStarSystems().get(0);
    }

    // A system answering vanilla's two names: the composed one every surface reads, and the base
    // name under it - the pair a display name is derived from.
    private static StarSystemAPI buildSystemNamed(String name, String nameProper) {

        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getName())
            .thenReturn(name);
        when(systemMock.getNameWithNoType())
            .thenReturn(nameProper);

        return systemMock;
    }

    // A star fixed at a location, so a central-star search can rank stars by nearness to the
    // system centre.
    private static PlanetAPI buildStarWithLocation(float x, float y) {

        var starMock = mock(PlanetAPI.class);

        when(starMock.isStar())
            .thenReturn(true);
        when(starMock.getLocation())
            .thenReturn(new Vector2f(x, y));

        return starMock;
    }

    // A star fixed at a location and carrying an id, so a central-star search's distance-tie
    // resolution by id can be pinned.
    private static PlanetAPI buildStarWithIdAt(String id, float x, float y) {

        var starMock = buildStarWithLocation(x, y);

        when(starMock.getId())
            .thenReturn(id);

        return starMock;
    }

    // A body on a circular orbit of the given radius around a focus, the unit an orbit-chain
    // distance sums.

    // A system with a centre token and its stars, the two a central-star search reads.
    private static StarSystemAPI buildSystemWithCentreAndStars(
            SectorEntityToken centre,
            PlanetAPI... stars) {

        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getCenter())
            .thenReturn(centre);
        when(systemMock.getPlanets())
            .thenReturn(List.of(stars));

        return systemMock;
    }

    // Wires a sector with one system whose economy holds the given markets, so a
    // hasKnownOwnedMarket read resolves through getEconomy().getMarkets(system).
    private static SectorAPI buildSectorWithMarkets(MarketAPI... markets) {

        var systemMock = mock(StarSystemAPI.class);
        var economyMock = mock(EconomyAPI.class);

        when(economyMock.getMarkets(systemMock))
            .thenReturn(List.of(markets));

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getStarSystems())
            .thenReturn(List.of(systemMock));
        when(sectorMock.getEconomy())
            .thenReturn(economyMock);

        return sectorMock;
    }

    // Hands the sector's one system the entities present in it - the second half of "what is in
    // this system", beside the economy's own listing.
    private static void placeEntitiesInOnlySystem(
            SectorAPI sector,
            SectorEntityToken... entities) {

        when(buildOnlySystem(sector).getAllEntities())
            .thenReturn(List.of(entities));
    }

    // A market on a body of its own, fixed at a location and carrying an id - the pair a nearest
    // search ranks by and settles its ties on. Wired both ways, so the market is reachable
    // whether the search meets it through the economy or by walking the system's entities.
    private static MarketAPI buildMarketOnBodyAt(String bodyId, float x, float y) {

        // The body finishes its own stubbing before the market's opens, so the two do not nest
        // into an unfinished-stubbing error.
        var bodyMock = mock(SectorEntityToken.class);

        when(bodyMock.getId())
            .thenReturn(bodyId);
        when(bodyMock.getLocation())
            .thenReturn(new Vector2f(x, y));

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getPrimaryEntity())
            .thenReturn(bodyMock);
        when(bodyMock.getMarket())
            .thenReturn(marketMock);

        return marketMock;
    }

    // The player's fleet as a nearest search sees it: a point in the system to measure from.
    private static SectorEntityToken buildFleetAt(float x, float y) {

        var fleetMock = mock(SectorEntityToken.class);

        when(fleetMock.getLocation())
            .thenReturn(new Vector2f(x, y));

        return fleetMock;
    }

    // An entity with a market hung on it, which is how an unregistered colony reaches a reader at
    // all: the economy does not list it, so the entity is the only thing that names it.
    private static SectorEntityToken buildEntityCarrying(MarketAPI market) {

        var entityMock = buildDiscoveredEntity();

        when(entityMock.getMarket())
            .thenReturn(market);

        return entityMock;
    }

    // An entity the player has found, so a colony on it passes the known-to-player gate.
    private static SectorEntityToken buildDiscoveredEntity() {

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.isDiscoverable())
            .thenReturn(false);

        return entityMock;
    }

    // A colony at a stated place under a stated owner - the pair that decides whether two market
    // objects stand for one holding or for two.
    private static MarketAPI buildColonyAtPlace(SectorEntityToken entity, FactionAPI faction) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(faction);
        when(marketMock.getPrimaryEntity())
            .thenReturn(entity);

        return marketMock;
    }

    // A visible owned colony: a faction owns it, it is not condition-only, and its
    // discovered entity passes the known-to-player gate.
    private static MarketAPI buildVisibleColony() {
        return buildColony(false, false, false);
    }

    // A bare planet's condition-only placeholder: owned but not a colony, so it is
    // filtered out by the ownership arm.
    private static MarketAPI buildConditionOnlyMarket() {
        return buildColony(true, false, false);
    }

    private static MarketAPI buildColony(
            boolean isConditionOnly,
            boolean isHidden,
            boolean isEntityDiscoverable) {

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.isDiscoverable())
            .thenReturn(isEntityDiscoverable);

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(mock(FactionAPI.class));
        when(marketMock.isPlanetConditionMarketOnly())
            .thenReturn(isConditionOnly);
        when(marketMock.isHidden())
            .thenReturn(isHidden);
        when(marketMock.getPrimaryEntity())
            .thenReturn(entityMock);

        return marketMock;
    }

    private static SectorEntityToken buildEntity(String id) {

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.getId())
            .thenReturn(id);

        return entityMock;
    }

    private static StarSystemAPI buildSystemNotCutOff(String id) {
        // Not cut off (hasTag defaults to false) and wired into hyperspace by a
        // jump point - a normally reachable system.
        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getId())
            .thenReturn(id);
        when(systemMock.getJumpPoints())
            .thenReturn(List.of(mock(SectorEntityToken.class)));

        return systemMock;
    }

    private static StarSystemAPI buildTransverseOnlySystem(String id, SectorEntityToken... gates) {
        // Not cut off and holds no jump point - reachable only by transverse jump
        // to a nascent gravity well. Any passed gates stand in for present-but-
        // inactive gates that must not confer access.
        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getId())
            .thenReturn(id);
        when(systemMock.getEntitiesWithTag(Tags.GATE))
            .thenReturn(List.of(gates));
        when(systemMock.getJumpPoints())
            .thenReturn(List.of());

        return systemMock;
    }

    private static StarSystemAPI cutOffSystem(String id, SectorEntityToken... gates) {

        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getId())
            .thenReturn(id);
        when(systemMock.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER))
            .thenReturn(true);
        when(systemMock.getEntitiesWithTag(Tags.GATE))
            .thenReturn(List.of(gates));

        return systemMock;
    }

    private static StarSystemAPI cutOffSystemWithEntities(String id, SectorEntityToken... entities) {
        // Cut off and holding no jump point or gate - access can come only from
        // one of the passed entities (a fracture in these cases).
        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getId())
            .thenReturn(id);
        when(systemMock.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER))
            .thenReturn(true);
        when(systemMock.getAllEntities())
            .thenReturn(List.of(entities));

        return systemMock;
    }

    private static SectorEntityToken buildGateWithPlugin(GateEntityPlugin plugin) {

        var gateMock = mock(SectorEntityToken.class);

        when(gateMock.getCustomPlugin())
            .thenReturn(plugin);

        return gateMock;
    }

    private static GateEntityPlugin buildGatePlugin(boolean isActive) {

        var pluginMock = mock(GateEntityPlugin.class);

        when(pluginMock.isActive())
            .thenReturn(isActive);

        return pluginMock;
    }

    private static SectorEntityToken buildFractureEntity() {

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.getCustomPlugin())
            .thenReturn(mock(AbyssalFracture.class));

        return entityMock;
    }

    private static void stubRatEnabled(MockedStatic<Global> globalMock, boolean isEnabled) {

        var settingsMock = mock(SettingsAPI.class);
        var modManagerMock = mock(ModManagerAPI.class);

        globalMock
            .when(Global::getSettings)
            .thenReturn(settingsMock);

        when(settingsMock.getModManager())
            .thenReturn(modManagerMock);

        when(modManagerMock.isModEnabled("assortment_of_things"))
            .thenReturn(isEnabled);
    }

    private static PlanetAPI buildPlanet(boolean isStar) {

        var planetMock = mock(PlanetAPI.class);

        when(planetMock.isStar())
            .thenReturn(isStar);

        return planetMock;
    }

    // A system whose memory carries the claiming-faction flag at the given value; a null id
    // stands for the flag never having been set.
    private static StarSystemAPI buildSystemClaimedBy(String factionId) {

        var memoryMock = mock(MemoryAPI.class);

        when(memoryMock.getString(CLAIMING_FACTION_FLAG))
            .thenReturn(factionId);

        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getMemoryWithoutUpdate())
            .thenReturn(memoryMock);

        return systemMock;
    }

}
