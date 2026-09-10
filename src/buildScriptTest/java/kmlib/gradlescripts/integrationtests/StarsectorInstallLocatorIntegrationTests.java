package kmlib.gradlescripts.integrationtests;

import org.gradle.testkit.runner.BuildResult;
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
 * Exercises the install locator against a real Gradle build, because that is the only place its
 * answers exist: the script is Groovy evaluated against a project, so nothing it decides can be
 * reached without a project to evaluate it on.
 *
 * <p>Each case stands up a throwaway install tree and a one-file build that applies the locator and
 * prints what it found. What is being checked is the shape of the answers - the resolution order of
 * the install root, and the depth of the mod scan - since both are silent when wrong: a build
 * pointed at the wrong root compiles nothing, and a scan at the wrong depth returns an empty
 * collection that reads exactly like an uninstalled mod.
 */
final class StarsectorInstallLocatorIntegrationTests {

    private static final String LOCATOR_SCRIPT =
        new File("gradle/starsector-install-locator.gradle").getAbsolutePath();

    /**
     * Writes a build that applies the locator and prints one line per located value, so a case can
     * assert on the answer rather than on a task's side effect.
     */
    private static void writeLocatorProbeProject(Path projectDirectory) throws IOException {

        Files.writeString(
            projectDirectory.resolve("settings.gradle"),
            "rootProject.name = 'probe'");

        Files.writeString(
            projectDirectory.resolve("build.gradle"),
            String.join(
                "\n",
                "apply from: '" + LOCATOR_SCRIPT.replace('\\', '/') + "'",
                "tasks.register('printLocated') {",
                "    doLast {",
                "        println 'root=' + starsectorRoot",
                "        println 'coreJar=' + resolveCoreJarFile('json.jar')",
                "        println 'modJar=' + resolveModJarFile('lunalib', 'LunaLib.jar')",
                "        println 'foundModJars=' + findModJars('Found.jar').files.size()",
                "    }",
                "}"));
    }

    private static BuildResult runProbe(Path projectDirectory, String installRoot) {

        return GradleRunner
            .create()
            .withProjectDir(projectDirectory.toFile())
            .withArguments("-q", "printLocated", "-PstarsectorRoot=" + installRoot)
            .build();
    }

    @Nested
    @DisplayName("install root resolution")
    final class ResolveInstallRoot {

        @Test
        void starsectorInstallLocator_ResolvesRoot_FromProjectProperty(@TempDir Path workspace)
                throws IOException {

            var projectDirectory = Files.createDirectories(workspace.resolve("probe"));
            var installRoot = Files.createDirectories(workspace.resolve("install"));

            writeLocatorProbeProject(projectDirectory);

            BuildResult result = runProbe(projectDirectory, installRoot.toString());

            assertThat(result.getOutput())
                .contains("root=" + installRoot);
        }

        @Test
        void starsectorInstallLocator_ComposesCoreJarPath_UnderStarsectorCore(
                @TempDir Path workspace) throws IOException {

            var projectDirectory = Files.createDirectories(workspace.resolve("probe"));
            var installRoot = Files.createDirectories(workspace.resolve("install"));

            writeLocatorProbeProject(projectDirectory);

            BuildResult result = runProbe(projectDirectory, installRoot.toString());

            assertThat(result.getOutput())
                .contains("coreJar=" + installRoot.resolve("starsector-core").resolve("json.jar"));
        }

        @Test
        void starsectorInstallLocator_ComposesModJarPath_UnderNamedModFolder(
                @TempDir Path workspace) throws IOException {

            var projectDirectory = Files.createDirectories(workspace.resolve("probe"));
            var installRoot = Files.createDirectories(workspace.resolve("install"));

            writeLocatorProbeProject(projectDirectory);

            BuildResult result = runProbe(projectDirectory, installRoot.toString());

            assertThat(result.getOutput())
                .contains("modJar=" + installRoot
                    .resolve("mods")
                    .resolve("lunalib")
                    .resolve("jars")
                    .resolve("LunaLib.jar"));
        }
    }

    @Nested
    @DisplayName("mod jar scan")
    final class FindModJars {

        /** Creates an empty file at the given path, which is all the scan's isFile() test reads. */
        private void writeJarFile(Path jarFile) throws IOException {

            Files.createDirectories(jarFile.getParent());
            Files.writeString(jarFile, "");
        }

        @Test
        void starsectorInstallLocator_FindsModJar_InAnyModFolder(@TempDir Path workspace)
                throws IOException {

            var projectDirectory = Files.createDirectories(workspace.resolve("probe"));
            var installRoot = workspace.resolve("install");

            // A version-stamped folder name, the case the scan exists for.
            writeJarFile(installRoot.resolve("mods/Nexerelin-0.12.0a/jars/Found.jar"));
            writeLocatorProbeProject(projectDirectory);

            BuildResult result = runProbe(projectDirectory, installRoot.toString());

            assertThat(result.getOutput())
                .contains("foundModJars=1");
        }

        @Test
        void starsectorInstallLocator_FindsNoModJar_WhenNoModShipsIt(@TempDir Path workspace)
                throws IOException {

            var projectDirectory = Files.createDirectories(workspace.resolve("probe"));
            var installRoot = workspace.resolve("install");

            writeJarFile(installRoot.resolve("mods/SomeMod/jars/Other.jar"));
            writeLocatorProbeProject(projectDirectory);

            BuildResult result = runProbe(projectDirectory, installRoot.toString());

            assertThat(result.getOutput())
                .contains("foundModJars=0");
        }

        /**
         * The one-level depth is the whole point of the scan: a recursive walk of a modded install
         * costs seconds per snapshot, so a jar buried deeper has to stay unfound rather than
         * quietly widen the search.
         */
        @Test
        void starsectorInstallLocator_FindsNoModJar_WhenNestedDeeperThanOneFolder(
                @TempDir Path workspace) throws IOException {

            var projectDirectory = Files.createDirectories(workspace.resolve("probe"));
            var installRoot = workspace.resolve("install");

            writeJarFile(installRoot.resolve("mods/SomeMod/nested/jars/Found.jar"));
            writeLocatorProbeProject(projectDirectory);

            BuildResult result = runProbe(projectDirectory, installRoot.toString());

            assertThat(result.getOutput())
                .contains("foundModJars=0");
        }

        @Test
        void starsectorInstallLocator_FindsNoModJar_WhenInstallHasNoModsFolder(
                @TempDir Path workspace) throws IOException {

            Path projectDirectory = Files.createDirectories(workspace.resolve("probe"));
            Path installRoot = Files.createDirectories(workspace.resolve("install"));

            writeLocatorProbeProject(projectDirectory);

            BuildResult result = runProbe(projectDirectory, installRoot.toString());

            assertThat(result.getOutput())
                .contains("foundModJars=0");
        }
    }
}
