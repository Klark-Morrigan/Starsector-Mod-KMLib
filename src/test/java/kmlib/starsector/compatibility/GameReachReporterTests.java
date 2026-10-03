package kmlib.starsector.compatibility;

import com.fs.starfarer.api.Global;

import kmlib.testfixtures.starsector.StubbedGlobalLogger;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Pins what a failed reach into the game files: one report per reporter, under the game as the
 * subject, stating the game version the consuming mod declares against the one running - and that
 * filing it never throws into the probe that caught the failure.
 */
final class GameReachReporterTests {

    private static final String FAILURE_SITE = "reading the sector map's view state";

    private static final String BROKEN_REACH = "CampaignUIPersistentData.getMapFilterData";

    private static final String TARGETED_GAME_VERSION = "0.98a-RC8";

    private static final String RUNNING_GAME_VERSION = "0.98a-RC9";

    private final CompatibilityFailures failureRecord = new CompatibilityFailures();

    @Nested
    class RecordReachFailure {

        @Test
        void filesTheFailureUnderTheGameWithTheDeclaredAndRunningVersions() {

            var reachFailure = new NoSuchMethodError("getMapFilterData");

            ModStateScopes.runWithGameVersions(
                CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID,
                TARGETED_GAME_VERSION,
                RUNNING_GAME_VERSION,
                () -> buildReporter().recordReachFailure(FAILURE_SITE, BROKEN_REACH, reachFailure));

            var failure = CompatibilityFailureFixture.takeNextBindingFailure(failureRecord);

            assertThat(failure.subject())
                .isEqualTo(new CompatibilitySubject("Starsector", TARGETED_GAME_VERSION, RUNNING_GAME_VERSION));
            assertThat(failure.consumer())
                .isEqualTo(CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER);
            assertThat(failure.breakage())
                .isEqualTo(new CompatibilityBreakage(FAILURE_SITE, BROKEN_REACH));
            assertThat(failure.cause())
                .isSameAs(reachFailure);
        }

        @Test
        void filesNoCauseForAReachThatAnsweredNothing() {

            ModStateScopes.runWithoutGameSettings(() ->
                buildReporter().recordReachFailure(FAILURE_SITE, BROKEN_REACH));

            assertThat(failureRecord.takeNextUnreported().cause())
                .isNull();
        }

        @Test
        void leavesBothVersionsUnreadBeforeTheSettingsAreUp() {

            ModStateScopes.runWithoutGameSettings(() ->
                buildReporter().recordReachFailure(FAILURE_SITE, BROKEN_REACH));

            assertThat(CompatibilityFailureFixture.takeNextBindingFailure(failureRecord).subject())
                .isEqualTo(new CompatibilitySubject("Starsector", null, null));
        }

        @Test
        void filesOnceHoweverOftenTheReachFails() {

            // The per-frame case: a reach failing on every frame composes its consumer once.
            var describerCalls = new AtomicInteger();
            var reporter = new GameReachReporter(
                () -> {
                    describerCalls.incrementAndGet();
                    return CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER;
                },
                failureRecord);

            ModStateScopes.runWithoutGameSettings(() -> {
                reporter.recordReachFailure(FAILURE_SITE, BROKEN_REACH);
                reporter.recordReachFailure(FAILURE_SITE, BROKEN_REACH);
            });

            assertThat(describerCalls)
                .hasValue(1);
            assertThat(failureRecord.takeNextUnreported())
                .isNotNull();
            assertThat(failureRecord.hasUnreported())
                .isFalse();
        }

        @Test
        void containsADescriberThatThrows() {

            var reporter = new GameReachReporter(
                () -> {
                    throw new IllegalStateException("The consumer's wording did not load.");
                },
                failureRecord);

            assertThatCode(() -> reporter.recordReachFailure(FAILURE_SITE, BROKEN_REACH))
                .doesNotThrowAnyException();
            assertThat(failureRecord.hasUnreported())
                .isFalse();
        }

        @Test
        void containsADescriberThatThrowsWhereNoLoggerAnswers() {
            // A report lost with nowhere to say so must still not throw into the probe.
            var reporter = new GameReachReporter(
                () -> {
                    throw new IllegalStateException("The consumer's wording did not load.");
                },
                failureRecord);

            try (var globalMock = StubbedGlobalLogger.openGlobalAnsweringLoggers()) {

                // Silenced for the reporter alone: any other class resolving its logger here keeps
                // a real one.
                globalMock
                    .when(() -> Global.getLogger(GameReachReporter.class))
                    .thenReturn(null);

                assertThatCode(() -> reporter.recordReachFailure(FAILURE_SITE, BROKEN_REACH))
                    .doesNotThrowAnyException();
            }
        }

        @Test
        void filesNothingForAReachNoPlayerWouldMiss() {

            CompatibilityFailureFixture.drainSessionRecord();

            GameReachReporter.UNREPORTED.recordReachFailure(FAILURE_SITE, BROKEN_REACH);

            assertThat(CompatibilityFailures.SESSION_RECORD.hasUnreported())
                .isFalse();
        }
    }

    private GameReachReporter buildReporter() {

        return new GameReachReporter(() -> CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER, failureRecord);
    }
}
