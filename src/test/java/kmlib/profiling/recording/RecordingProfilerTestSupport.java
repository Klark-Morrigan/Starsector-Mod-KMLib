package kmlib.profiling.recording;

import kmlib.profiling.CallLogThreshold;
import kmlib.profiling.IterationScope;
import kmlib.profiling.PhasedSection;
import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileOrigin;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.SectionTerms;
import kmlib.profiling.budget.ProfileBudget;
import kmlib.profiling.snapshot.PhaseTotal;
import kmlib.profiling.snapshot.ProfileCount;
import kmlib.profiling.snapshot.ProfileNode;
import kmlib.testfixtures.logging.LogAppenderFake;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * What every suite over {@link RecordingProfiler} shares: the section, counter, origin and tag
 * names the cases register under, the two terms several of them register a section on, a scripted
 * clock whose readings make every span a literal, and the reads that walk a capture back out of the
 * profiler.
 *
 * <p>One class rather than a copy per suite, since a name spelled differently in two suites would
 * be two rows in one JVM-wide registry.
 */
final class RecordingProfilerTestSupport {

    static final String PARENT_SECTION = "test.parent";
    static final String CHILD_SECTION = "test.child";
    static final String OTHER_PARENT_SECTION = "test.otherParent";
    static final String OUTER_SECTION = "test.outer";
    static final String INNER_SECTION = "test.inner";
    static final String SYSTEMS_COUNTER = "test.systems";
    static final String MARKETS_COUNTER = "test.markets";

    // A section on a per-item path, which is what a capture reading whole frames skips.
    static final String FINE_SECTION = "test.perItem";

    // Two games one profiler could be measuring across a session, which is the smallest capture
    // that has to keep its rows apart.
    static final String FIRST_ORIGIN_LABEL = "test.firstSector";
    static final String SECOND_ORIGIN_LABEL = "test.secondSector";
    static final ProfileOrigin FIRST_ORIGIN =
        ProfileOrigin.registerOrigin(FIRST_ORIGIN_LABEL);
    static final ProfileOrigin SECOND_ORIGIN =
        ProfileOrigin.registerOrigin(SECOND_ORIGIN_LABEL);

    // The two steps every loop below declares, which is the smallest pair that can show one step's
    // time landing in the other's slot.
    static final String PLAN_PHASE_NAME = "plan";
    static final String TRACE_PHASE_NAME = "trace";

    static final String FIRST_CALL_TAG = "test.firstCall";
    static final String SLOWEST_CALL_TAG = "test.slowestCall";
    static final String LAST_CALL_TAG = "test.lastCall";

    // What a caller composing a tag out of what it happens to hold can end up handing over.
    static final String BLANK_TAG = "   ";

    static final long ONE_MICROSECOND_IN_NANOS = 1_000L;
    static final long ONE_MILLISECOND_IN_NANOS = 1_000_000L;
    static final long TEN_MILLISECONDS_IN_NANOS = 10_000_000L;

    // The same bound stated as a section states one, which is in the unit a duration worth
    // noticing is written in.
    static final double ONE_MILLISECOND = 1.0;

    // A bound of one, and the two a call breaks it with - the smallest pair that tells a call which
    // kept a rule from one which did not.
    static final long ONE_ITEM = 1L;
    static final long TWO_ITEMS = 2L;

    // Two terms several cases register a section on, folded here so a registration fits on a line
    // and so the bound and the threshold each have one spelling.
    static final SectionTerms ONE_MILLISECOND_PER_CALL = SectionTerms.DEFAULT.withBudget(
        ProfileBudget.allowingDurationPerCall(() -> ONE_MILLISECOND_IN_NANOS));

    static final SectionTerms LOGGING_OVER_ONE_MILLISECOND = SectionTerms.DEFAULT
        .withCallLogThreshold(CallLogThreshold.loggingOverMillis(ONE_MILLISECOND));

    // What the profiler wrote while the work ran.
    static List<String> captureLogWhile(Runnable work) {
        return LogAppenderFake.captureLogOf(CaptureLog.class, work).getMessages();
    }

    // A section whose calls run a loop of two steps, which is what every case above opens over.
    // Named per case, since a registered section is one instance for the life of the JVM and two
    // cases sharing one would be two captures of the same row.
    static PhasedSection registerBakeSection(String name) {
        return PhasedSection.registerPhasedSection(name, PLAN_PHASE_NAME, TRACE_PHASE_NAME);
    }

    // One turn of that loop, marking both its steps - the shape a per-item loop has, and four
    // clock readings.
    static void runOneTurnOf(IterationScope loop, PhasedSection section, String tag) {

        loop.beginIteration(tag);
        loop.markPhase(section.resolvePhase(PLAN_PHASE_NAME));
        loop.markPhase(section.resolvePhase(TRACE_PHASE_NAME));
        loop.endIteration();
    }

    static long readPhaseNanos(RecordingProfiler profiler, String phaseName) {
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
    static List<ProfileNode> readRoots(RecordingProfiler profiler) {

        var originTrees = profiler.snapshot();

        return originTrees.isEmpty() ? List.of() : originTrees.get(0).getRoots();
    }

    static List<String> readSectionNames(List<ProfileNode> nodes) {
        var names = new ArrayList<String>();
        for (var node : nodes) {
            names.add(node.getSection().getName());
        }
        return names;
    }

    // One call of a section that counts what it was handed, which is the shape a walker under a
    // scope has: the count and the call it belongs to end together.
    static void countInACallOf(
            RecordingProfiler profiler,
            String sectionName,
            long systems) {

        try (var call = profiler.open(ProfileSection.registerSection(sectionName))) {
            call.addCount(ProfileCounter.registerCounter(SYSTEMS_COUNTER), systems);
        }
    }

    // One call of the parent section named as the caller would name it - at the end, which is
    // where a path that only finds out what it was handed part way through can name it.
    static void openTaggedCall(RecordingProfiler profiler, String tag) {
        try (var call = profiler.open(ProfileSection.registerSection(PARENT_SECTION))) {
            call.tagCall(tag);
        }
    }

    static ProfileCount readCount(ProfileNode node, String counterName) {
        return node
            .getCounts()
            .stream()
            .filter(count -> count.getCounter().getName().equals(counterName))
            .findFirst()
            .orElseThrow();
    }

    static void closeAnOuterWithAnInnerOpen(RecordingProfiler profiler) {
        var outer = profiler.open(ProfileSection.registerSection(OUTER_SECTION));

        profiler.open(ProfileSection.registerSection(INNER_SECTION));
        outer.close();
    }

    // Returns the supplied values in order on successive calls - one reading per open and one per
    // close - so every span below is a literal rather than a wall-clock delta. A script shorter
    // than the readings a case takes fails that case, which is how "records without reading the
    // clock" is stated.
    static final class ScriptedClock implements LongSupplier {

        private final long[] readings;
        private final AtomicLong index = new AtomicLong();

        ScriptedClock(long... readings) {
            this.readings = readings;
        }

        @Override
        public long getAsLong() {
            return readings[(int) index.getAndIncrement()];
        }
    }

    private RecordingProfilerTestSupport() {
    }
}
