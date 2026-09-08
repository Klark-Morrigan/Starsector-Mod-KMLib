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
    class OpenRoot {

        @Test
        void openRootHandsBackTheSameSharedScopeAndKeepsNoOrigin() {
            // A beat naming the game it runs in costs the same nothing as any other section here:
            // there is no tree for an origin to head, so the label goes nowhere.
            var scope = SilentProfiler.INSTANCE.openRoot(
                ProfileOrigin.registerOrigin("test.silent.sector"),
                ProfileSection.registerSection("frame"));

            scope.close();

            assertThat(scope)
                .isSameAs(SilentProfiler.INSTANCE.open(ProfileSection.registerSection("build")));
            assertThat(SilentProfiler.INSTANCE.snapshot())
                .isEmpty();
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
    class AddCountToOpenScope {

        @Test
        void countingWithNothingOpenKeepsNothing() {
            // What makes a shared read free to count what it traverses: with no readout bound
            // there is no open scope to charge and no reserved row to fall back to.
            SilentProfiler.INSTANCE.addCountToOpenScope(
                ProfileCounter.registerCounter("systems"),
                400);

            assertThat(SilentProfiler.INSTANCE.snapshot())
                .isEmpty();
        }
    }

    @Nested
    class TagCall {

        @Test
        void tagCallOnTheSharedScopeKeepsNothing() {
            // The shared scope is stateless, so a name handed to it cannot outlive the call and
            // reach the next section opened through it.
            var scope = SilentProfiler.INSTANCE.open(ProfileSection.registerSection("rebuild"));

            scope.tagCall("eos");
            scope.close();

            assertThat(SilentProfiler.INSTANCE.snapshot())
                .isEmpty();
        }
    }

    @Nested
    class OpenIterations {

        @Test
        void openIterationsHandsBackTheSameSharedScope() {
            // A loop is as free to leave unmeasured as the section holding it: nothing is
            // allocated for the turns of a bake with no readout bound.
            var scope = SilentProfiler.INSTANCE.openIterations(
                PhasedSection.registerPhasedSection("test.silent.bake", "plan"));

            assertThat(scope)
                .isSameAs(SilentProfiler.INSTANCE.open(ProfileSection.registerSection("build")));
        }

        @Test
        void turnsMarkedOnTheSharedScopeKeepNothing() {
            // The shared scope is stateless, so a turn cannot outlive the loop that ran it and
            // reach whatever opens next through it.
            var section = PhasedSection.registerPhasedSection("test.silent.loop", "plan");
            var scope = SilentProfiler.INSTANCE.openIterations(section);

            scope.beginIteration("cell");
            scope.markPhase(section.resolvePhase("plan"));
            scope.endIteration();
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
    class GetRecordedLevel {

        @Test
        void answersOffBecauseNothingIsKeptAtAnyDetail() {
            // What a caller choosing what to bind compares its knob against, so "off" resolves to
            // this profiler and asking for off again rebinds nothing.
            assertThat(SilentProfiler.INSTANCE.getRecordedLevel())
                .isEqualTo(ProfileLevel.OFF);
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
