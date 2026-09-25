package kmlib.gradlescripts.integrationtests;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
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
 * Exercises the locale file write against a real Gradle build, reading with the game's own
 * {@code json.jar}.
 *
 * <p>The task writes into the repository - the data files the game loads and the launcher file - so
 * what it has to get right is where it writes, what it refuses to write, and that a locale switch is
 * never mistaken for an up-to-date build. The reading rules are the game's, so the cases that lean on
 * them - comments, a byte-order mark, a duplicated key - pin that the build parses as the game does
 * rather than as some stricter or looser reader would.
 */
final class WriteLocaleFilesIntegrationTests {

    private static final String WRITE_LOCALE_FILES_SCRIPT =
        new File("gradle/tasks/generate/write-locale-files.gradle").getAbsolutePath();

    // Handed over by the build running this suite: a throwaway build is a process of its own and
    // cannot locate an install.
    private static final String GAME_JSON_JAR_FILE = System.getProperty("kmlib.gameJsonJarFile");

    private static final String TASK_PATH = ":writeLocaleFiles";

    // Written as UTF-8, it becomes the three bytes the game's parser meets as the first character.
    private static final String BYTE_ORDER_MARK = "﻿";

    // "Simplified Chinese" (U+7B80 U+4F53 U+4E2D U+6587): a locale's name in its own language.
    private static final String SIMPLIFIED_CHINESE_DISPLAY_NAME = "简体中文";

    // "Klark's Library" (U+514B U+62C9 U+514B U+7684 U+5E93): a translated mod name, standing in for
    // any launcher text outside Latin-1.
    private static final String TRANSLATED_MOD_NAME = "克拉克的库";

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

    /**
     * Stands up a Java project applying the locale file script over the given manifest, with no
     * bundle yet. Java so the hooks onto jar and test are registered as they are in a mod.
     */
    private static Path writeLocalisedProject(Path workspace, String manifestText) throws IOException {

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
                "apply from: '" + WRITE_LOCALE_FILES_SCRIPT.replace('\\', '/') + "'"));

        if (manifestText != null) {
            writeProjectFile(projectDirectory, "localisation/manifest.json", manifestText);
        }
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

        /** The copy is a write, so a mapping that climbs out of the mod root must never be followed. */
        @Test
        void failsWithoutWritingWhenAFileMapsOutsideTheModRoot(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeLocalisedProject(workspace, String.join(
                "\n",
                "{",
                "  \"defaultLocale\": \"en\",",
                "  \"files\": { \"strings.json\": \"../escaped.json\" },",
                "  \"locales\": { \"en\": { \"displayName\": \"English\" } }",
                "}"));

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

        /** A fragment varying the version would build a different mod per locale. */
        @Test
        void failsAFragmentCarryingAFunctionalField(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            writeProjectFile(projectDirectory, "mod_info.base.json", MOD_INFO_BASE);
            writeProjectFile(projectDirectory, "localisation/zh-hans/mod_info.json", "{\"version\":\"9.9.9\"}");

            assertThat(runFailingWriteLocaleFiles(projectDirectory, "-Plocale=zh-hans"))
                .contains("carries unknown keys [version]");
        }

        @Test
        void failsAFragmentNamingADependencyTheBaseDoesNotDeclare(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            writeProjectFile(projectDirectory, "mod_info.base.json", MOD_INFO_BASE);
            writeProjectFile(
                projectDirectory,
                "localisation/zh-hans/mod_info.json",
                "{\"dependencies\":{\"nexerelin\":\"Nex\"}}");

            assertThat(runFailingWriteLocaleFiles(projectDirectory, "-Plocale=zh-hans"))
                .contains("names a dependency mod_info.base.json does not declare")
                .contains("[lunalib, lw_lazylib]");
        }

        /** Refused rather than skipped: the fragment's text would otherwise never reach the launcher. */
        @Test
        void failsAFragmentWithNoBaseToMergeOver(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            writeProjectFile(projectDirectory, "localisation/zh-hans/mod_info.json", "{\"name\":\"KMX\"}");

            assertThat(runFailingWriteLocaleFiles(projectDirectory, "-Plocale=zh-hans"))
                .contains("has no mod_info.base.json beside the manifest to be merged over");
        }

        /** Comments and a trailing comma, both of which the game's reader takes and a strict one refuses. */
        @Test
        void readsAManifestTheWayTheGameReadsIt(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeLocalisedProject(workspace, String.join(
                "\n",
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

        @Test
        void failsAManifestStartingWithAByteOrderMark(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            writeProjectFile(projectDirectory, "localisation/manifest.json", BYTE_ORDER_MARK + TWO_LOCALE_MANIFEST);

            assertThat(runFailingWriteLocaleFiles(projectDirectory))
                .contains("starts with a UTF-8 byte-order mark, which the game refuses");
        }

        @Test
        void failsAManifestDeclaringAKeyTwice(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeTwoLocaleProject(workspace);

            writeProjectFile(projectDirectory, "localisation/manifest.json",
                TWO_LOCALE_MANIFEST.replaceFirst("\\{", "{ \"defaultLocale\": \"zh-hans\","));

            assertThat(runFailingWriteLocaleFiles(projectDirectory))
                .contains("does not parse as the game reads it")
                .contains("Duplicate key \"defaultLocale\"");
        }
    }

    @Nested
    final class RegisterWriteLocaleFiles {

        /** A mod keeping no bundles is untouched: no task, and nothing hung off jar or test. */
        @Test
        void registersNoTaskWithoutAManifest(@TempDir Path workspace) throws IOException {

            var projectDirectory = writeLocalisedProject(workspace, null);

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
    }
}
