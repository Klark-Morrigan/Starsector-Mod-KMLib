package kmlib.profiling.recording;

import kmlib.profiling.ProfileSection;
import kmlib.profiling.recording.RecordingProfilerTestSupport.ScriptedClock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.profiling.recording.RecordingProfilerTestSupport.CHILD_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.PARENT_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readRoots;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readSectionNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins, of {@link RecordingProfiler}, that a block measured by name lands on the row a scope of
 * that section would, closes down a throwing path, and hands back what the block returned.
 */
final class RecordingProfilerMeasureTest {

    @Nested
    class Measure {

        @Test
        void measureRecordsTheClockDeltaForASection() {

            var clock = new ScriptedClock(100, 250);
            var profiler = new RecordingProfiler(clock);

            profiler.measure("build", () -> {
            });

            var node = readRoots(profiler).get(0);

            assertThat(node.getSection().getName())
                .isEqualTo("build");
            assertThat(node.getTiming().getCount())
                .isEqualTo(1);
            assertThat(node.getTiming().getTotalNanos())
                .isEqualTo(150);
        }

        @Test
        void repeatedMeasuresAggregateCountTotalMinAndMax() {
            // Two runs of "render": 100->150 (50ns) then 150->350 (200ns).
            var clock = new ScriptedClock(100, 150, 150, 350);
            var profiler = new RecordingProfiler(clock);

            profiler.measure("render", () -> {
            });

            profiler.measure("render", () -> {
            });

            var timing = readRoots(profiler).get(0).getTiming();

            assertThat(timing.getCount())
                .isEqualTo(2);
            assertThat(timing.getTotalNanos())
                .isEqualTo(250);
            assertThat(timing.getMinNanos())
                .isEqualTo(50);
            assertThat(timing.getMaxNanos())
                .isEqualTo(200);
            assertThat(timing.getAverageNanos())
                .isEqualTo(125);
        }

        @Test
        void measureSupplierReturnsTheWorkResultAndStillTimesIt() {

            var clock = new ScriptedClock(0, 42);
            var profiler = new RecordingProfiler(clock);
            var result = profiler.measure("compute", () -> "value");

            assertThat(result)
                .isEqualTo("value");
            assertThat(readRoots(profiler).get(0).getTiming().getTotalNanos())
                .isEqualTo(42);
        }

        @Test
        void nestsAMeasuredBlockUnderTheSectionThatIsOpen() {
            // What lets a call site keep its measure() and still be read as part of the scope it
            // runs in, so converting one to a scope is a change of spelling and not of the tree.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10, 30, 90));

            var parent = profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            profiler.measure(CHILD_SECTION, () -> {
            });
            parent.close();

            var parentNode = readRoots(profiler).get(0);

            assertThat(readSectionNames(parentNode.getChildren()))
                .containsExactly(CHILD_SECTION);
            assertThat(parentNode.getChildren().get(0).getTiming().getTotalNanos())
                .isEqualTo(20);
        }
    }
}
