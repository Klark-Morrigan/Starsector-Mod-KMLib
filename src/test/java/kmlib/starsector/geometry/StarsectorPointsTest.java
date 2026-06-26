package kmlib.starsector.geometry;

import com.fs.starfarer.api.campaign.SectorEntityToken;

import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;

/**
 * Pins the entity-typed distance and bearing helpers. Entities are
 * plain {@link SectorEntityToken} mocks with real {@link Vector2f}
 * locations, so the suite runs with no booted game - the property the
 * helper exists to provide over {@code Misc.getDistance}.
 */
class StarsectorPointsTest {

    @Test
    void computeDistanceBetweenIsEuclidean() {
        var a = entityAt(0f, 0f);
        var b = entityAt(3f, 4f);

        assertThat(StarsectorPoints.computeDistanceBetween(a, b)).isEqualTo(5.0);
    }

    @Test
    void computeDistanceBetweenIsZeroForCoincidentEntities() {
        var a = entityAt(2f, 7f);
        var b = entityAt(2f, 7f);

        assertThat(StarsectorPoints.computeDistanceBetween(a, b)).isZero();
    }

    private SectorEntityToken entityAt(float x, float y) {
        var entity = mock(SectorEntityToken.class);
        Mockito.when(entity.getLocation()).thenReturn(new Vector2f(x, y));
        return entity;
    }
}
