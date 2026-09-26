package kmlib.extensions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins what a point promises about the one implementation it holds: that installing replaces, that
 * an absent implementation leaves what is there alone, that a blank name still leaves something
 * readable, that emptying it means nothing is installed, that an implementation which had to run
 * and did not fails the run rather than being passed over, and that one which fails at the work is
 * taken out and reported once.
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

    // What the registrant was told, in the order it was told it.
    private final List<Throwable> reportedFailures = new ArrayList<>();

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

            assertThatCode(() -> extensionPoint.offerWork(implementation -> DECLINED_WORK))
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
            extensionPoint.registerImplementation(
                IMPLEMENTATION_NAME,
                IMPLEMENTATION,
                null,
                reportedFailures::add);

            assertThatCode(() -> extensionPoint.offerWork(implementation -> DECLINED_WORK))
                .doesNotThrowAnyException();
        }

        @Test
        void installsAFreshImplementationOverOneThatFailed() {
            // A failed implementation is out for the session, not the point: a mod loading later
            // may still take the work over, and is offered it.
            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);
            extensionPoint.offerWork(implementation -> {
                throw buildLinkFailure();
            });

            installPermittingFallback("Some Other Mod", "founds them differently");

            assertThat(extensionPoint.offerWork(implementation -> new ExecutedWork()).wasExecuted())
                .isTrue();
        }

        @Test
        void refusesAnImplementationWithNobodyToTellOfItsFailure() {

            assertThatNullPointerException()
                .isThrownBy(() -> extensionPoint.registerImplementation(
                    IMPLEMENTATION_NAME,
                    IMPLEMENTATION,
                    FallbackToDefaults.PERMITTED,
                    null));
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

        @Test
        void isAbsentOnceWhatWasInstalledFailed() {
            // Nothing is doing the work any more, which is what a caller asking who does it is
            // owed - a name read back after the failure would name a mod that stopped.
            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            extensionPoint.offerWork(implementation -> {
                throw buildLinkFailure();
            });

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

            assertThatCode(() -> extensionPoint.offerWork(implementation -> DECLINED_WORK))
                .doesNotThrowAnyException();
        }

        @Test
        void takesTheFailedImplementationsPolicyWithIt() {
            // The failure's policy outlives the implementation, not a recomposition: once the
            // install is composed again from nothing there is no claim left to honour.
            installForbiddingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);
            assertThatThrownBy(() -> extensionPoint.offerWork(implementation -> {
                throw buildLinkFailure();
            }));

            extensionPoint.clearImplementation();

            assertThatCode(() -> extensionPoint.offerWork(implementation -> DECLINED_WORK))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    class OfferWork {

        @Test
        void refusesTheRunWhereAnImplementationThatHadToExecuteDidNot() {
            // The whole reason the outcome is reported rather than merely logged. Carrying on with
            // the ordinary sequence here would produce something this install has no correct
            // version of, and it would be found much later with nothing pointing back to the cause
            // - so the failure names both the work and who was meant to do it.
            installForbiddingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            assertThatThrownBy(() -> extensionPoint.offerWork(implementation -> DECLINED_WORK))
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

            assertThatThrownBy(() -> extensionPoint.offerWork(implementation -> DECLINED_WORK))
                .hasMessageContaining(DECLINE_REASON);
        }

        @Test
        void acceptsAnExecutionFromAnImplementationThatHadToExecute() {

            installForbiddingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            assertThat(extensionPoint.offerWork(implementation -> new ExecutedWork()).wasExecuted())
                .isTrue();
        }

        @Test
        void acceptsADeclineFromAnImplementationThatPermitsTheFallback() {
            // The ordinary case: declining is how an implementation says "not this one", and the
            // operation carries on with its own sequence.
            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            assertThat(extensionPoint.offerWork(implementation -> DECLINED_WORK))
                .isEqualTo(DECLINED_WORK);
        }

        @Test
        void answersADeclineNamingWhatTheInstallSuppliesNoneOf() {
            // An empty point is the ordinary state of an optional integration rather than a failed
            // one - and the caller is owed a sentence rather than a bare no, since from where it
            // stands an install with nothing here and one whose implementation declined look
            // identical.
            assertThat(extensionPoint.offerWork(implementation -> DECLINED_WORK))
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

            assertThat(extensionPoint.offerWork(implementation -> null))
                .isInstanceOfSatisfying(
                    DeclinedWork.class,
                    declinedWork -> assertThat(declinedWork.reason())
                        .isNotBlank());
        }

        @Test
        void refusesARunWhereAnImplementationThatHadToExecuteAnsweredWithNothing() {

            installForbiddingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            assertThatThrownBy(() -> extensionPoint.offerWork(implementation -> null))
                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void keepsAnImplementationWhoseDeclineItsOwnPolicyRefused() {
            // The refusal is this point's answer about a decline, not a failure of the
            // implementation - taking it out over one would punish it for saying no.
            installForbiddingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            assertThatThrownBy(() -> extensionPoint.offerWork(implementation -> DECLINED_WORK));

            assertThat(extensionPoint.readImplementation())
                .isEqualTo(IMPLEMENTATION);
            assertThat(reportedFailures)
                .isEmpty();
        }

        @Test
        void settlesAnImplementationThatCouldNotLinkAsADecline() {
            // A link failure is raised before any of the implementation's own work runs, so
            // nothing was touched and the ordinary sequence is safe to run in its place.
            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            var outcome = extensionPoint.offerWork(implementation -> {
                throw buildLinkFailure();
            });

            assertThat(outcome)
                .isInstanceOfSatisfying(
                    DeclinedWork.class,
                    declinedWork -> assertThat(declinedWork.reason())
                        .contains("NoSuchMethodError"));
        }

        @Test
        void refusesTheRunWhereAnImplementationThatHadToExecuteCouldNotLink() {
            // Settled as a decline, so it is the policy's to answer - and a registrant that forbade
            // the fallback forbade it for this as much as for a decline.
            installForbiddingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            assertThatThrownBy(() -> extensionPoint.offerWork(implementation -> {
                throw buildLinkFailure();
            }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(IMPLEMENTATION_NAME);
        }

        @Test
        void passesOnAFailurePartwayThroughRatherThanSettlingIt() {
            // A throw can come with the work half done, and the ordinary sequence run over that
            // would build on a state neither sequence produces - so the caller gets the failure.
            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);
            var workFailure = new IllegalStateException("half the colony was founded");

            assertThatThrownBy(() -> extensionPoint.offerWork(implementation -> {
                throw workFailure;
            }))
                .isSameAs(workFailure);
        }

        @Test
        void neverOffersWorkAgainToAnImplementationThatFailed() {
            // A link failure recurs on every call, and a failure partway through risks a second
            // half-done piece of work - either way the next offer must not reach it.
            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);
            var offersReceived = new AtomicInteger();

            assertThatThrownBy(() -> extensionPoint.offerWork(implementation -> {
                offersReceived.incrementAndGet();
                throw new IllegalStateException("half the colony was founded");
            }));
            extensionPoint.offerWork(implementation -> {
                offersReceived.incrementAndGet();
                return new ExecutedWork();
            });

            assertThat(offersReceived)
                .hasValue(1);
        }

        @Test
        void settlesTheOffersAfterAFailureAsDeclines() {
            // Out of the way rather than gone: the work goes back to the ordinary sequence, as on
            // an install without the mod.
            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);
            assertThatThrownBy(() -> extensionPoint.offerWork(implementation -> {
                throw new IllegalStateException("half the colony was founded");
            }));

            assertThat(extensionPoint.offerWork(implementation -> new ExecutedWork()).wasExecuted())
                .isFalse();
        }

        @Test
        void refusesTheOffersAfterAFailureWhereTheFallbackWasForbidden() {
            // The policy outlives the implementation. A point that forgot it would do the work the
            // ordinary way on the very install whose registrant said that is wrong.
            installForbiddingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);
            assertThatThrownBy(() -> extensionPoint.offerWork(implementation -> {
                throw new IllegalStateException("half the colony was founded");
            }));

            assertThatThrownBy(() -> extensionPoint.offerWork(implementation -> new ExecutedWork()))
                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void reportsWhatAFailedImplementationThrewOnce() {

            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);
            var linkFailure = buildLinkFailure();

            extensionPoint.offerWork(implementation -> {
                throw linkFailure;
            });
            extensionPoint.offerWork(implementation -> {
                throw buildLinkFailure();
            });

            assertThat(reportedFailures)
                .containsExactly(linkFailure);
        }

        @Test
        void settlesTheCallRatherThanPropagatingAReportThatThrew() {
            // The report runs where the work already failed. One that threw would replace the
            // failure it was about, and would take down a call about to be settled the plain way.
            extensionPoint.registerImplementation(
                IMPLEMENTATION_NAME,
                IMPLEMENTATION,
                FallbackToDefaults.PERMITTED,
                buildReportThatThrows());

            assertThatCode(() -> extensionPoint.offerWork(implementation -> {
                throw buildLinkFailure();
            }))
                .doesNotThrowAnyException();
        }

        @Test
        void reportsNothingForAnImplementationThatWorked() {

            installPermittingFallback(IMPLEMENTATION_NAME, IMPLEMENTATION);

            extensionPoint.offerWork(implementation -> new ExecutedWork());

            assertThat(reportedFailures)
                .isEmpty();
            assertThat(extensionPoint.readImplementation())
                .isEqualTo(IMPLEMENTATION);
        }
    }

    // What an implementation meets where the mod behind it moved what it binds to. An Error rather
    // than an exception, which is the whole point of the cases about one.
    private static NoSuchMethodError buildLinkFailure() {

        return new NoSuchMethodError("the mod moved what the implementation binds to");
    }

    private static Consumer<Throwable> buildReportThatThrows() {

        return implementationFailure -> {
            throw new IllegalArgumentException("the wording did not load");
        };
    }

    private void installForbiddingFallback(String implementationName, String implementation) {

        extensionPoint.registerImplementation(
            implementationName,
            implementation,
            FallbackToDefaults.FORBIDDEN,
            reportedFailures::add);
    }

    private void installPermittingFallback(String implementationName, String implementation) {

        extensionPoint.registerImplementation(
            implementationName,
            implementation,
            FallbackToDefaults.PERMITTED,
            reportedFailures::add);
    }
}
