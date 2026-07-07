package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link StarSystems#getHyperspacePositions},
 * {@link StarSystems#getPlayerStarSystem}, {@link StarSystems#getStars},
 * {@link StarSystems#hasKnownOwnedMarket}, and {@link StarSystems#find}. Each
 * method's cases live in a {@link Nested} group so the suite reports as a
 * per-method tree; the shared mock builders stay on the outer class.
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
