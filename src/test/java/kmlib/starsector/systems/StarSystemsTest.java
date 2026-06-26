package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link StarSystems#getHyperspacePositions}:
 *  - one {x, y} point per system, in order,
 *  - a null sector yields nothing,
 *  - a system with no location is skipped.
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
