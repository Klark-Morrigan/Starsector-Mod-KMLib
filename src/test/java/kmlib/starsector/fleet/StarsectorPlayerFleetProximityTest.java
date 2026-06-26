package kmlib.starsector.fleet;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Pins the proximity helper's contract:
 * <ul>
 *   <li>distance threshold is {@code planet.radius + orbitOffset}, inclusive;</li>
 *   <li>a {@code null} planet returns {@code false};</li>
 *   <li>a missing player fleet returns {@code false}.</li>
 * </ul>
 *
 * <p>Tests stub {@code Global.getSector()} via a static mock and feed
 * real {@link Vector2f} locations so the underlying
 * {@code Points.computeDistance} math runs unmocked - the distance
 * arithmetic is not what's under test, only the predicate around it.
 */
class StarsectorPlayerFleetProximityTest {

    private MockedStatic<Global> globalMock;
    private SectorAPI sectorMock;

    @BeforeEach
    void setUp() {
        sectorMock = mock(SectorAPI.class);
        globalMock = mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sectorMock);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Test
    void returnsTrueWhenFleetSitsInsideTheOrbitBand() {
        // Fleet at (0,0), planet at (250,0), radius 100, offset 200 -> band 300.
        // 250 <= 300 -> in orbit.
        var planet = planetAt(250f, 0f, 100f);
        stubPlayerFleetAt(0f, 0f);

        assertThat(StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(planet, 200f))
                .isTrue();
    }

    @Test
    void returnsFalseWhenFleetSitsOutsideTheOrbitBand() {
        // Fleet at (0,0), planet at (500,0), radius 100, offset 200 -> band 300.
        // 500 > 300 -> not in orbit.
        var planet = planetAt(500f, 0f, 100f);
        stubPlayerFleetAt(0f, 0f);

        assertThat(StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(planet, 200f))
                .isFalse();
    }

    @Test
    void treatsTheBandEdgeAsInOrbit() {
        // Distance exactly equal to the band -> `<=` keeps the fleet
        // in orbit so a hairline-precise approach does not flip the
        // classification mid-frame.
        var planet = planetAt(300f, 0f, 100f);
        stubPlayerFleetAt(0f, 0f);

        assertThat(StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(planet, 200f))
                .isTrue();
    }

    @Test
    void scalesTheBandWithPlanetRadius() {
        // A larger host planet pushes the in-orbit band outward.
        // Radius 800 + offset 200 = 1000; fleet at distance 900 is in
        // orbit even though it would be remote of a small moon.
        var planet = planetAt(900f, 0f, 800f);
        stubPlayerFleetAt(0f, 0f);

        assertThat(StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(planet, 200f))
                .isTrue();
    }

    @Test
    void returnsFalseWhenPlayerFleetIsMissing() {
        // No player-fleet stub; the SectorAPI mock returns null by
        // default. Callers that want "in orbit" as the defensive
        // default branch on the boolean themselves.
        var planet = planetAt(0f, 0f, 100f);

        assertThat(StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(planet, 200f))
                .isFalse();
    }

    @Test
    void returnsFalseWhenPlanetIsNull() {
        // No planet to measure against; the helper short-circuits
        // before touching Global so a null planet does not depend on
        // sector state.
        stubPlayerFleetAt(0f, 0f);

        assertThat(StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(null, 200f))
                .isFalse();
    }

    @Test
    void returnsFalseWhenSectorIsMissing() {
        // Pre-game-load / teardown safety: a null sector means there
        // is no player fleet to consult, so the predicate falls into
        // the same "not in orbit" branch as a null fleet.
        globalMock.when(Global::getSector).thenReturn(null);
        var planet = planetAt(0f, 0f, 100f);

        assertThat(StarsectorPlayerFleetProximity.isPlayerFleetInOrbitOf(planet, 200f))
                .isFalse();
    }

    private PlanetAPI planetAt(float x, float y, float radius) {
        var planetMock = mock(PlanetAPI.class);
        Mockito.when(planetMock.getLocation()).thenReturn(new Vector2f(x, y));
        Mockito.when(planetMock.getRadius()).thenReturn(radius);
        return planetMock;
    }

    private void stubPlayerFleetAt(float x, float y) {
        var fleetMock = mock(CampaignFleetAPI.class);
        Mockito.when(fleetMock.getLocation()).thenReturn(new Vector2f(x, y));
        Mockito.when(sectorMock.getPlayerFleet()).thenReturn(fleetMock);
    }
}
