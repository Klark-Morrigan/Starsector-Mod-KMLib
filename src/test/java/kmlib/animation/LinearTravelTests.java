package kmlib.animation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link LinearTravel}: a travel covers the elapsed share of its duration either way, stops exactly on
 * its end, cuts for a duration of nothing, and lands in the same place whether the time comes in one step or
 * in many - the property that lets the stepped and the clocked types share it.
 */
final class LinearTravelTests {

    private static final float FAR_END = 1f;
    private static final float REST = 0f;

    private static final double DURATION_SECONDS = 0.4;

    private static final boolean RISING = true;
    private static final boolean FALLING = false;

    @Nested
    class ResolveEndValue {

        @Test
        void isTheFarEndForARisingTravel() {

            assertThat(LinearTravel.resolveEndValue(RISING))
                .isEqualTo(FAR_END);
        }

        @Test
        void isRestForAFallingTravel() {

            assertThat(LinearTravel.resolveEndValue(FALLING))
                .isEqualTo(REST);
        }
    }

    @Nested
    class ResolveProgressAfter {

        @Test
        void coversTheElapsedShareTowardAHigherEnd() {

            assertThat(LinearTravel.resolveProgressAfter(REST, FAR_END, 0.1, DURATION_SECONDS))
                .isEqualTo(0.25f);
        }

        @Test
        void coversTheElapsedShareTowardALowerEnd() {

            assertThat(LinearTravel.resolveProgressAfter(FAR_END, REST, 0.1, DURATION_SECONDS))
                .isEqualTo(0.75f);
        }

        @Test
        void stopsExactlyOnTheEndRatherThanPastIt() {

            assertThat(LinearTravel.resolveProgressAfter(0.5f, FAR_END, 10, DURATION_SECONDS))
                .isEqualTo(FAR_END);
        }

        @Test
        void cutsToTheEndForADurationOfNothing() {

            assertThat(LinearTravel.resolveProgressAfter(0.3f, FAR_END, 0, 0))
                .isEqualTo(FAR_END);
            assertThat(LinearTravel.resolveProgressAfter(0.3f, REST, 0, -1))
                .isEqualTo(REST);
        }

        @Test
        void landsOnTheEndAlikeInOneStepOrInMany() {
            // A stepped type feeds the rule each frame's time and a clocked one the whole time since its
            // turn; both must arrive at the same end on the same elapsed total.
            var steppedProgress = REST;
            for (var step = 0; step < 5; step++) {
                steppedProgress = LinearTravel.resolveProgressAfter(steppedProgress, FAR_END, 0.1, DURATION_SECONDS);
            }

            assertThat(steppedProgress)
                .isEqualTo(LinearTravel.resolveProgressAfter(REST, FAR_END, 0.5, DURATION_SECONDS));
        }
    }
}
