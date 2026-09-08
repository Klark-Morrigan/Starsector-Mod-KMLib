package kmlib.starsector;

import kmlib.profiling.ActiveProfiler;
import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.SilentProfiler;
import kmlib.profiling.recording.RecordingProfiler;
import kmlib.profiling.snapshot.ProfileNode;

/**
 * Runs a sector read under a recording profiler and hands back what it counted, so a suite over
 * one reader states what that reader reports without binding, opening and unbinding by hand.
 *
 * <p>The profiler holder is process-wide, so every capture leaves it silent again however the read
 * ended - a binding left behind would have the next suite counting into a tree nobody reads.
 */
public final class WalkCountCapture {

    // Nothing captured here is a duration, so one reading answers every clock read.
    private static final long FIXED_CLOCK_NANOS = 0L;

    // What stands in for the caller a shared read is charged to.
    private static final String CALLER_SECTION = "test.caller";

    private WalkCountCapture() {
        // utility class, no instances.
    }

    /**
     * Runs {@code read} inside one open section, as a caller that opened a section of its own
     * would.
     *
     * @param read the read to run
     * @return what the section it ran in counted
     */
    public static WalkCounts captureCountsOf(Runnable read) {

        var profiler = new RecordingProfiler(() -> FIXED_CLOCK_NANOS);

        ActiveProfiler.bindProfiler(profiler);

        try (var call = profiler.open(ProfileSection.registerSection(CALLER_SECTION))) {
            read.run();
        } finally {
            ActiveProfiler.bindProfiler(SilentProfiler.INSTANCE);
        }
        return readFirstRow(profiler);
    }

    /**
     * Runs {@code read} with no section open at all, as a path nobody profiled would.
     *
     * @param read the read to run
     * @return what the profiler's reserved row counted
     */
    public static WalkCounts captureUnscopedCountsOf(Runnable read) {

        var profiler = new RecordingProfiler(() -> FIXED_CLOCK_NANOS);

        ActiveProfiler.bindProfiler(profiler);

        try {
            read.run();
        } finally {
            ActiveProfiler.bindProfiler(SilentProfiler.INSTANCE);
        }
        return readFirstRow(profiler);
    }

    private static WalkCounts readFirstRow(RecordingProfiler profiler) {

        var originTrees = profiler.snapshot();

        return new WalkCounts(
            originTrees.isEmpty() ? null : originTrees.get(0).getRoots().get(0));
    }

    /**
     * What one row counted, read by counter.
     */
    public static final class WalkCounts {

        // Null where the read counted nothing at all, which leaves the capture with no row rather
        // than with an empty one.
        private final ProfileNode row;

        private WalkCounts(ProfileNode row) {
            this.row = row;
        }

        /**
         * @param counter the counter to read
         * @return what the row counted of it, and zero where it never touched it - which a row
         *         reports as an absent counter rather than as a zero
         */
        public long readCount(ProfileCounter counter) {

            if (row == null) {
                return 0L;
            }
            return row
                .getCounts()
                .stream()
                .filter(count -> count.getCounter() == counter)
                .mapToLong(count -> count.getTotals().getTotal())
                .findFirst()
                .orElse(0L);
        }

        /**
         * @param counter the counter to look for
         * @return whether the row carries it at all, which is what tells a counter nothing was
         *         added to from one an amount of zero was
         */
        public boolean hasCount(ProfileCounter counter) {

            return row != null
                && row.getCounts().stream().anyMatch(count -> count.getCounter() == counter);
        }

        /**
         * @return the section the counts were charged to, or null where the read counted nothing
         */
        public ProfileSection getSection() {
            return row == null ? null : row.getSection();
        }
    }
}
