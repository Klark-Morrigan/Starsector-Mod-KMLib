package kmlib.mods.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.mods.console.commands.targets.MarketOwnerTarget;
import kmlib.mods.console.commands.targets.MarketOwnerTargetResolver;
import kmlib.mods.console.commands.targets.MarketTargetRequirement;
import kmlib.mods.console.commands.targets.UnresolvedTarget;
import kmlib.starsector.markets.ownership.MarketOwnershipTransfer;
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
 * Pins {@link TransferMarketCommand#runCommand} as the shell it is: which requirement it aims its
 * search under, that a refusal is passed on and hands over nothing, that a colony is not handed to
 * the faction already holding it, and what the player is told either way. Player feedback is read
 * back through a recording {@code CommandOutput} binding, so the message text is asserted without
 * a live console.
 *
 * <p>Both collaborators are stubbed through static mocks, so which place was meant and what a
 * hand-over consists of are left to their own suites - including the order the two halves of a
 * target are resolved in, which is {@code MarketOwnerTargetResolverTest}'s. Cases live under a
 * {@link Nested} group named for the method under test.
 *
 * <p>One collaborator is deliberately not stubbed: the ownership read the same-owner refusal is
 * built on. It is a comparison of the colony's own faction ID against the one resolved, so posing
 * it through the market keeps the refusal standing on the rule the hand-over itself refuses on
 * rather than on a stub agreeing with the assertion.
 *
 * <p>No case poses the player inside a star system, and that is deliberate: the sector answers
 * for no fleet, so every run here is made from outside every system. A command still guarding on
 * being in one would fail the whole suite rather than the one case about it.
 */
final class TransferMarketCommandTest {

    private static final String COLONY_NAME = "Corvus III";
    private static final String HEGEMONY_ID = "hegemony";
    private static final String INCUMBENT_OWNER_ID = "persean_league";
    private static final String PLAYER_FACTION_ID = "player";

    private MockedStatic<Global> globalMock;
    private MockedStatic<MarketOwnerTargetResolver> marketOwnerTargetResolverMock;
    private MockedStatic<MarketOwnershipTransfer> marketOwnershipTransferMock;

    private SectorAPI sectorMock;
    private MarketAPI marketMock;
    private CommandOutputFake outputFake;
    private TransferMarketCommand command;

    @BeforeEach
    void setUp() {

        // Each collaborator finishes its own stubbing before the next one's opens, so the calls
        // do not nest into an unfinished-stubbing error.
        sectorMock = mock(SectorAPI.class);

        marketMock = mock(MarketAPI.class);

        when(marketMock.getName())
            .thenReturn(COLONY_NAME);
        when(marketMock.getFactionId())
            .thenReturn(INCUMBENT_OWNER_ID);

        globalMock = mockStatic(Global.class);
        globalMock
            .when(Global::getSector)
            .thenReturn(sectorMock);

        marketOwnerTargetResolverMock = mockStatic(MarketOwnerTargetResolver.class);

        marketOwnershipTransferMock = mockStatic(MarketOwnershipTransfer.class);

        outputFake = new CommandOutputFake();
        command = new TransferMarketCommand(outputFake);
    }

    @AfterEach
    void tearDown() {
        marketOwnershipTransferMock.close();
        marketOwnerTargetResolverMock.close();
        globalMock.close();
    }

    @Nested
    class RunCommand {

        @Test
        void handsTheColonyToTheResolvedFactionAndReportsIt() {

            answerWithFaction(HEGEMONY_ID, "Hegemony");

            var result = command.runCommand("corvus_iii hegemony", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.SUCCESS);

            // Delegation is the contract: the command aims the run, MarketOwnershipTransfer owns
            // the hand-over.
            marketOwnershipTransferMock
                .verify(() -> MarketOwnershipTransfer.transferOwnership(
                    sectorMock,
                    marketMock,
                    HEGEMONY_ID));

            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Transferred Corvus III to Hegemony."));
        }

        @Test
        void aimsTheSearchAtAnExistingColonyAndPassesOnOmittedArguments() {
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
                    MarketTargetRequirement.EXISTING_COLONY));
        }

        @Test
        void namesThePlayerFactionByIdWhileItStillReportsAPlaceholder() {
            // A player faction with no identity of its own reports "Independent", which in this
            // sentence reads as having handed the colony to somebody else.
            answerWithFaction(PLAYER_FACTION_ID, "Independent");

            command.runCommand("", CommandContext.CAMPAIGN_MAP);

            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Transferred Corvus III to player."));
        }

        @Test
        void refusesAColonyTheNamedFactionAlreadyHoldsAndHandsOverNothing() {
            // Both halves resolved, so only the relation between them is wrong - and a hand-over
            // to the incumbent would still detach the colony from the owner it is not leaving.
            answerWithFaction(INCUMBENT_OWNER_ID, "Persean League");

            var result = command.runCommand(
                "corvus_iii persean_league",
                CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.ERROR);
            assertThat(outputFake.getMessages())
                .anyMatch(message ->
                    message.contains("Corvus III is already owned by Persean League."));

            marketOwnershipTransferMock
                .verifyNoInteractions();
        }

        @Test
        void passesOnTheSearchsRefusalAndHandsOverNothing() {

            marketOwnerTargetResolverMock
                .when(() -> MarketOwnerTargetResolver.resolveMarketAndOwner(
                    any(), any(), any(), any()))
                .thenReturn(new UnresolvedTarget<MarketOwnerTarget>(
                    "Nothing in Corvus is an existing colony."));

            var result = command.runCommand("", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.ERROR);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Nothing in Corvus is an existing colony."));

            marketOwnershipTransferMock
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
            marketOwnershipTransferMock
                .verifyNoInteractions();
        }

        @Test
        void returnsTheValidationResultOutsideACampaign() {

            var result = command.runCommand("", CommandContext.COMBAT_MISSION);

            assertThat(result)
                .isEqualTo(CommandResult.WRONG_CONTEXT);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("can only run in a campaign"));

            marketOwnershipTransferMock
                .verifyNoInteractions();
        }

        @Test
        void guardsOnNoStarSystemOfItsOwn() {
            // The sector answers for no fleet, so this run is made from outside every system.
            // Whether one is needed depends on the argument shape, so it is the search's
            // condition and the command must not turn a run away for it - which is what the
            // absence of that message says, the run having got as far as handing the colony over.
            answerWithFaction(HEGEMONY_ID, "Hegemony");

            var result = command.runCommand("corvus_iii hegemony", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.SUCCESS);
            assertThat(outputFake.getMessages())
                .noneMatch(message -> message.contains("star system"));
        }
    }

    // Has the search answer with this suite's colony held by a faction under the given ID and
    // display name, which is what the reported message is built from.
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
