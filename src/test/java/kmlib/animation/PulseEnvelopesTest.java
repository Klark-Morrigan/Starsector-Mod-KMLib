package kmlib.animation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link PulseEnvelopes}: a trigger reaches one key and leaves the rest alone, several keys can be
 * running at once, and a key whose cycle has run out reads at rest and can be triggered again. The set is
 * keyed by row index here, the identity a row that keeps its order has.
 */
final class PulseEnvelopesTest {

    private static final float TOLERANCE = 0.0001f;

    private static final float DURATION = 0.3f;
    private static final float FULL_DURATION = DURATION;
    private static final float HALF_DURATION = DURATION / 2f;

    private static final int FIRST_KEY = 0;
    private static final int SECOND_KEY = 1;

    // A key nothing has been triggered for, so a set answering for it proves the fallback rather than a
    // leftover entry.
    private static final int UNTOUCHED_KEY = 7;

    @Nested
    class AdvanceByElapsedTime {

        @Test
        void advanceByElapsedTimeRaisesEveryRunningPulseByTheSameFrame() {
            // Two events landing on two elements run side by side: a pulse is an event on one element, so
            // one starting says nothing about the others - unlike a hover, which one element holds at a time.
            var pulses = new PulseEnvelopes<Integer>();

            pulses.startPulseAt(FIRST_KEY);
            pulses.startPulseAt(SECOND_KEY);
            pulses.advanceByElapsedTime(HALF_DURATION, DURATION);

            assertThat(pulses.resolvePulseFractionAt(FIRST_KEY))
                .isCloseTo(0.5f, within(TOLERANCE));
            assertThat(pulses.resolvePulseFractionAt(SECOND_KEY))
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void advanceByElapsedTimeRunsAPulseAllTheWayOutWithoutASecondTrigger() {

            var pulses = new PulseEnvelopes<Integer>();

            pulses.startPulseAt(FIRST_KEY);
            pulses.advanceByElapsedTime(FULL_DURATION, DURATION);
            pulses.advanceByElapsedTime(FULL_DURATION, DURATION);

            assertThat(pulses.resolvePulseFractionAt(FIRST_KEY))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceByElapsedTimeLeavesASpentKeyTriggerableAgain() {
            // A spent envelope is dropped from the set, so this is what proves the drop costs nothing: the
            // same key triggered afterwards rises exactly as it did the first time.
            var pulses = new PulseEnvelopes<Integer>();

            pulses.startPulseAt(FIRST_KEY);
            pulses.advanceByElapsedTime(FULL_DURATION, DURATION);
            pulses.advanceByElapsedTime(FULL_DURATION, DURATION);

            pulses.startPulseAt(FIRST_KEY);
            pulses.advanceByElapsedTime(HALF_DURATION, DURATION);

            assertThat(pulses.resolvePulseFractionAt(FIRST_KEY))
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void advanceByElapsedTimeOnAnEmptySetLeavesEveryKeyAtRest() {
            // A render loop pumps the set every frame whether anything is running or not.
            var pulses = new PulseEnvelopes<Integer>();
            pulses.advanceByElapsedTime(FULL_DURATION, DURATION);

            assertThat(pulses.resolvePulseFractionAt(FIRST_KEY))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ResetPulses {

        @Test
        void resetPulsesDropsALiftLeftPartWayThroughItsCycle() {
            // Otherwise the next session opens decaying from a peak the player never saw rise.
            var pulses = new PulseEnvelopes<Integer>();

            pulses.startPulseAt(FIRST_KEY);
            pulses.advanceByElapsedTime(HALF_DURATION, DURATION);
            pulses.resetPulses();

            assertThat(pulses.resolvePulseFractionAt(FIRST_KEY))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ResolvePulseFractionAt {

        @Test
        void resolvePulseFractionAtReadsAtRestForAKeyWithNoPulse() {
            assertThat(new PulseEnvelopes<Integer>().resolvePulseFractionAt(UNTOUCHED_KEY))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class StartPulseAt {

        @Test
        void startPulseAtLiftsTheNamedKeyAndNoOther() {

            var pulses = new PulseEnvelopes<Integer>();

            pulses.startPulseAt(FIRST_KEY);
            pulses.advanceByElapsedTime(FULL_DURATION, DURATION);

            assertThat(pulses.resolvePulseFractionAt(FIRST_KEY))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(pulses.resolvePulseFractionAt(SECOND_KEY))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void startPulseAtRetriggersInPlaceRatherThanStackingASecondLift() {
            // Retriggered halfway down and stepped a quarter of a traverse, the lift stands three quarters
            // up - one envelope climbing again, not two summing past the peak.
            var pulses = new PulseEnvelopes<Integer>();
            
            pulses.startPulseAt(FIRST_KEY);
            pulses.advanceByElapsedTime(FULL_DURATION, DURATION);
            pulses.advanceByElapsedTime(HALF_DURATION, DURATION);

            pulses.startPulseAt(FIRST_KEY);
            pulses.advanceByElapsedTime(DURATION / 4f, DURATION);

            assertThat(pulses.resolvePulseFractionAt(FIRST_KEY))
                .isCloseTo(0.84375f, within(TOLERANCE));
        }
    }
}
