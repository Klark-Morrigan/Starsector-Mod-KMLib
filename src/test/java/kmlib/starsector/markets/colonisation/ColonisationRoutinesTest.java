package kmlib.starsector.markets.colonisation;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.extensions.DeclinedWork;
import kmlib.extensions.ExecutedWork;
import kmlib.extensions.FallbackToDefaults;
import kmlib.extensions.WorkOutcome;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

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
 * Pins what a founding is offered to: that an install with nothing installed takes nothing, that
 * the installed routine's verdict is the answer, that a second registration replaces the first
 * outright rather than queueing behind it, and that a routine which had to found and did not fails
 * the run.
 *
 * <p>The point is one per running game, so each case empties it before and after itself - a
 * routine left behind would be offered foundings in whatever suite ran next.
 *
 * <p>Routines here record that they were offered and answer a stated verdict, rather than founding
 * anything: what a founding consists of is {@code MarketColoniserTest}'s, and what this pins is
 * only who gets asked and what comes of it.
 *
 * <p>Of a routine that fails, only what this port adds is pinned here: that the failure reaches
 * the record under the integration that registered it, at a founding. What the point does with the
 * routine afterwards is {@code ExtensionPointTest}'s.
 */
final class ColonisationRoutinesTest {

    private static final int COLONY_SIZE = 3;
    private static final String FACTION_ID = "hegemony";

    // Where every routine here reports, so a failing case records into a record of its own rather
    // than into the session's.
    private final CompatibilityFailures failureRecord = new CompatibilityFailures();

    private List<String> offeredTo;
    private SectorAPI sectorMock;
    private MarketAPI marketMock;

    @BeforeEach
    void setUp() {

        ColonisationRoutines.clearRoutine();

        offeredTo = new ArrayList<>();
        sectorMock = mock(SectorAPI.class);
        marketMock = mock(MarketAPI.class);
    }

    @AfterEach
    void tearDown() {
        ColonisationRoutines.clearRoutine();
    }

    @Nested
    class OfferColonisation {

        @Test
        void takesNothingWhereNothingIsInstalled() {
            // The answer on every install running no mod with a colonisation of its own, and the
            // one that leaves the game's own sequence to found the colony.
            assertThat(offerColonisation().wasExecuted())
                .isFalse();
        }

        @Test
        void answersThatTheFoundingWasTakenWhereTheInstalledRoutineTakesIt() {

            installPermittingFallback("Some Mod", buildRoutineNamed("installed", true));

            assertThat(offerColonisation().wasExecuted())
                .isTrue();
            assertThat(offeredTo)
                .containsExactly("installed");
        }

        @Test
        void leavesTheFoundingWhereTheInstalledRoutineDeclines() {
            // Declining is how a routine says this install, or this body, is not its case - the
            // game's own sequence founds the colony instead.
            installPermittingFallback("Some Mod", buildRoutineNamed("installed", false));

            assertThat(offerColonisation().wasExecuted())
                .isFalse();
            assertThat(offeredTo)
                .containsExactly("installed");
        }

        @Test
        void failsTheFoundingWhereARoutineThatHadToFoundDeclines() {
            // A mod whose colonies are not the game's colonies has no correct outcome from the
            // plainer founding, so the run stops here rather than producing one nothing on that
            // install would recognise.
            installForbiddingFallback("Total Conversion", buildRoutineNamed("installed", false));

            assertThatThrownBy(ColonisationRoutinesTest.this::offerColonisation)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Total Conversion");
        }

        @Test
        void leavesTheFoundingAndReportsTheIntegrationWhereTheRoutineCouldNotLink() {
            // How a mod that changed underneath its routine is met: on the first founding, where
            // nothing was founded yet - so the game's own sequence runs, and the player is told.
            installPermittingFallback("Some Mod", (sector, market, factionId, colonySize) -> {
                throw new NoSuchMethodError("the mod moved what the routine founds with");
            });

            assertThat(offerColonisation().wasExecuted())
                .isFalse();

            var failure = failureRecord.takeNextUnreported();

            assertThat(failure.subject().name())
                .isEqualTo(CompatibilityFailureFixture.INTEGRATED_MOD_NAME);
            assertThat(failure.breakage().failureSite())
                .isEqualTo("founding a colony");
        }

        @Test
        void passesOnAndReportsARoutineThatFailedPartwayThroughAFounding() {
            // The market may be half founded, which the game's own sequence must not build on.
            var foundingFailure = new IllegalStateException("half the colony was founded");
            installPermittingFallback("Some Mod", (sector, market, factionId, colonySize) -> {
                throw foundingFailure;
            });

            assertThatThrownBy(ColonisationRoutinesTest.this::offerColonisation)
                .isSameAs(foundingFailure);
            assertThat(failureRecord.takeNextUnreported().cause())
                .isSameAs(foundingFailure);
        }
    }

    @Nested
    class RegisterRoutine {

        @Test
        void offersTheFoundingToTheLastRoutineRegistered() {
            // One routine founds colonies on an install, so registering is replacing: the mod that
            // registered first is not consulted afterwards, and is not meant to be.
            installPermittingFallback("First Mod", buildRoutineNamed("first", true));
            installPermittingFallback("Second Mod", buildRoutineNamed("second", true));

            offerColonisation();

            assertThat(offeredTo)
                .containsExactly("second");
        }

        @Test
        void answersWithTheNameTheInstalledRoutineWasRegisteredUnder() {
            // What a reader asks the register: which mod founds colonies on this install.
            installPermittingFallback("Some Mod", buildRoutineNamed("installed", true));

            assertThat(ColonisationRoutines.readRoutineName())
                .isEqualTo("Some Mod");
        }
    }

    @Nested
    class ReadRoutine {

        @Test
        void answersWithWhatThisInstallRegistered() {

            var colonisationRoutine = buildRoutineNamed("installed", false);

            installPermittingFallback("Some Mod", colonisationRoutine);

            assertThat(ColonisationRoutines.readRoutine())
                .isSameAs(colonisationRoutine);
        }

        @Test
        void isAbsentWhereNothingRegisteredOne() {

            assertThat(ColonisationRoutines.readRoutine())
                .isNull();
            assertThat(ColonisationRoutines.readRoutineName())
                .isNull();
        }
    }

    private WorkOutcome offerColonisation() {
        return ColonisationRoutines.offerColonisation(
            sectorMock,
            marketMock,
            FACTION_ID,
            COLONY_SIZE);
    }

    private void installForbiddingFallback(
            String integrationName,
            ColonisationRoutine colonisationRoutine) {

        ColonisationRoutines.registerRoutine(
            integrationName,
            colonisationRoutine,
            FallbackToDefaults.FORBIDDEN,
            () -> CompatibilityFailureFixture.MOD_INTEGRATION,
            failureRecord);
    }

    private void installPermittingFallback(
            String integrationName,
            ColonisationRoutine colonisationRoutine) {

        ColonisationRoutines.registerRoutine(
            integrationName,
            colonisationRoutine,
            FallbackToDefaults.PERMITTED,
            () -> CompatibilityFailureFixture.MOD_INTEGRATION,
            failureRecord);
    }

    // A routine that records having been offered a founding and answers the stated verdict, so a
    // case can assert both who was asked and what came of it.
    private ColonisationRoutine buildRoutineNamed(String name, boolean takesTheFounding) {

        return (sector, market, factionId, colonySize) -> {
            offeredTo.add(name);
            return takesTheFounding ? new ExecutedWork() : new DeclinedWork("this stub declines");
        };
    }
}
