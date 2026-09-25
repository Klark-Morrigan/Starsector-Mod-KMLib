package kmlib.testfixtures.starsector.json;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the read every shipped-JSON suite is built on against what the engine does with the same file.
 *
 * <p>The acceptances matter as much as the refusals. Vanilla's own strings file carries dozens of
 * comment lines, and a reading stricter than the game would fail a bundle the game loads - so the
 * leniency is pinned here, not left as whatever the parser happened to allow.
 */
final class ShippedJsonTest {

    // "Chinese" (U+4E2D U+6587): text far outside Latin-1, as a translated bundle carries it.
    private static final String CHINESE_TEXT = "中文";

    private static final String LOCATION = "file";

    private static Path writeJson(Path directory, String contents) throws IOException {

        var file = directory.resolve("shipped.json");

        Files.writeString(file, contents, StandardCharsets.UTF_8);

        return file;
    }

    @Nested
    class ReadObjectFile {

        @Test
        void membersComeBackSortedWithTheirJsonTypes(@TempDir Path directory) throws IOException {

            var file = writeJson(directory, """
                {
                  "second": { "inner": "text" },
                  "first": [1, true]
                }
                """);

            assertThat(ShippedJson.readObjectFile(file))
                .containsExactly(
                    Map.entry("first", List.of(1, true)),
                    Map.entry("second", Map.of("inner", "text")));
        }

        @Test
        void nonAsciiTextReadsIntact(@TempDir Path directory) throws IOException {

            var file = writeJson(directory, "{ \"name\": \"" + CHINESE_TEXT + "\" }");

            assertThat(ShippedJson.readObjectFile(file))
                .containsEntry("name", CHINESE_TEXT);
        }

        @Test
        void aHashCommentIsStrippedAsTheEngineStripsIt(@TempDir Path directory) throws IOException {

            // Vanilla's strings file is full of these; a reader refusing them refuses the game's own data.
            var file = writeJson(directory, """
                {
                  # a caption
                  "key": "text", # trailing
                }
                """);

            assertThat(ShippedJson.readObjectFile(file))
                .containsExactly(Map.entry("key", "text"));
        }

        @Test
        void aHashInsideQuotedWordingIsKept(@TempDir Path directory) throws IOException {

            var file = writeJson(directory, """
                { "key": "Rank #%d" }
                """);

            assertThat(ShippedJson.readObjectFile(file))
                .containsEntry("key", "Rank #%d");
        }

        @Test
        void theGamesLenientSyntaxParses(@TempDir Path directory) throws IOException {

            // A trailing comma, an unquoted key and a single-quoted string all load in the game.
            var file = writeJson(directory, """
                { unquoted: 'single', "key": "text", }
                """);

            assertThat(ShippedJson.readObjectFile(file))
                .containsExactly(
                    Map.entry("key", "text"),
                    Map.entry("unquoted", "single"));
        }

        @Test
        void aDuplicatedKeyIsRefusedAsTheGameRefusesIt(@TempDir Path directory) throws IOException {

            // The game's parser throws rather than keeping either value, and the mod fails to load.
            var file = writeJson(directory, """
                { "key": "first", "key": "second" }
                """);

            assertThatThrownBy(() -> ShippedJson.readObjectFile(file))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("does not parse as the game reads it")
                .hasMessageContaining("Duplicate key \"key\"");
        }

        @Test
        void aByteOrderMarkIsRefused(@TempDir Path directory) throws IOException {

            // The game refuses one with nothing better than "must begin with '{'", so it is named here.
            var file = directory.resolve("shipped.json");

            Files.write(file, new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '{', '}'});

            assertThatThrownBy(() -> ShippedJson.readObjectFile(file))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("byte-order mark");
        }

        @Test
        void invalidUtf8IsRefused(@TempDir Path directory) throws IOException {

            // A file saved in a legacy code page: 0xE9 is a Latin-1 e-acute and no valid UTF-8 start.
            var file = directory.resolve("shipped.json");

            Files.write(file, new byte[] {'{', '"', 'k', '"', ':', '"', (byte) 0xE9, '"', '}'});

            assertThatThrownBy(() -> ShippedJson.readObjectFile(file))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("is not valid UTF-8");
        }

        @Test
        void aNullIsRefusedNamingItsMember(@TempDir Path directory) throws IOException {

            var file = writeJson(directory, """
                { "outer": { "inner": null } }
                """);

            assertThatThrownBy(() -> ShippedJson.readObjectFile(file))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("shipped.json > outer > inner is null");
        }

        @Test
        void aTopLevelArrayIsRefused(@TempDir Path directory) throws IOException {

            var file = writeJson(directory, "[]");

            assertThatThrownBy(() -> ShippedJson.readObjectFile(file))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("must begin with '{'");
        }

        @Test
        void anUnreadableFileNamesItself(@TempDir Path directory) {

            var missing = directory.resolve("absent.json");

            assertThatThrownBy(() -> ShippedJson.readObjectFile(missing))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("absent.json");
        }
    }

    @Nested
    class StripComments {

        @Test
        void carriageReturnsAreDroppedAndNewlinesKept() {

            assertThat(ShippedJson.stripComments("{\r\n\"k\": \"v\" # note\r\n}"))
                .isEqualTo("{\n\"k\": \"v\" \n}");
        }

        @Test
        void aLineBreakClosesAnOpenQuote() {

            // The engine resets its quote tracking per line, so an unbalanced quote cannot shield a
            // comment on the next one.
            assertThat(ShippedJson.stripComments("\"open\n# gone\nkept"))
                .isEqualTo("\"open\n\nkept");
        }

        @Test
        void anEscapedQuoteTogglesTrackingAsTheEngineDoes() {

            // The engine counts every double quote, escaped or not, so the '#' after the escaped quote
            // below reads as outside quotes and the rest of the line is dropped. Pinned because the
            // point is to match the engine, not to parse JSON correctly.
            assertThat(ShippedJson.stripComments("\"a\\\"b # c\""))
                .isEqualTo("\"a\\\"b ");
        }
    }

    @Nested
    class RequireOnlyKeys {

        @Test
        void anUnknownKeyIsNamedBesideTheKnownOnes() {

            var object = Map.<String, Object>of("displayName", "English", "coreLocalization", "x");

            assertThatThrownBy(() -> ShippedJson.requireOnlyKeys(
                    object,
                    Set.of("displayName", "coreLocalisation"),
                    LOCATION))
                .isInstanceOf(AssertionError.class)
                .hasMessage(
                    "file carries unknown keys [coreLocalization]; known keys are [coreLocalisation, displayName]");
        }

        @Test
        void knownKeysPass() {

            var object = Map.<String, Object>of("displayName", "English");

            assertThatCode(() -> ShippedJson.requireOnlyKeys(
                    object,
                    Set.of("displayName", "coreLocalisation"),
                    LOCATION))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    class RequireString {

        @Test
        void anAbsentMemberIsReportedMissing() {

            assertThatThrownBy(() -> ShippedJson.requireString(null, LOCATION))
                .isInstanceOf(AssertionError.class)
                .hasMessage("file is missing");
        }

        @Test
        void aMemberOfAnotherTypeIsReportedAsSuch() {

            assertThatThrownBy(() -> ShippedJson.requireString(true, LOCATION))
                .isInstanceOf(AssertionError.class)
                .hasMessage("file must be a JSON string");
        }

        @Test
        void anEmptyStringIsAccepted() {

            assertThat(ShippedJson.requireString("", LOCATION))
                .isEmpty();
        }
    }

    @Nested
    class RequireList {

        @Test
        void anArrayComesBackInFileOrder() {

            assertThat(ShippedJson.requireList(List.of("b", "a"), LOCATION))
                .containsExactly("b", "a");
        }

        @Test
        void aMemberOfAnotherTypeIsReportedAsSuch() {

            assertThatThrownBy(() -> ShippedJson.requireList(Map.of(), LOCATION))
                .isInstanceOf(AssertionError.class)
                .hasMessage("file must be a JSON array");
        }
    }

    @Nested
    class RequireObject {

        @Test
        void aMemberOfAnotherTypeIsReportedAsSuch() {

            assertThatThrownBy(() -> ShippedJson.requireObject("text", LOCATION))
                .isInstanceOf(AssertionError.class)
                .hasMessage("file must be a JSON object");
        }
    }

    @Nested
    class ConstructValueAt {

        @Test
        void anAcceptedValueComesBackAsBuilt() {

            assertThat(ShippedJson.constructValueAt(LOCATION, () -> "built"))
                .isEqualTo("built");
        }

        @Test
        void aRefusalNamesTheLocationItWasReadFrom() {

            assertThatThrownBy(() -> ShippedJson.constructValueAt(LOCATION, () -> {
                    throw new IllegalArgumentException("the tag is uppercase");
                }))
                .isInstanceOf(AssertionError.class)
                .hasMessage("file: the tag is uppercase")
                .hasCauseInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class LocateMember {

        @Test
        void keysAreJoinedWithAMarkerRatherThanADot() {

            assertThat(ShippedJson.locateMember("manifest.json > files", "strings.json"))
                .isEqualTo("manifest.json > files > strings.json");
        }
    }
}
