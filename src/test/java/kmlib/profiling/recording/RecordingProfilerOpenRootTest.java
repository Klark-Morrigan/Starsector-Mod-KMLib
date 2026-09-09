package kmlib.profiling.recording;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileLevel;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.SectionTerms;
import kmlib.profiling.recording.RecordingProfilerTestSupport.ScriptedClock;
import kmlib.profiling.snapshot.CountTotals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.profiling.recording.RecordingProfilerTestSupport.CHILD_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.FINE_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.FIRST_ORIGIN;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.FIRST_ORIGIN_LABEL;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.OUTER_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.PARENT_SECTION;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.SECOND_ORIGIN;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.SECOND_ORIGIN_LABEL;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.SYSTEMS_COUNTER;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readCount;
import static kmlib.profiling.recording.RecordingProfilerTestSupport.readSectionNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins, of {@link RecordingProfiler}, that roots group by the origin they were opened under,
 * everything below a root belongs to it, and everything opened under no root at all lands in the
 * reserved group.
 */
final class RecordingProfilerOpenRootTest {

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

        @Test
        void opensARootFinerThanTheCaptureSilentlyAndNamesNoOrigin() {
            // A root is gated like any other section, and the origin goes with the span it would
            // have grouped: an origin standing in the snapshot with no row under it would read as
            // a game that was measured and found to do nothing.
            var section = ProfileSection.registerSection(
                FINE_SECTION, SectionTerms.DEFAULT.withLevel(ProfileLevel.FINE));
            var profiler = new RecordingProfiler(ProfileLevel.COARSE, new ScriptedClock());

            profiler.openRoot(FIRST_ORIGIN, section).close();

            assertThat(profiler.snapshot())
                .isEmpty();
        }
    }
}
