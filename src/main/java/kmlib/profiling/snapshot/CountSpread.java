package kmlib.profiling.snapshot;

/**
 * The least and the most one call of a row counted of one counter.
 *
 * <p>What a total cannot say: eleven systems over two calls is a row that walks
 * five or six each time, or one that walked three and then eight, and only the
 * second says which call to look at.
 *
 * <p>Over every call of the row, calls that counted none of it included - so a
 * row that usually walks once and occasionally not at all reads as a minimum of
 * zero rather than as one.
 *
 * <p>Grouped rather than passed as two loose longs, for the reason a bound
 * always is: a minimum and a maximum of the same type sit next to each other at
 * a call site, and handed over the wrong way round they still compile.
 */
public final class CountSpread {

    private final long minPerCall;
    private final long maxPerCall;

    public CountSpread(long minPerCall, long maxPerCall) {
        this.minPerCall = minPerCall;
        this.maxPerCall = maxPerCall;
    }

    /**
     * @return the least this row counted in any one call
     */
    public long getMinPerCall() {
        return minPerCall;
    }

    /**
     * @return the most this row counted in any one call
     */
    public long getMaxPerCall() {
        return maxPerCall;
    }
}
