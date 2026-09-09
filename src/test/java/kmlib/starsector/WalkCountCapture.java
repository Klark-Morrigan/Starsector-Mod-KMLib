package kmlib.starsector;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.recording.RecordingProfiler;
import kmlib.profiling.snapshot.ProfileNode;
import kmlib.testfixtures.profiling.RecordedCapture;

import java.util.function.Consumer;

/**
 * Runs a sector read under a recording profiler and hands back what it counted, so a suite over
 * one reader states what that reader reports without opening a section by hand.
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

        return captureCountsWhile(profiler -> {
            try (var call = profiler.open(ProfileSection.registerSection(CALLER_SECTION))) {
                read.run();
            }
        });
    }

    /**
     * Runs {@code read} with no section open at all, as a path nobody profiled would.
     *
     * @param read the read to run
     * @return what the profiler's reserved row counted
     */
    public static WalkCounts captureUnscopedCountsOf(Runnable read) {

        return captureCountsWhile(profiler -> read.run());
    }

    // The capture both forms are: the read run against the profiler it was bound to, and the one
    // row the capture then holds.
    private static WalkCounts captureCountsWhile(Consumer<RecordingProfiler> read) {

        var profiler = new RecordingProfiler(() -> FIXED_CLOCK_NANOS);
        var originTrees = RecordedCapture
            .recordWhile(profiler, () -> read.accept(profiler))
            .getOriginTrees();

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
