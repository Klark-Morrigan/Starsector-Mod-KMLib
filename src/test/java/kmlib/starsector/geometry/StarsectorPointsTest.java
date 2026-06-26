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

    @Test
    void computeAngleDegreesBetweenIsCounterClockwiseFromPositiveX() {
        var from = entityAt(0f, 0f);
        var to = entityAt(1f, 1f);

        assertThat(StarsectorPoints.computeAngleDegreesBetween(from, to)).isEqualTo(45.0);
    }

    @Test
    void computeAngleDegreesBetweenIsRelativeToTheFromEntity() {
        var from = entityAt(2f, 2f);
        var to = entityAt(5f, 6f);

        assertThat(StarsectorPoints.computeAngleDegreesBetween(from, to))
                .isCloseTo(53.13, within(0.01));
    }

    private SectorEntityToken entityAt(float x, float y) {
        var entityMock = mock(SectorEntityToken.class);
        Mockito.when(entityMock.getLocation()).thenReturn(new Vector2f(x, y));
        return entityMock;
    }
}
