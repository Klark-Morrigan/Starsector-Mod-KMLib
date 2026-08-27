package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.testfixtures.starsector.markets.MarketPlacementFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

    /**
     * What the star-system surface adds: a system is asked, and only a system may be. The read
     * itself - what it guarantees about order, and what it does with an unreachable economy - is
     * {@code LocationMarketsTest}'s, at the layer that owns it.
     */
    @Nested
    class ReadMarkets {

        @Test
        void returns_the_systems_markets_in_economy_order() {

            var first = MarketPlacementFixture.buildMarketOnBody("ancyra");
            var second = MarketPlacementFixture.buildMarketOnBody("tibicena");
            var sector = buildSectorWithMarkets(first, second);

            assertThat(StarSystems.readMarkets(sector, buildOnlySystem(sector)))
                .containsExactly(first, second);
        }

        @Test
        void returns_empty_for_a_null_system() {
            assertThat(StarSystems.readMarkets(mock(SectorAPI.class), null))
                .isEmpty();
        }
    }

    /**
     * What the star-system surface adds, as with {@link ReadMarkets}: the rule about which
     * markets the economy leaves out is {@code LocationMarketsTest}'s.
     */
    @Nested
    class ReadMarketsUnlistedByEconomy {

        @Test
        void yields_the_market_the_economy_does_not_list_and_not_the_ones_it_does() {

            var listed = MarketPlacementFixture.buildMarketOnBody("ancyra");
            var academy = MarketPlacementFixture.buildMarketOnBody("academy_station");
            var sector = buildSectorWithMarkets(listed);

            MarketPlacementFixture.placeMarketsIn(buildOnlySystem(sector), listed, academy);

            assertThat(StarSystems.readMarketsUnlistedByEconomy(sector, buildOnlySystem(sector)))
                .containsExactly(academy);
        }

        @Test
        void returns_empty_for_a_null_system() {
            assertThat(StarSystems.readMarketsUnlistedByEconomy(mock(SectorAPI.class), null))
                .isEmpty();
        }
    }

    /**
     * What the star-system surface adds, as with {@link ReadMarkets}: the search itself - what
     * it ranks, what it passes over, how it settles a tie - is {@code LocationMarketsTest}'s.
     */
    @Nested
    class FindNearestMarket {

        @Test
        void returns_the_admitted_market_whose_body_sits_closest() {

            var near = MarketPlacementFixture.buildMarketOnBodyAt("near", 100, 0);
            var far = MarketPlacementFixture.buildMarketOnBodyAt("far", 5000, 0);

            // Listed far-first so the pick is shown to come from distance, not economy order.
            var sector = buildSectorWithMarkets(far, near);

            assertThat(StarSystems.findNearestMarket(
                    sector,
                    buildOnlySystem(sector),
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .contains(near);
        }

        @Test
        void returns_empty_for_a_null_system() {
            assertThat(StarSystems.findNearestMarket(
                    mock(SectorAPI.class),
                    null,
                    MarketPlacementFixture.buildFleetAt(0, 0),
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

        // The routes are one set per running game, so a case that installs one empties the set
        // before and after itself rather than leaving it standing for whatever runs next - and
        // the cases that install none are then posed on a genuinely empty install.
        @BeforeEach
        void setUp() {
            SystemAccessRoutes.clearRoutes();
        }

        @AfterEach
        void tearDown() {
            SystemAccessRoutes.clearRoutes();
        }

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
        void returns_true_for_a_cut_off_system_an_installed_route_reaches() {
            // A route stands for a mod that moves fleets in without a jump
            // point, so it overrides the cut-off flag the way an active gate
            // does. Stated as a route rather than as any one mod's entity: what
            // this pins is that the read defers at all.
            SystemAccessRoutes.registerRoute("granting route", anySystem -> true);

            assertThat(StarSystems.isReachable(cutOffSystem("a")))
                .isTrue();
        }

        @Test
        void returns_false_for_a_cut_off_system_no_installed_route_reaches() {
            // A route that declines leaves the question where it found it, so
            // the system reads as the cut-off system it is.
            SystemAccessRoutes.registerRoute("declining route", anySystem -> false);

            assertThat(StarSystems.isReachable(cutOffSystem("a")))
                .isFalse();
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
