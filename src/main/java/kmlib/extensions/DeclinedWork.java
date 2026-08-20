package kmlib.extensions;

import kmlib.text.KmlibStrings;

/**
 * The work was handed back, and why.
 *
 * <p>The reason is required at construction rather than encouraged in a document, because that is
 * the only place the requirement can be enforced. A decline is the one answer whose cause nothing
 * downstream can reconstruct: the implementation had the market, the faction and the install in
 * front of it, and every one of those is gone by the time the plainer sequence has run.
 *
 * <p>Refusing at construction also puts the failure in the right place - the implementation's own
 * code, where the missing reason is - rather than in the operation that was merely told "no".
 *
 * @param reason why the work was handed back, in the words a reader diagnosing an install would
 *               want: what about this body, this owner or this install made it impossible. Read
 *               into a log line and into the failure raised where an implementation had to run, so
 *               it is written as a phrase that completes "did not execute (...)"
 */
public record DeclinedWork(String reason) implements WorkOutcome {

    public DeclinedWork {

        if (!KmlibStrings.hasText(reason)) {
            throw new IllegalArgumentException(
                "Work handed back without a reason. A decline has to say what about this call it "
                    + "could not do, that being the one thing nothing downstream can work out.");
        }
    }

    @Override
    public boolean wasExecuted() {
        return false;
    }
}
