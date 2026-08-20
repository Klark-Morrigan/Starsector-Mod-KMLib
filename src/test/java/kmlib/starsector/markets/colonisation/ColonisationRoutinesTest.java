package kmlib.starsector.markets.colonisation;

import com.fs.starfarer.api.campaign.SectorAPI;
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
 */
final class ColonisationRoutinesTest {

    private static final int COLONY_SIZE = 3;
    private static final String FACTION_ID = "hegemony";

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
        void takes_nothing_where_nothing_is_installed() {
            // The answer on every install running no mod with a colonisation of its own, and the
            // one that leaves the game's own sequence to found the colony.
            assertThat(offerColonisation().wasExecuted())
                .isFalse();
        }

        @Test
        void answers_that_the_founding_was_taken_where_the_installed_routine_takes_it() {

            installPermittingFallback("Some Mod", buildRoutineNamed("installed", true));

            assertThat(offerColonisation().wasExecuted())
                .isTrue();
            assertThat(offeredTo)
                .containsExactly("installed");
        }

        @Test
        void leaves_the_founding_where_the_installed_routine_declines() {
            // Declining is how a routine says this install, or this body, is not its case - the
            // game's own sequence founds the colony instead.
            installPermittingFallback("Some Mod", buildRoutineNamed("installed", false));

            assertThat(offerColonisation().wasExecuted())
                .isFalse();
            assertThat(offeredTo)
                .containsExactly("installed");
        }

        @Test
        void fails_the_founding_where_a_routine_that_had_to_found_declines() {
            // A mod whose colonies are not the game's colonies has no correct outcome from the
            // plainer founding, so the run stops here rather than producing one nothing on that
            // install would recognise.
            installForbiddingFallback("Total Conversion", buildRoutineNamed("installed", false));

            assertThatThrownBy(ColonisationRoutinesTest.this::offerColonisation)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Total Conversion");
        }
    }

    @Nested
    class RegisterRoutine {

        @Test
        void offers_the_founding_to_the_last_routine_registered() {
            // One routine founds colonies on an install, so registering is replacing: the mod that
            // registered first is not consulted afterwards, and is not meant to be.
            installPermittingFallback("First Mod", buildRoutineNamed("first", true));
            installPermittingFallback("Second Mod", buildRoutineNamed("second", true));

            offerColonisation();

            assertThat(offeredTo)
                .containsExactly("second");
        }

        @Test
        void answers_with_the_name_the_installed_routine_was_registered_under() {
            // What a reader asks the register: which mod founds colonies on this install.
            installPermittingFallback("Some Mod", buildRoutineNamed("installed", true));

            assertThat(ColonisationRoutines.readRoutineName())
                .isEqualTo("Some Mod");
        }
    }

    @Nested
    class ReadRoutine {

        @Test
        void answers_with_what_this_install_registered() {

            var colonisationRoutine = buildRoutineNamed("installed", false);

            installPermittingFallback("Some Mod", colonisationRoutine);

            assertThat(ColonisationRoutines.readRoutine())
                .isSameAs(colonisationRoutine);
        }

        @Test
        void is_absent_where_nothing_registered_one() {

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

    private static void installForbiddingFallback(
            String integrationName,
            ColonisationRoutine colonisationRoutine) {

        ColonisationRoutines.registerRoutine(
            integrationName,
            colonisationRoutine,
            FallbackToDefaults.FORBIDDEN);
    }

    private static void installPermittingFallback(
            String integrationName,
            ColonisationRoutine colonisationRoutine) {

        ColonisationRoutines.registerRoutine(
            integrationName,
            colonisationRoutine,
            FallbackToDefaults.PERMITTED);
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
