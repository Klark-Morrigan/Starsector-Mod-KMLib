package kmlib.starsector.compatibility;

import com.fs.starfarer.api.Global;

import org.apache.log4j.Logger;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Files the failure of a binding to a third-party mod as that integration's report, from wherever
 * the binding was caught failing: a start-up step, or an adapter the step installed, failing when
 * first called.
 *
 * <p>Every such boundary does the same two things once it has caught a failure, and does them
 * where a failure has already happened. It composes the report from a describer - the wording read
 * out of strings.json and the version off the mod manager, neither of which belongs on a path where
 * nothing broke - and it must not throw doing so, since a boundary that threw while reporting would
 * replace the failure it was reporting. So the describer is held here with the record it files into,
 * and the filing is guarded here once.
 *
 * <p>It also decides where the failure's trace is logged. The report's own block carries the trace
 * of the failure that was recorded, so a boundary logs one line and leaves the trace to this. Only
 * where no block will carry it is it logged here: a report that could not be composed, and a
 * failure the record dropped because the same binding had already been reported - that one's block
 * carries the first failure's trace, not this one's.
 */
public final class IntegrationFailureReporter {

    private static final Logger LOG = Global.getLogger(IntegrationFailureReporter.class);

    // What a failure the record already holds a report for is logged as. Its trace is the one thing
    // about it the log would otherwise never see.
    private static final String ALREADY_REPORTED_MESSAGE =
        "Failed again after the integration was reported; failed while ";

    // What a failure whose report could not be composed is logged as, beside the reason it could not.
    private static final String REPORT_NOT_COMPOSED_MESSAGE =
        "Failed, and the report of it could not be composed; failed while ";
    private static final String REPORT_FAILURE_MESSAGE = "Could not compose the integration report.";

    private final Supplier<ModIntegration> describeIntegration;
    private final CompatibilityFailures failureRecord;
    private final Logger reportLog;

    /**
     * A reporter filing into the session's record and logging as the library.
     *
     * @param describeIntegration which mod the binding is with and what the binding mod loses without
     *                            it, composed only where the binding has failed
     */
    public IntegrationFailureReporter(Supplier<ModIntegration> describeIntegration) {

        this(describeIntegration, CompatibilityFailures.SESSION_RECORD, LOG);
    }

    /**
     * @param describeIntegration which mod the binding is with and what the binding mod loses without
     *                            it, composed only where the binding has failed
     * @param failureRecord       where the failure is filed
     * @param reportLog           where a trace no report carries is logged - the binding mod's own
     *                            logger where the binding is one of its start-up steps, so the line
     *                            sits under the switch that mod's player turns up
     */
    public IntegrationFailureReporter(
            Supplier<ModIntegration> describeIntegration,
            CompatibilityFailures failureRecord,
            Logger reportLog) {

        this.describeIntegration = Objects.requireNonNull(
            describeIntegration,
            "A binding to another mod must say which mod, or its failure can report nothing.");
        this.failureRecord = Objects.requireNonNull(
            failureRecord,
            "A reporter with nowhere to record would degrade silently and tell no player why.");
        this.reportLog = Objects.requireNonNull(
            reportLog,
            "A reporter with nowhere to log would lose the trace no report carries.");
    }

    /**
     * Files a failure of the binding as this integration's report, once per session, and never
     * throws.
     *
     * <p>Guarded as widely as the binding itself: the describer belongs to the binding mod and can
     * name a third-party type, so it can fail to link exactly as the binding did.
     *
     * @param failureSite        where the binding stopped holding, as the report's "failed while" row
     *                           takes it - spelled by the boundary that caught it, the only thing
     *                           that knows which boundary that was
     * @param integrationFailure what the binding threw, carried into the report as its cause
     */
    public void recordFailure(String failureSite, Throwable integrationFailure) {

        try {
            var isRecorded = describeIntegration
                .get()
                .recordFailure(failureRecord, failureSite, integrationFailure);

            if (!isRecorded) {
                reportLog.error(ALREADY_REPORTED_MESSAGE + failureSite, integrationFailure);
            }

        } catch (LinkageError | RuntimeException reportFailure) {

            reportLog.error(REPORT_NOT_COMPOSED_MESSAGE + failureSite, integrationFailure);
            reportLog.error(REPORT_FAILURE_MESSAGE, reportFailure);
        }
    }
}
