package kmlib.gradlescripts.integrationtests;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the locale file write against a real Gradle build, reading with the game's own
 * {@code json.jar}.
 *
 * <p>The task writes into the repository - the data files the game loads and the launcher file - so
 * what it has to get right is where it writes, what it refuses to write, and that a locale switch is
 * never mistaken for an up-to-date build. How a file is parsed is the reader's, and pinned in its own
 * suite; one case here reads a manifest in the game's lenient syntax, to show the task reads through it.
 */
final class WriteLocaleFilesIntegrationTests {

    private static final String WRITE_LOCALE_FILES_SCRIPT =
        new File("gradle/tasks/generate/write-locale-files.gradle").getAbsolutePath();

    // The reader the task reads every file through, applied first as the conventions apply it.
    private static final String READER_SCRIPT =
        new File("gradle/shipped-json-reader.gradle").getAbsolutePath();

    // Handed over by the build running this suite: a throwaway build is a process of its own and
    // cannot locate an install.
    private static final String GAME_JSON_JAR_FILE = System.getProperty("kmlib.gameJsonJarFile");

    private static final String TASK_PATH = ":writeLocaleFiles";

    // "Simplified Chinese" (U+7B80 U+4F53 U+4E2D U+6587): a locale's name in its own language.
    private static final String SIMPLIFIED_CHINESE_DISPLAY_NAME = "简体中文";

    // "Klark's Library" (U+514B U+62C9 U+514B U+7684 U+5E93): a translated mod name, standing in for
    // any launcher text outside Latin-1.
    private static final String TRANSLATED_MOD_NAME = "克拉克的库";

    // The members a minimal manifest is built from, so a case varies only the one it is about.
    private static final String ENGLISH_LOCALE = "\"en\": { \"displayName\": \"English\" }";
    private static final String STRINGS_MAPPING = "\"strings.json\": \"data/strings/strings.json\"";

    private static final String TWO_LOCALE_MANIFEST = String.join(
        "\n",
        "{",
        "  \"defaultLocale\": \"en\",",
        "  \"files\": {",
        "    \"strings.json\": \"data/strings/strings.json\",",
        "    \"LunaSettings.csv\": \"data/config/LunaSettings.csv\"",
        "  },",
        "  \"locales\": {",
        "    \"en\": { \"displayName\": \"English\" },",
        "    \"zh-hans\": { \"displayName\": \"" + SIMPLIFIED_CHINESE_DISPLAY_NAME + "\" }",
        "  }",
        "}");

    private static final String MOD_INFO_BASE = String.join(
        "\n",
        "{",
        "  \"id\": \"kmx\",",
        "  \"name\": \"Klark's Library\",",
        "  \"description\": \"Shared code.\",",
        "  \"version\": \"1.2.3\",",
        "  \"jars\": [\"jars/KMX.jar\"],",
        "  \"dependencies\": [",
        "    { \"id\": \"lw_lazylib\", \"name\": \"LazyLib\" },",
        "    { \"id\": \"lunalib\", \"name\": \"LunaLib\" }",
        "  ]",
        "}");

    /** A manifest from its three members, each written as the JSON that follows its key. */
    private static String createManifest(String defaultLocaleValue, String filesMembers, String localesMembers) {

        return "{ \"defaultLocale\": " + defaultLocaleValue
            + ", \"files\": { " + filesMembers + " }"
            + ", \"locales\": { " + localesMembers + " } }";
    }

    /**
     * Stands up a Java project applying the locale file script, with no manifest and no bundle yet.
     * Java so the hooks onto jar and test are registered as they are in a mod.
     */
    private static Path writeProjectWithoutManifest(Path workspace) throws IOException {

        var projectDirectory = Files.createDirectories(workspace.resolve("kmx"));

        Files.writeString(
            projectDirectory.resolve("settings.gradle"),
            "rootProject.name = 'kmx'");

        Files.writeString(
            projectDirectory.resolve("build.gradle"),
            String.join(
                "\n",
                "apply plugin: 'java'",
                "ext.gameJsonJarFile = file('" + GAME_JSON_JAR_FILE.replace('\\', '/') + "')",
                "apply from: '" + READER_SCRIPT.replace('\\', '/') + "'",
                "apply from: '" + WRITE_LOCALE_FILES_SCRIPT.replace('\\', '/') + "'"));

        return projectDirectory;
    }

    /** The same project over the given manifest, with no bundle yet. */
    private static Path writeLocalisedProject(Path workspace, String manifestText) throws IOException {

        var projectDirectory = writeProjectWithoutManifest(workspace);

        writeProjectFile(projectDirectory, "localisation/manifest.json", manifestText);

        return projectDirectory;
    }

    /** Both locales' bundles, each file carrying its locale's tag so a copy says where it came from. */
    private static Path writeTwoLocaleProject(Path workspace) throws IOException {

        var projectDirectory = writeLocalisedProject(workspace, TWO_LOCALE_MANIFEST);

        for (var localeTag : new String[] {"en", "zh-hans"}) {

            writeProjectFile(projectDirectory, "localisation/" + localeTag + "/strings.json",
                "{\"kmx\":{\"greeting\":\"" + localeTag + "\"}}");
            writeProjectFile(projectDirectory, "localisation/" + localeTag + "/LunaSettings.csv",
                "fieldID,fieldName\nkmx_row," + localeTag + "\n");
        }
        return projectDirectory;
    }

    private static void writeProjectFile(Path projectDirectory, String relativePath, String text)
            throws IOException {

        var projectFile = projectDirectory.resolve(relativePath);

        Files.createDirectories(projectFile.getParent());
        Files.writeString(projectFile, text, StandardCharsets.UTF_8);
    }

    private static String readProjectFile(Path projectDirectory, String relativePath) throws IOException {
        return Files.readString(projectDirectory.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static GradleRunner createRunner(Path projectDirectory, String... arguments) {

        return GradleRunner
            .create()
            .withProjectDir(projectDirectory.toFile())
            .withArguments(arguments);
    }

    private static BuildResult runWriteLocaleFiles(Path projectDirectory, String... extraArguments) {

        return createRunner(projectDirectory, prependTask(extraArguments))
            .build();
    }

    private static String runFailingWriteLocaleFiles(Path projectDirectory, String... extraArguments) {

        return createRunner(projectDirectory, prependTask(extraArguments))
            .buildAndFail()
            .getOutput();
    }

    private static String[] prependTask(String... extraArguments) {

        var arguments = new String[extraArguments.length + 1];

        arguments[0] = "writeLocaleFiles";
        System.arraycopy(extraArguments, 0, arguments, 1, extraArguments.length);

        return arguments;
    }

    @Nested
    final class WriteLocaleFiles {

        /** Each manifest the task refuses before copying anything, beside what the refusal says. */
        static Stream<Arguments> listRefusedManifests() {

            return Stream.of(
                Arguments.of(
                    "a locale tag stepping out of localisation/",
                    createManifest(
                        "\"en\"",
                        STRINGS_MAPPING,
                        ENGLISH_LOCALE + ", \"../x\": { \"displayName\": \"X\" }"),
                    "\"../x\" is not a lowercased BCP 47 tag"),
                Arguments.of(
                    "a locale tag that is not lowercased",
                    createManifest(
                        "\"en\"",
                        STRINGS_MAPPING,
                        ENGLISH_LOCALE + ", \"zh-Hans\": { \"displayName\": \"X\" }"),
                    "\"zh-Hans\" is not a lowercased BCP 47 tag"),
                Arguments.of(
                    "a default the manifest does not declare",
                    createManifest("\"fr\"", STRINGS_MAPPING, ENGLISH_LOCALE),
                    "default locale fr is not among the declared locales [en]"),
                Arguments.of(
                    "a default that is not a string",
                    createManifest("3", STRINGS_MAPPING, ENGLISH_LOCALE),
                    "defaultLocale must be a JSON string"),
                Arguments.of(
                    "a misspelt key",
                    "{ \"coreLocalization\": \"x\", "
                        + createManifest("\"en\"", STRINGS_MAPPING, ENGLISH_LOCALE).substring(1),
                    "carries unknown keys [coreLocalization]"),
                Arguments.of(
                    "a bundle file named with a path",
                    createManifest("\"en\"", "\"../strings.json\": \"data/strings/strings.json\"", ENGLISH_LOCALE),
                    "\"../strings.json\" is not a bare file name"),
                Arguments.of(
                    "the launcher file mapped as a copy",
                    createManifest("\"en\"", "\"mod_info.json\": \"mod_info.json\"", ENGLISH_LOCALE),
                    "mod_info.json is the launcher file, which is merged from its base rather than copied"),
                Arguments.of(
                    "a data path on a drive",
                    createManifest("\"en\"", "\"strings.json\": \"C:/strings.json\"", ENGLISH_LOCALE),
                    "maps to \"C:/strings.json\", which is not a path inside the mod root"),
                Arguments.of(
                    "a data path from the filesystem root",
                    createManifest("\"en\"", "\"strings.json\": \"/strings.json\"", ENGLISH_LOCALE),
                    "maps to \"/strings.json\", which is not a path inside the mod root"),
                Arguments.of(
                    "a data path climbing out part way along",
                    createManifest("\"en\"", "\"strings.json\": \"data/../../strings.json\"", ENGLISH_LOCALE),
                    "maps to \"data/../../strings.json\", which is not a path inside the mod root"),
                Arguments.of(
                    "a data path written with a backslash",
                    createManifest("\"en\"", "\"strings.json\": \"data\\\\strings.json\"", ENGLISH_LOCALE),
                    "which is not written with forward slashes"),
                Arguments.of(
                    "two bundle files mapped onto one file",
                    createManifest(
                        "\"en\"",
                        STRINGS_MAPPING + ", \"other.json\": \"data/strings/strings.json\"",
                        ENGLISH_LOCALE),
                    "which another bundle file already maps to"),
                Arguments.of(
                    "a file map mapping nothing",
                    createManifest("\"en\"", "", ENGLISH_LOCALE),
                    "files maps no bundle files"));
        }

        /** Each launcher fragment the task refuses to merge, beside what the refusal says. */
        static Stream<Arguments> listRefusedFragments() {

            return Stream.of(
                Arguments.of(
                    "a functional field, which would build a different mod per locale",
                    "{\"version\":\"9.9.9\"}",
                    "carries unknown keys [version]"),
                Arguments.of(
                    "blank text, which the launcher draws as an empty row",
                    "{\"description\":\" \"}",
                    "description is blank"),
                Arguments.of(
                    "a blank dependency name",
                    "{\"dependencies\":{\"lunalib\":\"\"}}",
                    "lunalib is blank"),
                Arguments.of(
                    "dependencies listed as the base lists them",
                    "{\"dependencies\":[\"lunalib\"]}",
                    "dependencies must be a JSON object"),
                Arguments.of(
                    "a dependency the base does not declare",
                    "{\"dependencies\":{\"nexerelin\":\"Nex\"}}",
                    "names a dependency mod_info.base.json does not declare; it declares [lunalib, lw_lazylib]"));
        }

        /**
         * A copy rather than a rewrite: carriage returns, non-ASCII and a missing final newline all
         * survive, which is what lets a mod's first bundle reproduce its previous data files exactly.
         */
        @Test
        void copiesEachMappedFileByteForByteFromTheDefaultLocaleWhenNoneIsRequested(@TempDir Path workspace)
                throws IOException {

            var projectDirectory = writeLocalisedProject(workspace, TWO_LOCALE_MANIFEST);
            var stringsBytes = ("{\r\n\"kmx\":{\"name\":\"" + SIMPLIFIED_CHINESE_DISPLAY_NAME + "\"}}")
                .getBytes(StandardCharsets.UTF_8);

            Files.createDirectories(projectDirectory.resolve("localisation/en"));
            Files.write(projectDirectory.resolve("localisation/en/strings.json"), stringsBytes);

            writeProjectFile(projectDirectory, "localisation/en/LunaSettings.csv", "fieldID\r\nkmx_row");
            runWriteLocaleFiles(projectDirectory);

            assertThat(Files.readAllBytes(projectDirectory.resolve("data/strings/strings.json")))
                .isEqualTo(stringsBytes);
            assertThat(readProjectFile(projectDirectory, "data/config/LunaSettings.csv"))
                .isEqualTo("fieldID\r\nkmx_row");
        }

        @Test
        void copiesTheRequestedLocalesFilesWhenALocaleIsRequested(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            runWriteLocaleFiles(projectDirectory, "-Plocale=zh-hans");

            assertThat(readProjectFile(projectDirectory, "data/strings/strings.json"))
                .isEqualTo("{\"kmx\":{\"greeting\":\"zh-hans\"}}");
            assertThat(readProjectFile(projectDirectory, "data/config/LunaSettings.csv"))
                .isEqualTo("fieldID,fieldName\nkmx_row,zh-hans\n");
        }

        /**
         * The locale is an input of its own. Without it the bundle directories and the outputs are
         * all unchanged by a switch, and the task would report up to date over the previous language.
         */
        @Test
        void runsAgainWhenTheRequestedLocaleChangesAndOnlyThen(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            runWriteLocaleFiles(projectDirectory);

            var switchedResult = runWriteLocaleFiles(projectDirectory, "-Plocale=zh-hans");
            var repeatedResult = runWriteLocaleFiles(projectDirectory, "-Plocale=zh-hans");

            assertThat(switchedResult.task(TASK_PATH).getOutcome())
                .isEqualTo(TaskOutcome.SUCCESS);
            assertThat(repeatedResult.task(TASK_PATH).getOutcome())
                .isEqualTo(TaskOutcome.UP_TO_DATE);
            assertThat(readProjectFile(projectDirectory, "data/strings/strings.json"))
                .isEqualTo("{\"kmx\":{\"greeting\":\"zh-hans\"}}");
        }

        @Test
        void failsNamingTheDeclaredLocalesWhenTheRequestedOneIsUndeclared(@TempDir Path workspace)
                throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            assertThat(runFailingWriteLocaleFiles(projectDirectory, "-Plocale=fr"))
                .contains("Cannot write locale fr")
                .contains("declares only [en, zh-hans]");
        }

        /**
         * Strict, because the previous locale's copy left in place would ship as this one's, and a
         * missing strings file has nothing true to fall back to.
         */
        @Test
        void failsWhenTheBundleLacksAMappedFile(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            Files.delete(projectDirectory.resolve("localisation/zh-hans/LunaSettings.csv"));

            assertThat(runFailingWriteLocaleFiles(projectDirectory, "-Plocale=zh-hans"))
                .contains("Locale zh-hans holds no LunaSettings.csv");
        }

        /**
         * The path and tag cases matter most: each is all that stands between the copy and a read or
         * a write outside the repository.
         */
        @ParameterizedTest(name = "{0}")
        @MethodSource("listRefusedManifests")
        void failsAManifestItCannotActOn(
                String caseName,
                String manifestText,
                String expectedMessage,
                @TempDir Path workspace) throws IOException {

            var projectDirectory = writeLocalisedProject(workspace, manifestText);

            writeProjectFile(projectDirectory, "localisation/en/strings.json", "{}");

            assertThat(runFailingWriteLocaleFiles(projectDirectory))
                .contains(expectedMessage);
        }

        /** The copy is a write, so a mapping that climbs out of the mod root must never be followed. */
        @Test
        void failsWithoutWritingWhenAFileMapsOutsideTheModRoot(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeLocalisedProject(
                workspace,
                createManifest("\"en\"", "\"strings.json\": \"../escaped.json\"", ENGLISH_LOCALE));

            writeProjectFile(projectDirectory, "localisation/en/strings.json", "{}");

            assertThat(runFailingWriteLocaleFiles(projectDirectory))
                .contains("maps to \"../escaped.json\", which is not a path inside the mod root");
            assertThat(workspace.resolve("escaped.json"))
                .doesNotExist();
        }

        /**
         * Text only: the name, the description and a dependency's name come from the fragment, and
         * everything functional - the ID, the version, the jar list, a dependency's ID - from the base.
         * Written literally rather than escaped, so the generated file reads as the translation.
         */
        @Test
        void mergesTheFragmentsTextOverTheBase(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            writeProjectFile(projectDirectory, "mod_info.base.json", MOD_INFO_BASE);
            writeProjectFile(projectDirectory, "localisation/zh-hans/mod_info.json", String.join(
                "\n",
                "{",
                "  \"name\": \"" + TRANSLATED_MOD_NAME + "\",",
                "  \"dependencies\": { \"lunalib\": \"Luna\" }",
                "}"));

            runWriteLocaleFiles(projectDirectory, "-Plocale=zh-hans");

            assertThat(readProjectFile(projectDirectory, "mod_info.json"))
                .isEqualTo(String.join(
                    "\n",
                    "{",
                    "    \"dependencies\": [",
                    "        {",
                    "            \"id\": \"lw_lazylib\",",
                    "            \"name\": \"LazyLib\"",
                    "        },",
                    "        {",
                    "            \"id\": \"lunalib\",",
                    "            \"name\": \"Luna\"",
                    "        }",
                    "    ],",
                    "    \"description\": \"Shared code.\",",
                    "    \"id\": \"kmx\",",
                    "    \"jars\": [",
                    "        \"jars/KMX.jar\"",
                    "    ],",
                    "    \"name\": \"" + TRANSLATED_MOD_NAME + "\",",
                    "    \"version\": \"1.2.3\"",
                    "}",
                    ""));
        }

        /** A locale translating no launcher field shows the base file's text, degraded rather than broken. */
        @Test
        void writesTheBaseAloneWhenTheLocaleCarriesNoFragment(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            writeProjectFile(projectDirectory, "mod_info.base.json", "{\"id\":\"kmx\",\"version\":\"1.2.3\"}");
            runWriteLocaleFiles(projectDirectory, "-Plocale=zh-hans");

            assertThat(readProjectFile(projectDirectory, "mod_info.json"))
                .isEqualTo(String.join(
                    "\n",
                    "{",
                    "    \"id\": \"kmx\",",
                    "    \"version\": \"1.2.3\"",
                    "}",
                    ""));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("listRefusedFragments")
        void failsAFragmentItCannotMerge(
                String caseName,
                String fragmentText,
                String expectedMessage,
                @TempDir Path workspace) throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            writeProjectFile(projectDirectory, "mod_info.base.json", MOD_INFO_BASE);
            writeProjectFile(projectDirectory, "localisation/zh-hans/mod_info.json", fragmentText);

            assertThat(runFailingWriteLocaleFiles(projectDirectory, "-Plocale=zh-hans"))
                .contains(expectedMessage);
        }

        /** Refused rather than skipped: the fragment's text would otherwise never reach the launcher. */
        @Test
        void failsAFragmentWithNoBaseToMergeOver(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            writeProjectFile(projectDirectory, "localisation/zh-hans/mod_info.json", "{\"name\":\"KMX\"}");

            assertThat(runFailingWriteLocaleFiles(projectDirectory, "-Plocale=zh-hans"))
                .contains("has no mod_info.base.json beside the manifest to be merged over");
        }

        /**
         * Comments, carriage returns and a trailing comma, all of which the game's reader takes and a
         * strict one refuses.
         */
        @Test
        void readsAManifestTheWayTheGameReadsIt(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeLocalisedProject(workspace, String.join(
                "\r\n",
                "# Which locales this mod ships.",
                "{",
                "  \"defaultLocale\": \"en\", # the build's fallback",
                "  \"files\": { \"strings.json\": \"data/strings/strings.json\", },",
                "  \"locales\": { \"en\": { \"displayName\": \"English\" } }",
                "}"));

            writeProjectFile(projectDirectory, "localisation/en/strings.json", "{}");
            runWriteLocaleFiles(projectDirectory);

            assertThat(readProjectFile(projectDirectory, "data/strings/strings.json"))
                .isEqualTo("{}");
        }
    }

    @Nested
    final class RegisterWriteLocaleFiles {

        /** A mod keeping no bundles is untouched: no task, and nothing hung off jar or test. */
        @Test
        void registersNoTaskWithoutAManifest(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeProjectWithoutManifest(workspace);

            assertThat(createRunner(projectDirectory, "tasks", "--all").build().getOutput())
                .doesNotContain("writeLocaleFiles");
        }

        /** CI runs test before jar, so each has to reach the written files on its own. */
        @Test
        void runsAheadOfBothJarAndTest(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            assertThat(createRunner(projectDirectory, "jar", "--dry-run").build().getOutput())
                .contains(TASK_PATH + " SKIPPED");
            assertThat(createRunner(projectDirectory, "test", "--dry-run").build().getOutput())
                .contains(TASK_PATH + " SKIPPED");
        }

        /**
         * Inputs rather than a bare dependency, which is what re-runs the suites after a locale switch:
         * a changed input file makes test out of date where a dependency alone would not.
         */
        @Test
        void makesTheWrittenFilesInputsOfTest(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            Files.writeString(
                projectDirectory.resolve("build.gradle"),
                String.join(
                    "\n",
                    Files.readString(projectDirectory.resolve("build.gradle")),
                    "tasks.register('printTestInputs') {",
                    "    doLast {",
                    "        tasks.test.inputs.files.files.each { inputFile ->",
                    "            println 'test input: ' + projectDir.toPath().relativize(inputFile.toPath())"
                        + ".toString().replace('\\\\', '/')",
                    "        }",
                    "    }",
                    "}"));

            assertThat(createRunner(projectDirectory, "printTestInputs").build().getOutput())
                .contains("test input: data/strings/strings.json")
                .contains("test input: data/config/LunaSettings.csv");
        }
    }
}
