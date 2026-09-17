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
 * Exercises the version stamp against a real Gradle build, driving the leg that every development
 * machine here is the wrong shape to reach.
 *
 * <p>The stamp reads the bound jar, so the leg it takes is decided by the install the build runs
 * against - and those installs are Fast-Rendering-patched, which is the point of them. Applying the
 * script into a throwaway project is what lets the stub leg be posed on purpose rather than waited
 * for. What it has to get right is narrow and easy to lose: the stamped value is rendered into a
 * player-facing sentence, so an empty string there would read as a bug in KMLib on a path that runs
 * only once something else is already broken.
 */
final class StampFastRenderingVersionIntegrationTests {

    private static final String STAMP_SCRIPT =
        new File("gradle/tasks/generate/stamp-fast-rendering-version.gradle").getAbsolutePath();

    private static final String GENERATED_SOURCE_PATH =
        "build/generated/sources/fastrenderingversion/java/kmlib/opengl/FastRenderingBinding.java";

    /**
     * Stands up a Java project applying the stamp script, bound to the given jar - passed as the
     * Groovy expression that names it, so a case can pose the stub leg's absent jar as literally no
     * jar rather than as some stand-in for one.
     */
    private static Path writeStampingProject(Path workspace, String boundJarExpression)
            throws IOException {

        var projectDirectory = Files.createDirectories(workspace.resolve("consumer"));

        Files.writeString(
            projectDirectory.resolve("settings.gradle"),
            "rootProject.name = 'kmlib'");

        Files.writeString(
            projectDirectory.resolve("build.gradle"),
            String.join(
                "\n",
                "apply plugin: 'java'",
                "ext.boundFastRenderingJarFile = " + boundJarExpression,
                "apply from: '" + STAMP_SCRIPT.replace('\\', '/') + "'"));

        return projectDirectory;
    }

    /** Runs the stamp and answers the build's own output, for the cases about what it reports. */
    private static String runStamp(Path projectDirectory) {

        return GradleRunner
            .create()
            .withProjectDir(projectDirectory.toFile())
            .withArguments("stampFastRenderingVersion")
            .build()
            .getOutput();
    }

    private static String readGeneratedSource(Path projectDirectory) throws IOException {
        return Files.readString(projectDirectory.resolve(GENERATED_SOURCE_PATH));
    }

    @Nested
    @DisplayName("no Fast Rendering version can be read")
    final class StampUnknown {

        /** The stub leg: no jar was bound, so there is no version to have read. */
        @Test
        void stampFastRenderingVersion_StampsTheSentinel_WhenNoJarIsBound(
                @TempDir Path workspace) throws IOException {

            Path projectDirectory = writeStampingProject(workspace, "null");

            // No jar is nothing to warn about: it is the leg's intended state, and a warning on
            // every stub build would be one nobody reads by the second time it fires.
            assertThat(runStamp(projectDirectory))
                .doesNotContain("Could not read");

            assertThat(readGeneratedSource(projectDirectory))
                .contains("BOUND_VERSION = \"unknown\"")
                .doesNotContain("BOUND_VERSION = \"\"");
        }

        /**
         * Fast Rendering carried no version class before v0.8.2, and the class has been renamed
         * within genir's own tree before now. Either way the read comes back with nothing, and a
         * build that would otherwise compile must not fail over which release it happens to be
         * bound to - but it has to say why, because a jar that is present and unreadable is the
         * case someone will be asked to diagnose, and the sentinel alone points them at whether
         * the jar exists.
         */
        @Test
        void stampFastRenderingVersion_StampsTheSentinelAndWarns_WhenTheBoundJarCarriesNoVersionClass(
                @TempDir Path workspace) throws IOException {

            var versionlessJar = workspace.resolve("fr.jar");
            Files.writeString(versionlessJar, "not a jar");

            Path projectDirectory = writeStampingProject(
                workspace,
                "file('" + versionlessJar.toString().replace('\\', '/') + "')");

            assertThat(runStamp(projectDirectory))
                .contains("Could not read com.genir.renderer.Version.getVersion() from")
                .contains(versionlessJar.getFileName().toString())
                .contains("ClassNotFoundException");

            assertThat(readGeneratedSource(projectDirectory))
                .contains("BOUND_VERSION = \"unknown\"")
                .doesNotContain("BOUND_VERSION = \"\"");
        }
    }
}
