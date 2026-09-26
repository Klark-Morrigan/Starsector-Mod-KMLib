package kmlib.mods.nexerelin;

import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.compatibility.IntegrationFailureReporter;
import kmlib.starsector.factions.alliances.AllianceRecord;
import kmlib.starsector.factions.alliances.AllianceSource;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

/**
 * Pins what an alliance read does once the mod behind it stops answering: it reads no alliances
 * from then on, is never asked again, and is reported once.
 *
 * <p>The never-again half is the one worth the most. Alliances are read by a poll every few seconds
 * and by every map rebuild, so a read that kept failing would log and throw on each for the rest of
 * the session.
 */
final class GuardedAllianceSourceTest {

    private static final AllianceRecord ALLIANCE =
        new AllianceRecord("alliance-1", "The Accord", List.of("hegemony", "sindrian_diktat"));

    private final CompatibilityFailures failureRecord = new CompatibilityFailures();

    @Nested
    class ReadAlliances {

        @Test
        void answersWhatTheGuardedReadAnswers() {

            var guardedSource = buildGuardedSource(() -> List.of(ALLIANCE));

            assertThat(guardedSource.readAlliances())
                .containsExactly(ALLIANCE);
        }

        @Test
        void readsNoAlliancesWhereTheReadCouldNotLink() {
            // How a Nexerelin release that moved its alliance manager is met. No alliances is what
            // every install without the mod reads, so a caller needs nothing further.
            var guardedSource = buildGuardedSource(() -> {
                throw new NoClassDefFoundError("exerelin/campaign/AllianceManager");
            });

            assertThat(guardedSource.readAlliances())
                .isEmpty();
        }

        @Test
        void readsNoAlliancesWhereTheReadThrew() {
            // A read has no half-done work to protect, so a throw answers as a link failure does.
            var guardedSource = buildGuardedSource(() -> {
                throw new IllegalStateException("the alliance list was mid-change");
            });

            assertThat(guardedSource.readAlliances())
                .isEmpty();
        }

        @Test
        void neverAsksAReadAgainOnceItFailed() {

            var readsReceived = new AtomicInteger();
            var guardedSource = buildGuardedSource(() -> {
                readsReceived.incrementAndGet();
                throw new NoClassDefFoundError("exerelin/campaign/AllianceManager");
            });

            guardedSource.readAlliances();
            guardedSource.readAlliances();

            assertThat(readsReceived)
                .hasValue(1);
        }

        @Test
        void reportsAFailedReadOnceUnderTheIntegrationItWasBuiltWith() {

            var guardedSource = buildGuardedSource(() -> {
                throw new NoClassDefFoundError("exerelin/campaign/AllianceManager");
            });

            guardedSource.readAlliances();
            guardedSource.readAlliances();

            var failure = failureRecord.takeNextUnreported();

            assertThat(failure.subject().name())
                .isEqualTo(CompatibilityFailureFixture.INTEGRATED_MOD_NAME);
            assertThat(failure.breakage().failureSite())
                .isEqualTo("reading the alliances standing");
            assertThat(failureRecord.takeNextUnreported())
                .isNull();
        }

        @Test
        void readsNoAlliancesRatherThanPropagatingAReportThatThrew() {
            // The report is composed from wording that may not have loaded, on a read a poll
            // repeats every few seconds - a report that threw would take the read down with it.
            var guardedSource = new GuardedAllianceSource(
                () -> {
                    throw new NoClassDefFoundError("exerelin/campaign/AllianceManager");
                },
                new IntegrationFailureReporter(
                    () -> {
                        throw new IllegalArgumentException("the wording did not load");
                    },
                    failureRecord,
                    mock(Logger.class)));

            assertThatCode(guardedSource::readAlliances)
                .doesNotThrowAnyException();
        }
    }

    private GuardedAllianceSource buildGuardedSource(AllianceSource allianceSource) {

        return new GuardedAllianceSource(
            allianceSource,
            new IntegrationFailureReporter(
                () -> CompatibilityFailureFixture.MOD_INTEGRATION,
                failureRecord,
                mock(Logger.class)));
    }
}
