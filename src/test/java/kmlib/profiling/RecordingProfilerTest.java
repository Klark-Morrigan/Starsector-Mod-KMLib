package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the accumulation contract of {@link RecordingProfiler}: measure() captures
 * the clock delta, repeated sections aggregate into count/total/min/max, snapshot
 * order follows first-record order, and reset() clears everything.
 */
final class RecordingProfilerTest {

    @Nested
    class Measure {

        @Test
        void measureRecordsTheClockDeltaForASection() {

            var clock = new ScriptedClock(100, 250);
            var profiler = new RecordingProfiler(clock);

            profiler.measure("build", () -> {
            });

            var timing = profiler.snapshot().get(0);

            assertThat(timing.getSection())
                .isEqualTo("build");
            assertThat(timing.getCount())
                .isEqualTo(1);
            assertThat(timing.getTotalNanos())
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

            var timing = profiler.snapshot().get(0);

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
            assertThat(profiler.snapshot().get(0).getTotalNanos())
                .isEqualTo(42);
        }
    }

    @Nested
    class Snapshot {

        @Test
        void snapshotFollowsFirstRecordOrder() {

            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 0, 0));

            profiler.measure("second", () -> {
            });
            profiler.measure("first", () -> {
            });

            assertThat(profiler.snapshot()).extracting(SectionTiming::getSection)
                .containsExactly("second", "first");
        }
    }

    @Nested
    class Reset {

        @Test
        void resetClearsAllSections() {

            var profiler = new RecordingProfiler(new ScriptedClock(0, 10));

            profiler.measure("build", () -> {
            });

            profiler.reset();

            assertThat(profiler.snapshot())
                .isEmpty();
        }
    }

    // Returns the supplied values in order on successive calls, so a measure()
    // sees a known start then end and thus a known delta.
    private static final class ScriptedClock implements LongSupplier {

        private final long[] readings;
        private final AtomicLong index = new AtomicLong();

        private ScriptedClock(long... readings) {
            this.readings = readings;
        }

        @Override
        public long getAsLong() {
            return readings[(int) index.getAndIncrement()];
        }
    }
}
