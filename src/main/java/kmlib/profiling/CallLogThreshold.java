package kmlib.profiling;

import kmlib.time.Timings;

/**
 * How slow one call of a section has to be before it says so in the game log as
 * it closes.
 *
 * <p>What this replaces is the hand-written timing line: a clock read either
 * side of a block, a duration formatted by hand, and the counts that duration
 * should be read against printed beside it in prose the profiler never sees. A
 * section that states a threshold gets that line from the scope it is already
 * opening, so the duration in the log and the duration in the report are one
 * measurement rather than two that can disagree.
 *
 * <p>Stated as a threshold rather than as a flag because the two readings a
 * caller wants are different: a pass that runs a handful of times per rebuild is
 * worth a line every time, while one that runs on a busy path is worth a line
 * only when it is slow enough to be the answer to somebody's question. Both are
 * this value, so a site moving between them changes a constant rather than the
 * shape of its code.
 */
public final class CallLogThreshold {

    // Nothing measurable exceeds this, so a section holding it never writes a
    // line - which is the answer for nearly every section, and is why the
    // "never" end is a threshold like the others rather than a case to branch
    // on at every close.
    private static final long NEVER_REACHED_NANOS = Long.MAX_VALUE;

    // Every finished call exceeds this, a span being at worst zero nanoseconds
    // long.
    private static final long ALWAYS_REACHED_NANOS = -1L;

    /**
     * The threshold a section states nothing with: its calls are read in the
     * report and say nothing in the log.
     */
    public static final CallLogThreshold NO_LOGGING =
        new CallLogThreshold(NEVER_REACHED_NANOS);

    /**
     * Every call writes its line, however fast it was.
     *
     * <p>What a rebuild step states: it runs when something changed rather than
     * on a clock, so each of its calls is an event a reader following a rebuild
     * through the log wants to see, and the fast ones are as much a part of that
     * trace as the slow ones.
     */
    public static final CallLogThreshold LOGGING_EVERY_CALL =
        new CallLogThreshold(ALWAYS_REACHED_NANOS);

    private final long thresholdNanos;

    private CallLogThreshold(long thresholdNanos) {
        this.thresholdNanos = thresholdNanos;
    }

    /**
     * A threshold in milliseconds, which is the unit a duration worth noticing
     * is stated in.
     *
     * @param millis how long a call has to take before it writes its line
     * @return the threshold to register a section with
     */
    public static CallLogThreshold loggingOverMillis(double millis) {
        return new CallLogThreshold(Timings.convertMillisToNanos(millis));
    }

    /**
     * @param elapsedNanos how long the call that has just closed took
     * @return whether that call is worth a line of its own
     */
    public boolean shouldLogCall(long elapsedNanos) {
        return elapsedNanos > thresholdNanos;
    }

    /**
     * Whether any call at all could write a line under this threshold - which
     * is what marks a section's calls as events rather than per-frame cost, and
     * so as worth reading the conditions of.
     *
     * @return false only for {@link #NO_LOGGING}
     */
    public boolean canLogAnyCall() {
        return thresholdNanos != NEVER_REACHED_NANOS;
    }
}
