package kmlib.starsector.fleet;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the orbit check's contract:
 * <ul>
 *   <li>the fleet must share the entity's location;</li>
 *   <li>distance threshold is {@code entity.radius + orbitOffset}, inclusive;</li>
 *   <li>a {@code null} entity or player fleet returns {@code false}.</li>
 * </ul>
 *
 * <p>Real {@link Vector2f} locations feed the distance math, so it runs unmocked - the arithmetic is not what's under
 * test, only the predicate around it.
 */
final class PlayerFleetOrbitTests {

    private static final float ORBIT_OFFSET = 200f;

    private SectorAPI sectorMock;
    private LocationAPI systemMock;

    @BeforeEach
    void setUp() {

        sectorMock = mock(SectorAPI.class);
        systemMock = mock(LocationAPI.class);
    }

    private SectorEntityToken buildEntityAt(float x, float y, float radius) {

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.getContainingLocation())
            .thenReturn(systemMock);
        when(entityMock.getLocation())
            .thenReturn(new Vector2f(x, y));
        when(entityMock.getRadius())
            .thenReturn(radius);

        return entityMock;
    }

    private void stubPlayerFleetAt(LocationAPI location, float x, float y) {

        var fleetMock = mock(CampaignFleetAPI.class);

        when(fleetMock.getContainingLocation())
            .thenReturn(location);
        when(fleetMock.getLocation())
            .thenReturn(new Vector2f(x, y));
        when(sectorMock.getPlayerFleet())
            .thenReturn(fleetMock);
    }

    @Nested
    class IsPlayerFleetInOrbitOf {

        // Fleet at (0,0), entity at (250,0), radius 100, offset 200: the band is 300, and 250 is inside it.
        @Test
        void answersTrueWhenTheFleetSitsInsideTheOrbitBand() {

            // ARRANGE
            var entity = buildEntityAt(250f, 0f, 100f);

            stubPlayerFleetAt(systemMock, 0f, 0f);

            // ACT
            var actualIsInOrbit = PlayerFleetOrbit.isPlayerFleetInOrbitOf(sectorMock, entity, ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isTrue();
        }

        // Fleet at (0,0), entity at (500,0), radius 100, offset 200: the band is 300, and 500 is past it.
        @Test
        void answersFalseWhenTheFleetSitsOutsideTheOrbitBand() {

            // ARRANGE
            var entity = buildEntityAt(500f, 0f, 100f);

            stubPlayerFleetAt(systemMock, 0f, 0f);

            // ACT
            var actualIsInOrbit = PlayerFleetOrbit.isPlayerFleetInOrbitOf(sectorMock, entity, ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isFalse();
        }

        // Coordinates are local to a location, so a fleet in another system at in-band coordinates is not in orbit.
        @Test
        void answersFalseWhenTheFleetIsInAnotherLocation() {

            // ARRANGE
            var entity = buildEntityAt(250f, 0f, 100f);

            stubPlayerFleetAt(mock(LocationAPI.class), 0f, 0f);

            // ACT
            var actualIsInOrbit = PlayerFleetOrbit.isPlayerFleetInOrbitOf(sectorMock, entity, ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isFalse();
        }

        // Fleet at (0,0), entity at (300,0), radius 100, offset 200: the fleet sits exactly on the band's edge.
        @Test
        void treatsTheBandEdgeAsInOrbit() {

            // ARRANGE
            var entity = buildEntityAt(300f, 0f, 100f);

            stubPlayerFleetAt(systemMock, 0f, 0f);

            // ACT
            var actualIsInOrbit = PlayerFleetOrbit.isPlayerFleetInOrbitOf(sectorMock, entity, ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isTrue();
        }

        // Radius 800 plus offset 200 puts the band at 1000, so a fleet 900 out is in orbit of a large entity.
        @Test
        void scalesTheBandWithTheEntitysRadius() {

            // ARRANGE
            var entity = buildEntityAt(900f, 0f, 800f);

            stubPlayerFleetAt(systemMock, 0f, 0f);

            // ACT
            var actualIsInOrbit = PlayerFleetOrbit.isPlayerFleetInOrbitOf(sectorMock, entity, ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isTrue();
        }

        // A sector has no player fleet while a new game is being generated.
        @Test
        void answersFalseWithoutAPlayerFleet() {

            // ARRANGE
            var entity = buildEntityAt(0f, 0f, 100f);

            // ACT
            var actualIsInOrbit = PlayerFleetOrbit.isPlayerFleetInOrbitOf(sectorMock, entity, ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isFalse();
        }

        @Test
        void answersFalseWithoutAnEntity() {

            // ARRANGE
            stubPlayerFleetAt(systemMock, 0f, 0f);

            // ACT
            var actualIsInOrbit = PlayerFleetOrbit.isPlayerFleetInOrbitOf(sectorMock, null, ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isFalse();
        }
    }
}
