package kmlib.mods.console.commands;

import com.fs.starfarer.api.Global;

import kmlib.mods.console.commands.output.GameLogCommandOutput;
import kmlib.testfixtures.logging.LogAppenderFake;
import kmlib.testfixtures.mods.console.commands.output.CommandOutputFake;
import kmlib.testfixtures.starsector.StubbedGlobalLogger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * Pins what the command does with the words the player typed: prints the listing, accepts one
 * filter keyword and any of the options alongside it, rejects a keyword pair and an unknown word as
 * bad syntax, and sends the listing to the log rather than the console where asked.
 *
 * <p>What the listing says is {@link FactionListingReportTest}'s. Each keyword is asserted here
 * only as far as reaching the run, which is what a keyword declared on an enum but never registered
 * on the parameter spec would fail.
 */
final class ListFactionsCommandTest {

    @Nested
    class RunCommand {

        private MockedStatic<Global> globalMock;
        private CommandOutputFake outputFake;
        private ListFactionsCommand command;

        @BeforeEach
        void setUp() {

            // The fixture finishes its own stubbing before the static mock's opens,
            // so the two do not nest into an unfinished-stubbing error.
            var sector = new FactionListingFixture().getSector();

            globalMock = mockStatic(Global.class);
            globalMock
                .when(Global::getSector)
                .thenReturn(sector);

            // Owed because the run reaches the source read, whose logger is resolved once for the
            // JVM - left as the stand-in's null, every later suite logging through that class
            // faults on a line it never wrote.
            StubbedGlobalLogger.answerLoggersOn(globalMock);

            outputFake = new CommandOutputFake();
            command = new ListFactionsCommand(outputFake);
        }

        @AfterEach
        void tearDown() {
            globalMock.close();
        }

        @Test
        void printsTheReportForABareInvocation() {

            var result = command.runCommand("", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.SUCCESS);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Factions and their holdings:"));
        }

        // Every keyword spelled out, and each asserted to reach the filter that names
        // itself back in the header. That pairing is the one thing the spec's
        // flag-to-filter walk could get wrong without any single-keyword case
        // noticing: a flag bound to the wrong filter still parses and still prints.
        @ParameterizedTest
        @ValueSource(strings = {"markets", "hidden", "discoverable", "no_markets"})
        void acceptsEachFilterKeywordAndSelectsTheFilterItNames(String keyword) {

            var result = command.runCommand(keyword, CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.SUCCESS);
            assertThat(outputFake.getMessages())
                .anyMatch(message ->
                    message.contains("Factions and their holdings (" + keyword + "):"));
        }

        @Test
        void reportsTwoFilterKeywordsAsBadSyntax() {
            // The keywords are alternatives: a pair has no single honest answer for the
            // systems clause, and no_markets contradicts the other three outright. The
            // usage line is asserted as a literal because it is built from the filters
            // rather than written out, so nothing else pins what the player is offered.
            var result = command.runCommand("hidden discoverable", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Give at most one filter. "
                    + "Usage: kmlib_list_factions [markets|hidden|discoverable|no_markets] "
                    + "[no_holdings] [no_attitude] [to_log]."));
        }

        // Every option spelled out, each asserted to reach the run rather than to be reported as a
        // stray word - which is what a keyword the spec never registered would be.
        @ParameterizedTest
        @ValueSource(strings = {"no_holdings", "no_attitude", "to_log"})
        void acceptsEachOptionKeyword(String keyword) {

            var result = command.runCommand(keyword, CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.SUCCESS);
        }

        @Test
        void acceptsAFilterAndTheOptionsTogether() {
            // The options compose with each other and with a filter, which is what makes them
            // options rather than more filters.
            var result = command.runCommand(
                "hidden no_holdings no_attitude",
                CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.SUCCESS);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Factions (hidden):"));
        }

        @Test
        void writesTheReportToTheLogUnderTheToLogKeyword() {
            // Both halves, because either alone passes a run that lost the other: a console
            // asserted on its own stays green with the log write deleted, and the notice is what
            // keeps a run that printed nothing where the player is looking from reading as a run
            // that did nothing.
            var logFake = LogAppenderFake.captureLogOf(
                GameLogCommandOutput.class,
                () -> assertThat(command.runCommand("to_log", CommandContext.CAMPAIGN_MAP))
                    .isEqualTo(CommandResult.SUCCESS));

            assertThat(logFake.getMessages())
                .anyMatch(message -> message.contains("Factions and their holdings:"));
            assertThat(outputFake.getMessages())
                .containsExactly("Faction listing written to the game log.");
        }

        @Test
        void writesTheReportToTheConsoleWithoutTheToLogKeyword() {
            // The pair to the case above: without the keyword nothing reaches the log, which is
            // what makes the routing a choice rather than a copy to both.
            var logFake = LogAppenderFake.captureLogOf(
                GameLogCommandOutput.class,
                () -> assertThat(command.runCommand("", CommandContext.CAMPAIGN_MAP))
                    .isEqualTo(CommandResult.SUCCESS));

            assertThat(logFake.getMessages())
                .isEmpty();
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Factions and their holdings:"));
        }

        @Test
        void reportsAnUnknownWordAsBadSyntax() {

            var result = command.runCommand("bogus", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Too many arguments"));
        }

        @Test
        void returnsTheValidationResultOutsideACampaign() {

            var result = command.runCommand("", CommandContext.COMBAT_MISSION);

            assertThat(result)
                .isEqualTo(CommandResult.WRONG_CONTEXT);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("can only run in a campaign"));
        }
    }
}
