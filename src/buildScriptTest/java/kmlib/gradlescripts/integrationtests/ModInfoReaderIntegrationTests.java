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
 * Exercises the mod metadata reader against a real Gradle build, since the file it picks is read while
 * a project is being configured.
 *
 * <p>The choice between the two files is the substance: beside a committed base, mod_info.json is a
 * build output that is absent on a fresh checkout and on whichever locale was last written otherwise,
 * so reading it would version the jar from a stale or missing file.
 */
final class ModInfoReaderIntegrationTests {

    private static final String SHIPPED_JSON_READER_SCRIPT =
        new File("gradle/shipped-json-reader.gradle").getAbsolutePath();

    private static final String MOD_INFO_READER_SCRIPT =
        new File("gradle/mod-info-reader.gradle").getAbsolutePath();

    // Handed over by the build running this suite: a throwaway build is a process of its own and
    // cannot locate an install.
    private static final String GAME_JSON_JAR_FILE = System.getProperty("kmlib.gameJsonJarFile");

    private static final String MOD_INFO_FILE_NAME = "mod_info.json";

    private static final String MOD_INFO_BASE_FILE_NAME = "mod_info.base.json";

    // The statement each case runs: which file was read, and the version it held.
    private static final String PRINT_READING =
        "println \"read: ${modInfoFile.name} ${modInfo.version}\"";

    /** Writes a build applying the reader and printing what it read. */
    private static Path writeProbeProject(Path workspace, String statement) throws IOException {

        var projectDirectory = Files.createDirectories(workspace.resolve("probe"));

        Files.writeString(
            projectDirectory.resolve("settings.gradle"),
            "rootProject.name = 'probe'");

        Files.writeString(
            projectDirectory.resolve("build.gradle"),
            String.join(
                "\n",
                "ext.gameJsonJarFile = file('" + GAME_JSON_JAR_FILE.replace('\\', '/') + "')",
                "apply from: '" + SHIPPED_JSON_READER_SCRIPT.replace('\\', '/') + "'",
                "apply from: '" + MOD_INFO_READER_SCRIPT.replace('\\', '/') + "'",
                statement));

        return projectDirectory;
    }

    private static void writeManifest(Path directory, String fileName, String version) throws IOException {

        Files.writeString(
            directory.resolve(fileName),
            "{\"id\":\"kmu\",\"version\":\"" + version + "\"}");
    }

    private static GradleRunner createRunner(Path projectDirectory) {

        return GradleRunner
            .create()
            .withProjectDir(projectDirectory.toFile())
            .withArguments("help");
    }

    @Nested
    final class ModInfo {

        @Test
        void readsModInfoJsonWhenNoBaseIsCommitted(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeProbeProject(workspace, PRINT_READING);

            writeManifest(projectDirectory, MOD_INFO_FILE_NAME, "1.0.0");

            assertThat(createRunner(projectDirectory).build().getOutput())
                .contains("read: mod_info.json 1.0.0");
        }

        /** The generated file lags the base until the next locale write, so it is never the one read. */
        @Test
        void readsTheBaseOverAGeneratedModInfoJson(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeProbeProject(workspace, PRINT_READING);

            writeManifest(projectDirectory, MOD_INFO_FILE_NAME, "1.0.0");
            writeManifest(projectDirectory, MOD_INFO_BASE_FILE_NAME, "2.0.0");

            assertThat(createRunner(projectDirectory).build().getOutput())
                .contains("read: mod_info.base.json 2.0.0");
        }

        /** A fresh checkout of a mod keeping locales holds the base and nothing generated from it. */
        @Test
        void readsTheBaseOnACheckoutThatHasBuiltNothing(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeProbeProject(workspace, PRINT_READING);

            writeManifest(projectDirectory, MOD_INFO_BASE_FILE_NAME, "2.0.0");

            assertThat(createRunner(projectDirectory).build().getOutput())
                .contains("read: mod_info.base.json 2.0.0");
        }

        @Test
        void failsNamingBothFilesWhenNeitherIsPresent(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeProbeProject(workspace, PRINT_READING);

            assertThat(createRunner(projectDirectory).buildAndFail().getOutput())
                .contains("Neither mod_info.base.json nor mod_info.json found in");
        }
    }

    @Nested
    final class ReadModInfo {

        /** Another checkout is read by the rule the applying project's own metadata is. */
        @Test
        void readsAnotherCheckoutsBase(@TempDir Path workspace) throws IOException {

            var otherCheckout = Files.createDirectories(workspace.resolve("other"));

            writeManifest(otherCheckout, MOD_INFO_FILE_NAME, "1.0.0");
            writeManifest(otherCheckout, MOD_INFO_BASE_FILE_NAME, "3.0.0");

            var projectDirectory = writeProbeProject(
                workspace,
                "println \"other: ${readModInfo(file('" + otherCheckout.toString().replace('\\', '/')
                    + "')).version}\"");

            writeManifest(projectDirectory, MOD_INFO_FILE_NAME, "1.0.0");

            assertThat(createRunner(projectDirectory).build().getOutput())
                .contains("other: 3.0.0");
        }
    }
}
