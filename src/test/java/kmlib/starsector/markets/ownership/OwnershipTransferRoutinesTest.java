package kmlib.starsector.markets.ownership;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.extensions.DeclinedWork;
import kmlib.extensions.ExecutedWork;
import kmlib.extensions.FallbackToDefaults;
import kmlib.extensions.WorkOutcome;

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
 */
final class OwnershipTransferRoutinesTest {

    private static final String FACTION_ID = "hegemony";

    private List<String> offeredTo;
    private MarketAPI marketMock;

    @BeforeEach
    void setUp() {

        OwnershipTransferRoutines.clearRoutine();

        offeredTo = new ArrayList<>();
        marketMock = mock(MarketAPI.class);
    }

    @AfterEach
    void tearDown() {
        OwnershipTransferRoutines.clearRoutine();
    }

    @Nested
    class OfferTransfer {

        @Test
        void takes_nothing_where_nothing_is_installed() {
            // The answer on every install running no mod with a hand-over of its own, and the one
            // that leaves this library's own sequence to move the colony.
            assertThat(offerTransfer().wasExecuted())
                .isFalse();
        }

        @Test
        void answers_that_the_hand_over_was_taken_where_the_installed_routine_takes_it() {

            installPermittingFallback("Some Mod", buildRoutineNamed("installed", true));

            assertThat(offerTransfer().wasExecuted())
                .isTrue();
            assertThat(offeredTo)
                .containsExactly("installed");
        }

        @Test
        void leaves_the_hand_over_where_the_installed_routine_declines() {

            installPermittingFallback("Some Mod", buildRoutineNamed("installed", false));

            assertThat(offerTransfer().wasExecuted())
                .isFalse();
            assertThat(offeredTo)
                .containsExactly("installed");
        }

        @Test
        void fails_the_hand_over_where_a_routine_that_had_to_move_it_declines() {
            // A mod whose colonies cannot change hands without its own standing and intel moving
            // with them has no correct outcome from the plain sequence, so the run stops here.
            installForbiddingFallback("Total Conversion", buildRoutineNamed("installed", false));

            assertThatThrownBy(OwnershipTransferRoutinesTest.this::offerTransfer)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Total Conversion");
        }
    }

    @Nested
    class RegisterRoutine {

        @Test
        void offers_the_hand_over_to_the_last_routine_registered() {
            // One routine moves colonies on an install, so registering is replacing: the mod that
            // registered first is not consulted afterwards, and is not meant to be.
            installPermittingFallback("First Mod", buildRoutineNamed("first", true));
            installPermittingFallback("Second Mod", buildRoutineNamed("second", true));

            offerTransfer();

            assertThat(offeredTo)
                .containsExactly("second");
        }

        @Test
        void answers_with_the_name_the_installed_routine_was_registered_under() {

            installPermittingFallback("Some Mod", buildRoutineNamed("installed", true));

            assertThat(OwnershipTransferRoutines.readRoutineName())
                .isEqualTo("Some Mod");
        }
    }

    @Nested
    class ReadRoutine {

        @Test
        void answers_with_what_this_install_registered() {

            var ownershipTransferRoutine = buildRoutineNamed("installed", false);

            installPermittingFallback("Some Mod", ownershipTransferRoutine);

            assertThat(OwnershipTransferRoutines.readRoutine())
                .isSameAs(ownershipTransferRoutine);
        }

        @Test
        void is_absent_where_nothing_registered_one() {

            assertThat(OwnershipTransferRoutines.readRoutine())
                .isNull();
            assertThat(OwnershipTransferRoutines.readRoutineName())
                .isNull();
        }
    }

    private WorkOutcome offerTransfer() {
        return OwnershipTransferRoutines.offerTransfer(marketMock, FACTION_ID);
    }

    private static void installForbiddingFallback(
            String integrationName,
            OwnershipTransferRoutine ownershipTransferRoutine) {

        OwnershipTransferRoutines.registerRoutine(
            integrationName,
            ownershipTransferRoutine,
            FallbackToDefaults.FORBIDDEN);
    }

    private static void installPermittingFallback(
            String integrationName,
            OwnershipTransferRoutine ownershipTransferRoutine) {

        OwnershipTransferRoutines.registerRoutine(
            integrationName,
            ownershipTransferRoutine,
            FallbackToDefaults.PERMITTED);
    }

    // A routine that records having been offered a hand-over and answers the stated verdict, so a
    // case can assert both who was asked and what came of it.
    private OwnershipTransferRoutine buildRoutineNamed(String name, boolean takesTheHandOver) {

        return (market, factionId) -> {
            offeredTo.add(name);
            return takesTheHandOver ? new ExecutedWork() : new DeclinedWork("this stub declines");
        };
    }
}
