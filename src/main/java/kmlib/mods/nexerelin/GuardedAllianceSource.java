package kmlib.mods.nexerelin;

import com.fs.starfarer.api.Global;

import kmlib.starsector.compatibility.IntegrationFailureReporter;
import kmlib.starsector.factions.alliances.AllianceRecord;
import kmlib.starsector.factions.alliances.AllianceSource;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.Objects;

/**
 * An alliance read that stops being asked once it has failed: it answers no alliances for the rest
 * of the session, and the failure is reported once.
 *
 * <p>The read reaches the mod's types when it is first asked rather than when the library loads, so
 * a release that moved what it reads is met on a read. Reads come from a poll every few seconds and
 * from every map rebuild, where a throw would recur on each. No alliances is what every install
 * without the mod reads, so a caller built for that case needs nothing further.
 *
 * <p>Whatever was thrown is treated alike. A read has no half-done work to protect, so there is
 * nothing here that turns on telling a link failure from a throw partway through.
 */
final class GuardedAllianceSource implements AllianceSource {

    // Where a read that failed is said to have failed, as the report's "failed while" row takes it.
    private static final String WHILE_READING_ALLIANCES = "reading the alliances standing";

    private static final Logger LOG = Global.getLogger(GuardedAllianceSource.class);

    private final AllianceSource allianceSource;
    private final IntegrationFailureReporter failureReporter;

    // Set once the read has failed, and never cleared: a failure to link is a fact about the jars
    // loaded, and recurs on every read for as long as the game runs. Volatile so a failure one
    // reader met is seen by every other, whichever thread each reads on.
    private volatile boolean isTakenOut;

    /**
     * @param allianceSource  the read being guarded
     * @param failureReporter what a failed read is reported through, under the integration the read
     *                        is with
     */
    GuardedAllianceSource(AllianceSource allianceSource, IntegrationFailureReporter failureReporter) {

        this.allianceSource = Objects.requireNonNull(
            allianceSource,
            "A guard over no read would have nothing to answer with.");
        this.failureReporter = Objects.requireNonNull(
            failureReporter,
            "A read from another mod must say how its failure is reported, or it can report nothing.");
    }

    @Override
    public List<AllianceRecord> readAlliances() {

        if (isTakenOut) {
            return List.of();
        }

        try {
            return allianceSource.readAlliances();

        } catch (LinkageError | RuntimeException readFailure) {

            isTakenOut = true;

            // One line naming the cause rather than its trace: the reporter hands the failure to the
            // report's own block, and logs the trace itself wherever no block will.
            LOG.error("Reading alliances failed; none are read for the rest of the session: " + readFailure);
            failureReporter.recordFailure(WHILE_READING_ALLIANCES, readFailure);

            return List.of();
        }
    }
}
