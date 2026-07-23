package kmlib.math.easing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link Easing#easeInOut}: the smoothstep S-curve the collapsing tab panel eases its linear progress
 * through, so a constant-rate advance reads as accelerating off the start and settling into the end. The
 * hand-checkable points come from 3t^2 - 2t^3: 0.25 eases to 0.15625 and the curve is symmetric about its
 * 0.5 midpoint.
 */
final class EasingTest {
    private static final float TOLERANCE = 0.0001f;

    @Nested
    class EaseInOut {

        @Test
        void easeInOutPinsBothEndsToTheirLinearValues() {
            assertThat(Easing.easeInOut(0f)).isCloseTo(0f, within(TOLERANCE));
            assertThat(Easing.easeInOut(1f)).isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void easeInOutCrossesTheLinearLineAtTheMidpoint() {
            assertThat(Easing.easeInOut(0.5f)).isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void easeInOutLagsTheLinearRampWhileEasingInOutOfTheStart() {
            // Below the midpoint the eased value trails a straight ramp - the slow, accelerating start.
            assertThat(Easing.easeInOut(0.25f)).isCloseTo(0.15625f, within(TOLERANCE));
            assertThat(Easing.easeInOut(0.25f)).isLessThan(0.25f);
        }

        @Test
        void easeInOutLeadsTheLinearRampWhileEasingIntoTheEnd() {
            // Above the midpoint it runs ahead of the ramp, then decelerates - the settling finish.
            assertThat(Easing.easeInOut(0.75f)).isCloseTo(0.84375f, within(TOLERANCE));
            assertThat(Easing.easeInOut(0.75f)).isGreaterThan(0.75f);
        }

        @Test
        void easeInOutIsSymmetricAboutItsMidpoint() {
            // easeInOut(t) + easeInOut(1 - t) == 1, so a forward and a reversed run trace the same shape.
            for (var t = 0f; t <= 1f; t += 0.1f) {
                assertThat(Easing.easeInOut(t) + Easing.easeInOut(1f - t))
                        .isCloseTo(1f, within(TOLERANCE));
            }
        }

        @Test
        void easeInOutRisesMonotonicallyAcrossTheRange() {
            var previous = Easing.easeInOut(0f);
            for (var t = 0.05f; t <= 1f; t += 0.05f) {
                var current = Easing.easeInOut(t);
                assertThat(current).isGreaterThanOrEqualTo(previous);
                previous = current;
            }
        }

        @Test
        void easeInOutClampsProgressPastEitherEndToTheNearestBound() {
            // An overshooting progress settles at the end rather than folding back through the polynomial.
            assertThat(Easing.easeInOut(1.4f)).isCloseTo(1f, within(TOLERANCE));
            assertThat(Easing.easeInOut(-0.6f)).isCloseTo(0f, within(TOLERANCE));
        }
    }
}
