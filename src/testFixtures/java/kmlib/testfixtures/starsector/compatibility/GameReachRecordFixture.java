package kmlib.testfixtures.starsector.compatibility;

import kmlib.starsector.compatibility.CompatibilityFailure;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.compatibility.GameReachReporter;

/**
 * A reporter for a reach into the game, filing into a record of its own, and that record read back.
 *
 * <p>A suite about a probe asks one thing of its reporting: did a failure reach the record, and
 * with what. A record of the case's own keeps that answer from depending on which suites ran
 * before, the session's record latching a consumer for the whole JVM.
 *
 * <p>The consumer is {@link CompatibilityFailureFixture#MAP_OVERLAY_CONSUMER}, which a case names
 * only where it is asserting who lost something.
 */
public final class GameReachRecordFixture {

    private final CompatibilityFailures failureRecord = new CompatibilityFailures();

    private final GameReachReporter reporter = new GameReachReporter(
        () -> CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER,
        failureRecord);

    /**
     * @return the reporter to hand the probe under test
     */
    public GameReachReporter getReporter() {
        return reporter;
    }

    /**
     * @return whether anything reached the record
     */
    public boolean hasReported() {
        return failureRecord.hasUnreported();
    }

    /**
     * @return the oldest failure the reporter filed, or null where it filed none
     */
    public CompatibilityFailure takeReportedFailure() {
        return CompatibilityFailureFixture.takeNextBindingFailure(failureRecord);
    }
}
