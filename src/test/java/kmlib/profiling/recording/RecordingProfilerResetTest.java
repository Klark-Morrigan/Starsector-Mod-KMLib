package kmlib.profiling.recording;

import kmlib.profiling.ProfileSection;
import kmlib.profiling.recording.RecordingProfilerTestSupport.ScriptedClock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.profiling.recording.RecordingProfilerTestSupport.ONE_MILLISECOND_PER_CALL;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.PARENT_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.TEN_MILLISECONDS_IN_NANOS;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.captureLogWhile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins, of {@link RecordingProfiler}, that reset() clears everything, lets a breach be said again
 * for the next capture, and drops a scope that was open when the timings were cleared.
 */
final class RecordingProfilerResetTest {

    @Nested
    class Reset {

        @Test
        void resetClearsAllSections() {
            // Read as the whole capture rather than through one origin's roots: a group left
            // behind would still be holding the tree it was grouping.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10));

            profiler.measure("build", () -> {
            });

            profiler.reset();

            assertThat(profiler.snapshot())
                .isEmpty();
        }

        @Test
        void reportsABreachAgainInTheCaptureAfterAClearedOne() {
            // "Said once" is once per capture, not once per session: the capture a breach was
            // reported against is gone, and a reader who cleared the timings to watch one pass
            // would otherwise be told nothing about the pass they cleared for.
            var section = ProfileSection.registerSection(
                "test.budget.acrossAReset",
                ONE_MILLISECOND_PER_CALL);

            var profiler = new RecordingProfiler(
                new ScriptedClock(0, TEN_MILLISECONDS_IN_NANOS, 0, TEN_MILLISECONDS_IN_NANOS));

            var messages = captureLogWhile(() -> {
                profiler.open(section).close();
                profiler.reset();
                profiler.open(section).close();
            });

            assertThat(messages)
                .hasSize(2);
        }

        @Test
        void dropsAScopeThatWasOpenWhenTheTimingsWereCleared() {
            // Its node went with the tree, so its close has nowhere to land and must not raise a
            // new root out of a call that began before the reader asked for a clean slate.
            var profiler = new RecordingProfiler(new ScriptedClock(0));

            var scope = profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            profiler.reset();
            scope.close();

            assertThat(profiler.snapshot())
                .isEmpty();
        }
    }
}
