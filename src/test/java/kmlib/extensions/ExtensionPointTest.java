package kmlib.extensions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins what a point promises about the one implementation it holds: that installing replaces, that
 * an absent implementation leaves what is there alone, that a blank name still leaves something
 * readable, that emptying it means nothing is installed, and that an implementation which had to
 * run and did not fails the run rather than being passed over.
 *
 * <p>Cases live under {@link Nested} groups named for the method under test. Strings stand in for
 * implementations throughout - what is held is beside the point here, and a value with plain
 * equality makes the assertions read as what they pin.
 *
 * <p>What a log line looks like is not asserted. The log is diagnostics rather than contract, and a
 * suite pinning its wording would be one that has to be edited every time the wording improves.
 * The refusal is a different matter and is pinned down to the words that name its cause, that being
 * something a caller acts on.
 */
final class ExtensionPointTest {

    private static final String EXTENSION_NAME = "colonisation routine";
    private static final String IMPLEMENTATION = "founds colonies";
    private static final String IMPLEMENTATION_NAME = "Nexerelin";

    private static final String DECLINE_REASON = "the body under it is not a planet";
    private static final WorkOutcome DECLINED_WORK = new DeclinedWork(DECLINE_REASON);

    private ExtensionPoint<String> extensionPoint;

    @BeforeEach
    void setUp() {
        extensionPoint = new ExtensionPoint<>(EXTENSION_NAME);
    }

    @Nested
    class RegisterImplementation {

        @Test
        void installsAnImplementationOnAnEmptyPoint() {

            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            assertThat(extensionPoint.readImplementation())
                .isEqualTo(IMPLEMENTATION);
            assertThat(extensionPoint.readImplementationName())
                .isEqualTo(IMPLEMENTATION_NAME);
        }

        @Test
        void replacesWhatWasInstalledRatherThanQueueingBehindIt() {
            // The work is taken over whole or not at all, so a second implementation behind the
            // first would be one nothing ever reaches - kept, it would only hide that one of the
            // two is dead.
            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            installPermittingFallback("Some Other Mod", "founds them differently");

            assertThat(extensionPoint.readImplementation())
                .isEqualTo("founds them differently");
            assertThat(extensionPoint.readImplementationName())
                .isEqualTo("Some Other Mod");
        }

        @Test
        void replacesTheFallbackPolicyAlongWithTheImplementation() {
            // The policy belongs to whoever registered, so a mod taking the work over brings its
            // own - otherwise a permissive mod would inherit a refusal it never asked for.
            installForbiddingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            installPermittingFallback("Some Other Mod", "founds them differently");

            assertThatCode(() -> extensionPoint.settleWorkOutcome(DECLINED_WORK))
                .doesNotThrowAnyException();
        }

        @Test
        void passesOverAnAbsentImplementation() {
            // An integration that is not installed registers nothing rather than failing: absence
            // is the ordinary state of every optional mod.
            installPermittingFallback(IMPLEMENTATION_NAME, null);

            assertThat(extensionPoint.readImplementation())
                .isNull();
        }

        @Test
        void leavesAnInstalledImplementationAloneWhenPassedAnAbsentOne() {
            // Passing over an absent one must not empty the point behind whoever did install
            // something - an integration that is not there has nothing to say about the one that
            // is.
            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            installPermittingFallback("Some Other Mod", null);

            assertThat(extensionPoint.readImplementation())
                .isEqualTo(IMPLEMENTATION);
            assertThat(extensionPoint.readImplementationName())
                .isEqualTo(IMPLEMENTATION_NAME);
        }

        @Test
        void namesAnUnnamedImplementationByItsOwnType() {
            // A caller that names nothing still has to leave a line worth reading, the whole point
            // of the name being to answer "what is doing this on my install".
            installPermittingFallback("  ", IMPLEMENTATION);

            assertThat(extensionPoint.readImplementationName())
                .isEqualTo(String.class.getName());
        }

        @Test
        void readsAnUnstatedPolicyAsPermittingTheFallback() {
            // Whoever installs without saying has not claimed their work is the only correct
            // outcome, and inferring the claim would refuse runs nobody asked to have refused.
            extensionPoint.registerImplementation(IMPLEMENTATION_NAME, IMPLEMENTATION, null);

            assertThatCode(() -> extensionPoint.settleWorkOutcome(DECLINED_WORK))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    class ReadImplementation {

        @Test
        void isAbsentBeforeAnythingIsInstalled() {

            assertThat(extensionPoint.readImplementation())
                .isNull();
            assertThat(extensionPoint.readImplementationName())
                .isNull();
        }
    }

    @Nested
    class ClearImplementation {

        @Test
        void emptiesThePointSoItCanBeComposedAgainFromNothing() {

            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            extensionPoint.clearImplementation();

            assertThat(extensionPoint.readImplementation())
                .isNull();
            assertThat(extensionPoint.readImplementationName())
                .isNull();
        }

        @Test
        void takesTheFallbackPolicyWithIt() {
            // An emptied point is one nothing has claimed, so it cannot go on refusing runs on
            // behalf of an implementation that is no longer there.
            installForbiddingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            extensionPoint.clearImplementation();

            assertThatCode(() -> extensionPoint.settleWorkOutcome(DECLINED_WORK))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    class SettleWorkOutcome {

        @Test
        void refusesTheRunWhereAnImplementationThatHadToExecuteDidNot() {
            // The whole reason the outcome is reported rather than merely logged. Carrying on with
            // the ordinary sequence here would produce something this install has no correct
            // version of, and it would be found much later with nothing pointing back to the cause
            // - so the failure names both the work and who was meant to do it.
            installForbiddingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            assertThatThrownBy(() -> extensionPoint.settleWorkOutcome(DECLINED_WORK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(EXTENSION_NAME)
                .hasMessageContaining(IMPLEMENTATION_NAME);
        }

        @Test
        void carriesTheDeclinesOwnReasonIntoTheRefusal() {
            // The failure is the only report anyone gets of this, so it has to say what the
            // implementation said - a refusal naming the work and the mod but not the cause leaves
            // the reader exactly where a silent fallback would have.
            installForbiddingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            assertThatThrownBy(() -> extensionPoint.settleWorkOutcome(DECLINED_WORK))
                .hasMessageContaining(DECLINE_REASON);
        }

        @Test
        void acceptsAnExecutionFromAnImplementationThatHadToExecute() {

            installForbiddingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            assertThat(extensionPoint.settleWorkOutcome(new ExecutedWork()).wasExecuted())
                .isTrue();
        }

        @Test
        void acceptsADeclineFromAnImplementationThatPermitsTheFallback() {
            // The ordinary case: declining is how an implementation says "not this one", and the
            // operation carries on with its own sequence.
            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            assertThat(extensionPoint.settleWorkOutcome(DECLINED_WORK))
                .isEqualTo(DECLINED_WORK);
        }

        @Test
        void answersADeclineNamingWhatTheInstallSuppliesNoneOf() {
            // An empty point is the ordinary state of an optional integration rather than a failed
            // one - and the caller is owed a sentence rather than a bare no, since from where it
            // stands an install with nothing here and one whose implementation declined look
            // identical.
            assertThat(extensionPoint.settleWorkOutcome(DECLINED_WORK))
                .isInstanceOfSatisfying(
                    DeclinedWork.class,
                    declinedWork -> assertThat(declinedWork.reason())
                        .contains(EXTENSION_NAME));
        }

        @Test
        void treatsAnImplementationThatAnsweredWithNothingAsADecline() {
            // A port answering null has broken the one contract that makes a decline diagnosable.
            // It is still a decline - the work was not done - so the fallback policy decides what
            // happens next, and the answer says as much as there is to say.
            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            assertThat(extensionPoint.settleWorkOutcome(null))
                .isInstanceOfSatisfying(
                    DeclinedWork.class,
                    declinedWork -> assertThat(declinedWork.reason())
                        .isNotBlank());
        }

        @Test
        void refusesARunWhereAnImplementationThatHadToExecuteAnsweredWithNothing() {

            installForbiddingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            assertThatThrownBy(() -> extensionPoint.settleWorkOutcome(null))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    private void installForbiddingFallback(String implementationName, String implementation) {

        extensionPoint.registerImplementation(
            implementationName,
            implementation,
            FallbackToDefaults.FORBIDDEN);
    }

    private void installPermittingFallback(String implementationName, String implementation) {

        extensionPoint.registerImplementation(
            implementationName,
            implementation,
            FallbackToDefaults.PERMITTED);
    }
}
