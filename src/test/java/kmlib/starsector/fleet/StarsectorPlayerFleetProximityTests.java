package kmlib.starsector.fleet;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the proximity helper's contract:
 * <ul>
 *   <li>distance threshold is {@code planet.radius + orbitOffset}, inclusive;</li>
 *   <li>a {@code null} planet, sector or player fleet returns {@code false}.</li>
 * </ul>
 *
 * <p>Real {@link Vector2f} locations feed the distance math, so it runs unmocked - the arithmetic is not what's under
 * test, only the predicate around it.
 */
final class StarsectorPlayerFleetProximityTests {

    private static final float ORBIT_OFFSET = 200f;

    private SectorAPI sectorMock;

    private static PlanetAPI buildPlanetAt(float x, float y, float radius) {

        var planetMock = mock(PlanetAPI.class);

        when(planetMock.getLocation())
            .thenReturn(new Vector2f(x, y));
        when(planetMock.getRadius())
            .thenReturn(radius);

        return planetMock;
    }

    @BeforeEach
    void setUp() {

        sectorMock = mock(SectorAPI.class);
    }

    @AfterEach
    void tearDown() {

        Global.setSector(null);
    }

    private void stubPlayerFleetAt(float x, float y) {

        var fleetMock = mock(CampaignFleetAPI.class);

        when(fleetMock.getLocation())
            .thenReturn(new Vector2f(x, y));
        when(sectorMock.getPlayerFleet())
            .thenReturn(fleetMock);
    }

    @Nested
    class IsPlayerFleetInOrbitOf {

        // Fleet at (0,0), planet at (250,0), radius 100, offset 200: the band is 300, and 250 is inside it.
        @Test
        void answersTrueWhenTheFleetSitsInsideTheOrbitBand() {

            // ARRANGE
            var planet = buildPlanetAt(250f, 0f, 100f);

            stubPlayerFleetAt(0f, 0f);

            // ACT
            var actualIsInOrbit = StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(
                sectorMock,
                planet,
                ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isTrue();
        }

        // Fleet at (0,0), planet at (500,0), radius 100, offset 200: the band is 300, and 500 is past it.
        @Test
        void answersFalseWhenTheFleetSitsOutsideTheOrbitBand() {

            // ARRANGE
            var planet = buildPlanetAt(500f, 0f, 100f);

            stubPlayerFleetAt(0f, 0f);

            // ACT
            var actualIsInOrbit = StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(
                sectorMock,
                planet,
                ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isFalse();
        }

        // A fleet exactly on the band stays in orbit, so a hairline approach does not flip mid-frame.
        @Test
        void treatsTheBandEdgeAsInOrbit() {

            // ARRANGE
            var planet = buildPlanetAt(300f, 0f, 100f);

            stubPlayerFleetAt(0f, 0f);

            // ACT
            var actualIsInOrbit = StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(
                sectorMock,
                planet,
                ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isTrue();
        }

        // Radius 800 plus offset 200 puts the band at 1000, so a fleet 900 out is in orbit of a large planet.
        @Test
        void scalesTheBandWithThePlanetsRadius() {

            // ARRANGE
            var planet = buildPlanetAt(900f, 0f, 800f);

            stubPlayerFleetAt(0f, 0f);

            // ACT
            var actualIsInOrbit = StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(
                sectorMock,
                planet,
                ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isTrue();
        }

        @Test
        void answersFalseWithoutAPlayerFleet() {

            // ARRANGE
            var planet = buildPlanetAt(0f, 0f, 100f);

            // ACT
            var actualIsInOrbit = StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(
                sectorMock,
                planet,
                ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isFalse();
        }

        @Test
        void answersFalseWithoutAPlanet() {

            // ARRANGE
            stubPlayerFleetAt(0f, 0f);

            // ACT
            var actualIsInOrbit = StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(
                sectorMock,
                null,
                ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isFalse();
        }

        // Before a game loads there is no sector, and so no player fleet to consult.
        @Test
        void answersFalseWithoutASector() {

            // ARRANGE
            var planet = buildPlanetAt(0f, 0f, 100f);

            // ACT
            var actualIsInOrbit = StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(null, planet, ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isFalse();
        }

        @Test
        void measuresThePlayerFleetOfTheSectorGlobalHoldsWithoutASectorHandedIn() {

            // ARRANGE
            var planet = buildPlanetAt(250f, 0f, 100f);

            stubPlayerFleetAt(0f, 0f);
            Global.setSector(sectorMock);

            // ACT
            var actualIsInOrbit = StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(planet, ORBIT_OFFSET);

            // ASSERT
            assertThat(actualIsInOrbit)
                .isTrue();
        }
    }
}
