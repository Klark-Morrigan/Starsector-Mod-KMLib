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
 * Exercises the manifest-pair report against a real Gradle build, since the comparison it makes
 * only exists while a project is being configured.
 *
 * <p>The report is the only thing standing between a consumer and a mod a player cannot enable, and
 * it is a warning rather than a failure - so a broken comparison costs nothing at build time and
 * surfaces in someone's game instead. The cases below pin the two outcomes apart, because they are
 * not the same news: the launcher blocks a mod outright on a major or minor mismatch and merely
 * warns on a patch one.
 */
final class ReportKmlibVersionMismatchIntegrationTests {

    private static final String REPORT_SCRIPT =
        new File("gradle/tasks/checks/report-kmlib-version-mismatch.gradle").getAbsolutePath();

    private static final String SHIPPED_JSON_READER_SCRIPT =
        new File("gradle/shipped-json-reader.gradle").getAbsolutePath();

    private static final String MOD_INFO_READER_SCRIPT =
        new File("gradle/mod-info-reader.gradle").getAbsolutePath();

    // Handed over by the build running this suite: a throwaway build is a process of its own and
    // cannot locate an install.
    private static final String GAME_JSON_JAR_FILE = System.getProperty("kmlib.gameJsonJarFile");

    private static final String MOD_INFO_FILE_NAME = "mod_info.json";

    private static final String MOD_INFO_BASE_FILE_NAME = "mod_info.base.json";

    /**
     * Stands up a consumer whose manifest declares the given KMLib dependency, beside a KMLib
     * checkout whose manifest is the given file holding the given version. The dependency clause is
     * passed as written so a case can pose a declaration with no version at all, which is a shape of
     * its own.
     */
    private static Path writeConsumerProject(
            Path workspace,
            String declaredDependency,
            String kmlibManifestFileName,
            String builtKmlibVersion) throws IOException {

        var kmlibCheckout = Files.createDirectories(workspace.resolve("kmlib"));

        Files.writeString(
            kmlibCheckout.resolve(kmlibManifestFileName),
            "{\"id\":\"kmlib\",\"version\":\"" + builtKmlibVersion + "\"}");

        var projectDirectory = Files.createDirectories(workspace.resolve("consumer"));

        Files.writeString(
            projectDirectory.resolve("settings.gradle"),
            "rootProject.name = 'kmu'");

        Files.writeString(
            projectDirectory.resolve(MOD_INFO_FILE_NAME),
            "{\"id\":\"kmu\",\"version\":\"1.0.0\",\"dependencies\":[" + declaredDependency + "]}");

        Files.writeString(
            projectDirectory.resolve("build.gradle"),
            String.join(
                "\n",
                "ext.gameJsonJarFile = file('" + GAME_JSON_JAR_FILE.replace('\\', '/') + "')",
                "apply from: '" + SHIPPED_JSON_READER_SCRIPT.replace('\\', '/') + "'",
                "apply from: '" + MOD_INFO_READER_SCRIPT.replace('\\', '/') + "'",
                "ext.kmlibCheckoutDirectory = file('" + kmlibCheckout.toString().replace('\\', '/') + "')",
                "apply from: '" + REPORT_SCRIPT.replace('\\', '/') + "'"));

        return projectDirectory;
    }

    /** A consumer beside a KMLib checkout keeping the plain launcher file. */
    private static Path writeConsumerProject(
            Path workspace,
            String declaredDependency,
            String builtKmlibVersion) throws IOException {

        return writeConsumerProject(workspace, declaredDependency, MOD_INFO_FILE_NAME, builtKmlibVersion);
    }

    private static String runReport(Path projectDirectory) {

        var result = GradleRunner
            .create()
            .withProjectDir(projectDirectory.toFile())
            .withArguments("help")
            .build();

        return result.getOutput();
    }

    @Nested
    final class ReportNothing {

        @Test
        void reportsNothingWhenTheDeclaredVersionMatches(
                @TempDir Path workspace) throws IOException {

            Path projectDirectory =
                writeConsumerProject(workspace, "{\"id\":\"kmlib\",\"version\":\"0.1.0\"}", "0.1.0");

            assertThat(runReport(projectDirectory))
                .doesNotContain("Warning:");
        }

        /**
         * A version padded to three segments is the same version: Starsector's own manifests are
         * written both ways, and a report that called them different would fire on every build.
         */
        @Test
        void reportsNothingWhenTheDeclaredVersionOmitsThePatchSegment(
                @TempDir Path workspace) throws IOException {

            Path projectDirectory =
                writeConsumerProject(workspace, "{\"id\":\"kmlib\",\"version\":\"0.1\"}", "0.1.0");

            assertThat(runReport(projectDirectory))
                .doesNotContain("Warning:");
        }

        /** KMLib's own build declares no KMLib dependency, so there is no pair to compare. */
        @Test
        void reportsNothingWhenNoKmlibDependencyIsDeclared(
                @TempDir Path workspace) throws IOException {

            Path projectDirectory =
                writeConsumerProject(workspace, "{\"id\":\"lw_lazylib\"}", "0.1.0");

            assertThat(runReport(projectDirectory))
                .doesNotContain("Warning:");
        }
    }

    @Nested
    final class ReportMismatch {

        @Test
        void reportsABlockedModWhenTheMinorSegmentDiffers(
                @TempDir Path workspace) throws IOException {

            Path projectDirectory =
                writeConsumerProject(workspace, "{\"id\":\"kmlib\",\"version\":\"0.1.0\"}", "0.2.0");

            assertThat(runReport(projectDirectory))
                .contains("Warning:")
                .contains("would not be able to enable");
        }

        /**
         * A KMLib keeping its launcher text per locale states its version in the base, and a fresh
         * checkout of it holds no generated launcher file at all.
         */
        @Test
        void reportsABlockedModReadingKmlibsVersionFromItsBase(
                @TempDir Path workspace) throws IOException {

            Path projectDirectory = writeConsumerProject(
                workspace,
                "{\"id\":\"kmlib\",\"version\":\"0.1.0\"}",
                MOD_INFO_BASE_FILE_NAME,
                "0.2.0");

            assertThat(runReport(projectDirectory))
                .contains("Warning:")
                .contains("would not be able to enable");
        }

        @Test
        void reportsAVersionWarningWhenOnlyThePatchSegmentDiffers(
                @TempDir Path workspace) throws IOException {

            Path projectDirectory =
                writeConsumerProject(workspace, "{\"id\":\"kmlib\",\"version\":\"0.1.0\"}", "0.1.1");

            assertThat(runReport(projectDirectory))
                .contains("Warning:")
                .contains("this still links");
        }

        /**
         * A letter in a segment is ordinary in Starsector versions (0.12.2b), so the comparison
         * reads segments as text. Compared numerically it would throw, and a report that throws
         * takes the build with it.
         */
        @Test
        void reportsAVersionWarningWhenThePatchSegmentCarriesALetter(
                @TempDir Path workspace) throws IOException {

            Path projectDirectory =
                writeConsumerProject(workspace, "{\"id\":\"kmlib\",\"version\":\"0.1.0\"}", "0.1.0b");

            assertThat(runReport(projectDirectory))
                .contains("Warning:")
                .contains("this still links");
        }

        /**
         * A dependency without a version switches the launcher's own check off, so the build is the
         * only place a player's too-old KMLib could still be caught.
         */
        @Test
        void reportsAnUncheckedDependencyWhenDeclaredWithoutAVersion(
                @TempDir Path workspace) throws IOException {

            Path projectDirectory = writeConsumerProject(workspace, "{\"id\":\"kmlib\"}", "0.1.0");

            assertThat(runReport(projectDirectory))
                .contains("Warning:")
                .contains("Neither this build nor the");
        }
    }
}
