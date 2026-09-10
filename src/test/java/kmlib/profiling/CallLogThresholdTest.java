package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two ends and the middle of what a section can state about its own log lines: never,
 * always, and over a duration - all three answered by one comparison, so a close on a path that
 * states nothing pays for nothing.
 */
final class CallLogThresholdTest {

    private static final long ONE_NANOSECOND = 1L;
    private static final long ONE_MILLISECOND_IN_NANOS = 1_000_000L;
    private static final long TWO_MILLISECONDS_IN_NANOS = 2_000_000L;
    private static final long NO_TIME_AT_ALL = 0L;

    private static final double TWO_MILLISECONDS = 2.0;

    @Nested
    class ShouldLogCall {

        @Test
        void keepsTheFastestCallQuietWhereNothingWasStated() {

            assertThat(CallLogThreshold.NO_LOGGING.shouldLogCall(Long.MAX_VALUE))
                .isFalse();
        }

        @Test
        void reportsACallThatTookNoTimeWhereEveryCallIsWanted() {
            // A rebuild step runs when something changed rather than on a clock, so a fast call
            // is as much a part of the trace through the log as a slow one.
            assertThat(CallLogThreshold.LOGGING_EVERY_CALL.shouldLogCall(NO_TIME_AT_ALL))
                .isTrue();
        }

        @Test
        void reportsACallOverTheThreshold() {

            assertThat(CallLogThreshold
                    .loggingOverMillis(TWO_MILLISECONDS)
                    .shouldLogCall(TWO_MILLISECONDS_IN_NANOS + ONE_NANOSECOND))
                .isTrue();
        }

        @Test
        void keepsACallUnderTheThresholdQuiet() {

            assertThat(CallLogThreshold
                    .loggingOverMillis(TWO_MILLISECONDS)
                    .shouldLogCall(ONE_MILLISECOND_IN_NANOS))
                .isFalse();
        }

        @Test
        void keepsACallOfExactlyTheThresholdQuiet() {
            // Over, not at: the bound is what a call may take without being worth saying, so the
            // call that took exactly it kept the rule.
            assertThat(CallLogThreshold
                    .loggingOverMillis(TWO_MILLISECONDS)
                    .shouldLogCall(TWO_MILLISECONDS_IN_NANOS))
                .isFalse();
        }
    }

    @Nested
    class CanLogAnyCall {

        @Test
        void canLogAnyCallIsFalseWhereNothingWasStated() {
            // The one answer that marks a section's calls as per-frame cost rather than events,
            // which is what keeps the compilation clock off its path.
            assertThat(CallLogThreshold.NO_LOGGING.canLogAnyCall())
                .isFalse();
        }

        @Test
        void canLogAnyCallIsTrueWhereEveryCallIsWanted() {

            assertThat(CallLogThreshold.LOGGING_EVERY_CALL.canLogAnyCall())
                .isTrue();
        }

        @Test
        void canLogAnyCallIsTrueOverADuration() {
            // A threshold a call may or may not reach still names the section's calls as events:
            // the ones that reach it are the ones a reader has to judge, cold or not.
            assertThat(CallLogThreshold.loggingOverMillis(TWO_MILLISECONDS).canLogAnyCall())
                .isTrue();
        }
    }
}
