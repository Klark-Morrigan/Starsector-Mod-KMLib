package kmlib.starsector.entities;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins {@link EntitySpawner} on its create-then-place wiring. The custom-entity
 * path adds the entity to the focus's location and hands it to the orbit seam;
 * the jump-point path builds the point through the factory, places it, and -
 * only when the focus sits in a star system - generates the hyperspace entrance
 * and clears the cut-off tag, the steps that make the system reachable.
 *
 * <p>The static seams {@code Global} (the factory) and {@code EntityOrbits} (the
 * orbit placement) are stubbed via static mocks, so this pins the spawner's own
 * orchestration rather than the factory or the orbit math (covered by
 * {@link EntityOrbitsTest}). Cases live under a {@link Nested} group named for
 * the method under test.
 */
final class EntitySpawnerTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<EntityOrbits> entityOrbitsMock;
    private FactoryAPI factoryMock;

    @BeforeEach
    void setUp() {
        factoryMock = mock(FactoryAPI.class);
        globalMock = mockStatic(Global.class);
        globalMock.when(Global::getFactory).thenReturn(factoryMock);
        entityOrbitsMock = mockStatic(EntityOrbits.class);
    }

    @AfterEach
    void tearDown() {
        entityOrbitsMock.close();
        globalMock.close();
    }

    @Nested
    class SpawnOrbitingCustomEntity {
        @Test
        void addsTheEntityToTheFocusLocationAndPutsItOnTheOrbit() {
            var focusMock = mock(SectorEntityToken.class);
            var locationMock = mock(LocationAPI.class);
            when(focusMock.getContainingLocation()).thenReturn(locationMock);
            var entityMock = mock(CustomCampaignEntityAPI.class);
            // Null id and name let the engine auto-assign; the type and faction
            // are the spawn slots passed through.
            when(locationMock.addCustomEntity(null, null, "inactive_gate", "neutral"))
                    .thenReturn(entityMock);

            var result = EntitySpawner.spawnOrbitingCustomEntity(focusMock, "inactive_gate",
                    "neutral", 100f, 2f, 45f);

            assertThat(result).isSameAs(entityMock);
            // The freshly added entity is placed on the requested orbit geometry.
            entityOrbitsMock.verify(() -> EntityOrbits.applyCircularOrbit(
                    entityMock, focusMock, 100f, 2f, 45f));
        }
    }

    @Nested
    class SpawnOrbitingJumpPoint {
        @Test
        void buildsPlacesAndWiresHyperspaceWhenTheFocusIsInAStarSystem() {
            var focusMock = mock(SectorEntityToken.class);
            var systemMock = mock(StarSystemAPI.class);
            when(focusMock.getContainingLocation()).thenReturn(systemMock);
            var jumpPointMock = mock(JumpPointAPI.class);
            when(factoryMock.createJumpPoint(null, "Corvus Jump-point")).thenReturn(jumpPointMock);

            var result = EntitySpawner.spawnOrbitingJumpPoint(focusMock, "Corvus Jump-point",
                    200f, 3f, 90f);

            assertThat(result).isSameAs(jumpPointMock);
            verify(jumpPointMock).setStandardWormholeToHyperspaceVisual();
            verify(systemMock).addEntity(jumpPointMock);
            entityOrbitsMock.verify(() -> EntityOrbits.applyCircularOrbit(
                    jumpPointMock, focusMock, 200f, 3f, 90f));
            // The reachability wiring: generate the hyperspace entrance and clear
            // the cut-off tag so the system is no longer sealed off.
            verify(systemMock).autogenerateHyperspaceJumpPoints();
            verify(systemMock).removeTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
            verify(systemMock).updateAllOrbits();
        }

        @Test
        void skipsHyperspaceWiringWhenTheFocusIsNotInAStarSystem() {
            var focusMock = mock(SectorEntityToken.class);
            // A plain location (e.g. hyperspace itself) is not a StarSystemAPI, so
            // the reachability block is skipped - the point is just placed.
            var locationMock = mock(LocationAPI.class);
            when(focusMock.getContainingLocation()).thenReturn(locationMock);
            var jumpPointMock = mock(JumpPointAPI.class);
            when(factoryMock.createJumpPoint(null, "Lone Jump-point")).thenReturn(jumpPointMock);

            var result = EntitySpawner.spawnOrbitingJumpPoint(focusMock, "Lone Jump-point",
                    50f, 1f, 0f);

            assertThat(result).isSameAs(jumpPointMock);
            verify(jumpPointMock).setStandardWormholeToHyperspaceVisual();
            verify(locationMock).addEntity(jumpPointMock);
            entityOrbitsMock.verify(() -> EntityOrbits.applyCircularOrbit(
                    jumpPointMock, focusMock, 50f, 1f, 0f));
        }
    }
}
