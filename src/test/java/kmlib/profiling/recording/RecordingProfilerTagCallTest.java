package kmlib.profiling.recording;

import kmlib.profiling.ProfileSection;
import kmlib.profiling.recording.RecordingProfilerTestSupport.ScriptedClock;
import kmlib.profiling.snapshot.WorstCall;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.profiling.recording.RecordingProfilerTestSupport.BLANK_TAG;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.PARENT_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.SLOWEST_CALL_TAG;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.openTaggedCall;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readRoots;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins, of {@link RecordingProfiler}, that the slowest call is kept with the tag it ended on, a
 * blank tag leaves it unnamed, and a later name replaces an earlier one.
 */
final class RecordingProfilerTagCallTest {

    @Nested
    class TagCall {

        @Test
        void namesTheCallItWasSetOnAndNotTheOnesAroundIt() {
            // A tag belongs to one call: the section it was set on runs again with nothing named,
            // and the record still says which call the name was for.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 100, 100, 110));

            openTaggedCall(profiler, SLOWEST_CALL_TAG);
            profiler.open(ProfileSection.registerSection(PARENT_SECTION)).close();

            assertThat(readRoots(profiler).get(0).getWorstCall().getTag())
                .isEqualTo(SLOWEST_CALL_TAG);
        }

        @Test
        void leavesACallUnnamedWhereTheTagSaysNothing() {
            // A caller composing a tag out of what it happens to hold may end up with an empty
            // string, and a name that is a run of spaces reads as one that was lost.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 100));

            openTaggedCall(profiler, BLANK_TAG);

            assertThat(readRoots(profiler).get(0).getWorstCall().getTag())
                .isEqualTo(WorstCall.NO_TAG);
        }
    }
}
