package kmlib.starsector.compatibility;

import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pins the two promises every boundary filing a failed integration relies on: the filing never
 * throws, and a failure's trace reaches the log exactly where no report block will carry it.
 *
 * <p>The second is what lets a boundary log one line rather than the trace. A reporter that logged
 * the trace always would put it in the log twice beside every block; one that never did would lose
 * it for the failures no block is written for.
 */
final class IntegrationFailureReporterTest {

    private static final String FAILURE_SITE = "founding a colony";

    private final CompatibilityFailures failureRecord = new CompatibilityFailures();

    private final Logger reportLogMock = mock(Logger.class);

    @Nested
    class RecordFailure {

        @Test
        void filesTheFailureUnderTheDescribedIntegrationAtTheSiteItWasCaught() {

            var integrationFailure = new NoSuchMethodError("the mod moved what the routine founds with");

            ModStateScopes.runWithoutGameSettings(() ->
                buildReporter().recordFailure(FAILURE_SITE, integrationFailure));

            var failure = failureRecord.takeNextUnreported();

            assertThat(failure.subject().name())
                .isEqualTo(CompatibilityFailureFixture.INTEGRATED_MOD_NAME);
            assertThat(failure.breakage().failureSite())
                .isEqualTo(FAILURE_SITE);
            assertThat(failure.cause())
                .isSameAs(integrationFailure);
        }

        @Test
        void leavesTheTraceToTheReportItFiled() {
            // The block the notice writes carries this failure as its cause, so a trace here would
            // be the same trace twice.
            ModStateScopes.runWithoutGameSettings(() ->
                buildReporter().recordFailure(FAILURE_SITE, new IllegalStateException("half founded")));

            verify(reportLogMock, never())
                .error(any(), any(Throwable.class));
        }

        @Test
        void logsTheTraceOfAFailureTheRecordHadAlreadyReported() {
            // The latch drops a second failure of one binding, and the block written for the first
            // carries the first one's trace - so this one's goes to the log here or nowhere.
            var reporter = buildReporter();
            var secondFailure = new IllegalStateException("half handed over");

            ModStateScopes.runWithoutGameSettings(() -> {
                reporter.recordFailure(FAILURE_SITE, new IllegalStateException("half founded"));
                reporter.recordFailure(FAILURE_SITE, secondFailure);
            });

            verify(reportLogMock)
                .error(anyString(), eq(secondFailure));
        }

        @Test
        void logsTheFailureAndTheReasonWhereTheReportCouldNotBeComposed() {
            // Nothing reaches the record, so no block will carry either trace. The describer is
            // the binding mod's own and can fail as the binding did - a wording that did not load.
            var integrationFailure = new NoSuchMethodError("the mod moved what the routine founds with");
            var describerFailure = new IllegalArgumentException("the wording did not load");
            var reporter = new IntegrationFailureReporter(
                () -> {
                    throw describerFailure;
                },
                failureRecord,
                reportLogMock);

            assertThatCode(() -> reporter.recordFailure(FAILURE_SITE, integrationFailure))
                .doesNotThrowAnyException();

            verify(reportLogMock)
                .error(anyString(), eq(integrationFailure));
            verify(reportLogMock)
                .error(anyString(), eq(describerFailure));
            assertThat(failureRecord.hasUnreported())
                .isFalse();
        }
    }

    @Nested
    class Constructor {

        @Test
        void refusesABindingThatNamesNoIntegration() {

            assertThatNullPointerException()
                .isThrownBy(() -> new IntegrationFailureReporter(null));
        }
    }

    private IntegrationFailureReporter buildReporter() {

        return new IntegrationFailureReporter(
            () -> CompatibilityFailureFixture.MOD_INTEGRATION,
            failureRecord,
            reportLogMock);
    }
}
