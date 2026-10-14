package kmlib.animation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link ClockedEasedFraction}: a ramp with no turn recorded rests at the end it is read toward, a turn
 * rides the ramp off the clock and settles at the far end, a ramp of no time at all is a cut, its length is
 * read at every reading, and a reversal sets off from the linear position the turn caught it at.
 *
 * <p>The clock and the pace are injected because a ramp derived from elapsed real time is only checkable at
 * instants a case can name.
 *
 * <p>Each expected value is stated as the eased one, with the linear position it comes off named beside it.
 * The two are worth keeping apart in the reading: the linear position is what a turn records and what a
 * reversal sets off from, while the eased one is all a holder ever sees.
 */
final class ClockedEasedFractionTests {

    // A pace whose quarters and eighths land on clean instants below.
    private static final double RAMP_SECONDS = 0.4;

    // How close an eased reading has to land. Loose enough to absorb the float arithmetic behind the curve,
    // tight enough that no two positions in these cases could be confused for each other.
    private static final float TOLERANCE = 1e-6f;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private static final boolean TOWARD_FULL = true;
    private static final boolean TOWARD_REST = false;

    private final NanoClockFake nanoClockFake = new NanoClockFake();

    // The injected pace, which cases wind to nothing or change mid-ramp.
    private double rampSeconds = RAMP_SECONDS;

    private final ClockedEasedFraction fraction =
        new ClockedEasedFraction(nanoClockFake, () -> rampSeconds);

    @Nested
    class HeadToward {

        @Test
        void setsOffFromTheOtherEndWhenNothingHasTurnedYet() {

            fraction.headToward(TOWARD_REST);
            nanoClockFake.advanceBySeconds(0.1);

            // A quarter of the way down from full, so linearly 0.75 - eased, still 0.84, the curve being at
            // its flattest where it leaves an end.
            assertThat(fraction.resolveEasedValueToward(TOWARD_REST))
                .isCloseTo(0.84375f, within(TOLERANCE));
        }

        @Test
        void reversesFromTheLinearProgressTheTurnCaughtItAt() {

            fraction.headToward(TOWARD_REST);
            nanoClockFake.advanceBySeconds(0.1);

            fraction.headToward(TOWARD_FULL);
            nanoClockFake.advanceBySeconds(0.05);

            // Set off again from the 0.75 the way down had linearly reached and an eighth further up, so
            // 0.875 linearly and 0.957 eased. Recording the eased 0.84 instead would read 0.997 here, a
            // restart would read 0.043, and a resume from the far end would already be settled at 1.
            assertThat(fraction.resolveEasedValueToward(TOWARD_FULL))
                .isCloseTo(0.95703125f, within(TOLERANCE));
        }
    }

    @Nested
    class ResolveEasedValueToward {

        @Test
        void restsAtFullWithNoTurnRecorded() {

            assertThat(fraction.resolveEasedValueToward(TOWARD_FULL))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void restsAtRestWithNoTurnRecorded() {
            // A holder starting at that end reads it on its first frame rather than travelling to it.
            assertThat(fraction.resolveEasedValueToward(TOWARD_REST))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void settlesAtRestOnceTheRampIsPast() {

            fraction.headToward(TOWARD_REST);
            nanoClockFake.advanceBySeconds(0.4);

            assertThat(fraction.resolveEasedValueToward(TOWARD_REST))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void ridesUpTheRampAfterTurningTowardFull() {

            fraction.headToward(TOWARD_FULL);
            nanoClockFake.advanceBySeconds(0.1);

            // Linearly 0.25, eased to 0.15625 - the same flat start the way down has, mirrored.
            assertThat(fraction.resolveEasedValueToward(TOWARD_FULL))
                .isCloseTo(0.15625f, within(TOLERANCE));
        }

        @Test
        void settlesAtFullOnceTheRampIsPast() {
            // The far end of the ramp, which is the bound a ramp left running would climb past: a holder
            // multiplying by it would brighten beyond what it paints at rest.
            fraction.headToward(TOWARD_FULL);
            nanoClockFake.advanceBySeconds(4);

            assertThat(fraction.resolveEasedValueToward(TOWARD_FULL))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void cutsStraightToTheEndWithARampOfNoTimeAtAll() {

            rampSeconds = 0;
            fraction.headToward(TOWARD_REST);

            // Settled on the frame of the turn rather than dividing by a pace wound to nothing.
            assertThat(fraction.resolveEasedValueToward(TOWARD_REST))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void answersARampLengthChangedPartWay() {

            fraction.headToward(TOWARD_REST);
            nanoClockFake.advanceBySeconds(0.1);

            rampSeconds = 0.2;

            // Half of the shorter ramp has passed, so linearly 0.5 - which the curve leaves where it is.
            assertThat(fraction.resolveEasedValueToward(TOWARD_REST))
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void travelsTowardTheEndItIsReadTowardRatherThanTheOneItWasTurnedTo() {
            // The end is asked at every reading, so a holder whose own state moved without a turn is
            // answered by that state, travelling from where the last turn set off.
            fraction.headToward(TOWARD_REST);
            nanoClockFake.advanceBySeconds(0.2);
            fraction.headToward(TOWARD_FULL);
            nanoClockFake.advanceBySeconds(0.1);

            // The turn set off from a linear 0.5; read back toward rest a quarter of a ramp later, that is
            // 0.25 linearly and 0.15625 eased, where the turned-to end would read 0.84375.
            assertThat(fraction.resolveEasedValueToward(TOWARD_REST))
                .isCloseTo(0.15625f, within(TOLERANCE));
        }
    }

    // A hand-cranked stand-in for the JVM's monotonic clock, so the ramp is read at an instant the case
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
