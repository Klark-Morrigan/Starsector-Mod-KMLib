package kmlib.profiling.recording;

import kmlib.profiling.ProfileSection;
import kmlib.profiling.recording.RecordingProfilerTestSupport.ScriptedClock;
import kmlib.profiling.snapshot.WorstCall;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.profiling.recording.RecordingProfilerTestSupport.PARENT_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readRoots;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readSectionNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins, of {@link RecordingProfiler}, that snapshot order follows first-record order, and an open
 * scope's row reads as the zeroes its timing already reads as.
 */
final class RecordingProfilerSnapshotTest {

    @Nested
    class Snapshot {

        @Test
        void snapshotFollowsFirstRecordOrder() {

            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 0, 0));

            profiler.measure("second", () -> {
            });
            profiler.measure("first", () -> {
            });

            assertThat(readSectionNames(readRoots(profiler)))
                .containsExactly("second", "first");
        }

        @Test
        void reportsASectionStillOpenAsARowWithNoSpanYet() {
            // A readout asked for mid-frame has to show the section that is running, and show it
            // holding nothing - the row a span has not reached yet is zeroes, not a bound.
            var profiler = new RecordingProfiler(new ScriptedClock(0));

            profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            var openNode = readRoots(profiler).get(0);

            assertThat(openNode.getTiming().getCount())
                .isEqualTo(0);
            assertThat(openNode.getTiming().getMinNanos())
                .isEqualTo(0);
            assertThat(openNode.getTiming().getMaxNanos())
                .isEqualTo(0);
        }

        @Test
        void reportsNoWorstCallAndNoBandsForASectionNoCallHasFinishedOn() {
            // The same row from the other two columns' side: there is no slowest call to describe
            // and nowhere to place a span, so both read as the shared nothing.
            var profiler = new RecordingProfiler(new ScriptedClock(0));

            profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            var openNode = readRoots(profiler).get(0);

            assertThat(openNode.getWorstCall())
                .isSameAs(WorstCall.NO_CALL);
            assertThat(openNode.getTiming().getBuckets().hasAnyCalls())
                .isFalse();
        }
    }
}
