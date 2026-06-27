package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.SectorEntityToken;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins {@link EntityOrbits}. {@link EntityOrbits#deriveSpeedDegPerDay}: the base
 * divisor of 20 yields {@code 360 * 20 / radius} deg/day, the jitter widens the
 * divisor by the sampled fraction of the spread, a negative spread is treated as
 * none, and a non-positive radius pins the body in place; radius 3600 is used so
 * the base rate is a round 2.0 deg/day and the randomness source is mocked for an
 * exact sample. {@link EntityOrbits#applyCircularOrbit}: a positive speed becomes
 * a circular orbit with period 360/speed, and a non-positive speed pins the
 * entity in place instead. Each method has its own {@link Nested} group.
 */
final class EntityOrbitsTest {

    private static final float ROUND_RATE_RADIUS = 3600f;
    private static final float TOLERANCE = 0.0001f;

    @Nested
    class DeriveSpeedDegPerDay {
        @Test
        void usesTheBaseDivisorWhenTheJitterSpreadIsZero() {
            var randomMock = mock(Random.class);
            when(randomMock.nextFloat()).thenReturn(1f);

            var speed = EntityOrbits.deriveSpeedDegPerDay(ROUND_RATE_RADIUS, 0f, randomMock);

            // Zero spread: the sample is irrelevant, divisor stays 20, 360*20/3600.
            assertThat(speed).isCloseTo(2.0f, within(TOLERANCE));
        }

        @Test
        void widensTheDivisorByTheFullSpreadAtTheMaximumSample() {
            var randomMock = mock(Random.class);
            when(randomMock.nextFloat()).thenReturn(1f);

            var speed = EntityOrbits.deriveSpeedDegPerDay(ROUND_RATE_RADIUS,
                    EntityOrbits.VANILLA_JITTER_FRACTION, randomMock);

            // Divisor 20*(1 + 1*0.25) = 25, so 360*25/3600 = 2.5.
            assertThat(speed).isCloseTo(2.5f, within(TOLERANCE));
        }

        @Test
        void widensTheDivisorByHalfTheSpreadAtTheMidpointSample() {
            var randomMock = mock(Random.class);
            when(randomMock.nextFloat()).thenReturn(0.5f);

            var speed = EntityOrbits.deriveSpeedDegPerDay(ROUND_RATE_RADIUS,
                    EntityOrbits.VANILLA_JITTER_FRACTION, randomMock);

            // Divisor 20*(1 + 0.5*0.25) = 22.5, so 360*22.5/3600 = 2.25.
            assertThat(speed).isCloseTo(2.25f, within(TOLERANCE));
        }

        @Test
        void treatsANegativeSpreadAsNoJitter() {
            var randomMock = mock(Random.class);
            when(randomMock.nextFloat()).thenReturn(1f);

            var speed = EntityOrbits.deriveSpeedDegPerDay(ROUND_RATE_RADIUS, -1f, randomMock);

            // Clamped to zero spread, so back to the base 2.0 regardless of sample.
            assertThat(speed).isCloseTo(2.0f, within(TOLERANCE));
        }

        @Test
        void scalesInverselyWithRadius() {
            var randomMock = mock(Random.class);
            when(randomMock.nextFloat()).thenReturn(0f);

            var speed = EntityOrbits.deriveSpeedDegPerDay(ROUND_RATE_RADIUS * 2f, 0f, randomMock);

            // Twice the radius is half the angular speed: 360*20/7200 = 1.0.
            assertThat(speed).isCloseTo(1.0f, within(TOLERANCE));
        }

        @Test
        void pinsInPlaceWhenRadiusIsNonPositive() {
            var randomMock = mock(Random.class);

            var speed = EntityOrbits.deriveSpeedDegPerDay(0f, EntityOrbits.VANILLA_JITTER_FRACTION,
                    randomMock);

            assertThat(speed).isEqualTo(0f);
            // A degenerate radius short-circuits before sampling the jitter.
            verify(randomMock, never()).nextFloat();
        }
    }

    @Nested
    class ApplyCircularOrbit {
        @Test
        void appliesCircularOrbitWithPeriodFromSpeed() {
            var entityMock = mock(SectorEntityToken.class);
            var focusMock = mock(SectorEntityToken.class);

            EntityOrbits.applyCircularOrbit(entityMock, focusMock, 1000f, 2f, 90f);

            // 360 / 2 deg-per-day = a 180-day period at radius 1000, starting at 90.
            verify(entityMock).setCircularOrbit(focusMock, 90f, 1000f, 180f);
        }

        @Test
        void pinsEntityInPlaceWhenSpeedIsNonPositive() {
            var entityMock = mock(SectorEntityToken.class);
            var focusMock = mock(SectorEntityToken.class);
            when(focusMock.getLocation()).thenReturn(new Vector2f(100f, 200f));

            // Speed 0, angle 0: pinned at distance 50 along +x from the focus.
            EntityOrbits.applyCircularOrbit(entityMock, focusMock, 50f, 0f, 0f);

            verify(entityMock).setFixedLocation(150f, 200f);
            verify(entityMock, never()).setCircularOrbit(focusMock, 0f, 50f, 0f);
        }
    }
}
