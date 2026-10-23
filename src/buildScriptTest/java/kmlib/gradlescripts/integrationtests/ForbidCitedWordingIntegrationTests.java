package kmlib.gradlescripts.integrationtests;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the phrases a localisation reference's reasons may not use, declared over Common-Java's
 * forbidden-phrase gate, against a real Gradle build.
 *
 * <p>The rule exists because a reason citing wording kept being written by hand after being ruled out.
 * How the gate matches is Common-Java's to cover; what is pinned here is that the declarations catch
 * each phrase family seen in a reference, and leave a reason giving a meaning alone.
 */
final class ForbidCitedWordingIntegrationTests {

    // The checkout beside this one, as every KM build finds Common-Java.
    private static final String GATE_SCRIPT = new File(
        "../Common-Java/gradle/tasks/lint/docs/enforce-no-forbidden-phrases.gradle").getAbsolutePath();

    private static final String RULE_SCRIPT =
        new File("gradle/tasks/checks/forbid-cited-wording.gradle").getAbsolutePath();

    private static final String GATE_TASK = "enforceNoForbiddenPhrases";

    private static final String REFERENCE_HEADER = String.join(
        "\n",
        "# Reference",
        "",
        "| English | 简体中文 | Note |",
        "| --- | --- | --- |");

    // The row every case writes sits on this line of the reference.
    private static final String ROW_LOCATION = "localisation/zh-hans/README.md:5";

    // Writes a project applying the gate and the rule, with the given reference text, or with no reference
    // where the text is null.
    private static Path writeProject(Path workspace, String referenceText) throws IOException {

        var projectDirectory = Files.createDirectories(workspace.resolve("consumer"));

        Files.writeString(
            projectDirectory.resolve("settings.gradle"),
            "rootProject.name = 'kmu'");

        Files.writeString(
            projectDirectory.resolve("build.gradle"),
            String.join(
                "\n",
                "apply from: '" + GATE_SCRIPT.replace('\\', '/') + "'",
                "apply from: '" + RULE_SCRIPT.replace('\\', '/') + "'"));

        if (referenceText != null) {

            var localeDirectory = Files.createDirectories(projectDirectory.resolve("localisation/zh-hans"));

            Files.writeString(localeDirectory.resolve("README.md"), referenceText);
        }

        return projectDirectory;
    }

    private static GradleRunner createRunner(Path projectDirectory) {

        return GradleRunner
            .create()
            .withProjectDir(projectDirectory.toFile())
            .withArguments(GATE_TASK);
    }

    @Nested
    final class EnforceNoForbiddenPhrases {

        @Test
        void passesAReasonGivingWhatTheWordMeans(@TempDir Path workspace) throws IOException {

            // ARRANGE
            var projectDirectory = writeProject(
                workspace,
                REFERENCE_HEADER + "\n| centreline | 中线 | 中, the middle, with 线, a line |\n");

            // ACT
            var actualOutput = createRunner(projectDirectory)
                .build()
                .getOutput();

            // ASSERT
            assertThat(actualOutput)
                .contains("BUILD SUCCESSFUL");
        }

        // The gate fails a rule judging no file, so a mod with no translated bundle must be handed none.
        @Test
        void passesAModCarryingNoReference(@TempDir Path workspace) throws IOException {

            // ARRANGE
            var projectDirectory = writeProject(workspace, null);

            // ACT
            var actualOutput = createRunner(projectDirectory)
                .build()
                .getOutput();

            // ASSERT
            assertThat(actualOutput)
                .contains("BUILD SUCCESSFUL");
        }

        // One of each phrase family a reference has carried: a copy's wording named as the reason, whoever
        // wrote the copy.
        @ParameterizedTest
        @ValueSource(strings = {
            "the plain word, as the settings write it",
            "as the `0.5.0` entries write them",
            "as the settings write Mod 玩家势力 for modded player factions",
            "the plain word for a switch, as the settings' own toggles are",
            "as in the settings description",
            "the Dev tab's own wording for it"})
        void failsAReasonCitingWording(String citingReason, @TempDir Path workspace) throws IOException {

            // ARRANGE
            var projectDirectory = writeProject(
                workspace,
                REFERENCE_HEADER + "\n| outline | 轮廓 | " + citingReason + " |\n");

            // ACT
            var actualOutput = createRunner(projectDirectory)
                .buildAndFail()
                .getOutput();

            // ASSERT
            assertThat(actualOutput)
                .contains(ROW_LOCATION);
        }

        // A sentence of prose citing wording is the same habit as a row doing it.
        @Test
        void failsProseCitingWording(@TempDir Path workspace) throws IOException {

            // ARRANGE
            var projectDirectory = writeProject(
                workspace,
                "# Reference\n\nThe default is the shipped value as the English file writes it.\n");

            var expectedLocation = "localisation/zh-hans/README.md:3";

            // ACT
            var actualOutput = createRunner(projectDirectory)
                .buildAndFail()
                .getOutput();

            // ASSERT
            assertThat(actualOutput)
                .contains(expectedLocation);
        }
    }
}
