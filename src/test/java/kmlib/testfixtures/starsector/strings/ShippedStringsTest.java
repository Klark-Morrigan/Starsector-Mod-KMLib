package kmlib.testfixtures.starsector.strings;

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
 * Pins the strings file's shape - categories of keyed wording - as every guard and bundle reading
 * takes it. The syntax the file may use is {@code ShippedJson}'s and pinned there.
 */
final class ShippedStringsTest {

    private static Path writeStrings(Path directory, String contents) throws IOException {

        var stringsFile = directory.resolve("strings.json");

        Files.writeString(stringsFile, contents, StandardCharsets.UTF_8);

        return stringsFile;
    }

    @Nested
    class ReadStringsByCategory {

        @Test
        void categoriesAndKeysReadSorted(@TempDir Path directory) throws IOException {

            var stringsFile = writeStrings(directory, """
                {
                  "kmu": { "second": "Two %d", "first": "" },
                  "kmlib": { "only": "One" }
                }
                """);

            assertThat(ShippedStrings.readStringsByCategory(stringsFile))
                .containsExactly(
                    Map.entry("kmlib", Map.of("only", "One")),
                    Map.entry("kmu", Map.of("first", "", "second", "Two %d")));

            assertThat(ShippedStrings.readStringsByCategory(stringsFile).get("kmu").keySet())
                .containsExactly("first", "second");
        }

        @Test
        void aKeyOutsideLowercaseAndUnderscoresIsRead(@TempDir Path directory) throws IOException {

            // A key with digits or capitals is as much a key to the game as any other. A reading that
            // matched lowercase-and-underscore keys only would drop it from both directions of the
            // guard without a word.
            var stringsFile = writeStrings(directory, """
                { "kmu": { "tier2Label": "Tier 2" } }
                """);

            assertThat(ShippedStrings.readStringsByCategory(stringsFile).get("kmu"))
                .containsExactly(Map.entry("tier2Label", "Tier 2"));
        }

        @Test
        void wordingThatIsNotTextIsRefusedNamingItsKey(@TempDir Path directory) throws IOException {

            var stringsFile = writeStrings(directory, """
                { "kmu": { "count": 3 } }
                """);

            assertThatThrownBy(() -> ShippedStrings.readStringsByCategory(stringsFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("strings.json > kmu > count must be a JSON string");
        }

        @Test
        void aCategoryThatIsNotAnObjectIsRefused(@TempDir Path directory) throws IOException {

            var stringsFile = writeStrings(directory, """
                { "kmu": "text" }
                """);

            assertThatThrownBy(() -> ShippedStrings.readStringsByCategory(stringsFile))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("strings.json > kmu must be a JSON object");
        }
    }
}
