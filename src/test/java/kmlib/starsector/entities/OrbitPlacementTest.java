package kmlib.starsector.entities;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contracts of {@link OrbitPlacement#createOrbiting},
 * {@link OrbitPlacement#createPinned} and {@link OrbitPlacement#isPinned} - the value that carries
 * a body's place around its focus, and the standstill reading every placing caller branches on.
 * Each method's cases live in a {@link Nested} group so the suite reports as a per-method tree.
 */
final class OrbitPlacementTest {

    @Nested
    class CreateOrbiting {

        @Test
        void carriesTheDistanceSpeedAndStartAngleItWasGiven() {

            var placement = OrbitPlacement.createOrbiting(1000f, 2f, 90f);

            assertThat(placement.orbitDistance())
                .isEqualTo(1000f);
            assertThat(placement.speedDegPerDay())
                .isEqualTo(2f);
            assertThat(placement.startAngleDegrees())
                .isEqualTo(90f);
        }

        @Test
        void readsAsPinnedWhereTheSpeedItWasGivenIsNought() {
            // A rate derived from a radius or widened by a jitter can come out at nought, so the
            // orbiting factory accepts one rather than making every such caller test first.
            assertThat(OrbitPlacement.createOrbiting(1000f, 0f, 90f).isPinned())
                .isTrue();
        }
    }

    @Nested
    class CreatePinned {

        @Test
        void carriesTheDistanceAndAngleTheBodyIsHeldAt() {

            var placement = OrbitPlacement.createPinned(50f, 180f);

            assertThat(placement.orbitDistance())
                .isEqualTo(50f);
            assertThat(placement.startAngleDegrees())
                .isEqualTo(180f);
        }

        @Test
        void readsAsPinned() {
            // The whole point of the second factory: a caller says the standstill in the
            // signature rather than in a nought a reader has to recognise.
            assertThat(OrbitPlacement.createPinned(50f, 180f).isPinned())
                .isTrue();
        }
    }

    @Nested
    class IsPinned {

        @Test
        void reportsAPositiveSpeedAsTravelling() {

            assertThat(OrbitPlacement.createOrbiting(1000f, 0.001f, 0f).isPinned())
                .isFalse();
        }

        @Test
        void reportsANoughtSpeedAsPinned() {

            assertThat(OrbitPlacement.createOrbiting(1000f, 0f, 0f).isPinned())
                .isTrue();
        }

        @Test
        void reportsANegativeSpeedAsPinned() {
            // A standstill arrived at by arithmetic is the same standstill: nothing downstream
            // could do anything with a backwards rate but treat it as no motion.
            assertThat(OrbitPlacement.createOrbiting(1000f, -5f, 0f).isPinned())
                .isTrue();
        }
    }
}
