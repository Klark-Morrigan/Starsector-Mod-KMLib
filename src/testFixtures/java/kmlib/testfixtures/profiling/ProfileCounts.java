package kmlib.testfixtures.profiling;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.snapshot.ProfileNode;

/**
 * What one row of a capture counted, read the way a suite asserting on a number wants it.
 *
 * <p>A row carries a counter it never touched as an absent count rather than as a nought, which is
 * the right answer for a report - "does not count this" and "counted none" are different facts, and
 * a reader printing a column for the first would push every other row along. A suite naming an
 * expected amount wants the second reading: a walk that stopped happening then fails as the number
 * it is, rather than as a null dereference several frames from the claim.
 *
 * <p>Shipped from KMLib so the mods reading their own passes through {@link RecordedCapture} state
 * that once. Every suite that wrote this for itself wrote the same four lines.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state.
 */
public final class ProfileCounts {

    // What a row that never touched a counter counted of it.
    private static final long NOTHING_COUNTED = 0L;

    private ProfileCounts() {
        // utility class, no instances.
    }

    /**
     * @param node    the row to read
     * @param counter the counter asked about
     * @return whether the row carries that counter at all, which is what tells a counter nothing
     *         was ever added to from one an amount of nought was
     */
    public static boolean hasCountOf(ProfileNode node, ProfileCounter counter) {
        return node.findCount(counter) != null;
    }

    /**
     * @param node    the row to read
     * @param counter the counter asked about
     * @return everything counted under that row, its own adds and its children's alike, and nought
     *         where nothing under it touched the counter
     */
    public static long readTotalOf(ProfileNode node, ProfileCounter counter) {

        var count = node.findCount(counter);

        return count == null ? NOTHING_COUNTED : count.getTotals().getTotal();
    }
}
