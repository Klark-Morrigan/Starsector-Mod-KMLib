package kmlib.testfixtures.localisation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins what a fragment can reach of the base it is merged over: the text fields it would fall back to,
 * and the dependency IDs it may name.
 */
final class ModInfoBaseTest {

    private static Path writeBase(Path directory, String contents) throws IOException {

        var baseFile = directory.resolve("mod_info.base.json");

        Files.writeString(baseFile, contents, StandardCharsets.UTF_8);

        return baseFile;
    }

    @Nested
    class ReadBase {

        @Test
        void theTextFieldsAndDependencyIdsAreRead(@TempDir Path directory) throws IOException {

            var baseFile = writeBase(directory, """
                {
                  "id": "kmu",
                  "name": "Name",
                  "description": "Description",
                  "version": "1.0.0",
                  "dependencies": [
                    { "id": "lunalib", "name": "LunaLib" },
                    { "id": "kmlib", "name": "KMLib", "version": "0.4.0" }
                  ]
                }
                """);

            var base = ModInfoBase.readBase(baseFile);

            assertThat(base.translatableFieldNames())
                .containsExactly("description", "name");
            assertThat(base.dependencyIds())
                .containsExactly("kmlib", "lunalib");
        }

        @Test
        void aBaseDeclaringNoDependenciesLetsAFragmentNameNone(@TempDir Path directory) throws IOException {

            var baseFile = writeBase(directory, """
                { "name": "Name", "author": "Author" }
                """);

            var base = ModInfoBase.readBase(baseFile);

            assertThat(base.translatableFieldNames())
                .containsExactly("author", "name");
            assertThat(base.dependencyIds())
                .isEmpty();
        }

        @Test
        void aDependencyWithoutAnIdIsRefused(@TempDir Path directory) throws IOException {

            var baseFile = writeBase(directory, """
                { "dependencies": [ { "name": "KMLib" } ] }
                """);

            assertThatThrownBy(() -> ModInfoBase.readBase(baseFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("dependencies[0] > id is missing");
        }
    }
}
