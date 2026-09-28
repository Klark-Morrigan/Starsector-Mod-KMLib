package kmlib.starsector.compatibility;

import com.fs.starfarer.api.Global;

import kmlib.starsector.settings.modmanager.InstalledMods;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Files a reach into the game's own code that stopped holding as a compatibility report, with the
 * game itself as the third party: the version the consuming mod declares it was made for against
 * the version running.
 *
 * <p>A reach into the game's widgets or concrete classes degrades softly wherever it is made, and
 * until it reports here it degrades only to the log - the silent loss the channel exists to end.
 * The launcher does not stand in for it: a game release differing in anything but its major is a
 * warning there, and the mod runs.
 *
 * <p>Held by the probe making the reach and handed in by the mod that loses something by it, so a
 * report names that mod and says what it lost in that mod's own words. The consumer arrives as a
 * describer rather than a value, composed only on a failure: its sentences are read out of
 * strings.json, which has no place on the per-frame path of a reach that holds.
 *
 * <p>One report per reporter. A reporter stands for one consumer, and the record latches a consumer
 * once per subject, so every record after the first would be dropped there anyway - latched here
 * first, a reach failing on every frame costs one flag read rather than a describer call per frame.
 *
 * <p>Recorded and not raised on screen. The screen notice stands in the same widget tree these
 * reaches walk, and this sits below the interface packages that would raise it, so a report waits
 * for the campaign's dialog.
 */
public final class GameReachReporter {

    /**
     * The key a failed reach into the game is latched under, spelled once so every probe files
     * under one subject.
     */
    public static final String COMPATIBILITY_SUBJECT_KEY = "starsector";

    /** The game as a report names it. */
    public static final String COMPATIBILITY_SUBJECT_NAME = "Starsector";

    /**
     * A reporter that files nothing, for a reach whose loss no player would see: a diagnostic trace,
     * or a surface with a fallback of its own.
     */
    public static final GameReachReporter UNREPORTED = new GameReachReporter();

    // What a failure whose report could not be composed is logged as. The probe's own warning
    // already carries the failure, so what is left to say is why no report follows it.
    private static final String REPORT_NOT_COMPOSED_MESSAGE =
        "A failed reach into the game could not be reported; failed while ";

    // Null for the reporter that files nothing, whose latch is taken from the start so this is never
    // read.
    private final Supplier<CompatibilityConsumer> describeConsumer;

    private final CompatibilityFailures failureRecord;

    // Taken by the first failure, whether or not its report was composed.
    private final AtomicBoolean hasReported;

    /**
     * A reporter filing into the session's record.
     *
     * @param describeConsumer the mod that loses something where the reach fails, and what it
     *                         loses, composed only once it has failed
     */
    public GameReachReporter(Supplier<CompatibilityConsumer> describeConsumer) {

        this(describeConsumer, CompatibilityFailures.SESSION_RECORD);
    }

    /**
     * @param describeConsumer the mod that loses something where the reach fails, and what it
     *                         loses, composed only once it has failed
     * @param failureRecord    where the failure is filed
     */
    public GameReachReporter(
            Supplier<CompatibilityConsumer> describeConsumer,
            CompatibilityFailures failureRecord) {

        this.describeConsumer = Objects.requireNonNull(
            describeConsumer,
            "A reach reported for no consumer could not say whose feature it cost.");

        this.failureRecord = Objects.requireNonNull(
            failureRecord,
            "A reporter with nowhere to record would degrade silently and tell no player why.");

        this.hasReported = new AtomicBoolean();
    }

    private GameReachReporter() {

        this.describeConsumer = null;
        this.failureRecord = CompatibilityFailures.SESSION_RECORD;
        this.hasReported = new AtomicBoolean(true);
    }

    /**
     * Files a reach that failed by throwing, once per reporter, and never throws.
     *
     * @param failureSite  what the probe was doing, as the report's "failed while" row takes it
     * @param brokenReach  the member, class or widget that no longer holds, as a short phrase
     * @param reachFailure what the reach threw, carried into the report as its cause
     */
    public void recordReachFailure(String failureSite, String brokenReach, Throwable reachFailure) {

        // First on the path, so a reach failing on every frame pays one flag read from its second
        // failure on.
        if (!hasReported.compareAndSet(false, true)) {
            return;
        }

        // Over Throwable, as the describer is the consuming mod's code and the composition reads
        // the game's settings: a report that threw would replace the failure it was reporting.
        try {
            failureRecord.recordOnce(
                COMPATIBILITY_SUBJECT_KEY,
                describeConsumer.get(),
                recordedAs -> composeReachFailure(recordedAs, failureSite, brokenReach, reachFailure));

        } catch (Throwable exception) {

            logReportNotComposed(failureSite, exception);
        }
    }

    /**
     * Files a reach that failed by answering nothing where the screen it was read on can never
     * answer nothing, once per reporter, and never throws.
     *
     * <p>Only for an answer that cannot be ordinary. A probe whose empty answer is also what an
     * ordinary frame gives reports only its throws, since a break reported every time a screen
     * simply had nothing on it would be worse than no report.
     *
     * @param failureSite what the probe was doing, as the report's "failed while" row takes it
     * @param brokenReach the member, class or widget that no longer holds, as a short phrase
     */
    public void recordReachFailure(String failureSite, String brokenReach) {

        recordReachFailure(failureSite, brokenReach, null);
    }

    /**
     * Builds the failure a report is drawn from.
     *
     * <p>Targeted is the game version the consuming mod declares, detected the one running, so the
     * diagnosis advises updating or downgrading the game against that mod's own release.
     *
     * @param recordedAs   the consumer as the record filed it
     * @param failureSite  what the probe was doing
     * @param brokenReach  what no longer holds
     * @param reachFailure what was thrown, or null where the reach answered nothing
     * @return the failure to record
     */
    static CompatibilityFailure composeReachFailure(
            CompatibilityConsumer recordedAs,
            String failureSite,
            String brokenReach,
            Throwable reachFailure) {

        return new CompatibilityFailure(
            new CompatibilitySubject(
                COMPATIBILITY_SUBJECT_NAME,
                InstalledMods.readModGameVersion(recordedAs.modId()),
                readRunningGameVersion()),
            recordedAs,
            new CompatibilityBreakage(failureSite, brokenReach),
            reachFailure);
    }

    // Says a report was lost, and never throws while saying it: a throw from here would replace the
    // failure the probe was reporting.
    //
    // The logger is resolved here rather than held in a static field: this is the class's one line,
    // on a path that almost never runs, and the class itself is loaded by every probe's static state
    // - so a logger taken at load would be whatever answered then. Where none answers there is
    // nowhere to say it, and the probe's own warning has already named the failure.
    private static void logReportNotComposed(String failureSite, Throwable reportFailure) {

        var log = Global.getLogger(GameReachReporter.class);

        if (log != null) {
            log.error(REPORT_NOT_COMPOSED_MESSAGE + failureSite, reportFailure);
        }
    }

    // The game's own version, in the form a mod declares one, or null before the settings are up.
    private static String readRunningGameVersion() {

        var settings = Global.getSettings();

        return settings == null
            ? null
            : settings.getGameVersion();
    }
}
