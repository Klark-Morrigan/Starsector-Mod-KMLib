package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link SilentProfiler}: the work it is handed still runs and still
 * returns its result, and nothing it is handed is kept - the snapshot is empty
 * after any sequence of calls, counts included.
 */
final class SilentProfilerTest {

    @Nested
    class Open {

        @Test
        void openReturnsAScopeThatClosesWithoutEffect() {

            var scope = SilentProfiler.INSTANCE.open(ProfileSection.registerSection("build"));

            scope.close();

            assertThat(SilentProfiler.INSTANCE.snapshot())
                .isEmpty();
        }

        @Test
        void openHandsBackOneSharedScopeRatherThanANewOne() {
            // What makes a section opened on a per-frame path free: nothing is allocated to open
            // one, so library code can sit inside a scope with no readout bound.
            var scope = SilentProfiler.INSTANCE.open(ProfileSection.registerSection("build"));

            assertThat(SilentProfiler.INSTANCE.open(ProfileSection.registerSection("render")))
                .isSameAs(scope);
        }
    }

    @Nested
    class AddCount {

        @Test
        void addCountOnTheSharedScopeKeepsNothing() {
            // What lets a walker count what it traverses unconditionally: with no readout bound
            // the count goes nowhere, so the counting is a call and not a tally.
            var scope = SilentProfiler.INSTANCE.open(ProfileSection.registerSection("walk"));

            scope.addCount(ProfileCounter.registerCounter("systems"), 400);
            scope.close();

            assertThat(SilentProfiler.INSTANCE.snapshot())
                .isEmpty();
        }
    }

    @Nested
    class Measure {

        @Test
        void measureRunsTheWorkItIsHanded() {

            var runs = new AtomicInteger();

            SilentProfiler.INSTANCE.measure("build", runs::incrementAndGet);

            assertThat(runs.get())
                .isEqualTo(1);
        }

        @Test
        void measureSupplierReturnsTheWorkResult() {

            var result = SilentProfiler.INSTANCE.measure("compute", () -> "value");

            assertThat(result)
                .isEqualTo("value");
        }
    }

    @Nested
    class Snapshot {

        @Test
        void snapshotIsEmptyAfterMeasuringAndRecording() {

            SilentProfiler.INSTANCE.measure("build", () -> {
            });
            SilentProfiler.INSTANCE.measure("compute", () -> "value");
            SilentProfiler.INSTANCE.record("render", 2_000_000);

            assertThat(SilentProfiler.INSTANCE.snapshot())
                .isEmpty();
        }
    }

    @Nested
    class Reset {

        @Test
        void resetLeavesTheSnapshotEmpty() {

            SilentProfiler.INSTANCE.record("render", 2_000_000);

            SilentProfiler.INSTANCE.reset();

            assertThat(SilentProfiler.INSTANCE.snapshot())
                .isEmpty();
        }
    }
}
