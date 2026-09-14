package kmlib.logging;

import org.apache.log4j.Logger;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A diagnostic line reported only when what it says changes.
 *
 * <p>The shape it exists for is a reading that holds still for long stretches while the pass taking
 * it runs every frame. Repeated verbatim, such a line buries the one that moved - the only one worth
 * having.
 *
 * <p>Describing is the expensive half, a reading of this kind typically being a walk of a live tree,
 * so it is asked for only once the log is known to be taking it.
 *
 * <p>What counts as a change is the reading's own business, not this type's. A reading whose whole
 * text is the fact reports through {@link #createWholeLineTrace}; one whose text carries detail that
 * moves on its own hands back a {@link TracedLine} through {@link #createKeyedLineTrace} and says
 * there which half decides. Keying is not an optimisation here - a line that cannot hold still is a
 * line that reports continuously, which is the same as not reporting at all.
 *
 * <p>Holding the last line here rather than in the reader that produced it is what keeps the two
 * apart: a reader describes what it sees and says nothing about how often anyone wants to hear it.
 * The logger is handed in for the same reason, so a line answers to the verbosity of the feature it
 * diagnoses rather than to a category named after this type.
 */
public final class ChangedLineTrace {

    // What the line says and what decides whether it is news, asked per report rather than held,
    // the answer being a live read.
    private final Supplier<TracedLine> describeLine;

    // The key of the last line reported, or null before any. Compared rather than counted, so a
    // reading that returns to a value it held before is reported again - it moved twice.
    private String lastReportedKey;

    private final Logger log;

    // What the line is about, printed ahead of it so one log holds several of these apart.
    private final String subject;

    /**
     * Refuses a logger it was handed as null, rather than holding one and failing at the first line
     * it is asked for.
     *
     * <p>Not defensive noise: a holder of one of these is typically a static field, initialised
     * while its own class still is, so a logger declared below it in the same class arrives here as
     * null and nothing says so. Held, that surfaces as a null dereference on a later frame, inside a
     * render pass, several classes away from the declaration order that caused it. Refused, it
     * surfaces where it was made - at load, naming what was built without a logger.
     */
    private ChangedLineTrace(
            Logger log,
            String subject,
            Supplier<TracedLine> describeLine) {

        this.log = Objects.requireNonNull(log, "A trace with no logger could report nothing.");
        this.subject = subject;
        this.describeLine = describeLine;
    }

    /**
     * A trace over a reading whose whole text is the fact, so any difference in it is a change worth
     * reporting.
     *
     * @param log          the logger the line answers to, so it is governed by the verbosity of the
     *                     feature it diagnoses
     * @param subject      what the line is about, printed ahead of it
     * @param describeLine the live reading, answering null where there is nothing to report
     * @return a trace that reports whenever the described text differs from the last reported
     */
    public static ChangedLineTrace createWholeLineTrace(
            Logger log,
            String subject,
            Supplier<String> describeLine) {

        return new ChangedLineTrace(
            log,
            subject, () -> {
                var line = describeLine.get();
                return line == null ? null : TracedLine.createWholeLine(line);
            });
    }

    /**
     * A trace over a reading that says for itself which half of its text decides whether it is news.
     *
     * <p>For a reading whose text carries values that move without the fact behind them moving. Such
     * a reading reports on its key and prints its text, so the detail stays in the log without
     * costing a line every time it shifts.
     *
     * @param log          the logger the line answers to, so it is governed by the verbosity of the
     *                     feature it diagnoses
     * @param subject      what the line is about, printed ahead of it
     * @param describeLine the live reading, answering null where there is nothing to report
     * @return a trace that reports whenever the described key differs from the last reported
     */
    public static ChangedLineTrace createKeyedLineTrace(
            Logger log,
            String subject,
            Supplier<TracedLine> describeLine) {

        return new ChangedLineTrace(log, subject, describeLine);
    }

    /**
     * Reports this line, unless the log is above DEBUG, the reading has no line to give, or the line
     * is about what the last reported line was about.
     */
    public void traceWhenChanged() {

        if (!log.isDebugEnabled()) {
            return;
        }
        var line = describeLine.get();

        if (line == null || line.changeKey().equals(lastReportedKey)) {
            return;
        }
        lastReportedKey = line.changeKey();
        log.debug(subject + ": " + line.text());
    }
}
