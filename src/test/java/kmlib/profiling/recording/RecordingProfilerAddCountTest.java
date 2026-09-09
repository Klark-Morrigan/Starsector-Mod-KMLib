package kmlib.profiling.recording;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.recording.RecordingProfilerTestSupport.ScriptedClock;
import kmlib.profiling.snapshot.CountSpread;
import kmlib.profiling.snapshot.CountTotals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.profiling.recording.RecordingProfilerTestSupport.CHILD_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.INNER_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.MARKETS_COUNTER;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.OUTER_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.PARENT_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.SYSTEMS_COUNTER;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.countInACallOf;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readCount;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readRoots;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins, of {@link RecordingProfiler}, that a count added on a scope rolls into every scope it was
 * open inside, spreads over the calls of its own row, and is kept at the slowest call's own value.
 */
final class RecordingProfilerAddCountTest {

    @Nested
    class AddCount {

        @Test
        void countsWhatAChildCountedInTheParentsTotalButNotItsSelfTotal() {
            // The point of counting at chokepoints: a rebuild's row states how many systems were
            // walked beneath it whoever walked them, while the per-item cost of its own self time
            // stays priced against the items it handled itself - here, none.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 0, 0));

            var parent = profiler.open(ProfileSection.registerSection(PARENT_SECTION));
            var child = profiler.open(ProfileSection.registerSection(CHILD_SECTION));

            child.addCount(ProfileCounter.registerCounter(SYSTEMS_COUNTER), 7);
            child.close();
            parent.close();

            var parentNode = readRoots(profiler).get(0);
            var childNode = parentNode.getChildren().get(0);

            assertThat(readCount(parentNode, SYSTEMS_COUNTER).getTotals())
                .extracting(CountTotals::getTotal, CountTotals::getSelfTotal)
                .containsExactly(7L, 0L);
            assertThat(readCount(childNode, SYSTEMS_COUNTER).getTotals())
                .extracting(CountTotals::getTotal, CountTotals::getSelfTotal)
                .containsExactly(7L, 7L);
        }

        @Test
        void spreadsACountOverTheCallsOfTheRowRatherThanTheRowsTotal() {
            // Two calls of one row counting 3 and 8. A total of 11 says the row is worth looking
            // at; that one call did 8 of it says which call to look at.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 0, 0, 0, 0));

            var parent = profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            countInACallOf(profiler, CHILD_SECTION, 3);
            countInACallOf(profiler, CHILD_SECTION, 8);
            parent.close();

            var count = readCount(readRoots(profiler).get(0).getChildren().get(0), SYSTEMS_COUNTER);

            assertThat(count.getTotals().getTotal())
                .isEqualTo(11L);
            assertThat(count.getSpread())
                .extracting(CountSpread::getMinPerCall, CountSpread::getMaxPerCall)
                .containsExactly(3L, 8L);
        }

        @Test
        void readsACallThatCountedNoneOfItAsAZeroRatherThanSkippingIt() {
            // A row that usually walks and sometimes does not has a minimum of zero. A spread that
            // only saw the calls which counted would report a floor no call ever went under.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 0, 0, 0, 0));

            var parent = profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            countInACallOf(profiler, CHILD_SECTION, 5);
            profiler.open(ProfileSection.registerSection(CHILD_SECTION)).close();
            parent.close();

            var count = readCount(readRoots(profiler).get(0).getChildren().get(0), SYSTEMS_COUNTER);

            assertThat(count.getTotals().getTotal())
                .isEqualTo(5L);
            assertThat(count.getSpread())
                .extracting(CountSpread::getMinPerCall, CountSpread::getMaxPerCall)
                .containsExactly(0L, 5L);
        }

        @Test
        void countsTheCallsThatRanBeforeACounterWasFirstAddedTo() {
            // The same rule from the other side: a counter first seen on the second call did not
            // start existing then, and the call before it counted none of it.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 0, 0, 0, 0));

            var parent = profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            profiler.open(ProfileSection.registerSection(CHILD_SECTION)).close();
            countInACallOf(profiler, CHILD_SECTION, 5);
            parent.close();

            var childNode = readRoots(profiler).get(0).getChildren().get(0);

            assertThat(readCount(childNode, SYSTEMS_COUNTER).getSpread())
                .extracting(CountSpread::getMinPerCall, CountSpread::getMaxPerCall)
                .containsExactly(0L, 5L);
        }

        @Test
        void separatesWhatARowCountedItselfFromWhatItsChildrenCounted() {
            // The row that both counts and delegates is where the two totals part company: 9 in
            // all is what the row is answerable for, and 2 is what its self time may be priced
            // against - dividing that time by the child's 7 would cost work it never did.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 0, 0));

            var parent = profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            parent.addCount(ProfileCounter.registerCounter(SYSTEMS_COUNTER), 2);

            var child = profiler.open(ProfileSection.registerSection(CHILD_SECTION));

            child.addCount(ProfileCounter.registerCounter(SYSTEMS_COUNTER), 7);
            child.close();
            parent.close();

            assertThat(readCount(readRoots(profiler).get(0), SYSTEMS_COUNTER).getTotals())
                .extracting(CountTotals::getTotal, CountTotals::getSelfTotal)
                .containsExactly(9L, 2L);
        }

        @Test
        void addsUpRepeatedCountsOfOneCounterWithinASingleCall() {
            // A walker adds as it goes rather than once at the end, so one call reaches the same
            // counter many times and the row has to read as the one call it was.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0));

            var scope = profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            scope.addCount(ProfileCounter.registerCounter(SYSTEMS_COUNTER), 3);
            scope.addCount(ProfileCounter.registerCounter(SYSTEMS_COUNTER), 4);
            scope.close();

            var count = readCount(readRoots(profiler).get(0), SYSTEMS_COUNTER);

            assertThat(count.getTotals().getTotal())
                .isEqualTo(7L);
            assertThat(count.getSpread())
                .extracting(CountSpread::getMinPerCall, CountSpread::getMaxPerCall)
                .containsExactly(7L, 7L);
        }

        @Test
        void keepsSeveralCountersOnOneRowApart() {
            // A section counts more than one kind of thing - systems walked and markets read - and
            // a later call may touch only some of them. Each counter carries its own spread, so a
            // call that read markets without walking must not shorten the walk's tally.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 0, 0));

            var first = profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            first.addCount(ProfileCounter.registerCounter(SYSTEMS_COUNTER), 3);
            first.addCount(ProfileCounter.registerCounter(MARKETS_COUNTER), 10);
            first.close();

            var second = profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            second.addCount(ProfileCounter.registerCounter(MARKETS_COUNTER), 4);
            second.close();

            var node = readRoots(profiler).get(0);

            assertThat(readCount(node, SYSTEMS_COUNTER).getTotals().getTotal())
                .isEqualTo(3L);
            assertThat(readCount(node, SYSTEMS_COUNTER).getSpread())
                .extracting(CountSpread::getMinPerCall, CountSpread::getMaxPerCall)
                .containsExactly(0L, 3L);
            assertThat(readCount(node, MARKETS_COUNTER).getTotals().getTotal())
                .isEqualTo(14L);
            assertThat(readCount(node, MARKETS_COUNTER).getSpread())
                .extracting(CountSpread::getMinPerCall, CountSpread::getMaxPerCall)
                .containsExactly(4L, 10L);
        }

        @Test
        void keepsTheCountsOfAScopeTheUnwindClosedForIt() {
            // A scope left open is closed by the one outside it, and what it counted is part of
            // that call as much as its time is - dropping the count there would take the walk out
            // of the row whose budget is meant to catch it.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 0));

            var outer = profiler.open(ProfileSection.registerSection(OUTER_SECTION));

            profiler.open(ProfileSection.registerSection(INNER_SECTION))
                .addCount(ProfileCounter.registerCounter(SYSTEMS_COUNTER), 5);
            outer.close();

            var outerNode = readRoots(profiler).get(0);

            assertThat(readCount(outerNode, SYSTEMS_COUNTER).getTotals())
                .extracting(CountTotals::getTotal, CountTotals::getSelfTotal)
                .containsExactly(5L, 0L);
            assertThat(readCount(outerNode.getChildren().get(0), SYSTEMS_COUNTER)
                .getTotals()
                .getSelfTotal())
                .isEqualTo(5L);
        }

        @Test
        void leavesACounterOffARowThatNeverTouchedIt() {
            // Absent rather than zero, so a wide capture's rows carry only what they can answer.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0));

            profiler.open(ProfileSection.registerSection(PARENT_SECTION)).close();

            assertThat(readRoots(profiler).get(0).getCounts())
                .isEmpty();
        }
    }
}
