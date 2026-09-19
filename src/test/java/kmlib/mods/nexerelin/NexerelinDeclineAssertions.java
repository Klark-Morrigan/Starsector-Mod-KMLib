package kmlib.mods.nexerelin;

import kmlib.extensions.DeclinedWork;
import kmlib.extensions.WorkOutcome;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What every suite in this package checks of an adapter that handed its work back.
 *
 * <p>Each of these adapters can only be pinned on its declines - what it does when it takes the
 * work is the mod's own routine against a live game - so "declined" is the assertion this package
 * makes most, and making it in one place is what keeps the three suites saying the same thing by
 * it.
 *
 * <p>Two things are asserted rather than one. That the work was not done is the obvious half; that
 * the answer is a decline carrying a reason is the half worth having, since a decline is the only
 * answer whose cause nothing downstream can reconstruct, and an adapter that quietly stopped
 * saying why would still pass a test that only asked whether the work was done.
 */
final class NexerelinDeclineAssertions {

    private NexerelinDeclineAssertions() {
        // assertions of static methods, no instances.
    }

    /**
     * Asserts the work was handed back with a reason.
     *
     * @param outcome what the adapter answered
     */
    static void assertDeclined(WorkOutcome outcome) {

        assertThat(outcome)
            .isInstanceOfSatisfying(
                DeclinedWork.class,
                declinedWork -> assertThat(declinedWork.reason())
                    .isNotBlank());
    }

    /**
     * Asserts the work was handed back for the stated cause, which is what a reader diagnosing an
     * install is actually reaching for.
     *
     * @param outcome        what the adapter answered
     * @param expectedReason a phrase the reason has to contain - matched loosely, the wording being
     *                       diagnostics that should be free to read better tomorrow, while what it
     *                       names is the contract
     */
    static void assertDeclinedBecauseOf(WorkOutcome outcome, String expectedReason) {

        assertThat(outcome)
            .isInstanceOfSatisfying(
                DeclinedWork.class,
                declinedWork -> assertThat(declinedWork.reason())
                    .contains(expectedReason));
    }
}
