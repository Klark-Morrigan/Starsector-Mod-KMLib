package kmlib.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins {@link ListSystemEntitiesCommand#buildReport}: the orbit tree nests each
 * entity under its focus with distance/speed, unorbited entities and fleets
 * follow with coordinates, and the gates filter keeps gates plus their orbit
 * chain while dropping everything else.
 */
final class ListSystemEntitiesCommandTest {
    @Nested
    class BuildReport {
        @Test
        void buildsOrbitTreeWithTrailingUnorbitedAndFleets() {
            var star = entity("star", "Star", 0f, 0f, null, 0f, false);
            var planet = entity("planet", "Planet", 1000f, 0f, star, 360f, false);
            var gate = entity("gate1", "Gate", 1100f, 0f, planet, 180f, true);
            var drifting = entity("probe", "Probe", 500f, 0f, null, 0f, false);
            var fleet = fleet("Patrol", 2000f, 0f);
            var system = system(star, List.of(star, planet, gate, drifting), List.of(fleet));

            var report = ListSystemEntitiesCommand.buildReport(system, false);

            assertThat(report).contains("Star [star]");
            assertThat(report).contains("Planet [planet]  dist=1000  speed=1.00 deg/day");
            assertThat(report).contains("Gate [gate1]  dist=100  speed=2.00 deg/day");
            assertThat(report).contains("Unorbited entities (nearest center first):");
            assertThat(report).contains("Probe [probe]  (500, 0)");
            assertThat(report).contains("Fleets (nearest center first):");
            assertThat(report).contains("Patrol [Patrol]  (2000, 0)");
        }

        @Test
        void gatesFilterKeepsGatesAndTheirOrbitChainOnly() {
            var star = entity("star", "Star", 0f, 0f, null, 0f, false);
            var planet = entity("planet", "Planet", 1000f, 0f, star, 360f, false);
            var gate = entity("gate1", "Gate", 1100f, 0f, planet, 180f, true);
            var otherPlanet = entity("planet2", "Other", -1000f, 0f, star, 360f, false);
            var drifting = entity("probe", "Probe", 500f, 0f, null, 0f, false);
            var fleet = fleet("Patrol", 2000f, 0f);
            var system = system(star,
                    List.of(star, planet, gate, otherPlanet, drifting), List.of(fleet));

            var report = ListSystemEntitiesCommand.buildReport(system, true);

            // Gate plus its orbit chain (planet, star) are kept.
            assertThat(report).contains("Star [star]");
            assertThat(report).contains("Planet [planet]");
            assertThat(report).contains("Gate [gate1]");
            // A sibling planet with no gate, the drifting probe, and fleets are gone.
            assertThat(report).doesNotContain("Other [planet2]");
            assertThat(report).doesNotContain("Probe [probe]");
            assertThat(report).doesNotContain("Patrol");
        }
    }

    private static StarSystemAPI system(SectorEntityToken center,
            List<SectorEntityToken> allEntities, List<CampaignFleetAPI> fleets) {
        var systemMock = mock(StarSystemAPI.class);
        when(systemMock.getName()).thenReturn("Test System");
        when(systemMock.getCenter()).thenReturn(center);
        when(systemMock.getAllEntities()).thenReturn(allEntities);
        when(systemMock.getFleets()).thenReturn(fleets);
        return systemMock;
    }

    private static SectorEntityToken entity(String id, String name, float x, float y,
            SectorEntityToken focus, float orbitalPeriodDays, boolean isGate) {
        var entityMock = mock(SectorEntityToken.class);
        when(entityMock.getId()).thenReturn(id);
        when(entityMock.getName()).thenReturn(name);
        when(entityMock.getLocation()).thenReturn(new Vector2f(x, y));
        when(entityMock.getOrbitFocus()).thenReturn(focus);
        when(entityMock.hasTag(Tags.GATE)).thenReturn(isGate);
        if (focus != null) {
            var orbitMock = mock(OrbitAPI.class);
            when(orbitMock.getOrbitalPeriod()).thenReturn(orbitalPeriodDays);
            when(entityMock.getOrbit()).thenReturn(orbitMock);
        }
        return entityMock;
    }

    private static CampaignFleetAPI fleet(String name, float x, float y) {
        var fleetMock = mock(CampaignFleetAPI.class);
        when(fleetMock.getId()).thenReturn(name);
        when(fleetMock.getName()).thenReturn(name);
        when(fleetMock.getLocation()).thenReturn(new Vector2f(x, y));
        return fleetMock;
    }
}
