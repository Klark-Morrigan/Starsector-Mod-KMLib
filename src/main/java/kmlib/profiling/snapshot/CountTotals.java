package kmlib.profiling.snapshot;

/**
 * How much of one counter a row has accumulated: everything counted under it,
 * and how much of that it counted with its own hands.
 *
 * <p>The two are what the whole counting rule turns on. The total is inclusive,
 * by the same rule a row's time is, so a rebuild's row states how many walks
 * happened beneath it whoever performed them. The self total is what may be
 * divided into the row's self time - the items its children handled are their
 * rows' to price.
 *
 * <p>Grouped rather than passed as two loose longs, because a pair of numbers
 * of the same type that differ only in which one is inclusive is a pair a
 * caller can hand over the wrong way round, and the result reads as a row that
 * did all its own counting.
 */
public final class CountTotals {

    private final long total;
    private final long selfTotal;

    public CountTotals(long total, long selfTotal) {
        this.total = total;
        this.selfTotal = selfTotal;
    }

    /**
     * @return everything counted under this row, its own adds and its
     *         children's alike
     */
    public long getTotal() {
        return total;
    }

    /**
     * @return what this row counted itself, which is what its self time is
     *         spread over
     */
    public long getSelfTotal() {
        return selfTotal;
    }
}
