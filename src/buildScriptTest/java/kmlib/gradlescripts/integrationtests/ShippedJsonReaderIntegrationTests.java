package kmlib.gradlescripts.integrationtests;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the shipped JSON reader against a real Gradle build, parsing with the game's own
 * {@code json.jar}.
 *
 * <p>The reader is a Groovy copy of the fixtures' {@code ShippedJson}, and the copy is only worth
 * having if it agrees with the game: a file the game loads must read here, and a file the game
 * refuses must fail here. So the cases are the ones where a stricter or looser reader would part
 * company with the engine - comments, a {@code #} inside text, the engine's own quirk over an escaped
 * quote, a byte-order mark, a duplicated key.
 *
 * <p>Each case stands up a build that applies the reader and runs one Groovy statement against it,
 * so a case asserts on what the reader answered rather than on a task built over it.
 */
final class ShippedJsonReaderIntegrationTests {

    private static final String READER_SCRIPT =
        new File("gradle/shipped-json-reader.gradle").getAbsolutePath();

    // Handed over by the build running this suite: a throwaway build is a process of its own and
    // cannot locate an install.
    private static final String GAME_JSON_JAR_FILE = System.getProperty("kmlib.gameJsonJarFile");

    // U+FEFF, built from its code point rather than written into the source, where it is invisible.
    // Written as UTF-8 it becomes the three bytes the game's parser meets as the first character.
    private static final String BYTE_ORDER_MARK = String.valueOf((char) 0xFEFF);

    // "e acute" as a legacy code page saves it: one byte that UTF-8 reads as a truncated sequence.
    private static final byte WINDOWS_1252_E_ACUTE = (byte) 0xE9;

    // The statement a reading case runs: the file, read and printed.
    private static final String PRINT_READING = "println 'read: ' + shippedJson.readObjectFile(file('input.json'))";

    /** Writes a build applying the reader over the given json.jar and running one statement. */
    private static Path writeProbeProject(Path workspace, String jsonJarPath, String statement)
            throws IOException {

        var projectDirectory = Files.createDirectories(workspace.resolve("probe"));

        Files.writeString(
            projectDirectory.resolve("settings.gradle"),
            "rootProject.name = 'probe'");

        Files.writeString(
            projectDirectory.resolve("build.gradle"),
            String.join(
                "\n",
                "ext.gameJsonJarFile = file('" + jsonJarPath.replace('\\', '/') + "')",
                "apply from: '" + READER_SCRIPT.replace('\\', '/') + "'",
                "tasks.register('probe') {",
                "    doLast {",
                "        " + statement,
                "    }",
                "}"));

        return projectDirectory;
    }

    /** A probe reading an input file of the given text with the install's json.jar. */
    private static Path writeReadingProject(Path workspace, String inputText) throws IOException {

        var projectDirectory = writeProbeProject(workspace, GAME_JSON_JAR_FILE, PRINT_READING);

        Files.writeString(projectDirectory.resolve("input.json"), inputText, StandardCharsets.UTF_8);

        return projectDirectory;
    }

    private static String runProbe(Path projectDirectory) {

        return createRunner(projectDirectory)
            .build()
            .getOutput();
    }

    private static String runFailingProbe(Path projectDirectory) {

        return createRunner(projectDirectory)
            .buildAndFail()
            .getOutput();
    }

    private static GradleRunner createRunner(Path projectDirectory) {

        return GradleRunner
            .create()
            .withProjectDir(projectDirectory.toFile())
            .withArguments("probe");
    }

    @Nested
    final class ReadObjectFile {

        /** Members sorted by key whatever the file's order, each with the JSON type it was written as. */
        @Test
        void returnsMembersSortedWithTheirJsonTypes(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeReadingProject(workspace, "{\"b\": [1, true], \"a\": {\"c\": \"d\"}}");

            assertThat(runProbe(projectDirectory))
                .contains("read: [a:[c:d], b:[1, true]]");
        }

        /** Comments, carriage returns and a trailing comma: the game's reader takes all three. */
        @Test
        void readsTheGamesLenientSyntax(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeReadingProject(workspace, String.join(
                "\r\n",
                "# Heading comment.",
                "{",
                "  \"a\": \"b\", # trailing comment",
                "  \"c\": [1, 2,],",
                "}"));

            assertThat(runProbe(projectDirectory))
                .contains("read: [a:b, c:[1, 2]]");
        }

        /** Only a '#' outside quotes opens a comment; one inside text is text. */
        @Test
        void keepsAHashInsideQuotedText(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeReadingProject(workspace, "{\"name\": \"Build #2\"}");

            assertThat(runProbe(projectDirectory))
                .contains("read: [name:Build #2]");
        }

        /**
         * The engine's strip toggles its quote tracking on an escaped quote too, so a '#' after one is
         * read as a comment and the text breaks. Refused here exactly as the game would refuse it.
         */
        @Test
        void failsTextWhoseEscapedQuoteTheGameMisreads(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeReadingProject(workspace, "{\"name\": \"a \\\" # b\"}");

            assertThat(runFailingProbe(projectDirectory))
                .contains("does not parse as the game reads it");
        }

        @Test
        void failsAKeyDeclaredTwice(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeReadingProject(workspace, "{\"a\": 1, \"a\": 2}");

            assertThat(runFailingProbe(projectDirectory))
                .contains("does not parse as the game reads it")
                .contains("Duplicate key \"a\"");
        }

        @Test
        void failsAFileStartingWithAByteOrderMark(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeReadingProject(workspace, BYTE_ORDER_MARK + "{}");

            assertThat(runFailingProbe(projectDirectory))
                .contains("input.json starts with a UTF-8 byte-order mark, which the game refuses");
        }

        /** A legacy code page would otherwise read as replacement characters. */
        @Test
        void failsAFileThatIsNotUtf8(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeReadingProject(workspace, "{}");

            Files.write(
                projectDirectory.resolve("input.json"),
                new byte[] {'{', '"', WINDOWS_1252_E_ACUTE, '"', ':', '1', '}'});

            assertThat(runFailingProbe(projectDirectory))
                .contains("input.json is not valid UTF-8");
        }

        @Test
        void failsANullNamingItsMember(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeReadingProject(workspace, "{\"a\": {\"b\": null}}");

            assertThat(runFailingProbe(projectDirectory))
                .contains("input.json > a > b is null, which no shipped file uses");
        }

        @Test
        void failsATopLevelArray(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeReadingProject(workspace, "[1]");

            assertThat(runFailingProbe(projectDirectory))
                .contains("does not parse as the game reads it");
        }

        @Test
        void failsNamingWhereItLookedWhenTheGamesJsonJarIsMissing(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeProbeProject(workspace, "no/such/json.jar", PRINT_READING);

            Files.writeString(projectDirectory.resolve("input.json"), "{}");

            assertThat(runFailingProbe(projectDirectory))
                .contains("the game's json.jar was not found at")
                .contains("json.jar. Set STARSECTOR_HOME or pass -PstarsectorRoot=<path>.");
        }
    }

    @Nested
    final class RequireObject {

        /** Absent and wrongly typed are told apart, the fix for each being a different edit. */
        @Test
        void failsAnAbsentMemberAsMissing(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeProbeProject(
                workspace,
                GAME_JSON_JAR_FILE,
                "shippedJson.requireObject(null, 'here')");

            assertThat(runFailingProbe(projectDirectory))
                .contains("here is missing");
        }

        @Test
        void failsAMemberOfAnotherType(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeProbeProject(
                workspace,
                GAME_JSON_JAR_FILE,
                "shippedJson.requireObject('text', 'here')");

            assertThat(runFailingProbe(projectDirectory))
                .contains("here must be a JSON object");
        }
    }

    @Nested
    final class RequireList {

        @Test
        void failsAMemberOfAnotherType(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeProbeProject(
                workspace,
                GAME_JSON_JAR_FILE,
                "shippedJson.requireList([:], 'here')");

            assertThat(runFailingProbe(projectDirectory))
                .contains("here must be a JSON array");
        }
    }

    @Nested
    final class RequireString {

        @Test
        void failsAMemberOfAnotherType(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeProbeProject(
                workspace,
                GAME_JSON_JAR_FILE,
                "shippedJson.requireString(3, 'here')");

            assertThat(runFailingProbe(projectDirectory))
                .contains("here must be a JSON string");
        }
    }

    @Nested
    final class RequireOnlyKeys {

        /** Named beside the known keys, so a misspelling can be told from a key that does not belong. */
        @Test
        void failsAnUnknownKeyNamingTheKnownOnes(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeProbeProject(
                workspace,
                GAME_JSON_JAR_FILE,
                "shippedJson.requireOnlyKeys([colour: 1, size: 2], ['color', 'size'] as Set, 'here')");

            assertThat(runFailingProbe(projectDirectory))
                .contains("here carries unknown keys [colour]; known keys are [color, size]");
        }

        @Test
        void passesKnownKeys(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeProbeProject(
                workspace,
                GAME_JSON_JAR_FILE,
                "shippedJson.requireOnlyKeys([size: 2], ['color', 'size'] as Set, 'here'); println 'passed'");

            assertThat(runProbe(projectDirectory))
                .contains("passed");
        }
    }
}
