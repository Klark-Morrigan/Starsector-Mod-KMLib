package kmlib.profiling.recording;

import kmlib.profiling.IterationScope;
import kmlib.profiling.PhasedSection;
import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileLevel;
import kmlib.profiling.ProfileOrigin;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.budget.ProfileBudget;
import kmlib.profiling.snapshot.BudgetBreach;
import kmlib.profiling.snapshot.CallCount;
import kmlib.profiling.snapshot.CountSpread;
import kmlib.profiling.snapshot.CountTotals;
import kmlib.profiling.snapshot.DurationBuckets;
import kmlib.profiling.snapshot.PhaseTotal;
import kmlib.profiling.snapshot.ProfileCount;
import kmlib.profiling.snapshot.ProfileIterations;
import kmlib.profiling.snapshot.ProfileNode;
import kmlib.profiling.snapshot.ProfileOriginTree;
import kmlib.profiling.snapshot.WorstCall;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Pins the accumulation contract of {@link RecordingProfiler}: a scope's span lands under whatever
 * was open when it opened, repeated calls on one row aggregate into count/total/min/max, a row's
 * self time is what it did not spend in its children, a count added on a scope rolls into every
 * scope it was open inside and spreads over the calls of its own row, the slowest call is kept with
 * the counters and the tag it ended on, every span is placed in a duration band, a scope opened over
 * a loop counts its turns and charges each step of one to its own slot, roots group by the origin
 * they were opened under with everything below a root belonging to it and everything opened under
 * no root at all landing in the reserved group, snapshot order follows first-record order, a scope
 * closed out of order takes the scopes inside it with it, a call that breaks what its section
 * allows marks the row, is reported once and takes the record however fast it was, a section
 * registered finer than the capture is keeping opens without the clock being read at all, and
 * reset() clears everything.
 */
final class RecordingProfilerTest {

    private static final String PARENT_SECTION = "test.parent";
    private static final String CHILD_SECTION = "test.child";
    private static final String OTHER_PARENT_SECTION = "test.otherParent";
    private static final String OUTER_SECTION = "test.outer";
    private static final String INNER_SECTION = "test.inner";
    private static final String SYSTEMS_COUNTER = "test.systems";
    private static final String MARKETS_COUNTER = "test.markets";

    // A section on a per-item path, which is what a capture reading whole frames skips.
    private static final String FINE_SECTION = "test.perItem";

    // Two games one profiler could be measuring across a session, which is the smallest capture
    // that has to keep its rows apart.
    private static final String FIRST_ORIGIN_LABEL = "test.firstSector";
    private static final String SECOND_ORIGIN_LABEL = "test.secondSector";
    private static final ProfileOrigin FIRST_ORIGIN =
        ProfileOrigin.registerOrigin(FIRST_ORIGIN_LABEL);
    private static final ProfileOrigin SECOND_ORIGIN =
        ProfileOrigin.registerOrigin(SECOND_ORIGIN_LABEL);

    // The two steps every loop below declares, which is the smallest pair that can show one step's
    // time landing in the other's slot.
    private static final String PLAN_PHASE_NAME = "plan";
    private static final String TRACE_PHASE_NAME = "trace";

    private static final String FIRST_CALL_TAG = "test.firstCall";
    private static final String SLOWEST_CALL_TAG = "test.slowestCall";
    private static final String LAST_CALL_TAG = "test.lastCall";

    // What a caller composing a tag out of what it happens to hold can end up handing over.
    private static final String BLANK_TAG = "   ";

    private static final long ONE_MICROSECOND_IN_NANOS = 1_000L;
    private static final long ONE_MILLISECOND_IN_NANOS = 1_000_000L;
    private static final long TEN_MILLISECONDS_IN_NANOS = 10_000_000L;

    // A bound of one, and the two a call breaks it with - the smallest pair that tells a call which
    // kept a rule from one which did not.
    private static final long ONE_ITEM = 1L;
    private static final long TWO_ITEMS = 2L;

    @Nested
    class Open {

        @Test
        void recordsASectionOpenedInsideAnotherAsItsChild() {
            // Parent 0->100 with the child 10->40 inside it.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10, 40, 100));

            var parent = profiler.open(ProfileSection.registerSection(PARENT_SECTION));
            var child = profiler.open(ProfileSection.registerSection(CHILD_SECTION));

            child.close();
            parent.close();

            assertThat(readSectionNames(readRoots(profiler)))
                .containsExactly(PARENT_SECTION);

            var parentNode = readRoots(profiler).get(0);

            assertThat(parentNode.getTiming().getTotalNanos())
                .isEqualTo(100);
            assertThat(readSectionNames(parentNode.getChildren()))
                .containsExactly(CHILD_SECTION);
            assertThat(parentNode.getChildren().get(0).getTiming().getTotalNanos())
                .isEqualTo(30);
        }

        @Test
        void keepsOneSectionOpenedUnderTwoParentsAsTwoRows() {
            // The same child under two parents: 5ns under the first, 9ns under the second. One row
            // averaging them would say a child cost 7 and name neither call.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 5, 5, 0, 0, 9, 9));

            var parent = profiler.open(ProfileSection.registerSection(PARENT_SECTION));
            var child = profiler.open(ProfileSection.registerSection(CHILD_SECTION));

            child.close();
            parent.close();

            var otherParent = profiler.open(ProfileSection.registerSection(OTHER_PARENT_SECTION));
            var otherChild = profiler.open(ProfileSection.registerSection(CHILD_SECTION));

            otherChild.close();
            otherParent.close();

            var roots = readRoots(profiler);

            assertThat(readSectionNames(roots))
                .containsExactly(PARENT_SECTION, OTHER_PARENT_SECTION);
            assertThat(roots.get(0).getChildren().get(0).getTiming().getTotalNanos())
                .isEqualTo(5);
            assertThat(roots.get(1).getChildren().get(0).getTiming().getTotalNanos())
                .isEqualTo(9);
        }

        @Test
        void reportsSelfTimeAsTheTotalLessWhatRanInsideIt() {
            // Parent 0->100 holding a child 10->40, so 30 of the parent's 100 was the child's.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10, 40, 100));

            var parent = profiler.open(ProfileSection.registerSection(PARENT_SECTION));
            var child = profiler.open(ProfileSection.registerSection(CHILD_SECTION));

            child.close();
            parent.close();

            var parentNode = readRoots(profiler).get(0);

            assertThat(parentNode.getSelfNanos())
                .isEqualTo(70);
            assertThat(parentNode.getChildren().get(0).getSelfNanos())
                .isEqualTo(30);
        }

        @Test
        void aggregatesRepeatedOpensOfOneSectionUnderOneParent() {
            // Two children under one parent: 0->2 then 2->6.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 2, 2, 6, 6));

            var parent = profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            profiler.open(ProfileSection.registerSection(CHILD_SECTION)).close();
            profiler.open(ProfileSection.registerSection(CHILD_SECTION)).close();
            parent.close();

            var childNode = readRoots(profiler).get(0).getChildren().get(0);

            assertThat(childNode.getTiming().getCount())
                .isEqualTo(2);
            assertThat(childNode.getTiming().getTotalNanos())
                .isEqualTo(6);
            assertThat(childNode.getTiming().getMinNanos())
                .isEqualTo(2);
            assertThat(childNode.getTiming().getMaxNanos())
                .isEqualTo(4);
        }

        @Test
        void landsANameMeasuredAndASectionOpenedOnOneRow() {
            // A section is an identity resolved from its name, so converting a measure() site to a
            // scope keeps its history rather than starting a second row spelled the same.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 5, 5, 12));

            profiler.measure(PARENT_SECTION, () -> {
            });
            profiler.open(ProfileSection.registerSection(PARENT_SECTION)).close();

            var roots = readRoots(profiler);

            assertThat(roots)
                .hasSize(1);
            assertThat(roots.get(0).getTiming().getCount())
                .isEqualTo(2);
            assertThat(roots.get(0).getTiming().getTotalNanos())
                .isEqualTo(12);
        }

        @Test
        void keepsTheSlowestCallAndGivesItUpOnlyToASlowerOne() {
            // What the maximum column cannot say on its own: which call it was. The record has to
            // be the call the maximum reports, so a faster call after it changes nothing.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 20, 20, 120, 120, 130));

            openTaggedCall(profiler, FIRST_CALL_TAG);
            openTaggedCall(profiler, SLOWEST_CALL_TAG);
            openTaggedCall(profiler, LAST_CALL_TAG);

            assertThat(readRoots(profiler).get(0).getWorstCall())
                .extracting(WorstCall::getDurationNanos, WorstCall::getTag)
                .containsExactly(100L, SLOWEST_CALL_TAG);
        }

        @Test
        void keepsTheCountersAsTheSlowestCallLeftThemRatherThanTheRowsTotals() {
            // A duration is read against the work that call did, so the record carries that call's
            // 8 systems - not the 11 the row has walked in all, which price nothing.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10, 10, 110));

            countInACallOf(profiler, PARENT_SECTION, 3);
            countInACallOf(profiler, PARENT_SECTION, 8);

            var worstCall = readRoots(profiler).get(0).getWorstCall();

            assertThat(worstCall.getCounts())
                .extracting(count -> count.getCounter().getName(), CallCount::getAmount)
                .containsExactly(tuple(SYSTEMS_COUNTER, 8L));
        }

        @Test
        void placesEachCallInTheDurationBandItsSpanFallsIn() {
            // Two calls a thousandfold apart land in two bands, which is the shape that tells a
            // section that stalled once from one that is always this slow.
            var profiler = new RecordingProfiler(
                new ScriptedClock(0, ONE_MICROSECOND_IN_NANOS, 0, ONE_MILLISECOND_IN_NANOS));

            profiler.open(ProfileSection.registerSection(PARENT_SECTION)).close();
            profiler.open(ProfileSection.registerSection(PARENT_SECTION)).close();

            var buckets = readRoots(profiler).get(0).getTiming().getBuckets();

            assertThat(buckets.getCallsInBucket(
                DurationBuckets.resolveBucketIndex(ONE_MICROSECOND_IN_NANOS)))
                .isEqualTo(1);
            assertThat(buckets.getCallsInBucket(
                DurationBuckets.resolveBucketIndex(ONE_MILLISECOND_IN_NANOS)))
                .isEqualTo(1);
        }

        @Test
        void landsASectionOpenedWithNoRootOpenUnderTheReservedOrigin() {
            // A walk from a path nobody profiled has to be seen: dropping it would leave the one
            // traversal a reader most wants to find as the only one the capture never mentions.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10));

            profiler.open(ProfileSection.registerSection(PARENT_SECTION)).close();

            assertThat(profiler.snapshot())
                .singleElement()
                .extracting(ProfileOriginTree::getOrigin)
                .isSameAs(ProfileOrigin.UNSCOPED);
        }

        @Test
        void marksAndReportsOnceARowWhoseCallReachedMoreThanItsSectionAllows() {
            // The finding a table cannot state on its own, and the reason it is a warning as well
            // as a row: a pass that walked the sector twice does it on every frame it runs, so the
            // second line would say nothing the first did not.
            var section = ProfileSection.registerSection(
                "test.budget.walksCounted",
                ProfileBudget.allowingCountPerCall(
                    ProfileCounter.registerCounter(SYSTEMS_COUNTER), ONE_ITEM));

            var profiler = new RecordingProfiler(new ScriptedClock(0, 10, 10, 20));
            var messages = captureLogWhile(() -> {
                countInACallOf(profiler, section.getName(), TWO_ITEMS);
                countInACallOf(profiler, section.getName(), TWO_ITEMS);
            });

            assertThat(readRoots(profiler).get(0).getBudgetBreach().describeBreach())
                .isEqualTo("2 " + SYSTEMS_COUNTER + ", 1 allowed per call");
            assertThat(messages)
                .hasSize(1);
            assertThat(messages.get(0))
                .contains(section.getName());
        }

        @Test
        void marksARowWhoseCallRanLongerThanItsSectionAllows() {

            var section = ProfileSection.registerSection(
                "test.budget.durationRun",
                ProfileBudget.allowingDurationPerCall(() -> ONE_MILLISECOND_IN_NANOS));

            var profiler = new RecordingProfiler(new ScriptedClock(0, TEN_MILLISECONDS_IN_NANOS));

            profiler.open(section).close();

            assertThat(readRoots(profiler).get(0).getBudgetBreach().describeBreach())
                .isEqualTo("1.00ms allowed per call");
        }

        @Test
        void marksNothingOnARowWhoseCallsStayedInsideItsBudget() {

            var section = ProfileSection.registerSection(
                "test.budget.inside",
                ProfileBudget.allowingDurationPerCall(() -> ONE_MILLISECOND_IN_NANOS));

            var profiler = new RecordingProfiler(new ScriptedClock(0, ONE_MICROSECOND_IN_NANOS));

            profiler.open(section).close();

            assertThat(readRoots(profiler).get(0).getBudgetBreach())
                .isSameAs(BudgetBreach.NO_BREACH);
        }

        @Test
        void keepsABreachingCallAsTheWorstOneHoweverFastItWas() {
            // What is worth looking at on a flagged row is the call that broke the bound, and a
            // walk too many can be over in microseconds while the calls that behaved took longer.
            var section = ProfileSection.registerSection(
                "test.budget.worstCall",
                ProfileBudget.allowingCountPerCall(
                    ProfileCounter.registerCounter(SYSTEMS_COUNTER), ONE_ITEM));

            // The breaching call runs 10ns, the well-behaved one after it 100ns.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10, 10, 110));

            countInACallOf(profiler, section.getName(), TWO_ITEMS);
            countInACallOf(profiler, section.getName(), ONE_ITEM);

            assertThat(readRoots(profiler).get(0).getWorstCall())
                .extracting(WorstCall::getDurationNanos)
                .isEqualTo(10L);
        }

        @Test
        void opensASectionFinerThanTheCaptureWithoutReadingTheClock() {
            // The whole of what the level buys: a section on a per-item path costs a comparison
            // under a capture taken to read whole frames. The clock is scripted with no readings
            // at all, so a reading here would fail the case rather than pass it quietly.
            var section = ProfileSection.registerSection(FINE_SECTION, ProfileLevel.FINE);
            var profiler = new RecordingProfiler(ProfileLevel.COARSE, new ScriptedClock());

            profiler.open(section).close();

            assertThat(profiler.snapshot())
                .isEmpty();
        }

        @Test
        void recordsASectionTheCaptureIsCoarseEnoughToKeep() {
            // The other side of the same comparison: a beat states no level, so it is timed by any
            // capture that is running at all.
            var profiler = new RecordingProfiler(ProfileLevel.COARSE, new ScriptedClock(0, 20));

            profiler.open(ProfileSection.registerSection(PARENT_SECTION)).close();

            assertThat(readRoots(profiler).get(0).getTiming().getTotalNanos())
                .isEqualTo(20);
        }

        @Test
        void keepsASectionOpenedInsideASkippedOneUnderTheRowAround() {
            // A skipped section leaves no row, so what ran inside it belongs to the call that is
            // actually being timed - the alternative being work that vanishes with the row.
            var skipped = ProfileSection.registerSection(
                "test.level.skippedParent",
                ProfileLevel.FINE);

            var profiler =
                new RecordingProfiler(ProfileLevel.COARSE, new ScriptedClock(0, 5, 20, 30));

            var beat = profiler.open(ProfileSection.registerSection(PARENT_SECTION));
            var perItem = profiler.open(skipped);

            profiler.open(ProfileSection.registerSection(CHILD_SECTION)).close();
            perItem.close();
            beat.close();

            assertThat(readSectionNames(readRoots(profiler).get(0).getChildren()))
                .containsExactly(CHILD_SECTION);
        }
    }

    @Nested
    class OpenRoot {

        @Test
        void keepsTwoOriginsAsTwoTrees() {
            // The same section measured in two games: one row averaging them would say a beat cost
            // the mean of two sectors' worth of work, which describes neither and cannot be taken
            // back to either save.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 5, 5, 12));

            profiler.openRoot(FIRST_ORIGIN, ProfileSection.registerSection(PARENT_SECTION)).close();
            profiler.openRoot(SECOND_ORIGIN, ProfileSection.registerSection(PARENT_SECTION)).close();

            var originTrees = profiler.snapshot();

            assertThat(originTrees)
                .extracting(originTree -> originTree.getOrigin().getLabel())
                .containsExactly(FIRST_ORIGIN_LABEL, SECOND_ORIGIN_LABEL);
            assertThat(originTrees.get(0).getRoots().get(0).getTiming().getTotalNanos())
                .isEqualTo(5);
            assertThat(originTrees.get(1).getRoots().get(0).getTiming().getTotalNanos())
                .isEqualTo(7);
        }

        @Test
        void landsWhatOpensInsideARootUnderThatRootsOrigin() {
            // Which is what lets a caller name the origin once a beat rather than at every section
            // beneath it - and what makes a layer's own scopes its sector's without the layer
            // knowing there are origins at all.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10, 40, 100));

            var beat =
                profiler.openRoot(FIRST_ORIGIN, ProfileSection.registerSection(PARENT_SECTION));
            var walk = profiler.open(ProfileSection.registerSection(CHILD_SECTION));

            walk.close();
            beat.close();

            var originTree = profiler.snapshot().get(0);

            assertThat(originTree.getOrigin())
                .isSameAs(FIRST_ORIGIN);
            assertThat(readSectionNames(originTree.getRoots().get(0).getChildren()))
                .containsExactly(CHILD_SECTION);
        }

        @Test
        void keepsARootOffWhateverHappenedToBeOpenWhenItWasOpened() {
            // A root has no parent whatever the stack looks like. Opened inside a scope somebody
            // left running, its counts would otherwise roll into a row that never ran it and could
            // not answer for them.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10, 40, 100));

            var strayScope = profiler.open(ProfileSection.registerSection(OUTER_SECTION));

            try (var beat =
                    profiler.openRoot(FIRST_ORIGIN, ProfileSection.registerSection(PARENT_SECTION))) {

                beat.addCount(ProfileCounter.registerCounter(SYSTEMS_COUNTER), 3);
            }
            strayScope.close();

            var originTrees = profiler.snapshot();

            assertThat(originTrees.get(0).getRoots().get(0).getCounts())
                .isEmpty();
            assertThat(readCount(originTrees.get(1).getRoots().get(0), SYSTEMS_COUNTER).getTotals())
                .extracting(CountTotals::getTotal)
                .isEqualTo(3L);
        }
    }

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

    @Nested
    class Record {

        @Test
        void recordsAPreMeasuredSpanUnderTheSectionThatIsOpen() {
            // A caller that timed a span itself is still somewhere in the tree, and the row it
            // lands on has to be the one an open()/close() pair would have used.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 50));

            var parent = profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            profiler.record(CHILD_SECTION, 20);
            parent.close();

            var parentNode = readRoots(profiler).get(0);

            assertThat(parentNode.getSelfNanos())
                .isEqualTo(30);
            assertThat(parentNode.getChildren().get(0).getTiming().getTotalNanos())
                .isEqualTo(20);
        }

        @Test
        void recordsAPreMeasuredSpanAsARootWhereNothingIsOpen() {
            // The clock is scripted with no readings at all, which is how "a span handed over
            // whole is never re-timed" is stated: a reading here would run the script out.
            var profiler = new RecordingProfiler(new ScriptedClock());

            profiler.record(PARENT_SECTION, 20);

            var roots = readRoots(profiler);

            assertThat(readSectionNames(roots))
                .containsExactly(PARENT_SECTION);
            assertThat(roots.get(0).getTiming().getTotalNanos())
                .isEqualTo(20);
        }

        @Test
        void marksARowWhoseHandedOverSpanBrokeItsSectionsBudget() {
            // A span the caller timed itself is a call of that section like any other, so the
            // bound holds over it too - a site that has not been converted to a scope is not a
            // site the budget stops applying to.
            var section = ProfileSection.registerSection(
                "test.budget.recordedSpan",
                ProfileBudget.allowingDurationPerCall(() -> ONE_MILLISECOND_IN_NANOS));

            var profiler = new RecordingProfiler(new ScriptedClock());

            profiler.record(section.getName(), TEN_MILLISECONDS_IN_NANOS);

            assertThat(readRoots(profiler).get(0).getBudgetBreach().describeBreach())
                .isEqualTo("1.00ms allowed per call");
        }

        @Test
        void dropsAHandedOverSpanOfASectionFinerThanTheCapture() {
            // A span the caller timed itself is a call of that section like any other, so a
            // capture not reading that section does not keep it either - a row appearing only for
            // the sites that hand a duration over would be read as the section having run there
            // and nowhere else.
            ProfileSection.registerSection(FINE_SECTION, ProfileLevel.FINE);

            var profiler = new RecordingProfiler(ProfileLevel.COARSE, new ScriptedClock());

            profiler.record(FINE_SECTION, 20);

            assertThat(profiler.snapshot())
                .isEmpty();
        }
    }

    @Nested
    class GetRecordedLevel {

        @Test
        void answersTheLevelItWasBuiltAt() {
            // What a caller choosing what to bind compares its knob against, so an unrelated
            // settings change does not throw the capture away and start a new one.
            assertThat(new RecordingProfiler(ProfileLevel.COARSE).getRecordedLevel())
                .isEqualTo(ProfileLevel.COARSE);
        }

        @Test
        void keepsEveryLevelWhereTheCallerNamedNone() {
            // A profiler asked for without a level keeps what it is handed: the level is a
            // reader's restriction, not a default the library imposes.
            assertThat(new RecordingProfiler().getRecordedLevel())
                .isEqualTo(ProfileLevel.FINE);
        }
    }

    @Nested
    class CloseScope {

        @Test
        void closesWhatWasStillOpenInsideTheScopeBeingClosed() {
            // Outer 0->50 with an inner opened at 10 and never closed: the inner ends where the
            // outer did, since nothing it timed can have outlived the call around it.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10, 50));

            var outer = profiler.open(ProfileSection.registerSection(OUTER_SECTION));

            profiler.open(ProfileSection.registerSection(INNER_SECTION));
            outer.close();

            var outerNode = readRoots(profiler).get(0);

            assertThat(outerNode.getTiming().getTotalNanos())
                .isEqualTo(50);
            assertThat(outerNode.getChildren().get(0).getTiming().getTotalNanos())
                .isEqualTo(40);
        }

        @Test
        void leavesTheNextSectionOpeningAsARootAfterAnUnwind() {
            // The repair that matters: with the abandoned scope still on the stack, the next
            // unrelated section would be filed under a call that had already returned.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 0, 0, 0));

            var outer = profiler.open(ProfileSection.registerSection(OUTER_SECTION));

            profiler.open(ProfileSection.registerSection(INNER_SECTION));
            outer.close();
            profiler.open(ProfileSection.registerSection(PARENT_SECTION)).close();

            assertThat(readSectionNames(readRoots(profiler)))
                .containsExactly(OUTER_SECTION, PARENT_SECTION);
        }

        @Test
        void warnsOncePerSectionLeftOpenHoweverOftenItHappens() {
            // These sites run every frame, so a line per occurrence would be the log rather than a
            // note in it - and the second says nothing the first did not.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 0, 0, 0, 0));
            var messages = captureLogWhile(() -> {
                closeAnOuterScopeWithAnInnerStillOpen(profiler);
                closeAnOuterScopeWithAnInnerStillOpen(profiler);
            });

            assertThat(messages)
                .hasSize(1);
            assertThat(messages.get(0))
                .contains(INNER_SECTION);
        }

        @Test
        void recordsNothingForASecondCloseOfTheSameScope() {
            // A caller that closes explicitly and by try-with-resources closes twice, and the
            // second close has no span behind it to count.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10));

            var scope = profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            scope.close();
            scope.close();

            assertThat(readRoots(profiler).get(0).getTiming().getCount())
                .isEqualTo(1);
        }
    }

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
            ProfileSection.registerSection(FINE_SECTION, ProfileLevel.FINE);

            var section = registerBakeSection(FINE_SECTION);
            var profiler = new RecordingProfiler(ProfileLevel.COARSE, new ScriptedClock());

            profiler.openIterations(section).close();

            assertThat(profiler.snapshot())
                .isEmpty();
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
                ProfileBudget.allowingDurationPerCall(() -> ONE_MILLISECOND_IN_NANOS));

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

    // What the profiler wrote while the work ran. The appender is removed afterwards whatever the
    // work did, so a failing case does not leave the logger collecting into a dead list.
    private static List<String> captureLogWhile(Runnable work) {

        var appenderFake = new LogAppenderFake();
        var logger = Logger.getLogger(RecordingProfiler.class);

        logger.addAppender(appenderFake);
        try {
            work.run();
        } finally {
            logger.removeAppender(appenderFake);
        }
        return appenderFake.getMessages();
    }

    // A section whose calls run a loop of two steps, which is what every case above opens over.
    // Named per case, since a registered section is one instance for the life of the JVM and two
    // cases sharing one would be two captures of the same row.
    private static PhasedSection registerBakeSection(String name) {
        return PhasedSection.registerPhasedSection(name, PLAN_PHASE_NAME, TRACE_PHASE_NAME);
    }

    // One turn of that loop, marking both its steps - the shape a per-item loop has, and four
    // clock readings.
    private static void runOneTurnOf(IterationScope loop, PhasedSection section, String tag) {

        loop.beginIteration(tag);
        loop.markPhase(section.resolvePhase(PLAN_PHASE_NAME));
        loop.markPhase(section.resolvePhase(TRACE_PHASE_NAME));
        loop.endIteration();
    }

    private static long readPhaseNanos(RecordingProfiler profiler, String phaseName) {
        return readRoots(profiler)
            .get(0)
            .getIterations()
            .getPhaseTotals()
            .stream()
            .filter(phaseTotal -> phaseTotal.getPhase().getName().equals(phaseName))
            .mapToLong(PhaseTotal::getTotalNanos)
            .findFirst()
            .orElseThrow();
    }

    // The roots of the one origin a case records under. Nearly every case here opens no root of
    // its own, so what it records lands under the reserved origin and the tree beneath that is
    // what the case is about; the grouping itself is pinned in Origins below.
    private static List<ProfileNode> readRoots(RecordingProfiler profiler) {

        var originTrees = profiler.snapshot();

        return originTrees.isEmpty() ? List.of() : originTrees.get(0).getRoots();
    }

    private static List<String> readSectionNames(List<ProfileNode> nodes) {
        var names = new ArrayList<String>();
        for (var node : nodes) {
            names.add(node.getSection().getName());
        }
        return names;
    }

    // One call of a section that counts what it was handed, which is the shape a walker under a
    // scope has: the count and the call it belongs to end together.
    private static void countInACallOf(
            RecordingProfiler profiler,
            String sectionName,
            long systems) {

        try (var call = profiler.open(ProfileSection.registerSection(sectionName))) {
            call.addCount(ProfileCounter.registerCounter(SYSTEMS_COUNTER), systems);
        }
    }

    // One call of the parent section named as the caller would name it - at the end, which is
    // where a path that only finds out what it was handed part way through can name it.
    private static void openTaggedCall(RecordingProfiler profiler, String tag) {
        try (var call = profiler.open(ProfileSection.registerSection(PARENT_SECTION))) {
            call.tagCall(tag);
        }
    }

    private static ProfileCount readCount(ProfileNode node, String counterName) {
        return node
            .getCounts()
            .stream()
            .filter(count -> count.getCounter().getName().equals(counterName))
            .findFirst()
            .orElseThrow();
    }

    private static void closeAnOuterScopeWithAnInnerStillOpen(RecordingProfiler profiler) {
        var outer = profiler.open(ProfileSection.registerSection(OUTER_SECTION));

        profiler.open(ProfileSection.registerSection(INNER_SECTION));
        outer.close();
    }

    // Returns the supplied values in order on successive calls - one reading per open and one per
    // close - so every span below is a literal rather than a wall-clock delta. A script shorter
    // than the readings a case takes fails that case, which is how "records without reading the
    // clock" is stated.
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

    // Collects what the profiler wrote to log4j, so "said once" can be counted rather than read.
    private static final class LogAppenderFake extends AppenderSkeleton {

        private final List<String> messages = new ArrayList<>();

        @Override
        public void close() {
            // Nothing is held open; the messages stay readable after the appender is removed.
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }

        @Override
        protected void append(LoggingEvent event) {
            messages.add(String.valueOf(event.getMessage()));
        }

        private List<String> getMessages() {
            return messages;
        }
    }
}
