package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link SectorStarSystems#getHyperspacePositions},
 * {@link SectorStarSystems#collectPositionsById},
 * {@link SectorStarSystems#getPlayerStarSystem}, {@link SectorStarSystems#indexById} and
 * {@link SectorStarSystems#findById}. Each method's cases live in a {@link Nested} group so the
 * suite reports as a per-method tree; the shared mock builders stay on the outer class.
 */
final class SectorStarSystemsTest {

    @Nested
    class GetHyperspacePositions {

        @Test
        void collects_each_system_position_as_xy() {

            var sector = buildSectorWithSystemsAt(new float[] {10, 20}, new float[] {-5, 7});
            var positions = SectorStarSystems.getHyperspacePositions(sector);

            assertThat(positions)
                .hasSize(2);
            assertThat(positions.get(0))
                .containsExactly(10.0, 20.0);
            assertThat(positions.get(1))
                .containsExactly(-5.0, 7.0);
        }

        @Test
        void null_sector_yields_no_positions() {
            assertThat(SectorStarSystems.getHyperspacePositions(null))
                .isEmpty();
        }

        @Test
        void systems_without_a_location_are_skipped() {

            var locatedMock = mock(StarSystemAPI.class);

            when(locatedMock.getLocation())
                .thenReturn(new Vector2f(1, 2));

            var unlocatedMock = mock(StarSystemAPI.class);

            when(unlocatedMock.getLocation())
                .thenReturn(null);

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(locatedMock, unlocatedMock));

            assertThat(SectorStarSystems.getHyperspacePositions(sectorMock))
                .hasSize(1);
        }
    }

    @Nested
    class CollectPositionsById {

        @Test
        void keys_each_selected_system_by_id_with_its_position() {

            var a = buildSystemAt("a", 10, 20);
            var b = buildSystemAt("b", -5, 7);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(a, b));

            var positions = SectorStarSystems.collectPositionsById(sectorMock, system -> true);

            assertThat(positions.get("a"))
                .containsExactly(10.0, 20.0);
            assertThat(positions.get("b"))
                .containsExactly(-5.0, 7.0);
        }

        @Test
        void excludes_systems_the_predicate_rejects() {

            var kept = buildSystemAt("kept", 1, 1);
            var rejected = buildSystemAt("rejected", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(kept, rejected));

            var positions = SectorStarSystems.collectPositionsById(
                sectorMock,
                system -> system.getId().equals("kept"));

            assertThat(positions)
                .containsOnlyKeys("kept");
        }

        @Test
        void skips_a_selected_system_without_a_location() {

            var located = buildSystemAt("located", 1, 1);
            var unlocatedMock = mock(StarSystemAPI.class);

            when(unlocatedMock.getLocation())
                .thenReturn(null);

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(located, unlocatedMock));

            var positions = SectorStarSystems.collectPositionsById(sectorMock, system -> true);

            assertThat(positions)
                .containsOnlyKeys("located");
        }

        @Test
        void a_null_predicate_keeps_every_located_system() {

            var a = buildSystemAt("a", 1, 1);
            var b = buildSystemAt("b", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(a, b));

            var positions = SectorStarSystems.collectPositionsById(sectorMock, null);

            assertThat(positions)
                .containsOnlyKeys("a", "b");
        }

        @Test
        void null_sector_yields_no_positions() {
            assertThat(SectorStarSystems.collectPositionsById(null, system -> true))
                .isEmpty();
        }
    }

    @Nested
    class GetPlayerStarSystem {

        @Test
        void returns_the_fleets_system() {

            var systemMock = mock(StarSystemAPI.class);
            var fleetMock = mock(CampaignFleetAPI.class);

            when(fleetMock.getStarSystem())
                .thenReturn(systemMock);

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getPlayerFleet())
                .thenReturn(fleetMock);

            assertThat(SectorStarSystems.getPlayerStarSystem(sectorMock))
                .isSameAs(systemMock);
        }

        @Test
        void returns_null_for_a_null_sector() {
            assertThat(SectorStarSystems.getPlayerStarSystem(null))
                .isNull();
        }

        @Test
        void returns_null_without_a_player_fleet() {

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getPlayerFleet())
                .thenReturn(null);

            assertThat(SectorStarSystems.getPlayerStarSystem(sectorMock))
                .isNull();
        }

        @Test
        void returns_null_when_the_fleet_is_in_hyperspace() {

            var fleetMock = mock(CampaignFleetAPI.class);

            when(fleetMock.getStarSystem())
                .thenReturn(null);

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getPlayerFleet())
                .thenReturn(fleetMock);

            assertThat(SectorStarSystems.getPlayerStarSystem(sectorMock))
                .isNull();
        }
    }

    @Nested
    class FindById {

        @Test
        void returns_the_system_whose_id_matches() {

            var wanted = buildSystemAt("corvus", 1, 1);
            var other = buildSystemAt("yma", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(other, wanted));

            assertThat(SectorStarSystems.findById(sectorMock, "corvus"))
                .isSameAs(wanted);
        }

        @Test
        void returns_null_when_no_system_has_that_id() {

            var only = buildSystemAt("corvus", 1, 1);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(only));

            assertThat(SectorStarSystems.findById(sectorMock, "nowhere"))
                .isNull();
        }

        @Test
        void returns_null_for_a_null_sector() {
            assertThat(SectorStarSystems.findById(null, "corvus"))
                .isNull();
        }

        @Test
        void returns_null_for_a_blank_id() {
            // A blank id short-circuits before the walk, so a stubbed system list is not even
            // needed - a blank query matches nothing rather than the first system by accident.
            assertThat(SectorStarSystems.findById(mock(SectorAPI.class), " "))
                .isNull();
        }
    }

    @Nested
    class IndexById {

        @Test
        void keys_every_system_by_its_own_id_in_the_sectors_order() {
            // The bulk lookup a pass resolving many ids reaches for instead of walking the system
            // list once per id. The sector's own order is kept, so a caller iterating the index
            // sees the systems in the order the sector lists them rather than a hash's.
            var corvus = buildSystemAt("corvus", 1, 1);
            var yma = buildSystemAt("yma", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(yma, corvus));

            var indexed = SectorStarSystems.indexById(sectorMock);

            assertThat(indexed)
                .containsExactly(
                    entry("yma", yma),
                    entry("corvus", corvus));
        }

        @Test
        void returns_an_empty_index_for_a_null_sector() {
            assertThat(SectorStarSystems.indexById(null))
                .isEmpty();
        }
    }

    private static StarSystemAPI buildSystemAt(String id, float x, float y) {

        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getId())
            .thenReturn(id);
        when(systemMock.getLocation())
            .thenReturn(new Vector2f(x, y));

        return systemMock;
    }

    private static SectorAPI buildSectorWithSystemsAt(float[]... points) {

        var systems = new ArrayList<StarSystemAPI>();

        for (float[] point : points) {

            var systemMock = mock(StarSystemAPI.class);

            when(systemMock.getLocation())
                .thenReturn(new Vector2f(point[0], point[1]));

            systems.add(systemMock);
        }

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getStarSystems())
            .thenReturn(systems);

        return sectorMock;
    }
}
