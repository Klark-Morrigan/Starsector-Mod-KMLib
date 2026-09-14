package kmlib.starsector.geometry;

import com.fs.starfarer.api.campaign.SectorEntityToken;

import org.junit.jupiter.api.Nested;
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

    @Nested
    class ComputeDistanceBetween {
        @Test
        void computeDistanceBetweenIsEuclidean() {
            var a = buildEntityAt(0f, 0f);
            var b = buildEntityAt(3f, 4f);

            assertThat(StarsectorPoints.computeDistanceBetween(a, b)).isEqualTo(5.0);
        }

        @Test
        void computeDistanceBetweenIsZeroForCoincidentEntities() {
            var a = buildEntityAt(2f, 7f);
            var b = buildEntityAt(2f, 7f);

            assertThat(StarsectorPoints.computeDistanceBetween(a, b)).isZero();
        }
    }

    @Nested
    class ComputeAngleDegreesBetween {
        @Test
        void computeAngleDegreesBetweenIsCounterClockwiseFromPositiveX() {
            var from = buildEntityAt(0f, 0f);
            var to = buildEntityAt(1f, 1f);

            assertThat(StarsectorPoints.computeAngleDegreesBetween(from, to)).isEqualTo(45.0);
        }

        @Test
        void computeAngleDegreesBetweenIsRelativeToTheFromEntity() {
            var from = buildEntityAt(2f, 2f);
            var to = buildEntityAt(5f, 6f);

            assertThat(StarsectorPoints.computeAngleDegreesBetween(from, to))
                .isCloseTo(53.13, within(0.01));
        }
    }

    @Nested
    class IsNearerThan {

        @Test
        void prefersTheStrictlyCloserCandidate() {
            assertThat(StarsectorPoints.isNearerThan(
                    100.0,
                    500.0,
                    buildEntityWithId("far_but_first"),
                    buildEntityWithId("close")))
                .isTrue();
        }

        @Test
        void keepsTheIncumbentWhenTheCandidateIsFurther() {
            assertThat(StarsectorPoints.isNearerThan(
                    500.0,
                    100.0,
                    buildEntityWithId("alpha"),
                    buildEntityWithId("omega")))
                .isFalse();
        }

        @Test
        void settlesAnEqualDistanceOnTheLowerId() {
            // Equidistant bodies are ordinary - a shared orbit, a mirrored pair - and with
            // distance alone the winner would be whichever the traversal met first.
            assertThat(StarsectorPoints.isNearerThan(
                    250.0,
                    250.0,
                    buildEntityWithId("alpha"),
                    buildEntityWithId("beta")))
                .isTrue();
        }

        @Test
        void keepsTheIncumbentWhenAnEqualDistanceCarriesTheHigherId() {
            assertThat(StarsectorPoints.isNearerThan(
                    250.0,
                    250.0,
                    buildEntityWithId("beta"),
                    buildEntityWithId("alpha")))
                .isFalse();
        }

        @Test
        void takesAnyCandidateWhenNothingHasBeenFoundYet() {
            assertThat(StarsectorPoints.isNearerThan(
                    Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    buildEntityWithId("only"),
                    null))
                .isTrue();
        }
    }

    private SectorEntityToken buildEntityAt(float x, float y) {
        var entityMock = mock(SectorEntityToken.class);
        Mockito.when(entityMock.getLocation()).thenReturn(new Vector2f(x, y));
        return entityMock;
    }

    // An entity known only by its ID, which is what a distance tie is settled on.
    private SectorEntityToken buildEntityWithId(String id) {
        var entityMock = mock(SectorEntityToken.class);
        Mockito.when(entityMock.getId()).thenReturn(id);
        return entityMock;
    }
}
