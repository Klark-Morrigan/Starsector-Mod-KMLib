package kmlib.starsector.ui.map.icons;

import kmlib.starsector.ui.map.icons.MapIconReseatDecision.ReseatAction;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what the reading log keeps for each of its two lines: the latest readings up to their capacity,
 * and one lift's readings from its removal on, whatever came before it.
 */
class ReseatReadingLogTests {

    private static final ReseatReading CLEAR_READING =
        new ReseatReading(1, ReseatObservation.ICON_CLEAR, ReseatAction.NONE);

    private static final ReseatReading FIRST_REMOVAL =
        new ReseatReading(2, ReseatObservation.ICON_BURIED, ReseatAction.REMOVE);

    private static final ReseatReading FIRST_PUT_BACK =
        new ReseatReading(3, ReseatObservation.ICON_DROPPED, ReseatAction.ADD);

    private static final ReseatReading SECOND_REMOVAL =
        new ReseatReading(4, ReseatObservation.ICON_BURIED, ReseatAction.REMOVE);

    @Nested
    class DescribeLatestLiftReadings {

        @Test
        void describesNothingBeforeAnyLift() {

            var readingLog = new ReseatReadingLog();
            readingLog.recordReading(CLEAR_READING);

            assertThat(readingLog.describeLatestLiftReadings())
                .isEqualTo("[]");
        }
    }

    @Nested
    class DescribeRecentReadings {

        @Test
        void keepsOnlyTheLatestReadings() {
            // One reading more than the capacity: the first is dropped and the rest kept in order, so
            // the report is always the end of the story rather than its start.
            var readingLog = new ReseatReadingLog();

            for (var ordinal = 1; ordinal <= ReseatReadingLog.RECENT_READINGS_CAPACITY + 1; ordinal++) {
                readingLog.recordReading(new ReseatReading(ordinal, ReseatObservation.ICON_CLEAR, ReseatAction.NONE));
            }

            assertThat(readingLog.describeRecentReadings())
                .startsWith("[#2 ICON_CLEAR -> NONE, ")
                .endsWith(", #13 ICON_CLEAR -> NONE]")
                .doesNotContain("#1 ");
        }
    }

    @Nested
    class RecordLiftReading {

        @Test
        void keepsTheReadingAmongTheLatestToo() {

            var readingLog = new ReseatReadingLog();

            readingLog.recordReading(CLEAR_READING);
            readingLog.recordLiftStart(FIRST_REMOVAL);
            readingLog.recordLiftReading(FIRST_PUT_BACK);

            assertThat(readingLog.describeRecentReadings())
                .isEqualTo("[#1 ICON_CLEAR -> NONE, #2 ICON_BURIED -> REMOVE, #3 ICON_DROPPED -> ADD]");
        }
    }

    @Nested
    class RecordLiftStart {

        @Test
        void forgetsThePreviousLift() {

            var readingLog = new ReseatReadingLog();

            readingLog.recordLiftStart(FIRST_REMOVAL);
            readingLog.recordLiftReading(FIRST_PUT_BACK);
            readingLog.recordLiftStart(SECOND_REMOVAL);

            assertThat(readingLog.describeLatestLiftReadings())
                .isEqualTo("[#4 ICON_BURIED -> REMOVE]");
        }
    }
}
