package kmlib.gradlescripts.integrationtests;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises how the local VersionChecker fill is registered, against a real Gradle build.
 *
 * <p>What the fill writes is the release script's, covered by its own bats suite; what is
 * Gradle's is whether the task exists and which files it declares. The metadata input is the one
 * that matters: beside a committed base, mod_info.json is writeLocaleFiles' output, and declaring it
 * here as well leaves Gradle two tasks writing and reading one file with no order between them.
 */
final class WriteVersionFileIntegrationTests {

    private static final String SHIPPED_JSON_READER_SCRIPT =
        new File("gradle/shipped-json-reader.gradle").getAbsolutePath();

    private static final String MOD_INFO_READER_SCRIPT =
        new File("gradle/mod-info-reader.gradle").getAbsolutePath();

    private static final String WRITE_VERSION_FILE_SCRIPT =
        new File("gradle/tasks/release/write-version-file.gradle").getAbsolutePath();

    // The checkout the fill script is located from, which is this one.
    private static final String KMLIB_CHECKOUT_DIRECTORY = new File("").getAbsolutePath();

    // Handed over by the build running this suite: a throwaway build is a process of its own and
    // cannot locate an install.
    private static final String GAME_JSON_JAR_FILE = System.getProperty("kmlib.gameJsonJarFile");

    private static final String MOD_INFO_FILE_NAME = "mod_info.json";

    private static final String MOD_INFO_BASE_FILE_NAME = "mod_info.base.json";

    private static final String VERSION_FILE_TEMPLATE_NAME = "kmu.version.template";

    private static final String MOD_INFO_TEXT = "{\"id\":\"kmu\",\"version\":\"1.0.0\"}";

    // The statement a case runs: whether the task exists, and the names of the files it reads.
    private static final String PRINT_REGISTRATION = String.join(
        "\n",
        "def writeVersionFileTask = tasks.findByName('writeVersionFile')",
        "println \"registered: ${writeVersionFileTask != null}\"",
        "if (writeVersionFileTask != null) {",
        "    println \"inputs: ${writeVersionFileTask.inputs.files.files*.name.sort()}\"",
        "    println \"jar depends: ${tasks.jar.taskDependencies.getDependencies(tasks.jar)*.name}\"",
        "}");

    /** Writes a consumer applying the fill over its metadata readers, and printing how it registered. */
    private static Path writeConsumerProject(Path workspace) throws IOException {

        var projectDirectory = Files.createDirectories(workspace.resolve("consumer"));

        Files.writeString(
            projectDirectory.resolve("settings.gradle"),
            "rootProject.name = 'kmu'");

        Files.writeString(
            projectDirectory.resolve("build.gradle"),
            String.join(
                "\n",
                "apply plugin: 'java'",
                "ext.gameJsonJarFile = file('" + GAME_JSON_JAR_FILE.replace('\\', '/') + "')",
                "apply from: '" + SHIPPED_JSON_READER_SCRIPT.replace('\\', '/') + "'",
                "apply from: '" + MOD_INFO_READER_SCRIPT.replace('\\', '/') + "'",
                "ext.kmlibCheckoutDirectory = file('" + KMLIB_CHECKOUT_DIRECTORY.replace('\\', '/') + "')",
                "apply from: '" + WRITE_VERSION_FILE_SCRIPT.replace('\\', '/') + "'",
                PRINT_REGISTRATION));

        return projectDirectory;
    }

    private static String runHelp(Path projectDirectory) {

        return GradleRunner
            .create()
            .withProjectDir(projectDirectory.toFile())
            .withArguments("help")
            .build()
            .getOutput();
    }

    @Nested
    final class RegisterWriteVersionFile {

        /** Committing the template is the opt-in; a mod publishing no update information gets no task. */
        @Test
        void registersNoTaskWithoutATemplate(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeConsumerProject(workspace);

            Files.writeString(projectDirectory.resolve(MOD_INFO_FILE_NAME), MOD_INFO_TEXT);

            assertThat(runHelp(projectDirectory))
                .contains("registered: false");
        }

        @Test
        void declaresModInfoJsonAsItsMetadataWhenNoBaseIsCommitted(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeConsumerProject(workspace);

            Files.writeString(projectDirectory.resolve(MOD_INFO_FILE_NAME), MOD_INFO_TEXT);
            Files.writeString(projectDirectory.resolve(VERSION_FILE_TEMPLATE_NAME), "{}");

            assertThat(runHelp(projectDirectory))
                .contains("inputs: [fill_version_file_template.sh, kmu.version.template, mod_info.json, mod_info.sh]");
        }

        @Test
        void declaresTheBaseAsItsMetadataWhenOneIsCommitted(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeConsumerProject(workspace);

            Files.writeString(projectDirectory.resolve(MOD_INFO_FILE_NAME), MOD_INFO_TEXT);
            Files.writeString(projectDirectory.resolve(MOD_INFO_BASE_FILE_NAME), MOD_INFO_TEXT);
            Files.writeString(projectDirectory.resolve(VERSION_FILE_TEMPLATE_NAME), "{}");

            assertThat(runHelp(projectDirectory))
                .contains("inputs: [fill_version_file_template.sh, kmu.version.template, mod_info.base.json, mod_info.sh]");
        }

        /** Refreshing a dev install is what building the jar is, so the version file comes with it. */
        @Test
        void runsAheadOfJar(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeConsumerProject(workspace);

            Files.writeString(projectDirectory.resolve(MOD_INFO_FILE_NAME), MOD_INFO_TEXT);
            Files.writeString(projectDirectory.resolve(VERSION_FILE_TEMPLATE_NAME), "{}");

            assertThat(runHelp(projectDirectory))
                .containsPattern("jar depends: \\[.*writeVersionFile.*\\]");
        }
    }
}
