package kmlib.profiling.report;

import kmlib.time.Timings;

import java.util.Locale;

/**
 * How a capture's numbers are written, shared by everything that writes one.
 *
 * <p>Here rather than beside each writer, because the same quantity written two
 * ways is a table a reader has to convert in their head: a duration in the
 * columns and a duration on the line under them are the same measurement, and
 * two decimal places apart they would not read as one.
 *
 * <p>Milliseconds for what a frame is judged in, microseconds for what one item
 * of a loop cost - a per-item cost that read in milliseconds would be a row
 * needing no further attention.
 */
final class ReportFormats {

    /** What a row that never touched a counter says: nothing, rather than a zero. */
    static final String ABSENT_CELL = "";

    static final String MILLIS_UNIT = "ms";

    // What one of something cost, whether the something is an item a row counted
    // or a turn of its loop. One spelling, since a reader meets both in one table
    // and two would read as two different measures.
    static final String PER_ITEM_UNIT = "us/ea";

    // Two spaces per level of nesting: enough for the eye to follow a row to its
    // parent, narrow enough that a deep tree still fits a console line.
    private static final int INDENT_SPACES_PER_DEPTH = 2;

    private static final String MILLIS_FORMAT = "%.3f";
    private static final String PER_ITEM_FORMAT = "%.1f";

    // A count divided by the frames it was spread over is a fraction, and the
    // interesting ones are small: a pass that walks the sector once every other
    // frame has to read as something other than zero.
    private static final String SCALED_COUNT_FORMAT = "%.2f";

    private ReportFormats() {
        // utility class, no instances.
    }

    static String formatMillis(double millis) {
        return String.format(Locale.ROOT, MILLIS_FORMAT, millis);
    }

    /**
     * What one of {@code items} cost, in microseconds.
     *
     * @param nanos what the whole of it took
     * @param items how many of them there were, which the caller has already
     *              found to be some
     * @return the cost of one
     */
    static String formatMicrosPerItem(long nanos, long items) {

        return String.format(
            Locale.ROOT, PER_ITEM_FORMAT, Timings.convertNanosToMicros(nanos) / items);
    }

    static String formatScaledCount(double amount) {
        return String.format(Locale.ROOT, SCALED_COUNT_FORMAT, amount);
    }

    static String indentToDepth(int depth) {
        return " ".repeat(depth * INDENT_SPACES_PER_DEPTH);
    }
}
