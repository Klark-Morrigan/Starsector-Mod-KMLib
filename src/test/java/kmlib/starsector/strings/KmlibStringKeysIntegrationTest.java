package kmlib.starsector.strings;

import kmlib.testfixtures.reflection.DeclaredConstants;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shipped strings file against the code that names its keys. Nothing else holds the two
 * together: a key a constant names but the file never declares resolves to {@code [REDACTED]} on
 * screen, and no suite sees it, because every suite hands its subject a resolver of its own rather
 * than reading the file.
 *
 * <p>The guard earns its place here more than it would over text the player reads constantly. Most
 * of what this file carries is drawn only when a binding to third-party code has already stopped
 * holding - a path that runs on almost no install, and never on the one a release was played on -
 * so a missing key survives every playtest and reaches a player at the one moment the message
 * mattered.
 *
 * <p>The file is walked by hand rather than parsed, no JSON reader being on the test classpath. It
 * is flat - one key and its wording per line, inside a single category object, no escapes - so the
 * line shape below reads it exactly, and a file that stopped being flat would fail the walk rather
 * than quietly matching less of it.
 */
final class KmlibStringKeysIntegrationTest {

    private static final Path STRINGS_JSON = Path.of("data", "strings", "strings.json");

    // One entry as the file writes it: a key, its wording, and the comma every line but the last
    // carries. The wording is captured whole, including any trailing punctuation of its own.
    private static final Pattern ENTRY_LINE =
        Pattern.compile("^\\s*\"([a-z_]+)\"\\s*:\\s*\"(.*)\"\\s*,?\\s*$");

    // The one constant on KmlibStringKeys that names the category rather than a string inside it.
    private static final String CATEGORY_CONSTANT = "CATEGORY";

    @Nested
    class ShippedStringIds {

        @Test
        void everyStringIdKmlibStringKeysNamesIsDeclaredInTheFile() {
            // A constant naming a key the file never declares reads as [REDACTED] wherever it is
            // drawn - visible to the player, invisible to every suite, since each hands its subject
            // a resolver rather than the shipped file.
            assertThat(readShippedStrings())
                .containsKeys(readStringIdsByConstantName()
                    .values()
                    .toArray(String[]::new));
        }

        @Test
        void everyStringIdDeclaredInTheFileIsNamedByKmlibStringKeys() {
            // The opposite drift, and the quieter one: a key nothing names is wording that ships,
            // is translated, and is never drawn - a rename that left the old row behind.
            assertThat(readStringIdsByConstantName().values())
                .containsExactlyInAnyOrderElementsOf(readShippedStrings().keySet());
        }
    }

    // The file's entries in the order it declares them.
    private static Map<String, String> readShippedStrings() {

        var stringsByKey = new LinkedHashMap<String, String>();

        for (var line : readFileLines()) {
            var entry = ENTRY_LINE.matcher(line);

            if (entry.matches()) {
                stringsByKey.put(entry.group(1), entry.group(2));
            }
        }
        return stringsByKey;
    }

    private static Iterable<String> readFileLines() {
        try {
            return Files.readAllLines(STRINGS_JSON, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read " + STRINGS_JSON, exception);
        }
    }

    // Every string ID KmlibStringKeys names, kept against the constant naming it so a failure says
    // which constant is at fault rather than only which key is missing. CATEGORY names the category
    // the keys sit in rather than one of them, so it is dropped before the file is asked about it.
    private static Map<String, String> readStringIdsByConstantName() {

        var idsByConstantName = new LinkedHashMap<>(
            DeclaredConstants.readConstantsByName(KmlibStringKeys.class, String.class));

        idsByConstantName.remove(CATEGORY_CONSTANT);

        return idsByConstantName;
    }
}
