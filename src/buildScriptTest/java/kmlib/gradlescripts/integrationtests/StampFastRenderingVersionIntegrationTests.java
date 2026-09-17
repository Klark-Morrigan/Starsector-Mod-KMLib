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
 * Exercises the version stamp against a real Gradle build, driving the leg that every development
 * machine here is the wrong shape to reach.
 *
 * <p>The stamp reads the bound jar, so the leg it takes is decided by the install the build runs
 * against - and those installs are Fast-Rendering-patched, which is the point of them. Applying the
 * script into a throwaway project is what lets the stub leg be posed on purpose rather than waited
 * for. What it has to get right is narrow and easy to lose: the sentinel is what the generated
 * class answers as no version, so a build that stamped an empty string in its place would ship a
 * version that says nothing.
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

        var projectDirectory = Files.createDirectories(workspace.resolve("kmlib"));

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

    /**
     * The sentinel, and specifically not an empty string: the generated file would compile either
     * way, and only the sentinel is answered as no version.
     */
    private static void assertStampsTheSentinel(Path projectDirectory) throws IOException {

        assertThat(Files.readString(projectDirectory.resolve(GENERATED_SOURCE_PATH)))
            .contains("BOUND_VERSION = \"unknown\"")
            .doesNotContain("BOUND_VERSION = \"\"");
    }

    @Nested
    final class StampUnknown {

        /** The stub leg: no jar was bound, so there is no version to have read. */
        @Test
        void stampsTheSentinelWhenNoJarIsBound(@TempDir Path workspace) throws IOException {

            Path projectDirectory = writeStampingProject(workspace, "null");

            // No jar is nothing to warn about: it is the leg's intended state, and a warning on
            // every stub build would be one nobody reads by the second time it fires.
            assertThat(runStamp(projectDirectory))
                .doesNotContain("Could not read");

            assertStampsTheSentinel(projectDirectory);
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
        void stampsTheSentinelAndWarnsWhenTheBoundJarCarriesNoVersionClass(
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

            assertStampsTheSentinel(projectDirectory);
        }
    }
}
