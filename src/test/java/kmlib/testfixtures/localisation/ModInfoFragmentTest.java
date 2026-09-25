package kmlib.testfixtures.localisation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins what a locale's launcher fragment may carry. The refusal of a functional field is the case that
 * matters: a fragment varying the version or the jar list would build a different mod per locale.
 */
final class ModInfoFragmentTest {

    private static Path writeFragment(Path directory, String contents) throws IOException {

        var fragmentFile = directory.resolve("mod_info.json");

        Files.writeString(fragmentFile, contents, StandardCharsets.UTF_8);

        return fragmentFile;
    }

    @Nested
    class CreateUntranslatedFragment {

        @Test
        void noLauncherFieldIsTranslated() {

            var fragment = ModInfoFragment.createUntranslatedFragment();

            assertThat(fragment.nameText())
                .isEmpty();
            assertThat(fragment.descriptionText())
                .isEmpty();
            assertThat(fragment.authorText())
                .isEmpty();
            assertThat(fragment.dependencyNamesById())
                .isEmpty();
        }
    }

    @Nested
    class ReadFragment {

        @Test
        void everyTranslatableFieldReads(@TempDir Path directory) throws IOException {

            var fragmentFile = writeFragment(directory, """
                {
                  "name": "Name",
                  "description": "Description",
                  "author": "Author",
                  "dependencies": { "kmlib": "Library", "lunalib": "Settings" }
                }
                """);

            var fragment = ModInfoFragment.readFragment(fragmentFile);

            assertThat(fragment.nameText())
                .contains("Name");
            assertThat(fragment.descriptionText())
                .contains("Description");
            assertThat(fragment.authorText())
                .contains("Author");
            assertThat(fragment.dependencyNamesById())
                .containsExactly(Map.entry("kmlib", "Library"), Map.entry("lunalib", "Settings"));
        }

        @Test
        void aFieldLeftOutFallsBackRatherThanFailing(@TempDir Path directory) throws IOException {

            var fragmentFile = writeFragment(directory, """
                { "name": "Name" }
                """);

            var fragment = ModInfoFragment.readFragment(fragmentFile);

            assertThat(fragment.nameText())
                .contains("Name");
            assertThat(fragment.descriptionText())
                .isEmpty();
            assertThat(fragment.authorText())
                .isEmpty();
            assertThat(fragment.dependencyNamesById())
                .isEmpty();
        }

        @Test
        void anAbsentFileTranslatesNothing(@TempDir Path directory) {

            assertThat(ModInfoFragment.readFragment(directory.resolve("mod_info.json")))
                .isEqualTo(ModInfoFragment.createUntranslatedFragment());
        }

        @Test
        void aFunctionalFieldIsRefused(@TempDir Path directory) throws IOException {

            var fragmentFile = writeFragment(directory, """
                { "name": "Name", "version": "1.0.0", "jars": ["jars/Other.jar"] }
                """);

            assertThatThrownBy(() -> ModInfoFragment.readFragment(fragmentFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("unknown keys [jars, version]");
        }

        @Test
        void dependenciesAsTheBaseFileListsThemAreRefused(@TempDir Path directory) throws IOException {

            // The base file's array shape restates versions a locale has no reason to touch.
            var fragmentFile = writeFragment(directory, """
                { "dependencies": [ { "id": "kmlib", "name": "Library" } ] }
                """);

            assertThatThrownBy(() -> ModInfoFragment.readFragment(fragmentFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("mod_info.json > dependencies must be a JSON object");
        }

        @ParameterizedTest
        @ValueSource(strings = {"name", "description", "author"})
        void blankLauncherTextIsRefused(String fieldName, @TempDir Path directory) throws IOException {

            // The launcher would draw an empty row rather than fall back to the base file's text.
            var fragmentFile = writeFragment(directory, """
                { "%s": "  " }
                """.formatted(fieldName));

            assertThatThrownBy(() -> ModInfoFragment.readFragment(fragmentFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(fieldName + " is blank");
        }

        @Test
        void aBlankDependencyNameIsRefused(@TempDir Path directory) throws IOException {

            var fragmentFile = writeFragment(directory, """
                { "dependencies": { "kmlib": "" } }
                """);

            assertThatThrownBy(() -> ModInfoFragment.readFragment(fragmentFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("dependencies.kmlib is blank");
        }
    }
}
