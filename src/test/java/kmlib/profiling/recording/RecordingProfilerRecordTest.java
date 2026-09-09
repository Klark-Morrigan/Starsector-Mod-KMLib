package kmlib.profiling.recording;

import kmlib.profiling.ProfileLevel;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.SectionTerms;
import kmlib.profiling.recording.RecordingProfilerTestSupport.ScriptedClock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.profiling.recording.RecordingProfilerTestSupport.CHILD_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.FINE_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.ONE_MILLISECOND_PER_CALL;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.PARENT_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.TEN_MILLISECONDS_IN_NANOS;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readRoots;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readSectionNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins, of {@link RecordingProfiler}, that a span the caller timed itself lands on the named row
 * under whatever is open, breaks a bound like a scope's span would, and is dropped under a capture
 * too coarse for its section.
 */
final class RecordingProfilerRecordTest {

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
                ONE_MILLISECOND_PER_CALL);

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
            ProfileSection.registerSection(
                FINE_SECTION, SectionTerms.DEFAULT.withLevel(ProfileLevel.FINE));

            var profiler = new RecordingProfiler(ProfileLevel.COARSE, new ScriptedClock());

            profiler.record(FINE_SECTION, 20);

            assertThat(profiler.snapshot())
                .isEmpty();
        }
    }
}
