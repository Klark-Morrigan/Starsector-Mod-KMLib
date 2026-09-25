package kmlib.testfixtures.localisation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins how one locale's directory is read: the strings file by category and key, the launcher fragment
 * through its own reading, and the bundle's files held inside its own directory.
 */
final class LocaleBundleTest {

    // "Chinese" (U+4E2D U+6587): text far outside Latin-1, as a translated bundle carries it.
    private static final String CHINESE_TEXT = "中文";

    private static final DeclaredLocale ENGLISH = DeclaredLocale.createLocaleWithoutCoreLocalisation("en", "English");

    private static LocaleBundle createBundleWith(Path directory, String fileName, String contents)
            throws IOException {

        Files.writeString(directory.resolve(fileName), contents, StandardCharsets.UTF_8);

        return new LocaleBundle(directory, ENGLISH);
    }

    @Nested
    class ReadStrings {

        @Test
        void theBundlesOwnStringsFileIsRead(@TempDir Path directory) throws IOException {

            var bundle = createBundleWith(directory, "strings.json", """
                { "kmu": { "only": "%s" } }
                """.formatted(CHINESE_TEXT));

            assertThat(bundle.readStrings())
                .containsExactly(Map.entry("kmu", Map.of("only", CHINESE_TEXT)));
        }
    }

    @Nested
    class ReadModInfoFragment {

        @Test
        void theBundlesOwnFragmentIsRead(@TempDir Path directory) throws IOException {

            var bundle = createBundleWith(directory, "mod_info.json", """
                { "description": "Description" }
                """);

            assertThat(bundle.readModInfoFragment().descriptionText())
                .contains("Description");
        }
    }

    @Nested
    class ResolveBundleFile {

        @Test
        void aBareNameResolvesInsideTheBundle(@TempDir Path directory) {

            var bundle = new LocaleBundle(directory, ENGLISH);

            assertThat(bundle.resolveBundleFile("LunaSettings.csv"))
                .isEqualTo(directory.resolve("LunaSettings.csv"));
        }

        @Test
        void aNameReachingIntoAnotherBundleIsRefused(@TempDir Path directory) {

            var bundle = new LocaleBundle(directory.resolve("en"), ENGLISH);

            assertThatThrownBy(() -> bundle.resolveBundleFile("../zh-hans/strings.json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not a bare file name");
        }
    }

    @Nested
    class GetLocale {

        @Test
        void theBundleNamesTheLocaleItWasOpenedFor(@TempDir Path directory) {

            assertThat(new LocaleBundle(directory, ENGLISH).getLocale())
                .isEqualTo(ENGLISH);
        }
    }
}
