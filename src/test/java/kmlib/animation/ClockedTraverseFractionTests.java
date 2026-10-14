package kmlib.animation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link ClockedTraverseFraction}: a traverse with no turn recorded rests at the end it is read toward,
 * a turn rides the traverse off the clock and settles at the far end, each way travels at its own pace, a
 * pace of no time at all is a cut, the pace is read at every reading, and a reversal sets off from the linear
 * position the turn caught it at.
 *
 * <p>The clock and the pace are injected because a traverse derived from elapsed real time is only checkable
 * at instants a case can name.
 *
 * <p>Each expected value is stated as the eased one, with the linear position it comes off named beside it.
 * The two are worth keeping apart in the reading: the linear position is what a turn records and what a
 * reversal sets off from, while the eased one is all a holder ever sees.
 */
final class ClockedTraverseFractionTests {

    // A pace whose quarters and eighths land on clean instants below.
    private static final TraverseDurations SYMMETRIC_DURATIONS = TraverseDurations.createSymmetric(0.4f);

    // A way back twice as fast as the way out, so a reading taken at the wrong way's pace lands elsewhere.
    private static final TraverseDurations FAST_FALL_DURATIONS = new TraverseDurations(0.4f, 0.2f);

    // How close an eased reading has to land. Loose enough to absorb the float arithmetic behind the curve,
    // tight enough that no two positions in these cases could be confused for each other.
    private static final float TOLERANCE = 1e-6f;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private static final boolean RISING = true;
    private static final boolean FALLING = false;

    private final NanoClockFake nanoClockFake = new NanoClockFake();

    // The injected pace, which cases wind to nothing or change mid-traverse.
    private TraverseDurations durations = SYMMETRIC_DURATIONS;

    private final ClockedTraverseFraction fraction =
        new ClockedTraverseFraction(nanoClockFake, () -> durations);

    @Nested
    class HasSettledAtRest {

        @Test
        void holdsWithNoTurnRecordedReadFalling() {

            assertThat(fraction.hasSettledAtRest(FALLING))
                .isTrue();
        }

        @Test
        void failsWithNoTurnRecordedReadRising() {

            assertThat(fraction.hasSettledAtRest(RISING))
                .isFalse();
        }

        @Test
        void failsPartWayBack() {

            fraction.turnTowardEnd(FALLING);
            nanoClockFake.advanceBySeconds(0.1);

            assertThat(fraction.hasSettledAtRest(FALLING))
                .isFalse();
        }

        @Test
        void holdsOnceTheWayBackIsPast() {
            // Exact rather than near, which is what lets a holder drop a settled traverse on the frame it
            // arrives rather than one some tolerance later.
            fraction.turnTowardEnd(FALLING);
            nanoClockFake.advanceBySeconds(0.4);

            assertThat(fraction.hasSettledAtRest(FALLING))
                .isTrue();
        }
    }

    @Nested
    class ResolveEasedValue {

        @Test
        void restsAtTheFarEndWithNoTurnRecorded() {

            assertThat(fraction.resolveEasedValue(RISING))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void restsAtRestWithNoTurnRecorded() {
            // A holder starting at that end reads it on its first frame rather than travelling to it.
            assertThat(fraction.resolveEasedValue(FALLING))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void settlesAtRestOnceTheWayBackIsPast() {

            fraction.turnTowardEnd(FALLING);
            nanoClockFake.advanceBySeconds(0.4);

            assertThat(fraction.resolveEasedValue(FALLING))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void ridesOutAfterTurningRising() {

            fraction.turnTowardEnd(RISING);
            nanoClockFake.advanceBySeconds(0.1);

            // Linearly 0.25, eased to 0.15625 - the same flat start the way back has, mirrored.
            assertThat(fraction.resolveEasedValue(RISING))
                .isCloseTo(0.15625f, within(TOLERANCE));
        }

        @Test
        void settlesAtTheFarEndOnceTheWayOutIsPast() {
            // The far end of the traverse, which is the bound one left running would climb past: a holder
            // multiplying by it would brighten beyond what it paints at rest.
            fraction.turnTowardEnd(RISING);
            nanoClockFake.advanceBySeconds(4);

            assertThat(fraction.resolveEasedValue(RISING))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void travelsTheWayBackAtItsOwnPace() {

            durations = FAST_FALL_DURATIONS;
            fraction.turnTowardEnd(FALLING);
            nanoClockFake.advanceBySeconds(0.1);

            // Half of the faster way back, so linearly 0.5 - which the curve leaves where it is. The way out's
            // pace would read 0.84375 here.
            assertThat(fraction.resolveEasedValue(FALLING))
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void cutsStraightToTheEndWithAPaceOfNoTimeAtAll() {

            durations = TraverseDurations.SNAP;
            fraction.turnTowardEnd(FALLING);

            // Settled on the frame of the turn rather than dividing by a pace wound to nothing.
            assertThat(fraction.resolveEasedValue(FALLING))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void answersAPaceChangedPartWay() {

            fraction.turnTowardEnd(FALLING);
            nanoClockFake.advanceBySeconds(0.1);

            durations = TraverseDurations.createSymmetric(0.2f);

            // Half of the shorter traverse has passed, so linearly 0.5 - which the curve leaves where it is.
            assertThat(fraction.resolveEasedValue(FALLING))
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void travelsTowardTheEndItIsReadTowardRatherThanTheOneItWasTurnedTo() {
            // The end is asked at every reading, so a holder whose own state moved without a turn is
            // answered by that state, travelling from where the last turn set off.
            fraction.turnTowardEnd(FALLING);
            nanoClockFake.advanceBySeconds(0.2);
            fraction.turnTowardEnd(RISING);
            nanoClockFake.advanceBySeconds(0.1);

            // The turn set off from a linear 0.5; read back toward rest a quarter of a traverse later, that
            // is 0.25 linearly and 0.15625 eased, where the turned-to end would read 0.84375.
            assertThat(fraction.resolveEasedValue(FALLING))
                .isCloseTo(0.15625f, within(TOLERANCE));
        }
    }

    @Nested
    class TurnTowardEnd {

        @Test
        void setsOffFromTheOtherEndWhenNothingHasTurnedYet() {

            fraction.turnTowardEnd(FALLING);
            nanoClockFake.advanceBySeconds(0.1);

            // A quarter of the way down from the far end, so linearly 0.75 - eased, still 0.84, the curve
            // being at its flattest where it leaves an end.
            assertThat(fraction.resolveEasedValue(FALLING))
                .isCloseTo(0.84375f, within(TOLERANCE));
        }

        @Test
        void reversesFromTheLinearProgressTheTurnCaughtItAt() {

            fraction.turnTowardEnd(FALLING);
            nanoClockFake.advanceBySeconds(0.1);

            fraction.turnTowardEnd(RISING);
            nanoClockFake.advanceBySeconds(0.05);

            // Set off again from the 0.75 the way back had linearly reached and an eighth further out, so
            // 0.875 linearly and 0.957 eased. Recording the eased 0.84 instead would read 0.997 here, a
            // restart would read 0.043, and a resume from the far end would already be settled at 1.
            assertThat(fraction.resolveEasedValue(RISING))
                .isCloseTo(0.95703125f, within(TOLERANCE));
        }

        @Test
        void recordsWhereTheTurnCaughtItAtThePaceItWasTravellingAt() {
            // The way back covers a quarter in 0.05s at its faster pace; the way out then covers an eighth in
            // 0.05s at its own. Recording the turn at the way out's pace would set off from 0.875 instead.
            durations = FAST_FALL_DURATIONS;

            fraction.turnTowardEnd(FALLING);
            nanoClockFake.advanceBySeconds(0.05);

            fraction.turnTowardEnd(RISING);
            nanoClockFake.advanceBySeconds(0.05);

            // From a linear 0.75, an eighth further out: 0.875 linearly and 0.957 eased.
            assertThat(fraction.resolveEasedValue(RISING))
                .isCloseTo(0.95703125f, within(TOLERANCE));
        }
    }

    // A hand-cranked stand-in for the JVM's monotonic clock, so the traverse is read at an instant the case
    // chooses rather than at whatever the machine reads while the suite runs.
    private static final class NanoClockFake implements LongSupplier {

        private long nanos;

        @Override
        public long getAsLong() {
            return nanos;
        }

        private void advanceBySeconds(double seconds) {
            nanos += Math.round(seconds * NANOS_PER_SECOND);
        }
    }
}
