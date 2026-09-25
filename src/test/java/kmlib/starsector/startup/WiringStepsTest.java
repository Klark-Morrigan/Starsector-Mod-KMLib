package kmlib.starsector.startup;

import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.compatibility.ModIntegration;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Covers the boundary a start-up step runs behind: that a step which throws costs its own
 * registration and nothing else, and that where the step was an integration with another mod the
 * failure also reaches the player.
 *
 * <p>The cases worth the most are the two that keep the load standing. A guard that let a step's
 * throw out would take down every mod loading behind it over one registration, and so would a guard
 * whose own reporting threw - which is the likelier of the two, the report being composed from
 * wording that may not have loaded at the moment everything else is already going wrong.
 *
 * <p>Both ends of what is caught are pinned, because both are decisions. A step that cannot link
 * what it binds to is caught and reported, that being how a third party's changed contract arrives
 * and the one failure an integration guard exists for; a failure of the process itself is not.
 */
final class WiringStepsTest {

    private static final String FAILURE_MESSAGE = "Failed to install the test integration";

    private static final ModIntegration INTEGRATION = CompatibilityFailureFixture.MOD_INTEGRATION;

    private final Logger stepLogMock = mock(Logger.class);

    private final CompatibilityFailures failureRecord = new CompatibilityFailures();

    private final WiringSteps wiringSteps = new WiringSteps(stepLogMock, failureRecord);

    @Nested
    class RunGuardedStep {

        @Test
        void runsTheStepItWasGiven() {

            var stepRuns = new AtomicInteger();

            wiringSteps.runGuardedStep(stepRuns::incrementAndGet, FAILURE_MESSAGE);

            assertThat(stepRuns)
                .hasValue(1);
        }

        @Test
        void logsAStepThatThrowsRatherThanPropagatingIt() {

            var stepFailure = new IllegalStateException("nothing to register with");

            assertThatCode(() -> wiringSteps.runGuardedStep(
                    () -> {
                        throw stepFailure;
                    },
                    FAILURE_MESSAGE))
                .doesNotThrowAnyException();

            verify(stepLogMock)
                .error(FAILURE_MESSAGE, stepFailure);
        }

        @Test
        void logsAStepThatCouldNotLinkWhatItBindsToRatherThanPropagatingIt() {
            // The failure a guard in front of an integration exists for, and the one a
            // RuntimeException-only guard misses. A third party that moved a class or changed a
            // signature is met where the step first reaches it, which is inside this guard, and
            // arrives as an Error rather than an exception.
            var linkFailure = new NoSuchMethodError("the mod moved what the step binds to");

            assertThatCode(() -> wiringSteps.runGuardedStep(
                    () -> {
                        throw linkFailure;
                    },
                    FAILURE_MESSAGE))
                .doesNotThrowAnyException();

            verify(stepLogMock)
                .error(FAILURE_MESSAGE, linkFailure);
        }

        @Test
        void letsAFailureOfTheProcessItselfThrough() {
            // The limit on the breadth above. An exhausted heap is not this step's to answer for,
            // and a guard that swallowed one would leave a game that cannot run reporting that it
            // wired.
            assertThatExceptionOfType(OutOfMemoryError.class)
                .isThrownBy(() -> wiringSteps.runGuardedStep(
                    () -> {
                        throw new OutOfMemoryError("Java heap space");
                    },
                    FAILURE_MESSAGE));
        }

        @Test
        void leavesTheNextStepToRunAfterOneFailed() {
            // The whole reason wiring is a list of steps rather than a sequence of calls: a mod
            // whose first failing install aborted the rest comes back half wired.
            var laterStepRuns = new AtomicInteger();

            wiringSteps.runGuardedStep(
                () -> {
                    throw new IllegalStateException("nothing to register with");
                },
                FAILURE_MESSAGE);
            wiringSteps.runGuardedStep(laterStepRuns::incrementAndGet, FAILURE_MESSAGE);

            assertThat(laterStepRuns)
                .hasValue(1);
        }

        @Test
        void recordsNothingForAStepThatNamedNoThirdParty() {
            // A step wiring the mod to itself has nobody to name and nothing a player could act on,
            // so it is logged and left.
            wiringSteps.runGuardedStep(
                () -> {
                    throw new IllegalStateException("nothing to register with");
                },
                FAILURE_MESSAGE);

            assertThat(failureRecord.hasUnreported())
                .isFalse();
        }
    }

    @Nested
    class RunGuardedStepForAnIntegration {

        @Test
        void recordsOneFailureNamingTheThirdPartyAndWhatTheModLost() {

            wiringSteps.runGuardedStep(buildStepRefusingToInstall(), FAILURE_MESSAGE, () -> INTEGRATION);

            var failure = failureRecord.takeNextUnreported();

            assertThat(failure.subject().name())
                .isEqualTo(CompatibilityFailureFixture.INTEGRATED_MOD_NAME);
            assertThat(failure.consumer().lostFeature())
                .isEqualTo(CompatibilityFailureFixture.LOST_FEATURE);
            assertThat(failure.breakage().failureSite())
                .isEqualTo("installing the integration at start-up");
            assertThat(failureRecord.takeNextUnreported())
                .isNull();
        }

        @Test
        void reportsAnIntegrationThatCouldNotLinkTheModItBindsTo() {
            // The whole point of widening the guard: a third party that changed its contract is
            // exactly the case a player is entitled to be told about, and it never arrives as an
            // exception. What was thrown is carried as the failure's cause, so the log block beside
            // the notice renders a trace of the link that failed.
            var linkFailure = new NoSuchMethodError("the mod moved what the step binds to");

            wiringSteps.runGuardedStep(
                () -> {
                    throw linkFailure;
                },
                FAILURE_MESSAGE,
                () -> INTEGRATION);

            var failure = failureRecord.takeNextUnreported();

            assertThat(failure.cause())
                .isSameAs(linkFailure);
            assertThat(failure.breakage().brokenDetail())
                .contains("NoSuchMethodError");
        }

        @Test
        void recordsNothingWhereTheStepInstalled() {

            wiringSteps.runGuardedStep(() -> {
            }, FAILURE_MESSAGE, () -> INTEGRATION);

            assertThat(failureRecord.hasUnreported())
                .isFalse();
        }

        @Test
        void doesNotDescribeTheIntegrationWhereTheStepInstalled() {
            // What the describer costs is a strings.json read and a mod manager read, which is why
            // it is a supplier: neither belongs on the load path of an install where nothing broke.
            var describeCount = new AtomicInteger();

            wiringSteps.runGuardedStep(() -> {
            }, FAILURE_MESSAGE, () -> countAndDescribe(describeCount));

            assertThat(describeCount)
                .hasValue(0);
        }

        @Test
        void filesASecondFeatureTheWiringModPutUnderOneKeyAsANumberedOne() {
            // Two steps binding to one mod, under the one feature key the wiring mod spelled for
            // both. Reported against each integration's own consumer rather than the one the
            // record settled on, the second would file under the first's key and reach nobody.
            var secondFeatureUnderOneKey = new ModIntegration(
                CompatibilityFailureFixture.INTEGRATED_MOD_ID,
                CompatibilityFailureFixture.INTEGRATED_MOD_NAME,
                new CompatibilityConsumer(
                    CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID,
                    CompatibilityFailureFixture.MAP_OVERLAY_FEATURE_KEY,
                    "A second feature of the wiring mod stopped working."));

            wiringSteps.runGuardedStep(buildStepRefusingToInstall(), FAILURE_MESSAGE, () -> INTEGRATION);
            wiringSteps.runGuardedStep(
                buildStepRefusingToInstall(),
                FAILURE_MESSAGE,
                () -> secondFeatureUnderOneKey);

            failureRecord.takeNextUnreported();

            assertThat(failureRecord.takeNextUnreported().consumer().consumerKey())
                .isEqualTo("map-mod:map-overlay-2");
        }

        @Test
        void losesTheReportRatherThanTheLoadWhereTheDescriberThrows() {
            // The report is composed from wording that may not have loaded, on a path that already
            // runs inside a catch - so a throw here would escape into the game's load sequence.
            assertThatCode(() -> wiringSteps.runGuardedStep(
                    buildStepRefusingToInstall(),
                    FAILURE_MESSAGE,
                    () -> {
                        throw new IllegalArgumentException("the wording did not load");
                    }))
                .doesNotThrowAnyException();

            assertThat(failureRecord.hasUnreported())
                .isFalse();
            verify(stepLogMock)
                .error(any(), any(IllegalArgumentException.class));
        }

        @Test
        void refusesAStepSaidToIntegrateWithNothing() {

            assertThatNullPointerException()
                .isThrownBy(() -> wiringSteps.runGuardedStep(() -> {
                }, FAILURE_MESSAGE, null));
        }
    }

    // A step that binds to a third party and does not install, as every case needing a failure
    // from one arranges it.
    private static Runnable buildStepRefusingToInstall() {

        return () -> {
            throw new IllegalStateException("routes already registered");
        };
    }

    private static ModIntegration countAndDescribe(AtomicInteger describeCount) {

        describeCount.incrementAndGet();
        return INTEGRATION;
    }
}
