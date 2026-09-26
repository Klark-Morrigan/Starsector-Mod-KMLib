package kmlib.starsector.startup;

import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.compatibility.IntegrationFailureReporter;
import kmlib.starsector.compatibility.ModIntegration;

import org.apache.log4j.Logger;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Running one step of a mod's start-up wiring behind its own failure boundary, and telling the
 * player where the step that failed was the mod's integration with another one.
 *
 * <p>A step that throws costs its own registration and nothing else: the load carries on and every
 * later step still runs. That isolation is the reason start-up wiring is a list of steps rather
 * than a sequence of calls - a mod whose first failing install aborted the rest would come back
 * with a half-wired sector and no indication of which piece went missing. Throwing instead is not
 * an option worth having at load: it would take down every mod that depends on this library, or on
 * whichever mod is wiring, over one registration.
 *
 * <p>A failure to link is caught alongside a throw, and is the more important of the two here. A
 * third party that moved a class, renamed a member or changed a signature raises a
 * {@link LinkageError} rather than an exception, and raises it at the moment the step first reaches
 * the class - which is inside this guard. A guard catching only {@link RuntimeException} would
 * therefore miss the precise failure an integration step is guarded for, and a mod that merely
 * updated would take the load down.
 *
 * <p>Nothing wider than those two is caught. An exhausted heap or a blown stack is the process's
 * trouble rather than this step's, and swallowing one would leave a game that cannot run reporting
 * that it wired.
 *
 * <p>Logged as the mod that is wiring rather than as this package. The logger is the caller's,
 * because a level set through {@code KmLogging} scopes to a package subtree: a library logging a
 * consuming mod's failed step under {@code kmlib} would put it outside the switch that mod's player
 * turns up, and beside lines about code they were not running.
 *
 * <p>Where the step bound to a third-party mod, the failure also reaches the player. A silent
 * degradation is the same class of problem whether a binding broke at a render pass or at load: a
 * player who enabled a mod is entitled to be told when the mod they enabled did not integrate,
 * rather than finding out by playing a session without it. What that report says is
 * {@link ModIntegration}'s, and the library holds none of the wording; filing it without throwing is
 * {@link IntegrationFailureReporter}'s.
 */
public final class WiringSteps {

    // Where a guarded step is said to have failed, as the log block's "failed while" row takes it.
    // Spelled here because only the guard that caught a failure knows which guard caught it - and
    // spelled once, so the phrase a log is searched for is one phrase however many mods wire
    // through this.
    private static final String WHILE_INSTALLING_INTEGRATION = "installing the integration at start-up";

    private final Logger stepLog;

    // Where a failed integration is recorded, taken rather than reached for so a suite records into
    // one of its own instead of into the session's.
    private final CompatibilityFailures failureRecord;

    /**
     * A guard logging to the wiring mod's own logger and reporting into the session's record.
     *
     * @param stepLog where a step that did not install is logged, which is the wiring mod's logger
     *                rather than the library's - see the note on scoping above
     */
    public WiringSteps(Logger stepLog) {

        this(stepLog, CompatibilityFailures.SESSION_RECORD);
    }

    WiringSteps(Logger stepLog, CompatibilityFailures failureRecord) {

        this.stepLog = Objects.requireNonNull(
            stepLog,
            "A guard with nowhere to log would swallow every step that failed.");

        this.failureRecord = Objects.requireNonNull(
            failureRecord,
            "A guard with nowhere to record would degrade silently and tell no player why.");
    }

    /**
     * Runs one wiring step, logging rather than propagating whatever it throws.
     *
     * <p>The step comes before the message it fails with, so a reader of a wiring list meets what
     * each entry does before what it says when that does not happen.
     *
     * <p>For a step that binds to nothing a player could act on - one that wires the mod to itself,
     * or to the game - there is no third party to name and nothing to be told. The overload beside
     * this is for the rest.
     *
     * @param wiringStep     the registration to attempt
     * @param failureMessage what the log says when it throws, naming the piece that went missing
     */
    public void runGuardedStep(Runnable wiringStep, String failureMessage) {

        try {
            wiringStep.run();

        } catch (LinkageError | RuntimeException stepFailure) {

            // With the trace: nobody is told of this step, so the log is the only place it goes.
            stepLog.error(failureMessage, stepFailure);
        }
    }

    /**
     * Runs one wiring step that integrates with a third-party mod, logging whatever it throws and
     * recording it for the player to be told about once.
     *
     * @param wiringStep           the registration to attempt
     * @param failureMessage       what the log says when it throws
     * @param describeIntegration  which mod the step integrates with and what the wiring mod loses
     *                             without it, composed only where the step failed. A supplier
     *                             rather than the value, so wording read out of strings.json - and
     *                             the mod manager read behind the version - stay off the path where
     *                             everything installed
     */
    public void runGuardedStep(
            Runnable wiringStep,
            String failureMessage,
            Supplier<ModIntegration> describeIntegration) {

        // Built before the step runs, so a step said to integrate with nothing is refused whether
        // or not it would have failed. Logging to the wiring mod's own logger, for the reason given
        // on this class.
        var failureReporter = new IntegrationFailureReporter(describeIntegration, failureRecord, stepLog);

        try {
            wiringStep.run();

        } catch (LinkageError | RuntimeException stepFailure) {

            // One line rather than the trace: the report's own block carries that, and the reporter
            // logs it wherever no block will.
            stepLog.error(failureMessage + ": " + stepFailure);
            failureReporter.recordFailure(WHILE_INSTALLING_INTEGRATION, stepFailure);
        }
    }
}
