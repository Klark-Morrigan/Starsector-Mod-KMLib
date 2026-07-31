package kmlib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.starsector.entities.Gates;
import kmlib.starsector.systems.StarSystems;
import kmlib.testfixtures.console.output.CommandOutputFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins {@link ActivateGateCommand#runCommand} on its in-system outcome
 * branches: a resolved id activates the gate via {@link Gates#activateGate} and
 * reports success; an unknown id activates nothing and reports the miss. Player
 * feedback is read back through a recording {@code CommandOutput} binding, so the
 * message text is asserted without a live console.
 *
 * <p>The static seams the command reaches through - {@code Global},
 * {@code StarSystems}, {@code Gates} - are stubbed via static mocks. With the
 * console output decoupled behind {@code CommandOutput}, the failing-validation
 * branch is pinned here too: a wrong-context run returns the validation result
 * and touches no gate, and the spec's required-id and surplus-argument checks
 * report bad syntax without resolving anything. Cases live under a {@link Nested}
 * group named for the method under test.
 */
final class ActivateGateCommandTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<StarSystems> starSystemsMock;
    private MockedStatic<Gates> gatesMock;

    private StarSystemAPI systemMock;
    private CommandOutputFake outputFake;
    private ActivateGateCommand command;

    @BeforeEach
    void setUp() {
        var sectorMock = mock(SectorAPI.class);
        systemMock = mock(StarSystemAPI.class);
        when(systemMock.getName()).thenReturn("Test System");

        globalMock = mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sectorMock);

        starSystemsMock = mockStatic(StarSystems.class);
        starSystemsMock.when(() -> StarSystems.getPlayerStarSystem(any()))
            .thenReturn(systemMock);

        gatesMock = mockStatic(Gates.class);

        outputFake = new CommandOutputFake();
        command = new ActivateGateCommand(outputFake);
    }

    @AfterEach
    void tearDown() {
        gatesMock.close();
        starSystemsMock.close();
        globalMock.close();
    }

    @Nested
    class RunCommand {
        @Test
        void reports_the_unknown_id_and_activates_nothing() {
            starSystemsMock.when(() -> StarSystems.find(systemMock, Tags.GATE, "ghost"))
                .thenReturn(null);

            var result = command.runCommand("ghost", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.ERROR);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("No gate with id 'ghost'"));
            // A typo'd id must leave the gate network dark.
            gatesMock.verifyNoInteractions();
        }

        @Test
        void activates_the_resolved_gate_and_reports_success() {
            var gateMock = mock(SectorEntityToken.class);
            starSystemsMock.when(() -> StarSystems.find(systemMock, Tags.GATE, "gate1"))
                .thenReturn(gateMock);

            var result = command.runCommand("gate1", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.SUCCESS);
            // Delegation is the contract: the command resolves, Gates owns the
            // state change.
            gatesMock.verify(() -> Gates.activateGate(gateMock));
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Activated gate 'gate1'"));
        }

        @Test
        void reports_a_missing_id_as_bad_syntax_and_resolves_nothing() {
            var result = command.runCommand("", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Missing required parameter 'id'"));
            // With no id there is nothing to resolve or activate.
            gatesMock.verifyNoInteractions();
        }

        @Test
        void reports_a_surplus_argument_as_bad_syntax() {
            var result = command.runCommand("gate1 extra", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Too many arguments"));
            gatesMock.verifyNoInteractions();
        }

        @Test
        void returns_the_validation_result_outside_a_campaign() {
            var result = command.runCommand("gate1", CommandContext.COMBAT_MISSION);

            assertThat(result).isEqualTo(CommandResult.WRONG_CONTEXT);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("can only run in a campaign"));
            // A wrong-context run resolves no gate and changes no state.
            gatesMock.verifyNoInteractions();
        }
    }
}
