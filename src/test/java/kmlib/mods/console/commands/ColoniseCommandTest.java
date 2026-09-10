package kmlib.mods.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.mods.console.commands.targets.MarketOwnerTarget;
import kmlib.mods.console.commands.targets.MarketOwnerTargetResolver;
import kmlib.mods.console.commands.targets.MarketTargetRequirement;
import kmlib.mods.console.commands.targets.UnresolvedTarget;
import kmlib.starsector.markets.colonisation.MarketColoniser;
import kmlib.testfixtures.mods.console.commands.output.CommandOutputFake;

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
 * Pins {@link ColoniseCommand#runCommand} as the shell it is: which requirement it aims its
 * search under, that a refusal is passed on and founds nothing, and what the player is told about
 * the colony that now exists. Player feedback is read back through a recording
 * {@code CommandOutput} binding, so the message text is asserted without a live console.
 *
 * <p>Both collaborators are stubbed through static mocks, so which place was meant and what
 * founding a colony consists of are left to their own suites - including the order the two halves
 * of a target are resolved in, which is {@code MarketOwnerTargetResolverTest}'s. Cases live under
 * a {@link Nested} group named for the method under test.
 *
 * <p>No case poses the player inside a star system, and that is deliberate: the sector answers
 * for no fleet, so every run here is made from outside every system. A command still guarding on
 * being in one would fail the whole suite rather than the one case about it.
 */
final class ColoniseCommandTest {

    private static final String COLONY_NAME = "Corvus III";
    private static final String HEGEMONY_ID = "hegemony";
    private static final String PLAYER_FACTION_ID = "player";

    private MockedStatic<Global> globalMock;
    private MockedStatic<MarketOwnerTargetResolver> marketOwnerTargetResolverMock;
    private MockedStatic<MarketColoniser> marketColoniserMock;

    private SectorAPI sectorMock;
    private MarketAPI marketMock;
    private CommandOutputFake outputFake;
    private ColoniseCommand command;

    @BeforeEach
    void setUp() {

        // Each collaborator finishes its own stubbing before the next one's opens, so the calls
        // do not nest into an unfinished-stubbing error.
        sectorMock = mock(SectorAPI.class);

        marketMock = mock(MarketAPI.class);

        when(marketMock.getName())
            .thenReturn(COLONY_NAME);

        globalMock = mockStatic(Global.class);
        globalMock
            .when(Global::getSector)
            .thenReturn(sectorMock);

        marketOwnerTargetResolverMock = mockStatic(MarketOwnerTargetResolver.class);

        marketColoniserMock = mockStatic(MarketColoniser.class);

        outputFake = new CommandOutputFake();
        command = new ColoniseCommand(outputFake);
    }

    @AfterEach
    void tearDown() {
        marketColoniserMock.close();
        marketOwnerTargetResolverMock.close();
        globalMock.close();
    }

    @Nested
    class RunCommand {

        @Test
        void foundsAColonyForTheResolvedFactionAndReportsIt() {

            answerWithFaction(HEGEMONY_ID, "Hegemony");

            var result = command.runCommand("corvus_iii hegemony", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.SUCCESS);

            // Delegation is the contract: the command aims the run, MarketColoniser owns the
            // founding.
            marketColoniserMock
                .verify(() -> MarketColoniser.establishColony(
                    sectorMock,
                    marketMock,
                    HEGEMONY_ID));

            assertThat(outputFake.getMessages())
                .anyMatch(message ->
                    message.contains("Founded a colony on Corvus III for Hegemony."));
        }

        @Test
        void aimsTheSearchAtAColonisableBodyAndPassesOnOmittedArguments() {
            // Neither argument is defaulted on the command line, so what an omission means stays
            // the search's answer to give.
            answerWithFaction(PLAYER_FACTION_ID, "Sabre Company");

            var result = command.runCommand("", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.SUCCESS);

            marketOwnerTargetResolverMock
                .verify(() -> MarketOwnerTargetResolver.resolveMarketAndOwner(
                    sectorMock,
                    null,
                    null,
                    MarketTargetRequirement.COLONISABLE_BODY));
        }

        @Test
        void namesThePlayerFactionByIdWhileItStillReportsAPlaceholder() {
            // A player faction with no identity of its own reports "Independent", which in this
            // sentence reads as having colonised the body for somebody else.
            answerWithFaction(PLAYER_FACTION_ID, "Independent");

            command.runCommand("", CommandContext.CAMPAIGN_MAP);

            assertThat(outputFake.getMessages())
                .anyMatch(message ->
                    message.contains("Founded a colony on Corvus III for player."));
        }

        @Test
        void passesOnTheSearchsRefusalAndFoundsNothing() {

            marketOwnerTargetResolverMock
                .when(() -> MarketOwnerTargetResolver.resolveMarketAndOwner(
                    any(), any(), any(), any()))
                .thenReturn(new UnresolvedTarget<MarketOwnerTarget>(
                    "Nothing in Corvus is a body ready for colonisation."));

            var result = command.runCommand("", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.ERROR);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Nothing in Corvus is a body ready for "
                    + "colonisation."));

            marketColoniserMock
                .verifyNoInteractions();
        }

        @Test
        void reportsASurplusArgumentAsBadSyntaxAndResolvesNothing() {

            var result = command.runCommand(
                "corvus_iii hegemony extra",
                CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Too many arguments"));

            marketOwnerTargetResolverMock
                .verifyNoInteractions();
            marketColoniserMock
                .verifyNoInteractions();
        }

        @Test
        void returnsTheValidationResultOutsideACampaign() {

            var result = command.runCommand("", CommandContext.COMBAT_MISSION);

            assertThat(result)
                .isEqualTo(CommandResult.WRONG_CONTEXT);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("can only run in a campaign"));

            marketColoniserMock
                .verifyNoInteractions();
        }

        @Test
        void guardsOnNoStarSystemOfItsOwn() {
            // The sector answers for no fleet, so this run is made from outside every system.
            // Whether one is needed depends on the argument shape, so it is the search's
            // condition and the command must not turn a run away for it - which is what the
            // absence of that message says, the run having got as far as founding a colony.
            answerWithFaction(HEGEMONY_ID, "Hegemony");

            var result = command.runCommand("corvus_iii hegemony", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.SUCCESS);
            assertThat(outputFake.getMessages())
                .noneMatch(message -> message.contains("star system"));
        }
    }

    // Has the search answer with this suite's market held by a faction under the given id and
    // display name, which is what the success message is built from.
    private void answerWithFaction(String factionId, String displayName) {

        // The target is built before the stubbing opens: the fixture stubs a faction of its own,
        // and doing that inside the thenReturn would nest one stubbing in another.
        var resolvedTarget =
            CommandTargetFixture.buildResolvedTarget(marketMock, factionId, displayName);

        marketOwnerTargetResolverMock
            .when(() -> MarketOwnerTargetResolver.resolveMarketAndOwner(any(), any(), any(), any()))
            .thenReturn(resolvedTarget);
    }
}
