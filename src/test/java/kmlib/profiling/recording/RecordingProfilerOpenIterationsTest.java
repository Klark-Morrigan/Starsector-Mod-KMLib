package kmlib.profiling.recording;

import kmlib.profiling.ProfileLevel;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.SectionTerms;
import kmlib.profiling.recording.RecordingProfilerTestSupport.ScriptedClock;
import kmlib.profiling.snapshot.PhaseTotal;
import kmlib.profiling.snapshot.ProfileIterations;
import kmlib.profiling.snapshot.WorstCall;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.profiling.recording.RecordingProfilerTestSupport.BLANK_TAG;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.FINE_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.FIRST_CALL_TAG;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.LAST_CALL_TAG;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.PARENT_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.PLAN_PHASE_NAME;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.SLOWEST_CALL_TAG;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.TRACE_PHASE_NAME;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readPhaseNanos;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readRoots;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.registerBakeSection;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.runOneTurnOf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Pins, of {@link RecordingProfiler}, that a scope opened over a loop counts its turns and charges
 * each step of one to its own slot, drops a step of another section's loop and a mark outside a
 * turn, and keeps the slowest turn with its tag.
 */
final class RecordingProfilerOpenIterationsTest {

    @Nested
    class OpenIterations {

        @Test
        void sumsEachStepOfATurnIntoItsOwnSlot() {
            // What the loop is measured for: two steps that grow on different axes are two
            // numbers, and one span over the pair could only say the loop got slower.
            var section = registerBakeSection("test.iterations.slots");
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10, 13, 18, 20, 30));

            try (var loop = profiler.openIterations(section)) {
                loop.beginIteration(FIRST_CALL_TAG);
                loop.markPhase(section.resolvePhase(PLAN_PHASE_NAME));
                loop.markPhase(section.resolvePhase(TRACE_PHASE_NAME));
                loop.endIteration();
            }

            assertThat(readRoots(profiler).get(0).getIterations().getPhaseTotals())
                .extracting(phaseTotal -> phaseTotal.getPhase().getName(), PhaseTotal::getTotalNanos)
                .containsExactly(tuple("plan", 3L), tuple("trace", 5L));
        }

        @Test
        void countsTheTurnsOfTheLoopRatherThanTheCallsOfTheRow() {
            // A bake is one call and a cell is one turn: the row's own count answers what a bake
            // costs, and only the turns can answer what a cell does.
            var section = registerBakeSection("test.iterations.turns");
            var profiler = new RecordingProfiler(new ScriptedClock(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

            try (var loop = profiler.openIterations(section)) {
                runOneTurnOf(loop, section, "cell.first");
                runOneTurnOf(loop, section, "cell.second");
            }

            var bakeNode = readRoots(profiler).get(0);

            assertThat(bakeNode.getTiming().getCount())
                .isEqualTo(1);
            assertThat(bakeNode.getIterations().getCount())
                .isEqualTo(2);
        }

        @Test
        void keepsTheSlowestTurnAndWhatItsCallerNamedIt() {
            // A mean over thousands of turns cannot say whether one item was pathological, which
            // is the item worth looking at - and its name is the only thing that says which.
            var section = registerBakeSection("test.iterations.slowest");
            // A first turn of 5ns and a second of 20ns, so the record cannot be whichever ran
            // last.
            var profiler =
                new RecordingProfiler(new ScriptedClock(0, 10, 11, 12, 15, 20, 22, 24, 40, 50));

            try (var loop = profiler.openIterations(section)) {
                runOneTurnOf(loop, section, FIRST_CALL_TAG);
                runOneTurnOf(loop, section, SLOWEST_CALL_TAG);
            }

            var iterations = readRoots(profiler).get(0).getIterations();

            assertThat(iterations.getSlowestNanos())
                .isEqualTo(20);
            assertThat(iterations.getSlowestTag())
                .isEqualTo(SLOWEST_CALL_TAG);
        }

        @Test
        void leavesATurnItsCallerNamedNothingUnnamed() {
            // A slowest turn named a run of spaces reads as one whose name was lost rather than as
            // one that was never given.
            var section = registerBakeSection("test.iterations.blankTag");
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10, 12, 14, 20, 30));

            try (var loop = profiler.openIterations(section)) {
                runOneTurnOf(loop, section, BLANK_TAG);
            }

            assertThat(readRoots(profiler).get(0).getIterations().getSlowestTag())
                .isEqualTo(WorstCall.NO_TAG);
        }

        @Test
        void chargesAStepMarkedOutOfOrderWhereItWasMarked() {
            // The mark says what has just finished, so a caller that runs its steps in another
            // order gets the numbers that order produced rather than the ones it declared.
            var section = registerBakeSection("test.iterations.outOfOrder");
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10, 17, 19, 20, 30));

            try (var loop = profiler.openIterations(section)) {
                loop.beginIteration(FIRST_CALL_TAG);
                loop.markPhase(section.resolvePhase(TRACE_PHASE_NAME));
                loop.markPhase(section.resolvePhase(PLAN_PHASE_NAME));
                loop.endIteration();
            }

            assertThat(readRoots(profiler).get(0).getIterations().getPhaseTotals())
                .extracting(phaseTotal -> phaseTotal.getPhase().getName(), PhaseTotal::getTotalNanos)
                .containsExactly(tuple("plan", 2L), tuple("trace", 7L));
        }

        @Test
        void dropsAStepBelongingToAnotherSectionsLoop() {
            // Such a step numbers a slot of this loop that means something else, so it is dropped
            // rather than charged: a phase silently holding another loop's work is a diagnostic
            // that misleads about the very loop it was opened for.
            var section = registerBakeSection("test.iterations.ownLoop");
            var otherSection = registerBakeSection("test.iterations.otherLoop");
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10, 15, 20, 20, 20));

            try (var loop = profiler.openIterations(section)) {
                loop.beginIteration(FIRST_CALL_TAG);
                loop.markPhase(otherSection.resolvePhase(PLAN_PHASE_NAME));
                loop.markPhase(section.resolvePhase(PLAN_PHASE_NAME));
                loop.endIteration();
            }

            // The whole 10ns since the turn began, since the dropped mark moved no boundary.
            assertThat(readPhaseNanos(profiler, PLAN_PHASE_NAME))
                .isEqualTo(10);
        }

        @Test
        void dropsAStepMarkedWithNoTurnRunning() {
            // There is no boundary behind such a mark to measure from, so it says nothing about
            // the turn that begins after it.
            var section = registerBakeSection("test.iterations.markOutsideATurn");
            var profiler = new RecordingProfiler(new ScriptedClock(0, 5, 10, 14, 14, 14));

            try (var loop = profiler.openIterations(section)) {
                loop.markPhase(section.resolvePhase(PLAN_PHASE_NAME));
                loop.beginIteration(FIRST_CALL_TAG);
                loop.markPhase(section.resolvePhase(PLAN_PHASE_NAME));
                loop.endIteration();
            }

            assertThat(readPhaseNanos(profiler, PLAN_PHASE_NAME))
                .isEqualTo(4);
        }

        @Test
        void sumsTheTurnsOfEveryCallOfTheRow() {
            // A row is every call of it, for its loop as for its span: two bakes of a cell each are
            // two turns on the one row, with the slower of the two kept whichever call ran it.
            var section = registerBakeSection("test.iterations.twoCalls");
            // A turn of 20ns in the first call and one of 9ns in the second, each charging 1ns to
            // the plan.
            var profiler = new RecordingProfiler(
                new ScriptedClock(0, 10, 11, 12, 30, 40, 50, 51, 52, 55, 60, 70));

            try (var loop = profiler.openIterations(section)) {
                runOneTurnOf(loop, section, SLOWEST_CALL_TAG);
            }
            try (var loop = profiler.openIterations(section)) {
                runOneTurnOf(loop, section, LAST_CALL_TAG);
            }

            var iterations = readRoots(profiler).get(0).getIterations();

            assertThat(iterations.getCount())
                .isEqualTo(2);
            assertThat(readPhaseNanos(profiler, PLAN_PHASE_NAME))
                .isEqualTo(2);
            assertThat(iterations.getSlowestTag())
                .isEqualTo(SLOWEST_CALL_TAG);
        }

        @Test
        void reportsNoLoopForACallThatRanNoTurnOfOne() {
            // A section whose loop had nothing to iterate over says nothing about steps: a row of
            // zeroed phases would read as steps that cost nothing rather than as steps nothing
            // ran.
            var section = registerBakeSection("test.iterations.emptyLoop");
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10));

            profiler.openIterations(section).close();

            assertThat(readRoots(profiler).get(0).getIterations())
                .isSameAs(ProfileIterations.NO_ITERATIONS);
        }

        @Test
        void reportsNoLoopForASectionOpenedWithoutOne() {
            // Most rows, which is why the record they carry is the shared nothing rather than an
            // empty one of their own.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10));

            profiler.open(ProfileSection.registerSection(PARENT_SECTION)).close();

            assertThat(readRoots(profiler).get(0).getIterations())
                .isSameAs(ProfileIterations.NO_ITERATIONS);
        }

        @Test
        void keepsALoopsOwnSpanButNotItsTurnsUnderACoarseCapture() {
            // The two are worth keeping at different levels: a bake is one call of a rebuild's step
            // and worth a row whenever anything is being read, while a clock read per step per cell
            // is only worth it when the cell is what is being read. Two readings scripted - the
            // loop's own ends - so a turn timed here would fail the case.
            var section = registerBakeSection("test.level.bakeTurns");
            var profiler = new RecordingProfiler(ProfileLevel.COARSE, new ScriptedClock(0, 40));

            try (var loop = profiler.openIterations(section)) {
                runOneTurnOf(loop, section, FIRST_CALL_TAG);
            }

            var bakeNode = readRoots(profiler).get(0);

            assertThat(bakeNode.getTiming().getTotalNanos())
                .isEqualTo(40);
            assertThat(bakeNode.getIterations())
                .isSameAs(ProfileIterations.NO_ITERATIONS);
        }

        @Test
        void opensALoopOnASectionFinerThanTheCaptureSilently() {
            // A phased section holds the interned section of its name, so a loop declared on a name
            // registered as a per-item section is skipped exactly as a plain open of it would be.
            ProfileSection.registerSection(
                FINE_SECTION, SectionTerms.DEFAULT.withLevel(ProfileLevel.FINE));

            var section = registerBakeSection(FINE_SECTION);
            var profiler = new RecordingProfiler(ProfileLevel.COARSE, new ScriptedClock());

            profiler.openIterations(section).close();

            assertThat(profiler.snapshot())
                .isEmpty();
        }
    }
}
