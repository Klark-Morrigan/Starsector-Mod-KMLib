package kmlib.animation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link PulsePhaseClock}: elapsed time counted from the moment the clock was made whatever the source's
 * own origin, a phase that runs the cycle and wraps at every turn, a period of nothing that stands still
 * instead of dividing by it, and a subject's phase shifted along the same cycle by its own share of a turn.
 * The clock is driven off a hand-cranked source, so every case asks at an exact instant rather than at
 * whenever the suite happens to run.
 */
final class PulsePhaseClockTest {

    private static final float TOLERANCE = 0.000001f;
    private static final double SECONDS_TOLERANCE = 0.000001;

    // A period long enough that the instants below land on clean fractions of it, which keeps the expected
    // phases readable as quarters of a turn.
    private static final float PERIOD_SECONDS = 4f;

    // Two emitters named the way a caller names them, whose shares of a turn are pinned by the derivation's
    // own suite; here they only have to be different from each other.
    private static final String SUBJECT_ID = "beacon_1";
    private static final String OTHER_SUBJECT_ID = "beacon_2";

    // Where the subject stands a quarter and three quarters of the way through the turn, its share of the
    // turn included - the second having carried past the turn's end and wrapped.
    private static final float SUBJECT_PHASE_AT_A_QUARTER = 0.9339345f;
    private static final float SUBJECT_PHASE_AT_THREE_QUARTERS = 0.4339345f;

    // A source origin far from zero, so a clock that failed to subtract its start would be caught reporting
    // days of elapsed time instead of seconds.
    private static final long DISTANT_SOURCE_ORIGIN_NANOS = 7L * 24L * 60L * 60L * 1_000_000_000L;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    @Nested
    class ReadElapsedSeconds {

        @Test
        void readElapsedSecondsReportsTheTimeSinceTheClockWasMade() {

            var nanoClockFake = new NanoClockFake(0L);
            var clock = new PulsePhaseClock(nanoClockFake);

            nanoClockFake.advanceBySeconds(2.5);

            assertThat(clock.readElapsedSeconds())
                .isCloseTo(2.5, within(SECONDS_TOLERANCE));
        }

        @Test
        void readElapsedSecondsIgnoresWhereTheSourceStarted() {
            // A monotonic clock's origin is arbitrary, so elapsed time has to be measured against the reading
            // the clock was made at rather than against zero.
            var nanoClockFake = new NanoClockFake(DISTANT_SOURCE_ORIGIN_NANOS);
            var clock = new PulsePhaseClock(nanoClockFake);

            nanoClockFake.advanceBySeconds(1.0);

            assertThat(clock.readElapsedSeconds())
                .isCloseTo(1.0, within(SECONDS_TOLERANCE));
        }
    }

    @Nested
    class ResolvePhase {

        @Test
        void resolvePhaseRunsFromTheStartOfTheCycleToItsEnd() {

            var nanoClockFake = new NanoClockFake(0L);
            var clock = new PulsePhaseClock(nanoClockFake);

            assertThat(clock.resolvePhase(PERIOD_SECONDS))
                .isCloseTo(0f, within(TOLERANCE));

            nanoClockFake.advanceBySeconds(1.0);

            assertThat(clock.resolvePhase(PERIOD_SECONDS))
                .isCloseTo(0.25f, within(TOLERANCE));

            nanoClockFake.advanceBySeconds(2.0);

            assertThat(clock.resolvePhase(PERIOD_SECONDS))
                .isCloseTo(0.75f, within(TOLERANCE));
        }

        @Test
        void resolvePhaseWrapsAtEveryTurn() {
            // Two and a half turns in, the phase reads the same as it did half a turn in: what makes the
            // reading a place in a cycle rather than a count of how long the clock has run.
            var nanoClockFake = new NanoClockFake(0L);
            var clock = new PulsePhaseClock(nanoClockFake);

            nanoClockFake.advanceBySeconds(10.0);

            assertThat(clock.resolvePhase(PERIOD_SECONDS))
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void resolvePhaseStandsStillForAPeriodOfNothing() {
            // A knob wound down to nothing, and a negative one however it got there: both hold the animation
            // at the start of its cycle rather than dividing by a turn that takes no time.
            var nanoClockFake = new NanoClockFake(0L);
            var clock = new PulsePhaseClock(nanoClockFake);

            nanoClockFake.advanceBySeconds(3.0);

            assertThat(clock.resolvePhase(0f))
                .isZero();
            assertThat(clock.resolvePhase(-2f))
                .isZero();
        }
    }

    @Nested
    class ResolvePhaseForSubject {

        @Test
        void resolvePhaseForSubjectMovesTheSubjectAlongItsOwnShareOfTheTurn() {

            var nanoClockFake = new NanoClockFake(0L);
            var clock = new PulsePhaseClock(nanoClockFake);

            nanoClockFake.advanceBySeconds(1.0);

            assertThat(clock.resolvePhaseForSubject(PERIOD_SECONDS, SUBJECT_ID))
                .isCloseTo(SUBJECT_PHASE_AT_A_QUARTER, within(TOLERANCE));
        }

        @Test
        void resolvePhaseForSubjectWrapsWhenTheShareCarriesPastTheTurn() {
            // The shift has to fold back into the same cycle: a subject whose share pushes it past the end
            // reappears at the start rather than reading past a whole turn.
            var nanoClockFake = new NanoClockFake(0L);
            var clock = new PulsePhaseClock(nanoClockFake);

            nanoClockFake.advanceBySeconds(3.0);

            assertThat(clock.resolvePhaseForSubject(PERIOD_SECONDS, SUBJECT_ID))
                .isCloseTo(SUBJECT_PHASE_AT_THREE_QUARTERS, within(TOLERANCE));
        }

        @Test
        void resolvePhaseForSubjectSeparatesTwoSubjectsInTheSameCycle() {
            // The reason the subject reading exists: emitters sharing one clock must not peak together, or
            // the set reads as one thing happening rather than as several sources.
            var nanoClockFake = new NanoClockFake(0L);
            var clock = new PulsePhaseClock(nanoClockFake);

            nanoClockFake.advanceBySeconds(1.0);

            assertThat(clock.resolvePhaseForSubject(PERIOD_SECONDS, SUBJECT_ID))
                .isNotEqualTo(clock.resolvePhaseForSubject(PERIOD_SECONDS, OTHER_SUBJECT_ID));
        }

        @Test
        void resolvePhaseForSubjectStandsStillForAPeriodOfNothing() {
            // The still reading wins over the shift, so a stopped animation is stopped for every subject
            // rather than frozen at a different point per subject.
            var nanoClockFake = new NanoClockFake(0L);
            var clock = new PulsePhaseClock(nanoClockFake);

            nanoClockFake.advanceBySeconds(3.0);

            assertThat(clock.resolvePhaseForSubject(0f, SUBJECT_ID))
                .isZero();
        }
    }

    // A hand-cranked stand-in for the JVM's monotonic clock, so a phase is asked at an instant the case
    // chooses rather than at whatever the machine reads while the suite runs.
    private static final class NanoClockFake implements LongSupplier {

        private long nanos;

        private NanoClockFake(long startingNanos) {
            this.nanos = startingNanos;
        }

        @Override
        public long getAsLong() {
            return nanos;
        }

        private void advanceBySeconds(double seconds) {
            nanos += Math.round(seconds * NANOS_PER_SECOND);
        }
    }
}
