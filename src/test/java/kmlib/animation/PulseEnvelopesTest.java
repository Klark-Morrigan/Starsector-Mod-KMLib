package kmlib.animation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link PulseEnvelopes}: a trigger reaches one key and leaves the rest alone, several keys can be
 * running at once, and a key whose cycle has run out reads at rest and can be triggered again. A held
 * trigger is aimed the same way and the release is not, that asymmetry being the whole of why the two are
 * shaped differently. The set is keyed by row index here, the identity a row that keeps its order has.
 */
final class PulseEnvelopesTest {

    private static final float TOLERANCE = 0.0001f;

    private static final float DURATION = 0.3f;
    private static final float FULL_DURATION = DURATION;
    private static final float HALF_DURATION = DURATION / 2f;

    // The same pace each way, so a step reads as a fraction of one duration. A set hands the pair to each
    // envelope unchanged and every envelope keeps its own direction, so the two halves being timed apart is
    // the lone envelope's case rather than the set's.
    private static final TraverseDurations DURATIONS = TraverseDurations.createSymmetric(DURATION);

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
            pulses.advanceByElapsedTime(HALF_DURATION, DURATIONS);

            assertThat(pulses.resolvePulseFractionAt(FIRST_KEY))
                .isCloseTo(0.5f, within(TOLERANCE));
            assertThat(pulses.resolvePulseFractionAt(SECOND_KEY))
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void advanceByElapsedTimeRunsAPulseAllTheWayOutWithoutASecondTrigger() {

            var pulses = new PulseEnvelopes<Integer>();

            pulses.startPulseAt(FIRST_KEY);
            pulses.advanceByElapsedTime(FULL_DURATION, DURATIONS);
            pulses.advanceByElapsedTime(FULL_DURATION, DURATIONS);

            assertThat(pulses.resolvePulseFractionAt(FIRST_KEY))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceByElapsedTimeLeavesASpentKeyTriggerableAgain() {
            // A spent envelope is dropped from the set, so this is what proves the drop costs nothing: the
            // same key triggered afterwards rises exactly as it did the first time.
            var pulses = new PulseEnvelopes<Integer>();

            pulses.startPulseAt(FIRST_KEY);
            pulses.advanceByElapsedTime(FULL_DURATION, DURATIONS);
            pulses.advanceByElapsedTime(FULL_DURATION, DURATIONS);

            pulses.startPulseAt(FIRST_KEY);
            pulses.advanceByElapsedTime(HALF_DURATION, DURATIONS);

            assertThat(pulses.resolvePulseFractionAt(FIRST_KEY))
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void advanceByElapsedTimeOnAnEmptySetLeavesEveryKeyAtRest() {
            // A render loop pumps the set every frame whether anything is running or not.
            var pulses = new PulseEnvelopes<Integer>();
            pulses.advanceByElapsedTime(FULL_DURATION, DURATIONS);

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
            pulses.advanceByElapsedTime(HALF_DURATION, DURATIONS);
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
            pulses.advanceByElapsedTime(FULL_DURATION, DURATIONS);

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
            pulses.advanceByElapsedTime(FULL_DURATION, DURATIONS);
            pulses.advanceByElapsedTime(HALF_DURATION, DURATIONS);

            pulses.startPulseAt(FIRST_KEY);
            pulses.advanceByElapsedTime(DURATION / 4f, DURATIONS);

            assertThat(pulses.resolvePulseFractionAt(FIRST_KEY))
                .isCloseTo(0.84375f, within(TOLERANCE));
        }
    }

    @Nested
    class StartHeldPulseAt {

        @Test
        void startHeldPulseAtHoldsOnlyTheNamedKeyAtItsPeak() {
            // Aimed exactly as a plain trigger is - one element's act says nothing about the others - and
            // the hold is what parts them: the named key waits at the top while a neighbour's self-timed
            // lift has already run its course.
            var pulses = new PulseEnvelopes<Integer>();

            pulses.startHeldPulseAt(FIRST_KEY);
            pulses.startPulseAt(SECOND_KEY);
            pulses.advanceByElapsedTime(FULL_DURATION, DURATIONS);
            pulses.advanceByElapsedTime(FULL_DURATION, DURATIONS);

            assertThat(pulses.resolvePulseFractionAt(FIRST_KEY))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(pulses.resolvePulseFractionAt(SECOND_KEY))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ReleaseHeldPulses {

        @Test
        void releaseHeldPulsesEndsAHoldWhicheverKeyItIsOn() {
            // Unaimed, unlike the trigger: a pointer put down on one element is routinely lifted somewhere
            // else entirely, and the act it ends is still that element's. Released by where the pointer
            // finished, a lift the player dragged away from would stand at its peak indefinitely.
            var pulses = new PulseEnvelopes<Integer>();

            pulses.startHeldPulseAt(FIRST_KEY);
            pulses.advanceByElapsedTime(FULL_DURATION, DURATIONS);

            pulses.releaseHeldPulses();
            pulses.advanceByElapsedTime(FULL_DURATION, DURATIONS);
            pulses.advanceByElapsedTime(FULL_DURATION, DURATIONS);

            assertThat(pulses.resolvePulseFractionAt(FIRST_KEY))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void releaseHeldPulsesLeavesASetWithNoHoldsAlone() {
            // Every release is reported, most of them owed to nothing, so one arriving over a set of
            // self-timed lifts must not disturb their cycles.
            var pulses = new PulseEnvelopes<Integer>();

            pulses.startPulseAt(FIRST_KEY);
            pulses.releaseHeldPulses();
            pulses.advanceByElapsedTime(FULL_DURATION, DURATIONS);

            assertThat(pulses.resolvePulseFractionAt(FIRST_KEY))
                .isCloseTo(1f, within(TOLERANCE));
        }
    }
}
