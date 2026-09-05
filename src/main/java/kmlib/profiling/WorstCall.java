package kmlib.profiling;

import java.util.List;

/**
 * The slowest call a row has seen, kept with what it was doing: how long it
 * took, what its counters stood at when it ended, and the tag its caller named
 * it by.
 *
 * <p>A maximum on its own says a call was slow and nothing about which one -
 * which sector it ran in, why the refresh was asked for, how many cells were in
 * hand. That is the fact which would say what to optimise, so it is kept beside
 * the duration rather than dropped and looked for in a log afterwards.
 *
 * <p>Replaced only by a slower call, so what is held here is always the call
 * the row's maximum reports.
 */
public final class WorstCall {

    /**
     * What a call whose caller named nothing carries. The record is still kept
     * - a duration and its counters say plenty on their own - and a reader is
     * told there was no name rather than shown an empty one.
     */
    public static final String NO_TAG = "";

    /**
     * The record of a row no call has finished on - a section a snapshot caught
     * mid-call, or one left open. It holds the zeroes the row's timing holds,
     * by the same rule: a row a span has not reached yet reads as nothing
     * rather than as a bound nobody set. Shared, since every such row says the
     * same nothing.
     */
    public static final WorstCall NO_CALL = new WorstCall(0, NO_TAG, List.of());

    private final long durationNanos;
    private final String tag;
    private final List<CallCount> counts;

    public WorstCall(long durationNanos, String tag, List<CallCount> counts) {
        this.durationNanos = durationNanos;
        this.tag = tag;
        this.counts = List.copyOf(counts);
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    /**
     * @return what the caller named this call, or {@link #NO_TAG} where it
     *         named nothing
     */
    public String getTag() {
        return tag;
    }

    /**
     * @return what each counter the call touched stood at when it ended, in the
     *         order the call first added to them; empty where it counted
     *         nothing
     */
    public List<CallCount> getCounts() {
        return counts;
    }
}
