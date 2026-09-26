package kmlib.starsector.markets.ownership;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.extensions.DeclinedWork;
import kmlib.extensions.ExecutedWork;
import kmlib.extensions.FallbackToDefaults;
import kmlib.extensions.WorkOutcome;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.compatibility.IntegrationFailureReporter;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Pins what a hand-over is offered to: that an install with nothing installed takes nothing, that
 * the installed routine's verdict is the answer, that a second registration replaces the first
 * outright rather than queueing behind it, and that a routine which had to move the colony and did
 * not fails the run.
 *
 * <p>The point is one per running game, so each case empties it before and after itself - a
 * routine left behind would be offered hand-overs in whatever suite ran next.
 *
 * <p>Routines here record that they were offered and answer a stated verdict, rather than moving
 * anything: what a hand-over consists of is {@code MarketOwnershipTransferTest}'s, and what this
 * pins is only who gets asked and what comes of it.
 *
 * <p>Of a routine that fails, only what this port adds is pinned here: that the failure reaches
 * the record under the integration that registered it, at a hand-over. What the point does with
 * the routine afterwards is {@code ExtensionPointTest}'s.
 */
final class OwnershipTransferRoutinesTest {

    private static final String FACTION_ID = "hegemony";

    // Where every routine here reports, so a failing case records into a record of its own rather
    // than into the session's.
    private final CompatibilityFailures failureRecord = new CompatibilityFailures();

    private final IntegrationFailureReporter failureReporter = new IntegrationFailureReporter(
        () -> CompatibilityFailureFixture.MOD_INTEGRATION,
        failureRecord,
        mock(Logger.class));

    private List<String> offeredTo;
    private MarketAPI marketMock;
    private SectorAPI sectorMock;

    @BeforeEach
    void setUp() {

        OwnershipTransferRoutines.clearRoutine();

        offeredTo = new ArrayList<>();
        marketMock = mock(MarketAPI.class);
        sectorMock = mock(SectorAPI.class);
    }

    @AfterEach
    void tearDown() {
        OwnershipTransferRoutines.clearRoutine();
    }

    @Nested
    class OfferTransfer {

        @Test
        void takesNothingWhereNothingIsInstalled() {
            // The answer on every install running no mod with a hand-over of its own, and the one
            // that leaves this library's own sequence to move the colony.
            assertThat(offerTransfer().wasExecuted())
                .isFalse();
        }

        @Test
        void answersThatTheHandOverWasTakenWhereTheInstalledRoutineTakesIt() {

            installPermittingFallback("Some Mod", buildRoutineNamed("installed", true));

            assertThat(offerTransfer().wasExecuted())
                .isTrue();
            assertThat(offeredTo)
                .containsExactly("installed");
        }

        @Test
        void leavesTheHandOverWhereTheInstalledRoutineDeclines() {

            installPermittingFallback("Some Mod", buildRoutineNamed("installed", false));

            assertThat(offerTransfer().wasExecuted())
                .isFalse();
            assertThat(offeredTo)
                .containsExactly("installed");
        }

        @Test
        void failsTheHandOverWhereARoutineThatHadToMoveItDeclines() {
            // A mod whose colonies cannot change hands without its own standing and intel moving
            // with them has no correct outcome from the plain sequence, so the run stops here.
            installForbiddingFallback("Total Conversion", buildRoutineNamed("installed", false));

            assertThatThrownBy(OwnershipTransferRoutinesTest.this::offerTransfer)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Total Conversion");
        }

        @Test
        void leavesTheHandOverAndReportsTheIntegrationWhereTheRoutineCouldNotLink() {
            // How a mod that changed underneath its routine is met: on the first hand-over, where
            // nothing had moved yet - so this library's own sequence runs, and the player is told.
            installPermittingFallback("Some Mod", (sector, market, factionId) -> {
                throw new NoSuchMethodError("the mod moved what the routine hands over with");
            });

            assertThat(offerTransfer().wasExecuted())
                .isFalse();

            var failure = failureRecord.takeNextUnreported();

            assertThat(failure.subject().name())
                .isEqualTo(CompatibilityFailureFixture.INTEGRATED_MOD_NAME);
            assertThat(failure.breakage().failureSite())
                .isEqualTo("handing a colony over");
        }

        @Test
        void passesOnAndReportsARoutineThatFailedPartwayThroughAHandOver() {
            // The colony may be half moved, which this library's own sequence must not build on.
            var handOverFailure = new IllegalStateException("half the colony was handed over");
            installPermittingFallback("Some Mod", (sector, market, factionId) -> {
                throw handOverFailure;
            });

            assertThatThrownBy(OwnershipTransferRoutinesTest.this::offerTransfer)
                .isSameAs(handOverFailure);
            assertThat(failureRecord.takeNextUnreported().cause())
                .isSameAs(handOverFailure);
        }
    }

    @Nested
    class RegisterRoutine {

        @Test
        void offersTheHandOverToTheLastRoutineRegistered() {
            // One routine moves colonies on an install, so registering is replacing: the mod that
            // registered first is not consulted afterwards, and is not meant to be.
            installPermittingFallback("First Mod", buildRoutineNamed("first", true));
            installPermittingFallback("Second Mod", buildRoutineNamed("second", true));

            offerTransfer();

            assertThat(offeredTo)
                .containsExactly("second");
        }

        @Test
        void answersWithTheNameTheInstalledRoutineWasRegisteredUnder() {

            installPermittingFallback("Some Mod", buildRoutineNamed("installed", true));

            assertThat(OwnershipTransferRoutines.readRoutineName())
                .isEqualTo("Some Mod");
        }
    }

    @Nested
    class ReadRoutine {

        @Test
        void answersWithWhatThisInstallRegistered() {

            var ownershipTransferRoutine = buildRoutineNamed("installed", false);

            installPermittingFallback("Some Mod", ownershipTransferRoutine);

            assertThat(OwnershipTransferRoutines.readRoutine())
                .isSameAs(ownershipTransferRoutine);
        }

        @Test
        void isAbsentWhereNothingRegisteredOne() {

            assertThat(OwnershipTransferRoutines.readRoutine())
                .isNull();
            assertThat(OwnershipTransferRoutines.readRoutineName())
                .isNull();
        }
    }

    private WorkOutcome offerTransfer() {
        return OwnershipTransferRoutines.offerTransfer(sectorMock, marketMock, FACTION_ID);
    }

    private void installForbiddingFallback(
            String integrationName,
            OwnershipTransferRoutine ownershipTransferRoutine) {

        OwnershipTransferRoutines.registerRoutine(
            integrationName,
            ownershipTransferRoutine,
            FallbackToDefaults.FORBIDDEN,
            failureReporter);
    }

    private void installPermittingFallback(
            String integrationName,
            OwnershipTransferRoutine ownershipTransferRoutine) {

        OwnershipTransferRoutines.registerRoutine(
            integrationName,
            ownershipTransferRoutine,
            FallbackToDefaults.PERMITTED,
            failureReporter);
    }

    // A routine that records having been offered a hand-over and answers the stated verdict, so a
    // case can assert both who was asked and what came of it.
    private OwnershipTransferRoutine buildRoutineNamed(String name, boolean takesTheHandOver) {

        return (sector, market, factionId) -> {
            offeredTo.add(name);
            return takesTheHandOver ? new ExecutedWork() : new DeclinedWork("this stub declines");
        };
    }
}
