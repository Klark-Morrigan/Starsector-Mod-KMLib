package kmlib.starsector.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import assortment_of_things.abyss.entities.hyper.AbyssalFracture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link StarSystems#getHyperspacePositions},
 * {@link StarSystems#getPlayerStarSystem}, {@link StarSystems#getStars},
 * {@link StarSystems#hasKnownOwnedMarket}, {@link StarSystems#getCentremostStar},
 * {@link StarSystems#getOrbitalDistanceTo}, {@link StarSystems#isReachable},
 * {@link StarSystems#find}, and {@link StarSystems#findById}. Each method's cases live in a
 * {@link Nested} group so
 * the suite reports as a per-method tree; the shared mock builders stay on the
 * outer class.
 */
final class StarSystemsTest {

    @Nested
    class GetHyperspacePositions {
        @Test
        void collects_each_system_position_as_xy() {
            var sector = buildSectorWithSystemsAt(new float[] {10, 20}, new float[] {-5, 7});

            var positions = StarSystems.getHyperspacePositions(sector);

            assertThat(positions).hasSize(2);
            assertThat(positions.get(0)).containsExactly(10.0, 20.0);
            assertThat(positions.get(1)).containsExactly(-5.0, 7.0);
        }

        @Test
        void null_sector_yields_no_positions() {
            assertThat(StarSystems.getHyperspacePositions(null)).isEmpty();
        }

        @Test
        void systems_without_a_location_are_skipped() {
            var locatedMock = mock(StarSystemAPI.class);
            when(locatedMock.getLocation()).thenReturn(new Vector2f(1, 2));
            var unlocatedMock = mock(StarSystemAPI.class);
            when(unlocatedMock.getLocation()).thenReturn(null);
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getStarSystems()).thenReturn(List.of(locatedMock, unlocatedMock));

            assertThat(StarSystems.getHyperspacePositions(sectorMock)).hasSize(1);
        }
    }

    @Nested
    class CollectPositionsById {
        @Test
        void keys_each_selected_system_by_id_with_its_position() {
            var a = systemAt("a", 10, 20);
            var b = systemAt("b", -5, 7);
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getStarSystems()).thenReturn(List.of(a, b));

            var positions = StarSystems.collectPositionsById(sectorMock, system -> true);

            assertThat(positions.get("a")).containsExactly(10.0, 20.0);
            assertThat(positions.get("b")).containsExactly(-5.0, 7.0);
        }

        @Test
        void excludes_systems_the_predicate_rejects() {
            var kept = systemAt("kept", 1, 1);
            var rejected = systemAt("rejected", 2, 2);
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getStarSystems()).thenReturn(List.of(kept, rejected));

            var positions = StarSystems.collectPositionsById(sectorMock,
                    system -> system.getId().equals("kept"));

            assertThat(positions).containsOnlyKeys("kept");
        }

        @Test
        void skips_a_selected_system_without_a_location() {
            var located = systemAt("located", 1, 1);
            var unlocatedMock = mock(StarSystemAPI.class);
            when(unlocatedMock.getLocation()).thenReturn(null);
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getStarSystems()).thenReturn(List.of(located, unlocatedMock));

            var positions = StarSystems.collectPositionsById(sectorMock, system -> true);

            assertThat(positions).containsOnlyKeys("located");
        }

        @Test
        void a_null_predicate_keeps_every_located_system() {
            var a = systemAt("a", 1, 1);
            var b = systemAt("b", 2, 2);
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getStarSystems()).thenReturn(List.of(a, b));

            var positions = StarSystems.collectPositionsById(sectorMock, null);

            assertThat(positions).containsOnlyKeys("a", "b");
        }

        @Test
        void null_sector_yields_no_positions() {
            assertThat(StarSystems.collectPositionsById(null, system -> true)).isEmpty();
        }
    }

    @Nested
    class GetPlayerStarSystem {
        @Test
        void returns_the_fleets_system() {
            var systemMock = mock(StarSystemAPI.class);
            var fleetMock = mock(CampaignFleetAPI.class);
            when(fleetMock.getStarSystem()).thenReturn(systemMock);
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getPlayerFleet()).thenReturn(fleetMock);

            assertThat(StarSystems.getPlayerStarSystem(sectorMock)).isSameAs(systemMock);
        }

        @Test
        void returns_null_for_a_null_sector() {
            assertThat(StarSystems.getPlayerStarSystem(null)).isNull();
        }

        @Test
        void returns_null_without_a_player_fleet() {
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getPlayerFleet()).thenReturn(null);

            assertThat(StarSystems.getPlayerStarSystem(sectorMock)).isNull();
        }

        @Test
        void returns_null_when_the_fleet_is_in_hyperspace() {
            var fleetMock = mock(CampaignFleetAPI.class);
            when(fleetMock.getStarSystem()).thenReturn(null);
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getPlayerFleet()).thenReturn(fleetMock);

            assertThat(StarSystems.getPlayerStarSystem(sectorMock)).isNull();
        }
    }

    @Nested
    class FindById {
        @Test
        void returns_the_system_whose_id_matches() {
            var wanted = systemAt("corvus", 1, 1);
            var other = systemAt("yma", 2, 2);
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getStarSystems()).thenReturn(List.of(other, wanted));

            assertThat(StarSystems.findById(sectorMock, "corvus")).isSameAs(wanted);
        }

        @Test
        void returns_null_when_no_system_has_that_id() {
            var only = systemAt("corvus", 1, 1);
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getStarSystems()).thenReturn(List.of(only));

            assertThat(StarSystems.findById(sectorMock, "nowhere")).isNull();
        }

        @Test
        void returns_null_for_a_null_sector() {
            assertThat(StarSystems.findById(null, "corvus")).isNull();
        }

        @Test
        void returns_null_for_a_blank_id() {
            // A blank id short-circuits before the walk, so a stubbed system list is not even
            // needed - a blank query matches nothing rather than the first system by accident.
            assertThat(StarSystems.findById(mock(SectorAPI.class), " ")).isNull();
        }
    }

    @Nested
    class GetStars {
        @Test
        void keeps_only_the_stars_in_system_order() {
            var starA = buildPlanet(true);
            var gasGiant = buildPlanet(false);
            var starB = buildPlanet(true);
            var systemMock = mock(StarSystemAPI.class);
            when(systemMock.getPlanets()).thenReturn(List.of(starA, gasGiant, starB));

            assertThat(StarSystems.getStars(systemMock)).containsExactly(starA, starB);
        }

        @Test
        void returns_empty_for_a_null_system() {
            assertThat(StarSystems.getStars(null)).isEmpty();
        }
    }

    @Nested
    class GetCentremostStar {
        @Test
        void returns_the_only_star_in_a_single_star_system() {
            var star = starWithLocation(0, 0);
            var system = systemWithCentreAndStars(star, star);

            assertThat(StarSystems.getCentremostStar(system)).isSameAs(star);
        }

        @Test
        void returns_the_star_nearest_the_centre_in_a_multi_star_system() {
            var centreMock = mock(SectorEntityToken.class);
            when(centreMock.getLocation()).thenReturn(new Vector2f(0, 0));
            var nearStar = starWithLocation(0, 0);
            var farStar = starWithLocation(5000, 0);
            // Listed far-first so the pick is shown to come from distance, not list order.
            var system = systemWithCentreAndStars(centreMock, farStar, nearStar);

            assertThat(StarSystems.getCentremostStar(system)).isSameAs(nearStar);
        }

        @Test
        void breaks_an_equal_distance_tie_by_lowest_star_id() {
            var centreMock = mock(SectorEntityToken.class);
            when(centreMock.getLocation()).thenReturn(new Vector2f(0, 0));
            var starBeta = starWithIdAt("beta", 0, 100);
            var starAlpha = starWithIdAt("alpha", 0, -100);
            // Both stars sit the same distance from the centre; listed high-id first so the
            // lower id is shown to be the deterministic pick rather than the planet-list order.
            var system = systemWithCentreAndStars(centreMock, starBeta, starAlpha);

            assertThat(StarSystems.getCentremostStar(system)).isSameAs(starAlpha);
        }

        @Test
        void falls_back_to_the_centre_token_when_the_system_has_no_star() {
            var centreMock = mock(SectorEntityToken.class);
            var system = systemWithCentreAndStars(centreMock);

            assertThat(StarSystems.getCentremostStar(system)).isSameAs(centreMock);
        }

        @Test
        void returns_null_for_a_null_system() {
            assertThat(StarSystems.getCentremostStar(null)).isNull();
        }
    }

    @Nested
    class GetOrbitalDistanceTo {
        @Test
        void sums_a_planets_own_orbit_to_its_star() {
            var starMock = mock(SectorEntityToken.class);
            var planet = orbiting(300, starMock);

            assertThat(StarSystems.getOrbitalDistanceTo(planet, starMock)).isEqualTo(300.0);
        }

        @Test
        void sums_the_whole_orbit_chain_for_a_moon() {
            var starMock = mock(SectorEntityToken.class);
            var planet = orbiting(300, starMock);
            var moon = orbiting(50, planet);

            assertThat(StarSystems.getOrbitalDistanceTo(moon, starMock)).isEqualTo(350.0);
        }

        @Test
        void does_not_add_the_references_own_orbit() {
            var starMock = orbiting(9999, mock(SectorEntityToken.class));
            var planet = orbiting(300, starMock);

            assertThat(StarSystems.getOrbitalDistanceTo(planet, starMock)).isEqualTo(300.0);
        }

        @Test
        void yields_infinity_for_a_null_body() {
            assertThat(StarSystems.getOrbitalDistanceTo(null, mock(SectorEntityToken.class)))
                    .isEqualTo(Double.POSITIVE_INFINITY);
        }
    }

    @Nested
    class HasKnownOwnedMarket {
        @Test
        void returns_true_for_a_visible_owned_market() {
            var sector = sectorWithMarkets(visibleColony());

            assertThat(StarSystems.hasKnownOwnedMarket(sector, onlySystem(sector))).isTrue();
        }

        @Test
        void returns_true_when_one_of_several_markets_qualifies() {
            var sector = sectorWithMarkets(conditionOnlyMarket(), visibleColony());

            assertThat(StarSystems.hasKnownOwnedMarket(sector, onlySystem(sector))).isTrue();
        }

        @Test
        void returns_false_for_a_condition_only_market() {
            var sector = sectorWithMarkets(conditionOnlyMarket());

            assertThat(StarSystems.hasKnownOwnedMarket(sector, onlySystem(sector))).isFalse();
        }

        @Test
        void returns_false_for_an_undiscovered_concealed_station() {
            var sector = sectorWithMarkets(concealedStation());

            assertThat(StarSystems.hasKnownOwnedMarket(sector, onlySystem(sector))).isFalse();
        }

        @Test
        void returns_true_for_a_concealed_station_when_including_undiscovered_markets() {
            var sector = sectorWithMarkets(concealedStation());

            assertThat(StarSystems.hasKnownOwnedMarket(sector, onlySystem(sector), true)).isTrue();
        }

        @Test
        void returns_false_for_a_system_with_no_markets() {
            var sector = sectorWithMarkets();

            assertThat(StarSystems.hasKnownOwnedMarket(sector, onlySystem(sector))).isFalse();
        }

        @Test
        void returns_false_for_a_null_sector() {
            assertThat(StarSystems.hasKnownOwnedMarket(null, mock(StarSystemAPI.class))).isFalse();
        }

        @Test
        void returns_false_for_a_null_system() {
            assertThat(StarSystems.hasKnownOwnedMarket(mock(SectorAPI.class), null)).isFalse();
        }

        @Test
        void returns_false_when_the_sector_has_no_economy() {
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getEconomy()).thenReturn(null);

            assertThat(StarSystems.hasKnownOwnedMarket(sectorMock, mock(StarSystemAPI.class)))
                    .isFalse();
        }
    }

    @Nested
    class IsReachable {
        @Test
        void returns_true_for_a_system_with_a_jump_point() {
            assertThat(StarSystems.isReachable(systemNotCutOff("a"))).isTrue();
        }

        @Test
        void returns_false_for_a_transverse_only_system_with_an_inactive_gate() {
            // The hidden-system case: not cut off (the engine never tags a
            // nascent-well system), no jump point, only an unlit gate.
            var system = transverseOnlySystem("a", gateWithPlugin(gatePlugin(false)));

            assertThat(StarSystems.isReachable(system)).isFalse();
        }

        @Test
        void returns_false_for_a_cut_off_system_with_no_gate() {
            assertThat(StarSystems.isReachable(cutOffSystem("a"))).isFalse();
        }

        @Test
        void returns_false_for_a_cut_off_system_with_only_an_inactive_gate() {
            var system = cutOffSystem("a", gateWithPlugin(gatePlugin(false)));

            assertThat(StarSystems.isReachable(system)).isFalse();
        }

        @Test
        void returns_true_for_a_cut_off_system_with_an_active_gate() {
            var system = cutOffSystem("a", gateWithPlugin(gatePlugin(true)));

            assertThat(StarSystems.isReachable(system)).isTrue();
        }

        @Test
        void returns_true_for_a_cut_off_system_with_a_fracture_when_rat_enabled() {
            // A fracture ferries fleets in past the disabled jump points, so it
            // overrides the cut-off flag the way an active gate does.
            var system = cutOffSystemWithEntities("a", fractureEntity());
            try (MockedStatic<Global> globalMock = mockStatic(Global.class)) {
                stubRatEnabled(globalMock, true);

                assertThat(StarSystems.isReachable(system)).isTrue();
            }
        }

        @Test
        void returns_false_for_a_cut_off_system_with_a_fracture_when_rat_disabled() {
            // The optional dependency is off, so the matcher cannot see the
            // fracture and the system reads as the cut-off system it is.
            var system = cutOffSystemWithEntities("a", fractureEntity());
            try (MockedStatic<Global> globalMock = mockStatic(Global.class)) {
                stubRatEnabled(globalMock, false);

                assertThat(StarSystems.isReachable(system)).isFalse();
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
            when(systemMock.getEntitiesWithTag("gate")).thenReturn(List.of(other, gate));

            assertThat(StarSystems.find(systemMock, "gate", "alpha-gate")).isSameAs(gate);
        }

        @Test
        void returns_null_when_no_tagged_entity_has_the_id() {
            var other = buildEntity("beta-gate");
            var systemMock = mock(StarSystemAPI.class);
            when(systemMock.getEntitiesWithTag("gate")).thenReturn(List.of(other));

            assertThat(StarSystems.find(systemMock, "gate", "alpha-gate")).isNull();
        }

        @Test
        void returns_null_for_a_null_system() {
            assertThat(StarSystems.find(null, "gate", "alpha-gate")).isNull();
        }

        @Test
        void returns_null_for_a_blank_id() {
            var systemMock = mock(StarSystemAPI.class);

            assertThat(StarSystems.find(systemMock, "gate", " ")).isNull();
        }

        @Test
        void returns_null_for_a_null_id() {
            var systemMock = mock(StarSystemAPI.class);

            assertThat(StarSystems.find(systemMock, "gate", null)).isNull();
        }
    }

    private static StarSystemAPI onlySystem(SectorAPI sector) {
        return sector.getStarSystems().get(0);
    }

    // A star fixed at a location, so a central-star search can rank stars by nearness to the
    // system centre.
    private static PlanetAPI starWithLocation(float x, float y) {
        var starMock = mock(PlanetAPI.class);
        when(starMock.isStar()).thenReturn(true);
        when(starMock.getLocation()).thenReturn(new Vector2f(x, y));
        return starMock;
    }

    // A star fixed at a location and carrying an id, so a central-star search's distance-tie
    // resolution by id can be pinned.
    private static PlanetAPI starWithIdAt(String id, float x, float y) {
        var starMock = starWithLocation(x, y);
        when(starMock.getId()).thenReturn(id);
        return starMock;
    }

    // A body on a circular orbit of the given radius around a focus, the unit an orbit-chain
    // distance sums.
    private static SectorEntityToken orbiting(float radius, SectorEntityToken focus) {
        var bodyMock = mock(SectorEntityToken.class);
        when(bodyMock.getCircularOrbitRadius()).thenReturn(radius);
        when(bodyMock.getOrbitFocus()).thenReturn(focus);
        return bodyMock;
    }

    // A system with a centre token and its stars, the two a central-star search reads.
    private static StarSystemAPI systemWithCentreAndStars(SectorEntityToken centre,
            PlanetAPI... stars) {
        var systemMock = mock(StarSystemAPI.class);
        when(systemMock.getCenter()).thenReturn(centre);
        when(systemMock.getPlanets()).thenReturn(List.of(stars));
        return systemMock;
    }

    // Wires a sector with one system whose economy holds the given markets, so a
    // hasKnownOwnedMarket read resolves through getEconomy().getMarkets(system).
    private static SectorAPI sectorWithMarkets(MarketAPI... markets) {
        var systemMock = mock(StarSystemAPI.class);
        var economyMock = mock(EconomyAPI.class);
        when(economyMock.getMarkets(systemMock)).thenReturn(List.of(markets));
        var sectorMock = mock(SectorAPI.class);
        when(sectorMock.getStarSystems()).thenReturn(List.of(systemMock));
        when(sectorMock.getEconomy()).thenReturn(economyMock);
        return sectorMock;
    }

    // A visible owned colony: a faction owns it, it is not condition-only, and its
    // discovered entity passes the known-to-player gate.
    private static MarketAPI visibleColony() {
        return buildColony(false, false, false);
    }

    // A bare planet's condition-only placeholder: owned but not a colony, so it is
    // filtered out by the ownership arm.
    private static MarketAPI conditionOnlyMarket() {
        return buildColony(true, false, false);
    }

    // A concealed station: a hidden market on a still-discoverable entity, failing
    // the known-to-player gate until the player finds it.
    private static MarketAPI concealedStation() {
        return buildColony(false, true, true);
    }

    private static MarketAPI buildColony(boolean isConditionOnly, boolean isHidden,
            boolean isEntityDiscoverable) {
        var entityMock = mock(SectorEntityToken.class);
        when(entityMock.isDiscoverable()).thenReturn(isEntityDiscoverable);
        var marketMock = mock(MarketAPI.class);
        when(marketMock.getFaction()).thenReturn(mock(FactionAPI.class));
        when(marketMock.isPlanetConditionMarketOnly()).thenReturn(isConditionOnly);
        when(marketMock.isHidden()).thenReturn(isHidden);
        when(marketMock.getPrimaryEntity()).thenReturn(entityMock);
        return marketMock;
    }

    private static SectorEntityToken buildEntity(String id) {
        var entityMock = mock(SectorEntityToken.class);
        when(entityMock.getId()).thenReturn(id);
        return entityMock;
    }

    private static StarSystemAPI systemNotCutOff(String id) {
        // Not cut off (hasTag defaults to false) and wired into hyperspace by a
        // jump point - a normally reachable system.
        var systemMock = mock(StarSystemAPI.class);
        when(systemMock.getId()).thenReturn(id);
        when(systemMock.getJumpPoints()).thenReturn(List.of(mock(SectorEntityToken.class)));
        return systemMock;
    }

    private static StarSystemAPI transverseOnlySystem(String id, SectorEntityToken... gates) {
        // Not cut off and holds no jump point - reachable only by transverse jump
        // to a nascent gravity well. Any passed gates stand in for present-but-
        // inactive gates that must not confer access.
        var systemMock = mock(StarSystemAPI.class);
        when(systemMock.getId()).thenReturn(id);
        when(systemMock.getEntitiesWithTag(Tags.GATE)).thenReturn(List.of(gates));
        when(systemMock.getJumpPoints()).thenReturn(List.of());
        return systemMock;
    }

    private static StarSystemAPI cutOffSystem(String id, SectorEntityToken... gates) {
        var systemMock = mock(StarSystemAPI.class);
        when(systemMock.getId()).thenReturn(id);
        when(systemMock.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)).thenReturn(true);
        when(systemMock.getEntitiesWithTag(Tags.GATE)).thenReturn(List.of(gates));
        return systemMock;
    }

    private static StarSystemAPI cutOffSystemWithEntities(String id, SectorEntityToken... entities) {
        // Cut off and holding no jump point or gate - access can come only from
        // one of the passed entities (a fracture in these cases).
        var systemMock = mock(StarSystemAPI.class);
        when(systemMock.getId()).thenReturn(id);
        when(systemMock.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)).thenReturn(true);
        when(systemMock.getAllEntities()).thenReturn(List.of(entities));
        return systemMock;
    }

    private static SectorEntityToken gateWithPlugin(GateEntityPlugin plugin) {
        var gateMock = mock(SectorEntityToken.class);
        when(gateMock.getCustomPlugin()).thenReturn(plugin);
        return gateMock;
    }

    private static GateEntityPlugin gatePlugin(boolean isActive) {
        var pluginMock = mock(GateEntityPlugin.class);
        when(pluginMock.isActive()).thenReturn(isActive);
        return pluginMock;
    }

    private static SectorEntityToken fractureEntity() {
        var entityMock = mock(SectorEntityToken.class);
        when(entityMock.getCustomPlugin()).thenReturn(mock(AbyssalFracture.class));
        return entityMock;
    }

    private static void stubRatEnabled(MockedStatic<Global> globalMock, boolean isEnabled) {
        var settingsMock = mock(SettingsAPI.class);
        var modManagerMock = mock(ModManagerAPI.class);
        globalMock.when(Global::getSettings).thenReturn(settingsMock);
        when(settingsMock.getModManager()).thenReturn(modManagerMock);
        when(modManagerMock.isModEnabled("assortment_of_things")).thenReturn(isEnabled);
    }

    private static PlanetAPI buildPlanet(boolean isStar) {
        var planetMock = mock(PlanetAPI.class);
        when(planetMock.isStar()).thenReturn(isStar);
        return planetMock;
    }

    private static StarSystemAPI systemAt(String id, float x, float y) {
        var systemMock = mock(StarSystemAPI.class);
        when(systemMock.getId()).thenReturn(id);
        when(systemMock.getLocation()).thenReturn(new Vector2f(x, y));
        return systemMock;
    }

    private static SectorAPI buildSectorWithSystemsAt(float[]... points) {
        var systems = new ArrayList<StarSystemAPI>();
        for (float[] point : points) {
            var systemMock = mock(StarSystemAPI.class);
            when(systemMock.getLocation()).thenReturn(new Vector2f(point[0], point[1]));
            systems.add(systemMock);
        }
        var sectorMock = mock(SectorAPI.class);
        when(sectorMock.getStarSystems()).thenReturn(systems);
        return sectorMock;
    }
}
