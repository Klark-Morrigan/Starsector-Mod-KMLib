package kmlib.testfixtures.localisation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the manifest's schema: what a well-formed one reads as, and each malformed shape the tooling
 * would otherwise act on.
 *
 * <p>The refusals matter more than the happy path. Writing a locale copies onto the paths this reads
 * and the release body links the URLs, so a manifest this let through would be a write outside the
 * repository or a link handed to players.
 */
final class LocaleManifestTest {

    private static final String CORE_LOCALISATION_URL = "https://github.com/TruthOriginem/Starsector-Localization-CN";

    // "Simplified Chinese" (U+7B80 U+4F53 U+4E2D U+6587): the locale's name in its own language, the one
    // manifest field where non-ASCII is expected.
    private static final String SIMPLIFIED_CHINESE_DISPLAY_NAME = "简体中文";

    // Both locales and both files, so every reading below has something to read.
    private static final String WELL_FORMED_MANIFEST = """
        {
          "defaultLocale": "en",
          "files": {
            "strings.json": "data/strings/strings.json",
            "LunaSettings.csv": "data/config/LunaSettings.csv"
          },
          "locales": {
            "en": { "displayName": "English" },
            "zh-hans": {
              "displayName": "%s",
              "coreLocalisation": "%s"
            }
          }
        }
        """.formatted(SIMPLIFIED_CHINESE_DISPLAY_NAME, CORE_LOCALISATION_URL);

    private static Path writeManifest(Path directory, String contents) throws IOException {

        var manifestFile = directory.resolve("manifest.json");

        Files.writeString(manifestFile, contents, StandardCharsets.UTF_8);

        return manifestFile;
    }

    // A manifest valid in every respect but the files map, for the cases about one mapped path.
    private static String createManifestMappingStringsTo(String dataPath) {
        return """
            {
              "defaultLocale": "en",
              "files": { "strings.json": "%s" },
              "locales": { "en": { "displayName": "English" } }
            }
            """.formatted(dataPath.replace("\\", "\\\\"));
    }

    @Nested
    class ReadManifest {

        @Test
        void aWellFormedManifestReadsSortedByKey(@TempDir Path directory) throws IOException {

            var manifest = LocaleManifest.readManifest(writeManifest(directory, WELL_FORMED_MANIFEST));

            assertThat(manifest.defaultLocaleTag())
                .isEqualTo("en");

            assertThat(manifest.dataPathsByBundleFileName())
                .containsExactly(
                    Map.entry("LunaSettings.csv", Path.of("data", "config", "LunaSettings.csv")),
                    Map.entry("strings.json", Path.of("data", "strings", "strings.json")));

            assertThat(manifest.declaredLocalesByTag())
                .containsExactly(
                    Map.entry("en", DeclaredLocale.createLocaleWithoutCoreLocalisation("en", "English")),
                    Map.entry("zh-hans", DeclaredLocale.createLocaleWithCoreLocalisation(
                        "zh-hans",
                        SIMPLIFIED_CHINESE_DISPLAY_NAME,
                        URI.create(CORE_LOCALISATION_URL))));
        }

        @Test
        void anUnknownTopLevelKeyIsRefused(@TempDir Path directory) throws IOException {

            var manifestFile = writeManifest(directory, """
                {
                  "defaultLocale": "en",
                  "default": "en",
                  "files": { "strings.json": "data/strings/strings.json" },
                  "locales": { "en": { "displayName": "English" } }
                }
                """);

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("unknown keys [default]");
        }

        @Test
        void theAmericanSpellingOfCoreLocalisationIsRefused(@TempDir Path directory) throws IOException {

            // Read leniently, this is a locale with no core localisation - the one gap the parity
            // check exists to catch, hidden by a spelling.
            var manifestFile = writeManifest(
                directory,
                WELL_FORMED_MANIFEST
                    .replace("coreLocalisation", "coreLocalization"));

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("locales > zh-hans carries unknown keys [coreLocalization]");
        }

        @Test
        void aDefaultLocaleNobodyDeclaredIsRefused(@TempDir Path directory) throws IOException {

            var manifestFile = writeManifest(
                directory,
                WELL_FORMED_MANIFEST
                    .replace("\"defaultLocale\": \"en\"", "\"defaultLocale\": \"fr\""));

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Default locale fr is not among the declared locales [en, zh-hans]");
        }

        @Test
        void aMissingDefaultLocaleIsReportedMissing(@TempDir Path directory) throws IOException {

            var manifestFile = writeManifest(
                directory,
                WELL_FORMED_MANIFEST
                    .replace("\"defaultLocale\": \"en\",", ""));

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("manifest.json > defaultLocale is missing");
        }

        @ParameterizedTest
        @ValueSource(strings = {"zh-Hans", "zh_hans", "english", "../en"})
        void aTagThatIsNotLowercasedBcp47IsRefused(String localeTag, @TempDir Path directory) throws IOException {

            // The tag is a directory name and a release file suffix, so case and separators matter.
            var manifestFile = writeManifest(directory, """
                {
                  "defaultLocale": "%1$s",
                  "files": { "strings.json": "data/strings/strings.json" },
                  "locales": { "%1$s": { "displayName": "Name" } }
                }
                """.formatted(localeTag));

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("is not a lowercased BCP 47 tag");
        }

        @Test
        void aBlankDisplayNameIsRefused(@TempDir Path directory) throws IOException {

            var manifestFile = writeManifest(
                directory,
                WELL_FORMED_MANIFEST
                    .replace("\"English\"", "\" \""));

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Locale en has a blank display name");
        }

        @ParameterizedTest
        @ValueSource(strings = {"http://github.com/example", "javascript:alert(1)", "github.com/example"})
        void aCoreLocalisationThatIsNotAnHttpsUrlIsRefused(String project, @TempDir Path directory)
                throws IOException {

            // The release body links it, so it is a link handed to players.
            var manifestFile = writeManifest(
                directory,
                WELL_FORMED_MANIFEST
                    .replace(CORE_LOCALISATION_URL, project));

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("which is not an https URL");
        }

        @Test
        void aCoreLocalisationThatDoesNotParseAsAUrlIsRefused(@TempDir Path directory) throws IOException {

            var manifestFile = writeManifest(
                directory,
                WELL_FORMED_MANIFEST
                    .replace(CORE_LOCALISATION_URL, "https://github.com/two words"));

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("locales > zh-hans > coreLocalisation is not a URL");
        }

        @Test
        void aManifestDeclaringNoLocalesIsRefused(@TempDir Path directory) throws IOException {

            var manifestFile = writeManifest(directory, """
                {
                  "defaultLocale": "en",
                  "files": { "strings.json": "data/strings/strings.json" },
                  "locales": {}
                }
                """);

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("The manifest declares no locales");
        }

        @Test
        void aManifestMappingNoFilesIsRefused(@TempDir Path directory) throws IOException {

            var manifestFile = writeManifest(directory, """
                {
                  "defaultLocale": "en",
                  "files": {},
                  "locales": { "en": { "displayName": "English" } }
                }
                """);

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("The manifest maps no bundle files");
        }

        @ParameterizedTest
        @ValueSource(strings = {"../outside/strings.json", "data/../../strings.json", "/data/strings.json", ""})
        void aDataPathOutsideTheModRootIsRefused(String dataPath, @TempDir Path directory) throws IOException {

            // Writing a locale copies onto this path, so anything leaving the mod root is a write outside
            // the repository.
            var manifestFile = writeManifest(directory, createManifestMappingStringsTo(dataPath));

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("which is not a path inside the mod root");
        }

        @Test
        void aBackslashedDataPathIsRefused(@TempDir Path directory) throws IOException {

            // A separator on Windows and a filename character elsewhere, so the same manifest would
            // copy to different places depending on who built it.
            var manifestFile = writeManifest(directory, createManifestMappingStringsTo("data\\strings.json"));

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("which is not written with forward slashes");
        }

        @Test
        void aBundleFileNameWithASeparatorIsRefused(@TempDir Path directory) throws IOException {

            var manifestFile = writeManifest(
                directory,
                WELL_FORMED_MANIFEST
                    .replace("\"strings.json\":", "\"strings/strings.json\":"));

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("\"strings/strings.json\" is not a bare file name");
        }

        @Test
        void theLauncherFileCannotBeMappedAsACopiedFile(@TempDir Path directory) throws IOException {

            // It is merged from its base; a copy would overwrite the merge.
            var manifestFile = writeManifest(
                directory,
                WELL_FORMED_MANIFEST
                    .replace(
                        "\"strings.json\": \"data/strings/strings.json\"",
                        "\"mod_info.json\": \"mod_info.json\""));

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("is the launcher file");
        }

        @Test
        void twoBundleFilesOnOneDataPathAreRefused(@TempDir Path directory) throws IOException {

            var manifestFile = writeManifest(
                directory,
                WELL_FORMED_MANIFEST
                    .replace("data/config/LunaSettings.csv", "data/strings/./strings.json"));

            assertThatThrownBy(() -> LocaleManifest.readManifest(manifestFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("which another bundle file already maps to");
        }
    }

    @Nested
    class Constructor {

        private static final DeclaredLocale ENGLISH =
            DeclaredLocale.createLocaleWithoutCoreLocalisation("en", "English");

        private static final Map<String, Path> STRINGS_ONLY =
            Map.of("strings.json", Path.of("data", "strings", "strings.json"));

        @Test
        void aLocaleKeyedUnderAnotherTagIsRefused() {

            // Unreachable from a file, whose key is the tag; reachable from any code building one.
            var declaredLocalesByTag = Map.of("en", ENGLISH, "fr", ENGLISH);

            assertThatThrownBy(() -> new LocaleManifest("en", STRINGS_ONLY, declaredLocalesByTag))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Locale en is keyed under fr");
        }

        @Test
        void aDataPathOutsideTheModRootIsRefusedHoweverTheManifestWasBuilt() {

            // The path rules guard where writing a locale copies to, so they hold for a manifest
            // built in code as well as for one read from a file.
            var dataPathsByBundleFileName = Map.of("strings.json", Path.of("..", "strings.json"));
            var declaredLocalesByTag = Map.of("en", ENGLISH);

            assertThatThrownBy(() -> new LocaleManifest("en", dataPathsByBundleFileName, declaredLocalesByTag))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("which is not a path inside the mod root");
        }
    }

    @Nested
    class GetDefaultLocale {

        @Test
        void theDefaultTagResolvesToItsDeclaration(@TempDir Path directory) throws IOException {

            var manifest = LocaleManifest.readManifest(writeManifest(directory, WELL_FORMED_MANIFEST));

            assertThat(manifest.getDefaultLocale())
                .isEqualTo(DeclaredLocale.createLocaleWithoutCoreLocalisation("en", "English"));
        }
    }
}
