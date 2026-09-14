package kmlib.gradlescripts.integrationtests;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.DisplayName;
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

    /**
     * Stands up a consumer whose manifest declares the given KMLib dependency, beside a KMLib
     * checkout built at the given version. The dependency clause is passed as written so a case can
     * pose a declaration with no version at all, which is a shape of its own.
     */
    private static Path writeConsumerProject(
            Path workspace,
            String declaredDependency,
            String builtKmlibVersion) throws IOException {

        var kmlibCheckout = Files.createDirectories(workspace.resolve("kmlib"));

        Files.writeString(
            kmlibCheckout.resolve("mod_info.json"),
            "{\"id\":\"kmlib\",\"version\":\"" + builtKmlibVersion + "\"}");

        var projectDirectory = Files.createDirectories(workspace.resolve("consumer"));

        Files.writeString(
            projectDirectory.resolve("settings.gradle"),
            "rootProject.name = 'kmu'");

        Files.writeString(
            projectDirectory.resolve("mod_info.json"),
            "{\"id\":\"kmu\",\"version\":\"1.0.0\",\"dependencies\":[" + declaredDependency + "]}");

        Files.writeString(
            projectDirectory.resolve("build.gradle"),
            String.join(
                "\n",
                "ext.kmlibCheckoutDirectory = file('"
                    + kmlibCheckout.toString().replace('\\', '/')
                    + "')",
            "apply from: '"
                + REPORT_SCRIPT.replace('\\', '/') + "'"));

        return projectDirectory;
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
    @DisplayName("declared version matches the checkout")
    final class ReportNothing {

        @Test
        void reportKmlibVersionMismatch_ReportsNothing_WhenDeclaredVersionMatches(
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
        void reportKmlibVersionMismatch_ReportsNothing_WhenDeclaredVersionOmitsPatchSegment(
                @TempDir Path workspace) throws IOException {

            Path projectDirectory =
                writeConsumerProject(workspace, "{\"id\":\"kmlib\",\"version\":\"0.1\"}", "0.1.0");

            assertThat(runReport(projectDirectory))
                .doesNotContain("Warning:");
        }

        /** KMLib's own build declares no KMLib dependency, so there is no pair to compare. */
        @Test
        void reportKmlibVersionMismatch_ReportsNothing_WhenNoKmlibDependencyIsDeclared(
                @TempDir Path workspace) throws IOException {

            Path projectDirectory =
                writeConsumerProject(workspace, "{\"id\":\"lw_lazylib\"}", "0.1.0");

            assertThat(runReport(projectDirectory))
                .doesNotContain("Warning:");
        }
    }

    @Nested
    @DisplayName("declared version differs from the checkout")
    final class ReportMismatch {

        @Test
        void reportKmlibVersionMismatch_ReportsBlockedMod_WhenMinorSegmentDiffers(
                @TempDir Path workspace) throws IOException {

            Path projectDirectory =
                writeConsumerProject(workspace, "{\"id\":\"kmlib\",\"version\":\"0.1.0\"}", "0.2.0");

            assertThat(runReport(projectDirectory))
                .contains("Warning:")
                .contains("would not be able to enable");
        }

        @Test
        void reportKmlibVersionMismatch_ReportsVersionWarning_WhenOnlyPatchSegmentDiffers(
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
        void reportKmlibVersionMismatch_ReportsVersionWarning_WhenPatchSegmentCarriesLetter(
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
        void reportKmlibVersionMismatch_ReportsUncheckedDependency_WhenDeclaredWithoutVersion(
                @TempDir Path workspace) throws IOException {

            Path projectDirectory = writeConsumerProject(workspace, "{\"id\":\"kmlib\"}", "0.1.0");

            assertThat(runReport(projectDirectory))
                .contains("Warning:")
                .contains("Neither this build nor the");
        }
    }
}
