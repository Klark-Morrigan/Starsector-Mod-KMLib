package kmlib.mods.nexerelin;

import com.fs.starfarer.api.Global;

import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.compatibility.ModIntegration;
import kmlib.starsector.factions.alliances.AllianceRecord;
import kmlib.starsector.factions.alliances.AllianceSource;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

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
    private final Supplier<ModIntegration> describeIntegration;
    private final CompatibilityFailures failureRecord;

    // Set once the read has failed, and never cleared: a failure to link is a fact about the jars
    // loaded, and recurs on every read for as long as the game runs. Volatile so a failure one
    // reader met is seen by every other, whichever thread each reads on.
    private volatile boolean isTakenOut;

    /**
     * @param allianceSource      the read being guarded
     * @param describeIntegration which mod the read is from and what the library loses without it,
     *                            composed only where the read has failed
     * @param failureRecord       where that failure is recorded
     */
    GuardedAllianceSource(
            AllianceSource allianceSource,
            Supplier<ModIntegration> describeIntegration,
            CompatibilityFailures failureRecord) {

        this.allianceSource = Objects.requireNonNull(
            allianceSource,
            "A guard over no read would have nothing to answer with.");
        this.describeIntegration = Objects.requireNonNull(
            describeIntegration,
            "A read from another mod must say which mod, or its failure can report nothing.");
        this.failureRecord = Objects.requireNonNull(
            failureRecord,
            "A guard with nowhere to record would degrade silently and tell no player why.");
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

            LOG.error("Reading alliances failed; none are read for the rest of the session", readFailure);
            reportReadFailure(readFailure);

            return List.of();
        }
    }

    // The report, inside a guard of its own. It runs where the read has already failed, on a read a
    // poll repeats every few seconds: a report that threw would replace the failure it was
    // reporting and take the read down with it. Guarded as widely as the read, the describer being
    // able to fail to link as well.
    private void reportReadFailure(Throwable readFailure) {

        try {
            describeIntegration
                .get()
                .recordFailure(failureRecord, WHILE_READING_ALLIANCES, readFailure);

        } catch (LinkageError | RuntimeException reportThrown) {

            LOG.error("Could not report the failed alliance read", reportThrown);
        }
    }
}
