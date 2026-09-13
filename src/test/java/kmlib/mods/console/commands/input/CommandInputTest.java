package kmlib.mods.console.commands.input;

import kmlib.mods.console.commands.parsing.Parameter;
import kmlib.mods.console.commands.parsing.ParameterSpec;
import kmlib.mods.console.commands.parsing.ParameterValues;
import kmlib.testfixtures.mods.console.commands.output.CommandOutputFake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link CommandInput#parseArguments}: it sequences a run-context guard and
 * the argument parse into one {@link kmlib.mods.console.commands.parsing.ParsedParameters}.
 * Context is checked first, so a wrong-context run reports {@code WRONG_CONTEXT}
 * (never {@code BAD_SYNTAX}) even when the arguments are also malformed; once the
 * context passes, a bad argument reports {@code BAD_SYNTAX} and a good one yields
 * the typed values. {@code requireCampaign} is used as the guard since it reads
 * only the context, needing no live-sector stub. Feedback is read back through a
 * recording {@code CommandOutput}.
 */
final class CommandInputTest {

    private CommandOutputFake outputFake;

    @BeforeEach
    void setUp() {
        outputFake = new CommandOutputFake();
    }

    // A single required positional, enough to tell a parse failure (missing ID)
    // from a parse success (ID supplied) apart from the context outcome.
    private static final class SampleSpec extends ParameterSpec {
        private final Parameter<String> id =
            acceptsPositional("id", "<id>", ParameterValues.text()).markRequired();

        private SampleSpec() {
            super("Usage: sample <id>.");
        }
    }

    @Nested
    class ParseArguments {
        private final SampleSpec spec = new SampleSpec();

        @Test
        void reportsTheContextFailureBeforeParsingTheArguments() {
            // Context is wrong and the arguments are also malformed (surplus), but
            // the context guard runs first, so its result wins.
            var parsed = new CommandInput(CommandContext.COMBAT_MISSION, "a b c", outputFake)
                .requireCampaign()
                .parseArguments(spec);

            assertThat(parsed.isValid()).isFalse();
            assertThat(parsed.getResult()).isEqualTo(CommandResult.WRONG_CONTEXT);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("can only run in a campaign"));
            // The tokens were never parsed, so no syntax complaint reached the player.
            assertThat(outputFake.getMessages())
                .noneMatch(message -> message.contains("Too many arguments"));
        }

        @Test
        void parsesTheArgumentsOnceTheContextPasses() {
            var parsed = new CommandInput(CommandContext.CAMPAIGN_MAP, "gate1", outputFake)
                .requireCampaign()
                .parseArguments(spec);

            assertThat(parsed.isValid()).isTrue();
            assertThat(parsed.get(spec.id)).isEqualTo("gate1");
        }

        @Test
        void reportsABadArgumentAsBadSyntaxOnceTheContextPasses() {
            var parsed = new CommandInput(CommandContext.CAMPAIGN_MAP, "", outputFake)
                .requireCampaign()
                .parseArguments(spec);

            assertThat(parsed.isValid()).isFalse();
            assertThat(parsed.getResult()).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Missing required parameter 'id'"));
        }
    }
}
