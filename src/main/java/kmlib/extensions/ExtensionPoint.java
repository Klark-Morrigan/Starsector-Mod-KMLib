package kmlib.extensions;

import kmlib.text.KmlibStrings;

import org.apache.log4j.Logger;

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
 * <p>The logger is asked of log4j directly rather than of the game, which is the same logger under
 * the same name - the game's own helper is that call and nothing more. A package that knows nothing
 * about Starsector then stays that way, and the name still sits under {@code kmlib}, so the
 * library's own level control governs it like everything else.
 *
 * @param <T> the kind of implementation this point holds
 */
public final class ExtensionPoint<T> {

    private static final Logger LOG = Logger.getLogger(ExtensionPoint.class);

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
    }

    /**
     * Settles what came of offering the work to whatever is installed here: recorded with the
     * reason a decline carried, refused where the implementation had to run and did not, and
     * answered so the caller need not read the outcome itself.
     *
     * <p>The refusal is the whole reason this is a call rather than a log line. An implementation
     * registered with no fallback is one whose install has no correct outcome without it, so the
     * operation carrying on with its own sequence would be quietly producing the wrong thing - and
     * the wrongness would be discovered much later, in a save, as something nobody can trace back
     * to here. Failing at the moment it happens is the only report that names the cause, which is
     * why the decline's own reason is carried into it.
     *
     * <p>Everything that is enforced is settled before anything is written to the log, so that a
     * run with the log turned down is refused exactly as one with it turned up. What is enforced
     * cannot depend on what is being recorded.
     *
     * @param outcome what the installed implementation answered; null is a broken implementation
     *                rather than a decline - reported as such, and then treated as a decline with
     *                no reason to give
     * @return the outcome a caller can act on: never null, and carrying a reason wherever the work
     *         was not done, so that whoever asked is holding the same account the log has
     * @throws IllegalStateException where an implementation registered
     *                               {@link FallbackToDefaults#FORBIDDEN} did not perform the work
     */
    public WorkOutcome settleWorkOutcome(WorkOutcome outcome) {

        if (implementation == null) {
            logNothingInstalled();
            return new DeclinedWork(NO_IMPLEMENTATION_SUPPLIED + extensionName);
        }

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

        var reason = readDeclineReason(outcome);

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

    /**
     * @return what this install put here, or null where nothing did - which is the answer on every
     *         install running no mod that supplies one
     */
    public T readImplementation() {
        return implementation;
    }

    /**
     * @return the name whatever is installed here was registered under, or null where nothing is
     */
    public String readImplementationName() {
        return implementationName;
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
     */
    public void registerImplementation(
            String implementationName,
            T implementation,
            FallbackToDefaults fallbackToDefaults) {

        if (implementation == null) {
            return;
        }

        var displacedName = this.implementationName;

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
