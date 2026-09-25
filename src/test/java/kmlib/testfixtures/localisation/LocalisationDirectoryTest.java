package kmlib.testfixtures.localisation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the layout of a mod's {@code localisation/} directory: the manifest at its root, bundles beside it.
 */
final class LocalisationDirectoryTest {

    @Nested
    class ListBundleDirectoryNames {

        @Test
        void everyDirectoryIsListedSortedAndTheManifestIsNot(@TempDir Path directory) throws IOException {

            // An undeclared directory is listed too: a bundle nobody declared is what a check over the
            // manifest and the directory has to find.
            Files.createDirectory(directory.resolve("zh-hans"));
            Files.createDirectory(directory.resolve("en"));
            Files.createDirectory(directory.resolve("undeclared"));
            Files.writeString(directory.resolve("manifest.json"), "{}", StandardCharsets.UTF_8);

            assertThat(new LocalisationDirectory(directory).listBundleDirectoryNames())
                .containsExactly("en", "undeclared", "zh-hans");
        }

        @Test
        void anAbsentDirectoryNamesItself(@TempDir Path directory) {

            var localisationDirectory = new LocalisationDirectory(directory.resolve("localisation"));

            assertThatThrownBy(localisationDirectory::listBundleDirectoryNames)
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("localisation");
        }
    }

    @Nested
    class OpenBundle {

        @Test
        void aDeclaredLocaleOpensTheDirectoryNamedByItsTag(@TempDir Path directory) {

            var locale = DeclaredLocale.createLocaleWithoutCoreLocalisation("zh-hans", "Name");
            var bundle = new LocalisationDirectory(directory).openBundle(locale);

            assertThat(bundle.resolveBundleFile("strings.json"))
                .isEqualTo(directory.resolve("zh-hans").resolve("strings.json"));
        }
    }

    @Nested
    class ReadManifest {

        @Test
        void theManifestIsReadFromTheDirectoryRoot(@TempDir Path directory) throws IOException {

            Files.writeString(directory.resolve("manifest.json"), """
                {
                  "defaultLocale": "en",
                  "files": { "strings.json": "data/strings/strings.json" },
                  "locales": { "en": { "displayName": "English" } }
                }
                """, StandardCharsets.UTF_8);

            assertThat(new LocalisationDirectory(directory).readManifest().defaultLocaleTag())
                .isEqualTo("en");
        }
    }
}
