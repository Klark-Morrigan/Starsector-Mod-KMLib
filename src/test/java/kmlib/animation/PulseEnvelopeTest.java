package kmlib.animation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link PulseEnvelope}: one trigger produces a whole in-and-out cycle, the turn at the peak happens
 * without a second call, and a retrigger climbs from wherever the lift currently stands rather than dropping
 * it to nothing first. Elapsed times are expressed as fractions of one duration, keeping the arithmetic
 * independent of the concrete pace, and the expectations are the smoothstep values of the linear positions
 * those steps land on - the same curve every other animation on a surface eases along.
 */
final class PulseEnvelopeTest {

    private static final float TOLERANCE = 0.0001f;

    private static final float DURATION = 0.3f;
    private static final float FULL_DURATION = DURATION;
    private static final float HALF_DURATION = DURATION / 2f;
    private static final float QUARTER_DURATION = DURATION / 4f;

    // The same pace each way, so a step reads as a fraction of one duration whichever half of the cycle it
    // lands in. The case that pins the two halves being timed apart names its own pair.
    private static final TraverseDurations DURATIONS = TraverseDurations.createSymmetric(DURATION);

    @Nested
    class AdvanceByElapsedTime {

        @Test
        void advanceByElapsedTimeLeavesAnUntriggeredEnvelopeAtRest() {
            // A render loop pumps every envelope it holds unconditionally, so one nothing has triggered must
            // sit still rather than drift up on its own.
            var envelope = new PulseEnvelope();
            envelope.advanceByElapsedTime(FULL_DURATION, DURATIONS);

            assertThat(envelope.getPulseFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceByElapsedTimeReachesThePeakOneTraverseAfterATrigger() {

            var envelope = new PulseEnvelope();

            envelope.startPulse();
            envelope.advanceByElapsedTime(FULL_DURATION, DURATIONS);

            assertThat(envelope.getPulseFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceByElapsedTimeFallsBackToRestWithoutASecondTrigger() {
            // The whole point of an envelope over a held fraction: the caller reports the event and nothing
            // else, and the lift finds its own way back down.
            var envelope = new PulseEnvelope();

            envelope.startPulse();
            envelope.advanceByElapsedTime(FULL_DURATION, DURATIONS);
            envelope.advanceByElapsedTime(FULL_DURATION, DURATIONS);

            assertThat(envelope.getPulseFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceByElapsedTimeStandsPartWayUpHalfATraverseIn() {

            var envelope = new PulseEnvelope();

            envelope.startPulse();
            envelope.advanceByElapsedTime(HALF_DURATION, DURATIONS);

            // The halfway position eases to the smoothstep midpoint, which happens to sit on the linear
            // line - so a lift caught mid-rise reads as a real position rather than as either end.
            assertThat(envelope.getPulseFraction())
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void advanceByElapsedTimeHoldsAtThePeakForAFrameThatOverrunsTheRise() {

            var envelope = new PulseEnvelope();
            envelope.startPulse();

            // A frame long enough for both halves of the cycle spends its remainder at the peak rather than
            // carrying it into the fall, so a stalled frame cannot swallow a whole pulse unseen.
            envelope.advanceByElapsedTime(FULL_DURATION * 2f, DURATIONS);

            assertThat(envelope.getPulseFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceByElapsedTimeTimesTheFallApartFromTheRise() {
            // What the pair buys a lift: it can strike quickly and release slowly. The rise here takes half
            // a duration, so one half-step reaches the peak; the same step then spends only half of the
            // longer fall, leaving the lift at the curve's midpoint rather than back at rest.
            var envelope = new PulseEnvelope();
            var durations = new TraverseDurations(HALF_DURATION, DURATION);

            envelope.startPulse();
            envelope.advanceByElapsedTime(HALF_DURATION, durations);

            assertThat(envelope.getPulseFraction())
                .isCloseTo(1f, within(TOLERANCE));

            envelope.advanceByElapsedTime(HALF_DURATION, durations);

            assertThat(envelope.getPulseFraction())
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void advanceByElapsedTimeSnapsToThePeakInOneStepWhenDurationIsZero() {

            var envelope = new PulseEnvelope();

            envelope.startPulse();
            envelope.advanceByElapsedTime(QUARTER_DURATION, TraverseDurations.SNAP);

            assertThat(envelope.getPulseFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }
    }

    @Nested
    class GetPulseFraction {

        @Test
        void getPulseFractionStartsAtRestForAFreshEnvelope() {
            // A fresh envelope has confirmed nothing, so its first painted frame must lift nothing.
            assertThat(new PulseEnvelope().getPulseFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class HasSettled {

        @Test
        void hasSettledIsTrueForAFreshEnvelope() {
            // Nothing has run on it, so an owner minting one and finding it spent has lost nothing.
            assertThat(new PulseEnvelope().hasSettled())
                .isTrue();
        }

        @Test
        void hasSettledIsFalseForATriggeredEnvelopeStillAtRest() {
            // The half that a fraction alone cannot answer: an envelope triggered but not yet stepped stands
            // at 0 like a spent one, and an owner pruning on the reading alone would drop it before it rose.
            var envelope = new PulseEnvelope();
            envelope.startPulse();

            assertThat(envelope.hasSettled())
                .isFalse();
        }

        @Test
        void hasSettledIsFalseAtThePeak() {

            var envelope = new PulseEnvelope();

            envelope.startPulse();
            envelope.advanceByElapsedTime(FULL_DURATION, DURATIONS);

            assertThat(envelope.hasSettled())
                .isFalse();
        }

        @Test
        void hasSettledIsTrueOnceTheCycleHasRunOut() {

            var envelope = new PulseEnvelope();

            envelope.startPulse();
            envelope.advanceByElapsedTime(FULL_DURATION, DURATIONS);
            envelope.advanceByElapsedTime(FULL_DURATION, DURATIONS);

            assertThat(envelope.hasSettled())
                .isTrue();
        }
    }

    @Nested
    class StartPulse {

        @Test
        void startPulseAimsAFreshEnvelopeAtItsPeak() {

            var envelope = new PulseEnvelope();

            envelope.startPulse();
            envelope.advanceByElapsedTime(QUARTER_DURATION, DURATIONS);

            // A quarter of the way up, the eased value trails the linear 0.25 - the slow, accelerating start
            // a lift shares with every other motion on the surface.
            assertThat(envelope.getPulseFraction())
                .isCloseTo(0.15625f, within(TOLERANCE));
        }

        @Test
        void startPulseClimbsAgainFromWhereAFallingLiftStands() {

            var envelope = new PulseEnvelope();
            
            envelope.startPulse();
            envelope.advanceByElapsedTime(FULL_DURATION, DURATIONS);
            envelope.advanceByElapsedTime(HALF_DURATION, DURATIONS);

            // Retriggered halfway down and stepped a quarter, the lift stands three quarters up: it climbed
            // on from where it was rather than dropping to nothing and rebuilding, which the player would
            // see as a dip in answer to a second click.
            envelope.startPulse();
            envelope.advanceByElapsedTime(QUARTER_DURATION, DURATIONS);

            assertThat(envelope.getPulseFraction())
                .isCloseTo(0.84375f, within(TOLERANCE));
        }

        @Test
        void startPulseCannotDriveALiftPastItsPeak() {
            // A repeated trigger restarts the curve rather than summing onto it, so a tab clicked twice in
            // a frame is no brighter than one clicked once.
            var envelope = new PulseEnvelope();
            
            envelope.startPulse();
            envelope.startPulse();
            envelope.advanceByElapsedTime(FULL_DURATION, DURATIONS);

            assertThat(envelope.getPulseFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }
    }
}
