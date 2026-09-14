package kmlib.profiling.snapshot;

/**
 * Under what conditions one call ran: whether it was its row's first, and how much
 * of the JVM's compilation happened while it was running.
 *
 * <p>What tells a cold call from a slow one. The first call of a pass in a session
 * runs on code the JIT has not compiled yet and lands in the same row as every
 * warm call after it, inflating the maximum, the kept call and the cost per item
 * - and nothing in a duration says which it was. A first-call bit alone does not
 * either: a capture cleared mid-session makes every call a first again, on a JVM
 * that is by then warm. The compilation time is the signal, the first-call bit
 * says why it moved, and the two together read as "cold", "first but warm", or
 * neither.
 *
 * <p>Measured for the sections whose calls are events - the ones that state a log
 * threshold - and left unmeasured for the rest, since reading the compilation
 * clock on a path that runs thirty thousand times a second is a cost the answer
 * is not worth there. The JVM's compilation clock is process-wide, so what is
 * counted is compilation of anything during the span rather than of this call's
 * own code: the question it answers is whether the JVM was busy compiling under
 * the call, which is the question a cold reading raises.
 *
 * @param isFirstCall whether this was the first call its row saw in the capture
 * @param jitMillis   how far the JVM's total compilation time advanced during the
 *                    call, or {@link #NOT_MEASURED_MILLIS} where it was not read
 */
public record CallWarmth(
    boolean isFirstCall,
    long jitMillis) {

    /**
     * What an unmeasured call carries in place of a compilation reading.
     */
    public static final long NOT_MEASURED_MILLIS = -1L;

    /**
     * A call whose conditions were not read: a section stating no threshold, or
     * a count that arrived with no scope open. Shared, since every such call
     * says the same nothing.
     */
    public static final CallWarmth UNMEASURED = new CallWarmth(false, NOT_MEASURED_MILLIS);

    private static final String FIRST_CALL_MARK = "first";
    private static final String JIT_LABEL = "jitMs=";
    private static final String PART_GAP = " ";

    /**
     * @return whether the compilation clock was read around this call at all
     */
    public boolean isMeasured() {
        return jitMillis != NOT_MEASURED_MILLIS;
    }

    /**
     * Whether these conditions are worth a word at all: the call was read, and
     * what was read of it was not the ordinary answer.
     *
     * <p>Asked rather than answered by building the sentence and finding it empty,
     * because a writer deciding whether a call has earned a line of its own asks
     * this of every row it writes, and nearly every one of them says no.
     *
     * @return whether {@link #describeWarmth()} would say anything
     */
    public boolean hasAnythingToSay() {
        return isMeasured() && (isFirstCall || jitMillis > 0);
    }

    /**
     * What a line about the call says of its conditions, which is nothing unless
     * there is something to say: an unmeasured call, and a measured one that was
     * neither first nor compiled under, both read as the plain call they were.
     *
     * @return the parts worth writing, space-separated, or an empty string
     */
    public String describeWarmth() {

        if (!hasAnythingToSay()) {
            return "";
        }
        var description = new StringBuilder();

        if (isFirstCall) {
            description.append(FIRST_CALL_MARK);
        }
        if (jitMillis > 0) {
            if (description.length() > 0) {
                description.append(PART_GAP);
            }
            description.append(JIT_LABEL).append(jitMillis);
        }
        return description.toString();
    }
}
