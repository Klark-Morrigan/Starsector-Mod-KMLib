package kmlib.testfixtures.localisation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins each comparison against a bundle broken the one way it looks for, beside an agreeing pair it must
 * pass. Every case starts from an English default and a Simplified Chinese translation that agree, and
 * breaks one file.
 */
final class LocaleParityTest {

    // "Chinese" (U+4E2D U+6587): text far outside Latin-1, as a translated bundle carries it.
    private static final String CHINESE_TEXT = "中文";

    // "Cafe" with an acute e (U+00E9): accented, but inside Latin-1, which the vanilla atlases draw.
    private static final String LATIN_1_TEXT = "Café";

    private static final String FIELD_ID_PREFIX = "kmu_";

    private static final String MANIFEST_WITH_CORE_LOCALISATION = """
        {
          "defaultLocale": "en",
          "files": {
            "strings.json": "data/strings/strings.json",
            "LunaSettings.csv": "data/config/LunaSettings.csv"
          },
          "locales": {
            "en": { "displayName": "English" },
            "zh-hans": { "displayName": "Chinese", "coreLocalisation": "https://example.com/core" }
          }
        }
        """;

    private static final String MANIFEST_WITHOUT_CORE_LOCALISATION = """
        {
          "defaultLocale": "en",
          "files": {
            "strings.json": "data/strings/strings.json",
            "LunaSettings.csv": "data/config/LunaSettings.csv"
          },
          "locales": {
            "en": { "displayName": "English" },
            "zh-hans": { "displayName": "Chinese" }
          }
        }
        """;

    // A mod shipping no settings table, which maps its strings alone.
    private static final String MANIFEST_MAPPING_STRINGS_ONLY = """
        {
          "defaultLocale": "en",
          "files": { "strings.json": "data/strings/strings.json" },
          "locales": {
            "en": { "displayName": "English" },
            "zh-hans": { "displayName": "Chinese" }
          }
        }
        """;

    private static final String ENGLISH_STRINGS = """
        { "kmu": { "greeting": "Hello %s, you hold %d", "farewell": "Goodbye" } }
        """;

    private static final String TRANSLATED_STRINGS = """
        { "kmu": { "greeting": "Bonjour %s, %d", "farewell": "Au revoir" } }
        """;

    private static final String SETTINGS_HEADER_LINE =
        "fieldID,,,,fieldName,,fieldType,defaultValue,secondaryValue,,,fieldDescription,,,minValue,maxValue,tab\n";

    private static final String ENGLISH_SETTINGS = SETTINGS_HEADER_LINE + """
        kmu_caption,,,,Colours,,Header,Colours,,,,,,,,,Visuals
        kmu_palette,,,,Palette,,Radio,Gold,"Gold, Silver",,,Which palette,,,,,Visuals
        kmu_width,,,,Width,,Int,4,,,,How wide,,,1,8,Visuals
        kmu_note,,,,,,Text,A note,,,,,,,,,General
        """;

    // Every word translated, every behaviour kept: the shape a translation is meant to take.
    private static final String TRANSLATED_SETTINGS = SETTINGS_HEADER_LINE + """
        kmu_caption,,,,Couleurs,,Header,Couleurs,,,,,,,,,Visuel
        kmu_palette,,,,Palette,,Radio,Gold,"Gold, Silver",,,Quelle palette,,,,,Visuel
        kmu_width,,,,Largeur,,Int,4,,,,Quelle largeur,,,1,8,Visuel
        kmu_note,,,,,,Text,Une note,,,,,,,,,Autre
        """;

    private static final String MOD_INFO_BASE = """
        {
          "id": "kmu",
          "name": "Name",
          "description": "Description",
          "dependencies": [ { "id": "kmlib", "name": "KMLib" } ]
        }
        """;

    private static void writeFile(Path file, String contents) throws IOException {

        Files.createDirectories(file.getParent());
        Files.writeString(file, contents, StandardCharsets.UTF_8);
    }

    // The agreeing pair every case breaks one file of: a manifest, then both bundles holding both files.
    private static Path writeAgreeingMod(Path modRoot, String manifest) throws IOException {

        var localisation = modRoot.resolve("localisation");

        writeFile(localisation.resolve("manifest.json"), manifest);
        writeFile(localisation.resolve("en").resolve("strings.json"), ENGLISH_STRINGS);
        writeFile(localisation.resolve("en").resolve("LunaSettings.csv"), ENGLISH_SETTINGS);
        writeFile(localisation.resolve("zh-hans").resolve("strings.json"), TRANSLATED_STRINGS);
        writeFile(localisation.resolve("zh-hans").resolve("LunaSettings.csv"), TRANSLATED_SETTINGS);

        return localisation;
    }

    private static Path writeAgreeingMod(Path modRoot) throws IOException {
        return writeAgreeingMod(modRoot, MANIFEST_WITH_CORE_LOCALISATION);
    }

    private static LocaleParity createParity(Path localisation) {
        return new LocaleParity(new LocalisationDirectory(localisation), FIELD_ID_PREFIX);
    }

    @Nested
    class FindDeclaredLocalesWithoutBundleDirectory {

        @Test
        void everyDeclaredLocaleHoldingADirectoryPasses(@TempDir Path modRoot) throws IOException {

            assertThat(createParity(writeAgreeingMod(modRoot)).findDeclaredLocalesWithoutBundleDirectory())
                .isEmpty();
        }

        @Test
        void aDeclaredLocaleWithNoDirectoryIsFound(@TempDir Path modRoot) throws IOException {

            var localisation = modRoot.resolve("localisation");

            writeFile(localisation.resolve("manifest.json"), MANIFEST_WITH_CORE_LOCALISATION);
            writeFile(localisation.resolve("en").resolve("strings.json"), ENGLISH_STRINGS);

            assertThat(createParity(localisation).findDeclaredLocalesWithoutBundleDirectory())
                .containsExactly("zh-hans: declared by the manifest, but has no bundle directory");
        }
    }

    @Nested
    class FindFormatArgumentMismatches {

        @Test
        void agreeingSlotsPass(@TempDir Path modRoot) throws IOException {

            assertThat(createParity(writeAgreeingMod(modRoot)).findFormatArgumentMismatches())
                .isEmpty();
        }

        @Test
        void slotsNumberedIntoAnotherOrderPass(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(localisation.resolve("zh-hans").resolve("strings.json"), """
                { "kmu": { "greeting": "%2$d, %1$s", "farewell": "Au revoir" } }
                """);

            assertThat(createParity(localisation).findFormatArgumentMismatches())
                .isEmpty();
        }

        @Test
        void aDroppedSlotIsFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(localisation.resolve("zh-hans").resolve("strings.json"), """
                { "kmu": { "greeting": "Bonjour %s", "farewell": "Au revoir" } }
                """);

            assertThat(createParity(localisation).findFormatArgumentMismatches())
                .containsExactly("zh-hans: strings.json kmu > greeting takes [%1$s] where en takes [%1$s, %2$d]");
        }

        @Test
        void aRetypedSlotIsFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(localisation.resolve("zh-hans").resolve("strings.json"), """
                { "kmu": { "greeting": "Bonjour %s, %s", "farewell": "Au revoir" } }
                """);

            assertThat(createParity(localisation).findFormatArgumentMismatches())
                .containsExactly("zh-hans: strings.json kmu > greeting takes [%1$s, %2$s] where en takes [%1$s, %2$d]");
        }
    }

    @Nested
    class FindLocalesMissingCoreLocalisation {

        @Test
        void textOutsideLatin1PassesUnderACoreLocalisation(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(localisation.resolve("zh-hans").resolve("strings.json"), """
                { "kmu": { "greeting": "%s %%s %%d", "farewell": "%s" } }
                """.formatted(CHINESE_TEXT, CHINESE_TEXT));

            assertThat(createParity(localisation).findLocalesMissingCoreLocalisation())
                .isEmpty();
        }

        @Test
        void accentedTextInsideLatin1PassesWithoutOne(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot, MANIFEST_WITHOUT_CORE_LOCALISATION);

            writeFile(localisation.resolve("zh-hans").resolve("strings.json"), """
                { "kmu": { "greeting": "%s %%s %%d", "farewell": "%s" } }
                """.formatted(LATIN_1_TEXT, LATIN_1_TEXT));

            assertThat(createParity(localisation).findLocalesMissingCoreLocalisation())
                .isEmpty();
        }

        @Test
        void stringsOutsideLatin1AreFoundWithoutOne(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot, MANIFEST_WITHOUT_CORE_LOCALISATION);

            writeFile(localisation.resolve("zh-hans").resolve("strings.json"), """
                { "kmu": { "greeting": "%s %%s %%d", "farewell": "Au revoir" } }
                """.formatted(CHINESE_TEXT));

            assertThat(createParity(localisation).findLocalesMissingCoreLocalisation())
                .containsExactly("zh-hans: strings.json draws characters outside Latin-1, "
                    + "but the manifest names no coreLocalisation for the locale");
        }

        @Test
        void aSettingsRowOutsideLatin1IsFoundWithoutOne(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot, MANIFEST_WITHOUT_CORE_LOCALISATION);

            writeFile(localisation.resolve("zh-hans").resolve("LunaSettings.csv"), SETTINGS_HEADER_LINE + """
                kmu_caption,,,,Couleurs,,Header,Couleurs,,,,,,,,,Visuel
                kmu_palette,,,,Palette,,Radio,Gold,"Gold, Silver",,,Quelle palette,,,,,Visuel
                kmu_width,,,,Largeur,,Int,4,,,,Quelle largeur,,,1,8,Visuel
                kmu_note,,,,,,Text,%s,,,,,,,,,Autre
                """.formatted(CHINESE_TEXT));

            assertThat(createParity(localisation).findLocalesMissingCoreLocalisation())
                .containsExactly("zh-hans: LunaSettings.csv draws characters outside Latin-1, "
                    + "but the manifest names no coreLocalisation for the locale");
        }
    }

    @Nested
    class FindMissingBundleFiles {

        @Test
        void bundlesHoldingEveryMappedFilePass(@TempDir Path modRoot) throws IOException {

            assertThat(createParity(writeAgreeingMod(modRoot)).findMissingBundleFiles())
                .isEmpty();
        }

        @Test
        void aMappedFileABundleLacksIsFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            Files.delete(localisation.resolve("zh-hans").resolve("LunaSettings.csv"));

            assertThat(createParity(localisation).findMissingBundleFiles())
                .containsExactly("zh-hans: holds no LunaSettings.csv, which the manifest maps");
        }

        @Test
        void aLocaleWithNoDirectoryIsLeftToItsOwnFinding(@TempDir Path modRoot) throws IOException {

            var localisation = modRoot.resolve("localisation");

            writeFile(localisation.resolve("manifest.json"), MANIFEST_WITH_CORE_LOCALISATION);
            writeFile(localisation.resolve("en").resolve("strings.json"), ENGLISH_STRINGS);
            writeFile(localisation.resolve("en").resolve("LunaSettings.csv"), ENGLISH_SETTINGS);

            assertThat(createParity(localisation).findMissingBundleFiles())
                .isEmpty();
        }
    }

    @Nested
    class FindModInfoFragmentMismatches {

        @Test
        void aFragmentNamingDeclaredDependenciesPasses(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(modRoot.resolve("mod_info.base.json"), MOD_INFO_BASE);
            writeFile(localisation.resolve("zh-hans").resolve("mod_info.json"), """
                { "name": "Nom", "dependencies": { "kmlib": "Bibliotheque" } }
                """);

            assertThat(createParity(localisation).findModInfoFragmentMismatches())
                .isEmpty();
        }

        @Test
        void aDependencyTheBaseDoesNotDeclareIsFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(modRoot.resolve("mod_info.base.json"), MOD_INFO_BASE);
            writeFile(localisation.resolve("zh-hans").resolve("mod_info.json"), """
                { "dependencies": { "lunalib": "Reglages" } }
                """);

            assertThat(createParity(localisation).findModInfoFragmentMismatches())
                .containsExactly(
                    "zh-hans: mod_info.json names dependency lunalib, which mod_info.base.json does not declare");
        }

        @Test
        void aFragmentWithNoBaseIsFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(localisation.resolve("zh-hans").resolve("mod_info.json"), """
                { "name": "Nom" }
                """);

            assertThat(createParity(localisation).findModInfoFragmentMismatches())
                .containsExactly("zh-hans: mod_info.json has no mod_info.base.json to be merged over");
        }

        @Test
        void aFragmentVaryingAFunctionalFieldIsRefusedOnTheRead(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(modRoot.resolve("mod_info.base.json"), MOD_INFO_BASE);
            writeFile(localisation.resolve("zh-hans").resolve("mod_info.json"), """
                { "version": "2.0.0" }
                """);

            assertThatThrownBy(() -> createParity(localisation).findModInfoFragmentMismatches())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("unknown keys [version]");
        }
    }

    @Nested
    class FindSettingsBehaviourMismatches {

        @Test
        void translatedWordingPasses(@TempDir Path modRoot) throws IOException {

            assertThat(createParity(writeAgreeingMod(modRoot)).findSettingsBehaviourMismatches())
                .isEmpty();
        }

        @Test
        void translatedRadioOptionsAreFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(localisation.resolve("zh-hans").resolve("LunaSettings.csv"), SETTINGS_HEADER_LINE + """
                kmu_caption,,,,Couleurs,,Header,Couleurs,,,,,,,,,Visuel
                kmu_palette,,,,Palette,,Radio,Or,"Or, Argent",,,Quelle palette,,,,,Visuel
                kmu_width,,,,Largeur,,Int,4,,,,Quelle largeur,,,1,8,Visuel
                kmu_note,,,,,,Text,Une note,,,,,,,,,Autre
                """);

            assertThat(createParity(localisation).findSettingsBehaviourMismatches())
                .containsExactly(
                    "zh-hans: LunaSettings.csv row kmu_palette differs from en in defaultValue: "
                        + "\"Or\" against \"Gold\"",
                    "zh-hans: LunaSettings.csv row kmu_palette differs from en in secondaryValue: "
                        + "\"Or, Argent\" against \"Gold, Silver\"");
        }

        @Test
        void aMovedBoundIsFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(localisation.resolve("zh-hans").resolve("LunaSettings.csv"), SETTINGS_HEADER_LINE + """
                kmu_caption,,,,Couleurs,,Header,Couleurs,,,,,,,,,Visuel
                kmu_palette,,,,Palette,,Radio,Gold,"Gold, Silver",,,Quelle palette,,,,,Visuel
                kmu_width,,,,Largeur,,Int,4,,,,Quelle largeur,,,1,9,Visuel
                kmu_note,,,,,,Text,Une note,,,,,,,,,Autre
                """);

            assertThat(createParity(localisation).findSettingsBehaviourMismatches())
                .containsExactly(
                    "zh-hans: LunaSettings.csv row kmu_width differs from en in maxValue: \"9\" against \"8\"");
        }

        @Test
        void aModMappingNoSettingsTableComparesNone(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot, MANIFEST_MAPPING_STRINGS_ONLY);

            // A settings file left in a bundle the manifest does not map is not the mod's settings table.
            writeFile(localisation.resolve("zh-hans").resolve("LunaSettings.csv"), SETTINGS_HEADER_LINE + """
                kmu_width,,,,Largeur,,Int,4,,,,Quelle largeur,,,1,9,Visuel
                """);

            assertThat(createParity(localisation).findSettingsBehaviourMismatches())
                .isEmpty();
        }
    }

    @Nested
    class FindSettingsRowMismatches {

        @Test
        void agreeingRowsPass(@TempDir Path modRoot) throws IOException {

            assertThat(createParity(writeAgreeingMod(modRoot)).findSettingsRowMismatches())
                .isEmpty();
        }

        @Test
        void aMissingAndAnAddedRowAreFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(localisation.resolve("zh-hans").resolve("LunaSettings.csv"), SETTINGS_HEADER_LINE + """
                kmu_caption,,,,Couleurs,,Header,Couleurs,,,,,,,,,Visuel
                kmu_palette,,,,Palette,,Radio,Gold,"Gold, Silver",,,Quelle palette,,,,,Visuel
                kmu_height,,,,Hauteur,,Int,4,,,,Quelle hauteur,,,1,8,Visuel
                kmu_note,,,,,,Text,Une note,,,,,,,,,Autre
                """);

            assertThat(createParity(localisation).findSettingsRowMismatches())
                .containsExactly(
                    "zh-hans: LunaSettings.csv lacks row kmu_width, which en declares",
                    "zh-hans: LunaSettings.csv declares row kmu_height, which en does not");
        }

        @Test
        void theFirstRowOutOfPlaceIsFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(localisation.resolve("zh-hans").resolve("LunaSettings.csv"), SETTINGS_HEADER_LINE + """
                kmu_caption,,,,Couleurs,,Header,Couleurs,,,,,,,,,Visuel
                kmu_width,,,,Largeur,,Int,4,,,,Quelle largeur,,,1,8,Visuel
                kmu_palette,,,,Palette,,Radio,Gold,"Gold, Silver",,,Quelle palette,,,,,Visuel
                kmu_note,,,,,,Text,Une note,,,,,,,,,Autre
                """);

            assertThat(createParity(localisation).findSettingsRowMismatches())
                .containsExactly("zh-hans: LunaSettings.csv places kmu_width at row 2, where en places kmu_palette");
        }

        @Test
        void aRowDeclaredTwiceIsFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(localisation.resolve("zh-hans").resolve("LunaSettings.csv"), SETTINGS_HEADER_LINE + """
                kmu_caption,,,,Couleurs,,Header,Couleurs,,,,,,,,,Visuel
                kmu_palette,,,,Palette,,Radio,Gold,"Gold, Silver",,,Quelle palette,,,,,Visuel
                kmu_width,,,,Largeur,,Int,4,,,,Quelle largeur,,,1,8,Visuel
                kmu_note,,,,,,Text,Une note,,,,,,,,,Autre
                kmu_note,,,,,,Text,Une note,,,,,,,,,Autre
                """);

            assertThat(createParity(localisation).findSettingsRowMismatches())
                .containsExactly("zh-hans: LunaSettings.csv declares 5 rows, where en declares 4");
        }
    }

    @Nested
    class FindSettingsTabMismatches {

        @Test
        void tabsRenamedTogetherPass(@TempDir Path modRoot) throws IOException {

            assertThat(createParity(writeAgreeingMod(modRoot)).findSettingsTabMismatches())
                .isEmpty();
        }

        @Test
        void aSplitTabIsFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(localisation.resolve("zh-hans").resolve("LunaSettings.csv"), SETTINGS_HEADER_LINE + """
                kmu_caption,,,,Couleurs,,Header,Couleurs,,,,,,,,,Visuel
                kmu_palette,,,,Palette,,Radio,Gold,"Gold, Silver",,,Quelle palette,,,,,Visuel
                kmu_width,,,,Largeur,,Int,4,,,,Quelle largeur,,,1,8,Visuels
                kmu_note,,,,,,Text,Une note,,,,,,,,,Autre
                """);

            assertThat(createParity(localisation).findSettingsTabMismatches())
                .containsExactly("zh-hans: LunaSettings.csv splits en tab Visuals across tabs [Visuel, Visuels]");
        }

        @Test
        void mergedTabsAreFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(localisation.resolve("zh-hans").resolve("LunaSettings.csv"), SETTINGS_HEADER_LINE + """
                kmu_caption,,,,Couleurs,,Header,Couleurs,,,,,,,,,Visuel
                kmu_palette,,,,Palette,,Radio,Gold,"Gold, Silver",,,Quelle palette,,,,,Visuel
                kmu_width,,,,Largeur,,Int,4,,,,Quelle largeur,,,1,8,Visuel
                kmu_note,,,,,,Text,Une note,,,,,,,,,Visuel
                """);

            assertThat(createParity(localisation).findSettingsTabMismatches())
                .containsExactly("zh-hans: LunaSettings.csv merges en tabs [General, Visuals] into tab Visuel");
        }
    }

    @Nested
    class FindStringsKeyMismatches {

        @Test
        void agreeingKeysPass(@TempDir Path modRoot) throws IOException {

            assertThat(createParity(writeAgreeingMod(modRoot)).findStringsKeyMismatches())
                .isEmpty();
        }

        @Test
        void aMissingAndAnAddedKeyAreFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(localisation.resolve("zh-hans").resolve("strings.json"), """
                { "kmu": { "greeting": "Bonjour %s, %d", "welcome": "Bienvenue" } }
                """);

            assertThat(createParity(localisation).findStringsKeyMismatches())
                .containsExactly(
                    "zh-hans: strings.json lacks string kmu > farewell, which en declares",
                    "zh-hans: strings.json declares string kmu > welcome, which en does not");
        }

        @Test
        void aKeyMovedToAnotherCategoryIsFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(localisation.resolve("zh-hans").resolve("strings.json"), """
                { "kmu": { "greeting": "Bonjour %s, %d" }, "other": { "farewell": "Au revoir" } }
                """);

            assertThat(createParity(localisation).findStringsKeyMismatches())
                .containsExactly(
                    "zh-hans: strings.json lacks string kmu > farewell, which en declares",
                    "zh-hans: strings.json declares string other > farewell, which en does not");
        }

        @Test
        void aDefaultHoldingNoStringsFileComparesNone(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            Files.delete(localisation.resolve("en").resolve("strings.json"));

            assertThat(createParity(localisation).findStringsKeyMismatches())
                .isEmpty();
        }
    }

    @Nested
    class FindUndeclaredBundleDirectories {

        @Test
        void declaredDirectoriesPass(@TempDir Path modRoot) throws IOException {

            assertThat(createParity(writeAgreeingMod(modRoot)).findUndeclaredBundleDirectories())
                .isEmpty();
        }

        @Test
        void aDirectoryTheManifestDoesNotDeclareIsFound(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            Files.createDirectory(localisation.resolve("fr"));

            assertThat(createParity(localisation).findUndeclaredBundleDirectories())
                .containsExactly("fr: a bundle directory the manifest does not declare");
        }
    }

    @Nested
    class ListModInfoFallbackFieldNames {

        @Test
        void eachLocaleListsTheFieldsItLeavesToTheBase(@TempDir Path modRoot) throws IOException {

            var localisation = writeAgreeingMod(modRoot);

            writeFile(modRoot.resolve("mod_info.base.json"), MOD_INFO_BASE);
            writeFile(localisation.resolve("zh-hans").resolve("mod_info.json"), """
                { "name": "Nom" }
                """);

            assertThat(createParity(localisation).listModInfoFallbackFieldNames())
                .containsExactly(
                    Map.entry("en", List.of("description", "name", "dependencies > kmlib")),
                    Map.entry("zh-hans", List.of("description", "dependencies > kmlib")));
        }

        @Test
        void aModCommittingNoBaseListsNothing(@TempDir Path modRoot) throws IOException {

            assertThat(createParity(writeAgreeingMod(modRoot)).listModInfoFallbackFieldNames())
                .isEmpty();
        }
    }
}
