package kmlib.profiling.recording;

import kmlib.profiling.CallLogThreshold;
import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.SectionTerms;
import kmlib.profiling.recording.RecordingProfilerTestSupport.ScriptedClock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.profiling.recording.RecordingProfilerTestSupport.FIRST_CALL_TAG;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.INNER_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.LOGGING_OVER_ONE_MILLISECOND;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.MARKETS_COUNTER;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.ONE_MICROSECOND_IN_NANOS;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.ONE_MILLISECOND_IN_NANOS;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.OUTER_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.PARENT_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.SYSTEMS_COUNTER;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.TEN_MILLISECONDS_IN_NANOS;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.TWO_ITEMS;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.captureLogWhile;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.closeAnOuterWithAnInnerOpen;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readRoots;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readSectionNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins, of {@link RecordingProfiler}, that a scope closed out of order takes the scopes inside it
 * with it, a second close records nothing, and a call of a section stating a threshold it ran over
 * writes one line carrying its counts and its tag.
 */
final class RecordingProfilerCloseScopeTest {

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
                closeAnOuterWithAnInnerOpen(profiler);
                closeAnOuterWithAnInnerOpen(profiler);
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

        @Test
        void writesTheClosingLineOfACallOverItsSectionsThreshold() {
            // The line the site used to write by hand, off the span the row was accumulated from
            // rather than off a second clock read beside it.
            var section = ProfileSection.registerSection(
                "test.closeLine.overThreshold",
                LOGGING_OVER_ONE_MILLISECOND);

            var profiler = new RecordingProfiler(
                new ScriptedClock(0, TEN_MILLISECONDS_IN_NANOS));

            var messages = captureLogWhile(() -> profiler.open(section).close());

            assertThat(messages)
                .hasSize(1);
            assertThat(messages.get(0))
                .contains("test.closeLine.overThreshold")
                .contains("10.00ms");
        }

        @Test
        void keepsACallUnderItsSectionsThresholdOutOfTheLog() {
            // The whole point of a threshold: a pass that ran as it should says nothing, so what
            // is in the log is what somebody has to look at.
            var section = ProfileSection.registerSection(
                "test.closeLine.underThreshold",
                LOGGING_OVER_ONE_MILLISECOND);

            var profiler = new RecordingProfiler(new ScriptedClock(0, ONE_MICROSECOND_IN_NANOS));

            assertThat(captureLogWhile(() -> profiler.open(section).close()))
                .isEmpty();
        }

        @Test
        void writesNothingForASectionThatStatedNoThreshold() {

            var profiler = new RecordingProfiler(
                new ScriptedClock(0, TEN_MILLISECONDS_IN_NANOS));

            assertThat(captureLogWhile(() ->
                    profiler.open(ProfileSection.registerSection(PARENT_SECTION)).close()))
                .isEmpty();
        }

        @Test
        void carriesTheCallsCountsAndTagOnTheClosingLine() {
            // A duration on its own cannot be judged, which is why these lines existed at all:
            // the counts the site used to print beside its "took=" come off the scope instead.
            var section = ProfileSection.registerSection(
                "test.closeLine.counted",
                SectionTerms.DEFAULT.withCallLogThreshold(CallLogThreshold.LOGGING_EVERY_CALL));

            var profiler = new RecordingProfiler(new ScriptedClock(0, ONE_MILLISECOND_IN_NANOS));

            var messages = captureLogWhile(() -> {
                var scope = profiler.open(section);

                scope.addCount(ProfileCounter.registerCounter(SYSTEMS_COUNTER), TWO_ITEMS);
                scope.tagCall(FIRST_CALL_TAG);
                scope.close();
            });

            assertThat(messages.get(0))
                .contains(SYSTEMS_COUNTER + "=" + TWO_ITEMS)
                .contains('"' + FIRST_CALL_TAG + '"');
        }

        @Test
        void countsWhatWasCountedInsideTheCallOnItsClosingLine() {
            // The inclusive amount, which is the quantity the call's own duration covered - a
            // pass that walked the sector through a collaborator still walked it.
            var outerSection = ProfileSection.registerSection(
                "test.closeLine.inclusive",
                SectionTerms.DEFAULT.withCallLogThreshold(CallLogThreshold.LOGGING_EVERY_CALL));

            var profiler = new RecordingProfiler(
                new ScriptedClock(0, 0, 0, ONE_MILLISECOND_IN_NANOS));

            var messages = captureLogWhile(() -> {
                var outer = profiler.open(outerSection);
                var inner = profiler.open(ProfileSection.registerSection(INNER_SECTION));

                inner.addCount(ProfileCounter.registerCounter(MARKETS_COUNTER), TWO_ITEMS);
                inner.close();
                outer.close();
            });

            assertThat(messages)
                .hasSize(1);
            assertThat(messages.get(0))
                .contains(MARKETS_COUNTER + "=" + TWO_ITEMS);
        }
    }
}
