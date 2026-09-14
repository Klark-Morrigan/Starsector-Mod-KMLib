package kmlib.animation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins one two-ended travel: it starts at rest, travels out while the flag says so and back when it stops
 * saying so, and - the whole reason it is a fraction rather than a flag of its own - carries on from where
 * it stands when the direction turns over mid-travel.
 *
 * <p>The asymmetric pace is pinned as behaviour rather than as arithmetic, because a pair read the wrong way
 * round is the failure nothing else would catch: both directions still travel, both still arrive, and only
 * the feel is wrong.
 */
final class TraverseFractionTest {

    private static final float TOLERANCE = 0.0001f;

    // A whole duration in one step, so a test that only cares about an end state reaches it without walking
    // frames.
    private static final float FULL_STEP_SECONDS = 1f;
    private static final float DURATION_SECONDS = 1f;

    // Half a duration, the step the eased curve's midpoint is pinned at.
    private static final float HALF_STEP_SECONDS = 0.5f;
    private static final float QUARTER_STEP_SECONDS = 0.25f;

    // The same pace each way, so every case below reads one duration off the step it charges rather than
    // having to say which direction it is travelling. The asymmetric case names its own pair.
    private static final TraverseDurations DURATIONS =
        TraverseDurations.createSymmetric(DURATION_SECONDS);

    private static final boolean RISING = true;
    private static final boolean FALLING = false;

    @Nested
    class GetEasedValue {

        @Test
        void getEasedValueStartsAtRest() {
            assertThat(new TraverseFraction().getEasedValue())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void getEasedValueEasesTheHalfwayProgressToTheCurveMidpoint() {
            // The stored progress is linear and the curve is applied on read, so half a duration reads as
            // the curve's own midpoint rather than as the raw half.
            var fraction = new TraverseFraction();
            fraction.advanceTowardEnd(RISING, HALF_STEP_SECONDS, DURATIONS);

            assertThat(fraction.getEasedValue())
                .isCloseTo(0.5f, within(TOLERANCE));
        }
    }

    @Nested
    class AdvanceTowardEnd {

        @Test
        void advanceTowardEndReachesTheFarEndWhileHeadingThere() {

            var fraction = new TraverseFraction();
            fraction.advanceTowardEnd(RISING, FULL_STEP_SECONDS, DURATIONS);

            assertThat(fraction.getEasedValue())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceTowardEndComesBackToRestOnceHeadingBack() {

            var fraction = new TraverseFraction();
            fraction.advanceTowardEnd(RISING, FULL_STEP_SECONDS, DURATIONS);
            fraction.advanceTowardEnd(FALLING, FULL_STEP_SECONDS, DURATIONS);

            assertThat(fraction.getEasedValue())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceTowardEndFallsFromWhereItStandsWhenTheDirectionTurnsMidRise() {
            // The point of a retargeted travel: something abandoned half-way out falls from half-way rather
            // than snapping to either end or replaying a curve from the top.
            var fraction = new TraverseFraction();
            fraction.advanceTowardEnd(RISING, HALF_STEP_SECONDS, DURATIONS);
            fraction.advanceTowardEnd(FALLING, QUARTER_STEP_SECONDS, DURATIONS);

            // Half a duration out then a quarter back leaves the linear progress at a quarter, which the
            // curve reads out below its linear value.
            assertThat(fraction.getEasedValue())
                .isCloseTo(0.15625f, within(TOLERANCE));
        }

        @Test
        void advanceTowardEndSnapsAllTheWayForANonPositiveDuration() {
            // A zero pace means "no animation", so the travel lands on its far end in one step rather than
            // dividing by a zero duration.
            var fraction = new TraverseFraction();
            fraction.advanceTowardEnd(RISING, FULL_STEP_SECONDS, TraverseDurations.SNAP);

            assertThat(fraction.getEasedValue())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceTowardEndTravelsOutAtTheRisePaceAndBackAtTheFallPace() {
            // The reason the two are a pair rather than one value: a travel taking the same time either way
            // reads as the slower half. The rise here is charged its whole duration and arrives; the same
            // step then spends only half of the longer fall, so it is still out at the midpoint rather than
            // back at rest - which is what a single duration would have given.
            var fraction = new TraverseFraction();
            var durations = new TraverseDurations(HALF_STEP_SECONDS, DURATION_SECONDS);

            fraction.advanceTowardEnd(RISING, HALF_STEP_SECONDS, durations);

            assertThat(fraction.getEasedValue())
                .isCloseTo(1f, within(TOLERANCE));

            fraction.advanceTowardEnd(FALLING, HALF_STEP_SECONDS, durations);

            assertThat(fraction.getEasedValue())
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void advanceTowardEndLeavesASettledFractionWhereItIs() {
            // A render loop calls this unconditionally every frame, so a fraction already at its end must
            // rest there rather than creeping past it.
            var fraction = new TraverseFraction();
            fraction.advanceTowardEnd(RISING, FULL_STEP_SECONDS, DURATIONS);
            fraction.advanceTowardEnd(RISING, FULL_STEP_SECONDS, DURATIONS);

            assertThat(fraction.getEasedValue())
                .isCloseTo(1f, within(TOLERANCE));
        }
    }

    @Nested
    class HasSettledAtRest {

        @Test
        void hasSettledAtRestIsTrueForAFreshFraction() {
            assertThat(new TraverseFraction().hasSettledAtRest())
                .isTrue();
        }

        @Test
        void hasSettledAtRestIsFalseWhileStillPartWayOut() {

            var fraction = new TraverseFraction();
            fraction.advanceTowardEnd(RISING, HALF_STEP_SECONDS, DURATIONS);

            assertThat(fraction.hasSettledAtRest())
                .isFalse();
        }

        @Test
        void hasSettledAtRestIsTrueOnceTheTravelHasComeAllTheWayBack() {

            var fraction = new TraverseFraction();
            fraction.advanceTowardEnd(RISING, FULL_STEP_SECONDS, DURATIONS);
            fraction.advanceTowardEnd(FALLING, FULL_STEP_SECONDS, DURATIONS);

            assertThat(fraction.hasSettledAtRest())
                .isTrue();
        }
    }

    @Nested
    class DropToRest {

        @Test
        void dropToRestDropsAFractionLeftPartWayOutWithoutWindingItDown() {
            // Whatever was travelling has stopped showing, so there is no travel left to see: the fraction
            // has to be at rest outright rather than falling from where it stood over the next frames.
            var fraction = new TraverseFraction();
            fraction.advanceTowardEnd(RISING, HALF_STEP_SECONDS, DURATIONS);
            fraction.dropToRest();

            assertThat(fraction.getEasedValue())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(fraction.hasSettledAtRest())
                .isTrue();
        }

        @Test
        void dropToRestRisesAgainFromRestWhenWhateverItPacesComesBack() {
            // A drop must leave a reusable fraction, not a spent one - the next session's travel has to run
            // from the bottom exactly as a fresh one's does.
            var fraction = new TraverseFraction();
            fraction.advanceTowardEnd(RISING, FULL_STEP_SECONDS, DURATIONS);
            fraction.dropToRest();
            fraction.advanceTowardEnd(RISING, HALF_STEP_SECONDS, DURATIONS);

            assertThat(fraction.getEasedValue())
                .isCloseTo(0.5f, within(TOLERANCE));
        }
    }
}
