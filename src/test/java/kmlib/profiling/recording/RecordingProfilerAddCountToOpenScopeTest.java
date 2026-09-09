package kmlib.profiling.recording;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileOrigin;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.recording.RecordingProfilerTestSupport.ScriptedClock;
import kmlib.profiling.snapshot.CountTotals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.profiling.recording.RecordingProfilerTestSupport.CHILD_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.PARENT_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.SYSTEMS_COUNTER;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readCount;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readRoots;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins, of {@link RecordingProfiler}, that a count made by a read holding no scope lands on the
 * innermost open scope, and on the reserved row of the reserved origin when nothing is open.
 */
final class RecordingProfilerAddCountToOpenScopeTest {

    @Nested
    class AddCountToOpenScope {

        @Test
        void chargesACountWithNoScopeOfItsOwnToTheInnermostOpenOne() {
            // How a shared read reports what it traversed: it holds no scope, so the row that
            // asked for the traversal is the one that answers for it - and the row above that,
            // since the count rolls up like any other.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 0, 0));

            var parent = profiler.open(ProfileSection.registerSection(PARENT_SECTION));
            var child = profiler.open(ProfileSection.registerSection(CHILD_SECTION));

            profiler.addCountToOpenScope(ProfileCounter.registerCounter(SYSTEMS_COUNTER), 48);
            child.close();
            parent.close();

            var parentNode = readRoots(profiler).get(0);

            assertThat(readCount(parentNode, SYSTEMS_COUNTER).getTotals())
                .extracting(CountTotals::getTotal, CountTotals::getSelfTotal)
                .containsExactly(48L, 0L);
            assertThat(readCount(parentNode.getChildren().get(0), SYSTEMS_COUNTER).getTotals())
                .extracting(CountTotals::getTotal, CountTotals::getSelfTotal)
                .containsExactly(48L, 48L);
        }

        @Test
        void landsACountMadeWithNothingOpenOnTheReservedRowOfTheReservedOrigin() {
            // A traversal from a path nobody profiled is the first thing a reader hunting stray
            // walks looks for, so it is kept - with no duration, nothing having timed it.
            var profiler = new RecordingProfiler(new ScriptedClock());

            profiler.addCountToOpenScope(ProfileCounter.registerCounter(SYSTEMS_COUNTER), 48);

            var originTree = profiler.snapshot().get(0);
            var unscopedRow = originTree.getRoots().get(0);

            assertThat(originTree.getOrigin())
                .isSameAs(ProfileOrigin.UNSCOPED);
            assertThat(unscopedRow.getSection())
                .isSameAs(ProfileSection.UNSCOPED_COUNTS);
            assertThat(unscopedRow.getTiming().getTotalNanos())
                .isEqualTo(0L);
            assertThat(readCount(unscopedRow, SYSTEMS_COUNTER).getTotals().getTotal())
                .isEqualTo(48L);
        }

        @Test
        void keepsTwoCountsMadeWithNothingOpenAsTwoCallsOfTheReservedRow() {
            // Each arrival is a call of its own, there being no scope to gather them into one, so
            // the row states how many times something counted from an unprofiled path as well as
            // how much it counted.
            var profiler = new RecordingProfiler(new ScriptedClock());
            var counter = ProfileCounter.registerCounter(SYSTEMS_COUNTER);

            profiler.addCountToOpenScope(counter, 3);
            profiler.addCountToOpenScope(counter, 8);

            var unscopedRow = profiler.snapshot().get(0).getRoots().get(0);

            assertThat(unscopedRow.getTiming().getCount())
                .isEqualTo(2L);
            assertThat(readCount(unscopedRow, SYSTEMS_COUNTER).getTotals().getTotal())
                .isEqualTo(11L);
        }
    }
}
