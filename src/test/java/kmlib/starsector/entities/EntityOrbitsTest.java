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
 * Pins {@link EntityOrbits}. {@link EntityOrbits#deriveBaseSpeedDegPerDay}: the
 * base divisor of 20 yields {@code 360 * 20 / radius} deg/day and a non-positive
 * radius pins the body in place; radius 3600 gives a round 2.0 deg/day base.
 * {@link EntityOrbits#applyJitter}: the rate is scaled by
 * {@code 1 + sample * spread}, a negative spread is treated as none, and a
 * non-positive speed stays non-positive; the randomness source is mocked for an
 * exact sample. {@link EntityOrbits#applyCircularOrbit}: a positive speed becomes
 * a circular orbit with period 360/speed, and a non-positive speed pins the
 * entity in place instead. Each method has its own {@link Nested} group.
 */
final class EntityOrbitsTest {

    private static final float ROUND_RATE_RADIUS = 3600f;
    // The base rate at ROUND_RATE_RADIUS: 360 * 20 / 3600.
    private static final float ROUND_BASE_SPEED = 2.0f;
    private static final float TOLERANCE = 0.0001f;

    @Nested
    class DeriveBaseSpeedDegPerDay {
        @Test
        void usesTheBaseDivisor() {
            var speed = EntityOrbits.deriveBaseSpeedDegPerDay(ROUND_RATE_RADIUS);

            // Divisor 20: 360 * 20 / 3600 = 2.0.
            assertThat(speed).isCloseTo(ROUND_BASE_SPEED, within(TOLERANCE));
        }

        @Test
        void scalesInverselyWithRadius() {
            var speed = EntityOrbits.deriveBaseSpeedDegPerDay(ROUND_RATE_RADIUS * 2f);

            // Twice the radius is half the angular speed: 360 * 20 / 7200 = 1.0.
            assertThat(speed).isCloseTo(1.0f, within(TOLERANCE));
        }

        @Test
        void pinsInPlaceWhenRadiusIsNonPositive() {
            assertThat(EntityOrbits.deriveBaseSpeedDegPerDay(0f)).isEqualTo(0f);
        }
    }

    @Nested
    class ApplyJitter {
        @Test
        void leavesTheSpeedUnchangedWhenTheSpreadIsZero() {
            var randomMock = mock(Random.class);
            when(randomMock.nextFloat()).thenReturn(1f);

            var speed = EntityOrbits.applyJitter(ROUND_BASE_SPEED, 0f, randomMock);

            // Zero spread: the sample is irrelevant, the speed is untouched.
            assertThat(speed).isCloseTo(ROUND_BASE_SPEED, within(TOLERANCE));
        }

        @Test
        void widensByTheFullSpreadAtTheMaximumSample() {
            var randomMock = mock(Random.class);
            when(randomMock.nextFloat()).thenReturn(1f);

            var speed = EntityOrbits.applyJitter(ROUND_BASE_SPEED,
                EntityOrbits.VANILLA_JITTER_FRACTION, randomMock);

            // 2.0 * (1 + 1 * 0.25) = 2.5.
            assertThat(speed).isCloseTo(2.5f, within(TOLERANCE));
        }

        @Test
        void widensByHalfTheSpreadAtTheMidpointSample() {
            var randomMock = mock(Random.class);
            when(randomMock.nextFloat()).thenReturn(0.5f);

            var speed = EntityOrbits.applyJitter(ROUND_BASE_SPEED,
                EntityOrbits.VANILLA_JITTER_FRACTION, randomMock);

            // 2.0 * (1 + 0.5 * 0.25) = 2.25.
            assertThat(speed).isCloseTo(2.25f, within(TOLERANCE));
        }

        @Test
        void treatsANegativeSpreadAsNoJitter() {
            var randomMock = mock(Random.class);
            when(randomMock.nextFloat()).thenReturn(1f);

            var speed = EntityOrbits.applyJitter(ROUND_BASE_SPEED, -1f, randomMock);

            // Clamped to zero spread, so the speed is unchanged regardless of sample.
            assertThat(speed).isCloseTo(ROUND_BASE_SPEED, within(TOLERANCE));
        }

        @Test
        void leavesANonPositiveSpeedNonPositive() {
            var randomMock = mock(Random.class);
            when(randomMock.nextFloat()).thenReturn(1f);

            // A pinned (zero) speed must stay pinned even with a spread applied.
            var speed = EntityOrbits.applyJitter(0f, EntityOrbits.VANILLA_JITTER_FRACTION,
                randomMock);

            assertThat(speed).isEqualTo(0f);
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

    @Nested
    class GetOrbitalDistanceTo {

        @Test
        void sums_a_planets_own_orbit_to_its_star() {

            var starMock = mock(SectorEntityToken.class);
            var planet = buildOrbiting(300, starMock);

            assertThat(EntityOrbits.computeOrbitalDistanceTo(planet, starMock))
                .isEqualTo(300.0);
        }

        @Test
        void sums_the_whole_orbit_chain_for_a_moon() {

            var starMock = mock(SectorEntityToken.class);
            var planet = buildOrbiting(300, starMock);
            var moon = buildOrbiting(50, planet);

            assertThat(EntityOrbits.computeOrbitalDistanceTo(moon, starMock))
                .isEqualTo(350.0);
        }

        @Test
        void does_not_add_the_references_own_orbit() {

            var starMock = buildOrbiting(9999, mock(SectorEntityToken.class));
            var planet = buildOrbiting(300, starMock);

            assertThat(EntityOrbits.computeOrbitalDistanceTo(planet, starMock))
                .isEqualTo(300.0);
        }

        @Test
        void yields_infinity_for_a_null_body() {
            assertThat(EntityOrbits.computeOrbitalDistanceTo(null, mock(SectorEntityToken.class)))
                .isEqualTo(Double.POSITIVE_INFINITY);
        }
    }

    private static SectorEntityToken buildOrbiting(float radius, SectorEntityToken focus) {

        var bodyMock = mock(SectorEntityToken.class);

        when(bodyMock.getCircularOrbitRadius())
            .thenReturn(radius);
        when(bodyMock.getOrbitFocus())
            .thenReturn(focus);

        return bodyMock;
    }
}
