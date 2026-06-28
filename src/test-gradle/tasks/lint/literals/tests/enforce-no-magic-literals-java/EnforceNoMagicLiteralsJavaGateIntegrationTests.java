import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// Integration test for the shared enforce-no-magic-literals-java gate: it
// applies the real gate (and its real checkstyle-magic-literals.xml, resolved by
// the gate relative to its own location) into a throwaway project and runs the
// task, asserting the outcome and the message a developer sees. Lives only in
// KMLib - the gate is shared with KMU, its tests are not. Fixtures load from
// .txt files under java/ so this src/test-gradle tree never holds a violating
// .java line the unit-test gates could see.
//
// The throwaway build mirrors the gate's real contract: java-conventions
// supplies the checkstyle plugin (so a second Checkstyle task type exists) and
// mavenCentral (so the plugin resolves its tool jars), and the gate is guarded
// to KM mods, so the fixture sets group 'kmlib'. Default package: the grouping
// folder is the source root, and its kebab name cannot be a Java package.
class EnforceNoMagicLiteralsJavaGateIntegrationTests {
    private static final String TASK_PATH = ":enforceNoMagicLiteralsJava";

    @Test
    void flagsAnInlineMagicNumber(@TempDir Path projectDir) throws IOException {
        writeJavaSource(projectDir, "inline-number-is-flagged");

        var result = runGate(projectDir, true);

        assertThat(result.getOutput()).contains("magic number");
    }

    @Test
    void passesWhenNumbersAreNamedOrStructural(@TempDir Path projectDir) throws IOException {
        writeJavaSource(projectDir, "named-numbers-pass");

        var result = runGate(projectDir, false);

        assertThat(result.task(TASK_PATH).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }

    private BuildResult runGate(Path projectDir, boolean expectFailure) throws IOException {
        // An explicit settings file stops Gradle walking up into a real build.
        Files.writeString(projectDir.resolve("settings.gradle"),
                "rootProject.name = 'gate-fixture'\n");
        // The checkstyle plugin (the gate's Checkstyle task type), mavenCentral
        // (tool-jar resolution) and the KM group (the gate's guard) all come from
        // the real build chain; the fixture supplies them so the gate sees the
        // same world it runs in for real.
        Files.writeString(projectDir.resolve("build.gradle"),
                "apply plugin: 'checkstyle'\n"
                + "repositories { mavenCentral() }\n"
                + "group = 'kmlib'\n"
                + "apply from: '" + scriptPath() + "'\n");

        var runner = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments("enforceNoMagicLiteralsJava");

        return expectFailure ? runner.buildAndFail() : runner.build();
    }

    private void writeJavaSource(Path projectDir, String fixture) throws IOException {
        var dir = projectDir.resolve("src/main/java");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("Sample.java"), loadFixture("java/" + fixture));
    }

    // The gate script path is handed in by the test task so the test does not
    // assume a working directory; forward slashes keep it valid inside the
    // generated build script on Windows.
    private String scriptPath() {
        return System.getProperty("java.literals.gate.script.path").replace('\\', '/');
    }

    private String loadFixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/" + name + ".txt")) {
            if (in == null) {
                throw new IllegalStateException("Missing fixture: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
