package kmlib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.console.factions.FactionTargetResolver;
import kmlib.console.factions.ResolvedFactionTarget;
import kmlib.console.factions.UnresolvedFactionTarget;
import kmlib.console.markets.MarketTargetRequirement;
import kmlib.console.markets.MarketTargetResolver;
import kmlib.console.markets.ResolvedMarketTarget;
import kmlib.console.markets.UnresolvedMarketTarget;
import kmlib.starsector.markets.MarketColoniser;
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
 * Pins {@link ColoniseCommand#runCommand} as the shell it is: what it asks of each collaborator,
 * which of their refusals it passes on, and that nothing is founded until both refusals are past.
 * Player feedback is read back through a recording {@code CommandOutput} binding, so the message
 * text is asserted without a live console.
 *
 * <p>The three collaborators are stubbed through static mocks, so what makes a body colonisable,
 * which body is nearest and what founding a colony consists of are left to their own suites.
 * What is asserted here is the wiring between them - the requirement the search is posed under,
 * the arguments an omitted one becomes, and the order the two refusals are taken in. Cases live
 * under a {@link Nested} group named for the method under test.
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
    private MockedStatic<MarketTargetResolver> marketTargetResolverMock;
    private MockedStatic<FactionTargetResolver> factionTargetResolverMock;
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

        marketTargetResolverMock = mockStatic(MarketTargetResolver.class);
        marketTargetResolverMock
            .when(() -> MarketTargetResolver.resolveTargetMarket(any(), any(), any()))
            .thenReturn(new ResolvedMarketTarget(marketMock));

        factionTargetResolverMock = mockStatic(FactionTargetResolver.class);

        marketColoniserMock = mockStatic(MarketColoniser.class);

        outputFake = new CommandOutputFake();
        command = new ColoniseCommand(outputFake);
    }

    @AfterEach
    void tearDown() {
        marketColoniserMock.close();
        factionTargetResolverMock.close();
        marketTargetResolverMock.close();
        globalMock.close();
    }

    @Nested
    class RunCommand {

        @Test
        void founds_a_colony_for_the_named_faction_and_reports_it() {

            answerWithFaction(HEGEMONY_ID, "Hegemony");

            var result = command.runCommand("corvus_iii hegemony", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.SUCCESS);

            // Delegation is the contract: the command resolves, MarketColoniser owns the
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
        void colonises_the_nearest_colonisable_body_for_the_player_on_a_bare_invocation() {
            // Both arguments are left unsupplied rather than defaulted on the command line, so
            // what an omission means stays the resolvers' answer to give.
            answerWithFaction(PLAYER_FACTION_ID, "Sabre Company");

            var result = command.runCommand("", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.SUCCESS);

            marketTargetResolverMock
                .verify(() -> MarketTargetResolver.resolveTargetMarket(
                    sectorMock,
                    null,
                    MarketTargetRequirement.COLONISABLE_BODY));
            factionTargetResolverMock
                .verify(() -> FactionTargetResolver.resolveOwningFaction(sectorMock, null));
        }

        @Test
        void names_the_player_faction_by_id_while_it_still_reports_a_placeholder() {
            // A player faction with no identity of its own reports "Independent", which in this
            // sentence reads as having colonised the body for somebody else.
            answerWithFaction(PLAYER_FACTION_ID, "Independent");

            command.runCommand("", CommandContext.CAMPAIGN_MAP);

            assertThat(outputFake.getMessages())
                .anyMatch(message ->
                    message.contains("Founded a colony on Corvus III for player."));
        }

        @Test
        void passes_on_the_searchs_refusal_and_founds_nothing() {

            marketTargetResolverMock
                .when(() -> MarketTargetResolver.resolveTargetMarket(any(), any(), any()))
                .thenReturn(new UnresolvedMarketTarget("Nothing in Corvus is a body ready for "
                    + "colonisation."));

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
        void passes_on_an_unknown_factions_refusal_and_founds_nothing() {
            // The body was found and would have been colonised; a mistyped owner has to leave it
            // exactly as it was rather than half-colonised under nobody.
            factionTargetResolverMock
                .when(() -> FactionTargetResolver.resolveOwningFaction(any(), any()))
                .thenReturn(new UnresolvedFactionTarget("No faction with id 'hegmony'."));

            var result = command.runCommand("corvus_iii hegmony", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.ERROR);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("No faction with id 'hegmony'."));

            marketColoniserMock
                .verifyNoInteractions();
        }

        @Test
        void reports_the_place_rather_than_the_owner_when_both_are_wrong() {
            // A run with two mistakes in it has to report one of them, and the place is the
            // argument a player is likelier to have got wrong - so the target is resolved first
            // and its refusal is the one that gets said.
            marketTargetResolverMock
                .when(() -> MarketTargetResolver.resolveTargetMarket(any(), any(), any()))
                .thenReturn(new UnresolvedMarketTarget("No entity with id 'corvus_iv' in the "
                    + "sector."));
            factionTargetResolverMock
                .when(() -> FactionTargetResolver.resolveOwningFaction(any(), any()))
                .thenReturn(new UnresolvedFactionTarget("No faction with id 'hegmony'."));

            command.runCommand("corvus_iv hegmony", CommandContext.CAMPAIGN_MAP);

            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("No entity with id 'corvus_iv'"))
                .noneMatch(message -> message.contains("No faction"));
        }

        @Test
        void reports_a_surplus_argument_as_bad_syntax_and_resolves_nothing() {

            var result = command.runCommand(
                "corvus_iii hegemony extra",
                CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Too many arguments"));

            marketTargetResolverMock
                .verifyNoInteractions();
            marketColoniserMock
                .verifyNoInteractions();
        }

        @Test
        void returns_the_validation_result_outside_a_campaign() {

            var result = command.runCommand("", CommandContext.COMBAT_MISSION);

            assertThat(result)
                .isEqualTo(CommandResult.WRONG_CONTEXT);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("can only run in a campaign"));

            marketColoniserMock
                .verifyNoInteractions();
        }

        @Test
        void guards_on_no_star_system_of_its_own() {
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

    // Has the owner search answer with a faction under the given id and display name, which is
    // what the success message is built from.
    private void answerWithFaction(String factionId, String displayName) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(factionId);
        when(factionMock.getDisplayName())
            .thenReturn(displayName);

        factionTargetResolverMock
            .when(() -> FactionTargetResolver.resolveOwningFaction(any(), any()))
            .thenReturn(new ResolvedFactionTarget(factionMock));
    }
}
