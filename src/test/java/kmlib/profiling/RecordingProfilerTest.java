package kmlib.profiling;

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

/**
 * Pins the accumulation contract of {@link RecordingProfiler}: a scope's span lands under whatever
 * was open when it opened, repeated calls on one row aggregate into count/total/min/max, a row's
 * self time is what it did not spend in its children, snapshot order follows first-record order,
 * a scope closed out of order takes the scopes inside it with it, and reset() clears everything.
 */
final class RecordingProfilerTest {

    private static final String PARENT_SECTION = "test.parent";
    private static final String CHILD_SECTION = "test.child";
    private static final String OTHER_PARENT_SECTION = "test.otherParent";
    private static final String OUTER_SECTION = "test.outer";
    private static final String INNER_SECTION = "test.inner";

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

            assertThat(readSectionNames(profiler.snapshot()))
                .containsExactly(PARENT_SECTION);

            var parentNode = profiler.snapshot().get(0);

            assertThat(parentNode.getTotalNanos())
                .isEqualTo(100);
            assertThat(readSectionNames(parentNode.getChildren()))
                .containsExactly(CHILD_SECTION);
            assertThat(parentNode.getChildren().get(0).getTotalNanos())
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

            var roots = profiler.snapshot();

            assertThat(readSectionNames(roots))
                .containsExactly(PARENT_SECTION, OTHER_PARENT_SECTION);
            assertThat(roots.get(0).getChildren().get(0).getTotalNanos())
                .isEqualTo(5);
            assertThat(roots.get(1).getChildren().get(0).getTotalNanos())
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

            var parentNode = profiler.snapshot().get(0);

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

            var childNode = profiler.snapshot().get(0).getChildren().get(0);

            assertThat(childNode.getCount())
                .isEqualTo(2);
            assertThat(childNode.getTotalNanos())
                .isEqualTo(6);
            assertThat(childNode.getMinNanos())
                .isEqualTo(2);
            assertThat(childNode.getMaxNanos())
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

            var roots = profiler.snapshot();

            assertThat(roots)
                .hasSize(1);
            assertThat(roots.get(0).getCount())
                .isEqualTo(2);
            assertThat(roots.get(0).getTotalNanos())
                .isEqualTo(12);
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

            var timing = profiler.snapshot().get(0);

            assertThat(timing.getSection().getName())
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

        @Test
        void nestsAMeasuredBlockUnderTheSectionThatIsOpen() {
            // What lets a call site keep its measure() and still be read as part of the scope it
            // runs in, so converting one to a scope is a change of spelling and not of the tree.
            var profiler = new RecordingProfiler(new ScriptedClock(0, 10, 30, 90));

            var parent = profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            profiler.measure(CHILD_SECTION, () -> {
            });
            parent.close();

            var parentNode = profiler.snapshot().get(0);

            assertThat(readSectionNames(parentNode.getChildren()))
                .containsExactly(CHILD_SECTION);
            assertThat(parentNode.getChildren().get(0).getTotalNanos())
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

            var parentNode = profiler.snapshot().get(0);

            assertThat(parentNode.getSelfNanos())
                .isEqualTo(30);
            assertThat(parentNode.getChildren().get(0).getTotalNanos())
                .isEqualTo(20);
        }

        @Test
        void recordsAPreMeasuredSpanAsARootWhereNothingIsOpen() {
            // The clock is scripted with no readings at all, which is how "a span handed over
            // whole is never re-timed" is stated: a reading here would run the script out.
            var profiler = new RecordingProfiler(new ScriptedClock());

            profiler.record(PARENT_SECTION, 20);

            var roots = profiler.snapshot();

            assertThat(readSectionNames(roots))
                .containsExactly(PARENT_SECTION);
            assertThat(roots.get(0).getTotalNanos())
                .isEqualTo(20);
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

            var outerNode = profiler.snapshot().get(0);

            assertThat(outerNode.getTotalNanos())
                .isEqualTo(50);
            assertThat(outerNode.getChildren().get(0).getTotalNanos())
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

            assertThat(readSectionNames(profiler.snapshot()))
                .containsExactly(OUTER_SECTION, PARENT_SECTION);
        }

        @Test
        void warnsOncePerSectionLeftOpenHoweverOftenItHappens() {
            // These sites run every frame, so a line per occurrence would be the log rather than a
            // note in it - and the second says nothing the first did not.
            var appenderFake = new LogAppenderFake();
            var logger = Logger.getLogger(RecordingProfiler.class);
            var profiler = new RecordingProfiler(new ScriptedClock(0, 0, 0, 0, 0, 0));

            logger.addAppender(appenderFake);
            try {
                closeAnOuterScopeWithAnInnerStillOpen(profiler);
                closeAnOuterScopeWithAnInnerStillOpen(profiler);
            } finally {
                logger.removeAppender(appenderFake);
            }

            assertThat(appenderFake.getMessages())
                .hasSize(1);
            assertThat(appenderFake.getMessages().get(0))
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

            assertThat(profiler.snapshot().get(0).getCount())
                .isEqualTo(1);
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

            assertThat(readSectionNames(profiler.snapshot()))
                .containsExactly("second", "first");
        }

        @Test
        void reportsASectionStillOpenAsARowWithNoSpanYet() {
            // A readout asked for mid-frame has to show the section that is running, and show it
            // holding nothing - the row a span has not reached yet is zeroes, not a bound.
            var profiler = new RecordingProfiler(new ScriptedClock(0));

            profiler.open(ProfileSection.registerSection(PARENT_SECTION));

            var openNode = profiler.snapshot().get(0);

            assertThat(openNode.getCount())
                .isEqualTo(0);
            assertThat(openNode.getMinNanos())
                .isEqualTo(0);
            assertThat(openNode.getMaxNanos())
                .isEqualTo(0);
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

    private static List<String> readSectionNames(List<ProfileNode> nodes) {
        var names = new ArrayList<String>();
        for (var node : nodes) {
            names.add(node.getSection().getName());
        }
        return names;
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
