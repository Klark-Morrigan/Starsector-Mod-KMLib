package kmlib.console.validation;

import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.testfixtures.console.output.CommandOutputFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Pins the validation outcome of {@link CommandContextValidation} and
 * {@link CommandValidationResult}: a satisfied chain (and an empty one) is valid
 * and silent, each guard fails with its own result and message, and the chain
 * stops at the first failure. Feedback is read back through a recording
 * {@code CommandOutput} binding, so the failing paths are pinned here rather than
 * left to in-game exercise; the {@code requireStarSystem} guard is handed the
 * sector to look in, so a case poses one answering for no fleet rather than
 * standing up a game. Each subject's cases live in a {@link Nested} group so the
 * suite reports as a per-subject tree.
 */
final class CommandContextValidationTest {

    @Nested
    class ValidateAndPrintFeedback {

        @Test
        void satisfiedCampaignChainIsValidAndSilent() {

            var outputFake = new CommandOutputFake();
            var result = new CommandContextValidation(CommandContext.CAMPAIGN_MAP, outputFake)
                .requireCampaign()
                .validateAndPrintFeedback();

            assertThat(result.isValid())
                .isTrue();
            assertThat(outputFake.getMessages())
                .isEmpty();
        }

        @Test
        void emptyChainIsValidAndSilent() {

            var outputFake = new CommandOutputFake();
            var result = new CommandContextValidation(CommandContext.CAMPAIGN_MAP, outputFake)
                .validateAndPrintFeedback();

            assertThat(result.isValid())
                .isTrue();
            assertThat(outputFake.getMessages())
                .isEmpty();
        }

        @Test
        void requireCampaignFailsOutsideCampaignAndReportsIt() {

            var outputFake = new CommandOutputFake();
            var result = new CommandContextValidation(CommandContext.COMBAT_MISSION, outputFake)
                .requireCampaign()
                .validateAndPrintFeedback();

            assertThat(result.isValid())
                .isFalse();
            assertThat(result.getResult())
                .isEqualTo(CommandResult.WRONG_CONTEXT);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("can only run in a campaign"));
        }

        @Test
        void requireStarSystemFailsWithNoCurrentSystemAndReportsIt() {
            var outputFake = new CommandOutputFake();
            // The guard is handed the sector to look in rather than reading the game's own, so a
            // sector answering for no fleet is the whole of what this case has to pose - no
            // running game, and no static read to stand in for one.
            var sectorWithNoFleetMock = mock(SectorAPI.class);

            var result = new CommandContextValidation(CommandContext.CAMPAIGN_MAP, outputFake)
                .requireStarSystem(sectorWithNoFleetMock)
                .validateAndPrintFeedback();

            assertThat(result.isValid())
                .isFalse();
            assertThat(result.getResult())
                .isEqualTo(CommandResult.WRONG_CONTEXT);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("must be run inside a star system"));
        }

        @Test
        void stopsAtFirstFailureAndReportsOnlyThatOne() {

            var outputFake = new CommandOutputFake();

            // The first guard (context) fails; a later guard would too, but the
            // chain must stop at the first, so only its message reaches the player
            // and the second guard's live-sector lookup is never reached.
            var result = new CommandContextValidation(CommandContext.COMBAT_MISSION, outputFake)
                .requireCampaign()
                .requireStarSystem(mock(SectorAPI.class))
                .validateAndPrintFeedback();

            assertThat(result.isValid())
                .isFalse();
            assertThat(result.getResult())
                .isEqualTo(CommandResult.WRONG_CONTEXT);
            assertThat(outputFake.getMessages())
                .containsExactly("This command can only run in a campaign.");
        }
    }

    @Nested
    class BuildsInvalidResult {

        @Test
        void invalidResultCarriesTheCommandResult() {

            var result = CommandValidationResult.createInvalid(CommandResult.WRONG_CONTEXT);

            assertThat(result.isValid())
                .isFalse();
            assertThat(result.getResult())
                .isEqualTo(CommandResult.WRONG_CONTEXT);
        }
    }
}
