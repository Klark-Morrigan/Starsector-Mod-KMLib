package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what a timing derives rather than receives: the mean per call, and that a row no call has
 * finished under yet - a section still running when the snapshot was taken - reports no mean rather
 * than dividing by nothing.
 */
final class ProfileTimingTest {

    private static final long TOTAL_NANOS = 250L;

    @Nested
    class GetAverageNanos {

        @Test
        void reportsTheTotalSpreadOverTheCallsThatMadeIt() {

            assertThat(new ProfileTiming(2, TOTAL_NANOS, 50L, 200L, DurationBuckets.NO_CALLS)
                .getAverageNanos())
                .isEqualTo(125L);
        }

        @Test
        void reportsNoMeanWhereNoCallHasFinished() {
            // A readout asked for mid-frame holds the section that is running, and its row has a
            // count of nothing to divide by.
            assertThat(new ProfileTiming(0, 0, 0, 0, DurationBuckets.NO_CALLS).getAverageNanos())
                .isEqualTo(0L);
        }
    }
}
