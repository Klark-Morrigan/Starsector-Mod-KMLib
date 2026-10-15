package kmlib.starsector;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.testfixtures.starsector.systems.StarSystemFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link SectorWalks#walkStarSystems}: which systems a walk reaches, in what
 * order, and what it charges. The cases live in a {@link Nested} group so the suite reports as a
 * per-method tree.
 */
final class SectorWalksTests {

    @Nested
    class WalkStarSystems {

        @Test
        void visitsEverySystemInTheSectorsOwnOrder() {

            var corvus = StarSystemFixture.buildSystem("corvus");
            var yma = StarSystemFixture.buildSystem("yma");
            var visited = new ArrayList<StarSystemAPI>();

            SectorWalks.walkStarSystems(StarSystemFixture.buildSectorOf(corvus, yma), visited::add);

            assertThat(visited)
                .containsExactly(corvus, yma);
        }

        @Test
        void countsOneWalkOverEverySystemItReached() {

            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.buildSystem("corvus"),
                StarSystemFixture.buildSystem("yma"));

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorWalks.walkStarSystems(sector, system -> { }));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(2L);
        }

        @Test
        void visitsNothingAndChargesNoWalkForANullSector() {

            var visited = new ArrayList<StarSystemAPI>();

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorWalks.walkStarSystems(null, visited::add));

            assertThat(visited)
                .isEmpty();
            assertThat(counts.hasCount(SectorWalkCounters.SECTOR_WALKS))
                .isFalse();
        }

        @Test
        void visitsNothingAndChargesNoWalkForASectorListingNoSystemsAtAll() {
            // A sector mid-teardown answers nothing for its systems. There was no list to go over,
            // so nothing is charged - and nothing faults, which is what lets a load survive it.
            var sectorMock = mock(SectorAPI.class);
            var visited = new ArrayList<StarSystemAPI>();

            when(sectorMock.getStarSystems())
                .thenReturn(null);

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorWalks.walkStarSystems(sectorMock, visited::add));

            assertThat(visited)
                .isEmpty();
            assertThat(counts.hasCount(SectorWalkCounters.SECTOR_WALKS))
                .isFalse();
        }
    }
}
