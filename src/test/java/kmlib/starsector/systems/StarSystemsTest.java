package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

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
    @Test
    void collects_each_system_position_as_xy() {
        var sector = sectorWithSystemsAt(new float[] {10, 20}, new float[] {-5, 7});

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
        var located = mock(StarSystemAPI.class);
        when(located.getLocation()).thenReturn(new Vector2f(1, 2));
        var unlocated = mock(StarSystemAPI.class);
        when(unlocated.getLocation()).thenReturn(null);
        var sector = mock(SectorAPI.class);
        when(sector.getStarSystems()).thenReturn(List.of(located, unlocated));

        assertThat(StarSystems.getHyperspacePositions(sector)).hasSize(1);
    }

    private static SectorAPI sectorWithSystemsAt(float[]... points) {
        var systems = new ArrayList<StarSystemAPI>();
        for (float[] point : points) {
            var system = mock(StarSystemAPI.class);
            when(system.getLocation()).thenReturn(new Vector2f(point[0], point[1]));
            systems.add(system);
        }
        var sector = mock(SectorAPI.class);
        when(sector.getStarSystems()).thenReturn(systems);
        return sector;
    }
}
