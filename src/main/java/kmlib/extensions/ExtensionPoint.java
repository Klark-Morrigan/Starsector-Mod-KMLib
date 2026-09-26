package kmlib.extensions;

import kmlib.text.KmlibStrings;

import org.apache.log4j.Logger;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The one place an implementation supplied from outside is installed, so that the code offering
 * work to it never names one.
 *
 * <p>It exists because the two halves of an optional integration belong in different places. What
 * a piece of work consists of, and the shape of an implementation that can take it over, are the
 * operation's own business; <em>which</em> implementation this particular install has is a fact
 * about the install, and an operation that names one has taken a decision it cannot see the inputs
 * to. Held here, the operation asks its own point and the point is filled wherever the install is
 * composed.
 *
 * <p>One implementation, not a collection of them. The work is taken over whole or not at all, so
 * a second implementation behind the first would be one nothing ever reaches - a register that
 * accumulated them would be keeping what it cannot use and hiding the fact that one of the two is
 * dead. Registering therefore replaces: the last to register is the one that runs, which on an
 * install is the last mod to load, and a mod loading after another to take its work over is
 * ordinarily what a mod loading later intends.
 *
 * <p>Which is exactly why a replacement is worth saying out loud. One integration going silently
 * dark is the kind of thing that is noticed three sessions later as "my colonies come out wrong",
 * so an install over an occupied point names what it displaced. It is reported as an event rather
 * than as a problem: a mod taking another's work over is a thing a mod may mean to do, and which
 * of the two the reader is looking at is theirs to judge.
 *
 * <p>What happens when the installed implementation declines is the registrant's to state, not
 * this point's to assume - see {@link FallbackToDefaults}. Most integrations hand the work back
 * and the operation does it the ordinary way; one that has no correct outcome without itself says
 * so at registration, and a decline from that one fails the run where it happened rather than
 * producing something wrong that nothing later can trace.
 *
 * <p>An implementation that fails while doing the work is taken out for the rest of the session and
 * reported once, through whatever report its registrant handed in. The work is offered through
 * {@link #offerWork} for that reason, rather than handed to the implementation by the caller: an
 * implementation from another mod reaches that mod's types only when it is first called, so the
 * first call is where a mod that changed underneath it is met, and a point that did not guard it
 * would leave every caller to guard it alone. What the failure means for the call in hand depends
 * on what was thrown - see {@link #offerWork}.
 *
 * <p>The logger is asked of log4j directly rather than of the game, which is the same logger under
 * the same name - the game's own helper is that call and nothing more. A package that knows nothing
 * about Starsector then stays that way, and the name still sits under {@code kmlib}, so the
 * library's own level control governs it like everything else.
 *
 * @param <T> the kind of implementation this point holds
 */
public final class ExtensionPoint<T> {

    private static final Logger LOG = Logger.getLogger(ExtensionPoint.class);

    // How a decline reads once the implementation is out, completing "did not execute (...)". Said
    // as what happened to it rather than as a reason it gave, since it gave none - it failed, and the
    // point stopped asking it.
    private static final String TAKEN_OUT_AFTER_FAILING = "taken out for the session after failing: ";

    // How an empty point is named in a line about one, so that a clean install and a displacement
    // read as the same sentence with one word different.
    private static final String NOTHING_INSTALLED = "nothing";

    // What is running when the work stays where it started - the operation's own sequence. Both an
    // empty point and a decline leave the reader in that state, so both say so in the same words,
    // and neither line stops at what did not happen.
    private static final String DEFAULTS_IN_EFFECT = "defaults are in effect";

    // What stands in for the cause where an implementation answered with nothing at all. The
    // contract has no way to reach this state, so a line carrying it is a line about the
    // implementation rather than about the work.
    private static final String NO_REASON_GIVEN = "no reason given";

    // How an empty point describes itself to a caller, worded as what the install has rather than
    // as something missing from it: most installs supply no implementation here, and that is the
    // ordinary shape of an optional integration rather than a gap in one.
    private static final String NO_IMPLEMENTATION_SUPPLIED = "this install supplies no ";

    private final String extensionName;

    private FallbackToDefaults fallbackToDefaults = FallbackToDefaults.PERMITTED;
    private String implementationName;
    private T implementation;

    // What the installed implementation's registrant is told when it fails, as it handed it in.
    private Consumer<Throwable> reportFailure;

    // Why the implementation is out, where it failed; null while it is installed, and where nothing
    // ever was. Kept apart from the implementation, which is emptied, because the policy it was
    // registered with still stands: a point that forgot a failed implementation had forbidden the
    // fallback would do the work the ordinary way on an install where that is wrong.
    private String takenOutReason;

    /**
     * @param extensionName what is installed here, in the words a log line should use - "colonisation
     *                      routine", "owner submarket rule". Every line this point writes is about
     *                      one of these, so the noun is stated once here rather than at each of them
     */
    public ExtensionPoint(String extensionName) {
        this.extensionName = extensionName;
    }

    /**
     * Empties the point, so an install can be composed again from nothing rather than on top of
     * what a previous composition left.
     *
     * <p>Recorded at debug, and worth recording for the same reason an install is: an operation
     * that quietly went back to doing its own work is otherwise a change with nothing anywhere
     * saying it happened.
     */
    public void clearImplementation() {

        logClearing();

        fallbackToDefaults = FallbackToDefaults.PERMITTED;
        implementationName = null;
        implementation = null;
        reportFailure = null;
        takenOutReason = null;
    }

    /**
     * Offers the work to whatever is installed here, and settles what came of it: recorded with
     * the reason a decline carried, refused where the implementation had to run and did not, and
     * answered so the caller need not read the outcome itself.
     *
     * <p>The refusal is the whole reason settling is a call rather than a log line. An
     * implementation registered with no fallback is one whose install has no correct outcome
     * without it, so the operation carrying on with its own sequence would be quietly producing the
     * wrong thing - and the wrongness would be discovered much later, in a save, as something
     * nobody can trace back to here. Failing at the moment it happens is the only report that names
     * the cause, which is why the decline's own reason is carried into it.
     *
     * <p>An implementation that throws is taken out for the session, logged, and reported once
     * through what its registrant handed in. What that means for the call in hand turns on what was
     * thrown:
     *
     * <ul>
     *   <li>A {@link LinkageError} is raised where the implementation first reaches a type or member
     *       that is no longer there, before any of its own work has run. Nothing was touched, so the
     *       call settles as a decline and the fallback policy decides what happens next, exactly as
     *       it would had the implementation said no.</li>
     *   <li>A {@link RuntimeException} can come from partway through, with the work half done. The
     *       ordinary sequence run over that would build on a state neither sequence produces, so the
     *       failure is passed on to the caller rather than settled - the same answer a refusal
     *       gives, for the same reason.</li>
     * </ul>
     *
     * <p>Nothing wider is caught. An exhausted heap is the process's trouble rather than the
     * implementation's, and taking it out over one would blame a mod for the machine.
     *
     * @param work the work, as a call on the installed implementation; not called where nothing is
     *             installed, or where what was is out
     * @return the outcome a caller can act on: never null, and carrying a reason wherever the work
     *         was not done, so that whoever asked is holding the same account the log has
     * @throws IllegalStateException where an implementation registered
     *                               {@link FallbackToDefaults#FORBIDDEN} did not perform the work
     * @throws RuntimeException      whatever the implementation threw partway through its work
     */
    public WorkOutcome offerWork(Function<T, WorkOutcome> work) {

        if (implementation == null) {
            return settleEmptyPoint();
        }

        WorkOutcome outcome;

        // Only the implementation's own call is inside the boundary. A refusal raised by settling
        // is this point's own answer about a decline, not a failure of the implementation, and
        // taking the implementation out over it would punish it for declining.
        try {
            outcome = work.apply(implementation);

        } catch (LinkageError linkFailure) {

            takeOut(linkFailure);
            return settleDecline(takenOutReason);

        } catch (RuntimeException workFailure) {

            takeOut(workFailure);
            throw workFailure;
        }
        return settleOutcome(outcome);
    }

    /**
     * @return what this install put here, or null where nothing did or what did is out - which is
     *         the answer on every install running no mod that supplies one
     */
    public T readImplementation() {
        return implementation;
    }

    /**
     * @return the name whatever is installed here was registered under, or null where nothing is,
     *         including where what was installed failed and is out
     */
    public String readImplementationName() {
        return implementation != null ? implementationName : null;
    }

    /**
     * Installs an implementation, replacing whatever was here.
     *
     * @param implementationName who is taking the work over, for the log - a mod's name reads as
     *                           the answer to "what is doing this on my install"; blank falls back
     *                           to the implementation's own type, so a lazy caller still leaves a
     *                           line worth reading
     * @param implementation     the implementation to install; null is passed over, an absent
     *                           integration being a state to leave alone rather than one that
     *                           should empty the point behind whoever did install something
     * @param fallbackToDefaults whether the work may be done the ordinary way when this
     *                           implementation declines it - stated by whoever installs it,
     *                           because only they know whether a decline is an ordinary answer or
     *                           a broken install
     * @param reportFailure      what whoever installs it is told where the implementation fails
     *                           and is taken out, handed what it threw. Called at most once per
     *                           registration, a failed implementation not being offered work again
     */
    public void registerImplementation(
            String implementationName,
            T implementation,
            FallbackToDefaults fallbackToDefaults,
            Consumer<Throwable> reportFailure) {

        Objects.requireNonNull(
            reportFailure,
            "An implementation with nobody to tell of its failure would be taken out in silence.");

        if (implementation == null) {
            return;
        }

        // Read through the accessor, so an implementation that failed and is out reads as nothing
        // displaced - it had stopped doing the work before this took it over.
        var displacedName = readImplementationName();

        // An unstated policy reads as the permissive one. Whoever installs without saying has not
        // claimed their work is the only correct outcome on this install, and a library that
        // inferred the claim for them would refuse runs nobody asked it to refuse.
        this.fallbackToDefaults = fallbackToDefaults != null
            ? fallbackToDefaults
            : FallbackToDefaults.PERMITTED;

        this.implementationName = KmlibStrings.hasText(implementationName)
            ? implementationName
            : implementation.getClass().getName();

        this.implementation = implementation;
        this.reportFailure = reportFailure;
        this.takenOutReason = null;

        logInstallation(displacedName);
    }

    // The reason a decline carried, or the one this library states on its behalf. An outcome that
    // is not a decline at all cannot say why, so the line says that instead of showing an empty
    // pair of brackets and leaving the reader to guess what was in them.
    private static String readDeclineReason(WorkOutcome outcome) {

        if (outcome instanceof DeclinedWork declinedWork) {
            return declinedWork.reason();
        }
        return NO_REASON_GIVEN;
    }

    // Tells the registrant its implementation is out. Guarded because it runs where the work has
    // already failed: a report that threw would replace the failure it was reporting, and on a link
    // failure would take down a call that was about to be settled the ordinary way. Guarded as
    // widely as the work, the report being the registrant's own and able to fail to link as well.
    private void reportTakenOut(Throwable implementationFailure) {

        try {
            reportFailure.accept(implementationFailure);

        } catch (LinkageError | RuntimeException reportThrown) {

            LOG.error(extensionName + ": the failure of " + implementationName
                + " could not be reported", reportThrown);
        }
    }

    // Settles work that was not done. Everything enforced is settled before anything is written to
    // the log, so that a run with the log turned down is refused exactly as one with it turned up.
    private WorkOutcome settleDecline(String reason) {

        if (fallbackToDefaults == FallbackToDefaults.FORBIDDEN) {
            throw new IllegalStateException(extensionName + ": " + implementationName
                + " did not execute (" + reason + "), and was installed with no fallback to "
                + "defaults");
        }

        logDeclined(reason);

        // Answered as a decline carrying the reason that was logged, rather than as whatever came
        // in: a caller reading this back gets the same account the log has, including where what
        // came in was nothing at all.
        return new DeclinedWork(reason);
    }

    // Settles an offer nothing was asked to take. Where nothing was ever installed that is the
    // ordinary shape of an optional integration; where something was and failed, it is a decline
    // under the policy it was registered with, which outlives it.
    private WorkOutcome settleEmptyPoint() {

        if (takenOutReason != null) {
            return settleDecline(takenOutReason);
        }

        logNothingInstalled();
        return new DeclinedWork(NO_IMPLEMENTATION_SUPPLIED + extensionName);
    }

    // Settles what the installed implementation answered.
    private WorkOutcome settleOutcome(WorkOutcome outcome) {

        if (outcome != null && outcome.wasExecuted()) {
            logExecuted();
            return outcome;
        }

        // An implementation answering with nothing at all has broken the one contract that makes a
        // decline diagnosable, and saying so names the mod that has to fix it. Warned rather than
        // thrown on its own account: what happens to a run that did not get its work done is the
        // fallback policy's answer, the same as for any other decline.
        if (outcome == null) {
            LOG.warn(extensionName + ": " + implementationName
                + " answered with no outcome at all, which is a decline that cannot say why");
        }

        return settleDecline(readDeclineReason(outcome));
    }

    // Stops offering work to an implementation that failed at it. For the session rather than for
    // the call: a link failure is a fact about the jars loaded and recurs on every call, and a
    // failure partway through has already left one piece of work half done - offering the next to
    // the same implementation risks a second.
    private void takeOut(Throwable implementationFailure) {

        takenOutReason = TAKEN_OUT_AFTER_FAILING + implementationFailure;
        implementation = null;

        LOG.error(extensionName + ": " + implementationName
            + " failed and is taken out for the session", implementationFailure);

        reportTakenOut(implementationFailure);
    }

    // Says the work was handed back, and why. Guarded on the level because it is written once per
    // piece of work, unlike the lines about an install being composed - and separate from the
    // refusal above for the same reason: a line is worth skipping where nobody is reading, and a
    // refusal never is.
    private void logDeclined(String reason) {

        if (LOG.isDebugEnabled()) {
            LOG.debug(extensionName + ": " + implementationName
                + " not executed (" + reason + "), " + DEFAULTS_IN_EFFECT);
        }
    }

    // Says the installed implementation did the work, which is all there is to say about it.
    private void logExecuted() {

        if (LOG.isDebugEnabled()) {
            LOG.debug(extensionName + ": " + implementationName + " executed");
        }
    }

    // Says the work was never offered to anything, which is the ordinary state of an install
    // running no mod that supplies one.
    private void logNothingInstalled() {

        if (LOG.isDebugEnabled()) {
            LOG.debug(extensionName + ": "
                + NOTHING_INSTALLED + " installed, " + DEFAULTS_IN_EFFECT);
        }
    }

    // Says what was emptied out. Not guarded on the level the way an offer's outcome is: that one
    // is written once per piece of work and is worth not building a line for, while this happens
    // where an install is composed and log4j's own check is the whole of the cost.
    private void logClearing() {

        if (implementation == null) {
            LOG.debug(extensionName + ": cleared, " + NOTHING_INSTALLED + " was installed");
            return;
        }
        LOG.debug(extensionName + ": cleared " + implementationName);
    }

    // Says what the install did to whatever was here. A displacement is reported at the same level
    // as a clean install rather than raised above it: taking another mod's work over is a thing a
    // mod may perfectly well mean to do, and a level that called it a problem would be this library
    // second-guessing a composition it cannot see the reasons for. What the line owes the reader is
    // that it happened and who is doing the work now.
    private void logInstallation(String displacedName) {

        if (displacedName == null) {

            LOG.info(extensionName + ": installed " + implementationName
                + " (" + NOTHING_INSTALLED + " was installed)");

            return;
        }
        LOG.info(extensionName + ": installed " + implementationName
            + ", displacing " + displacedName);
    }
}
