package kmlib.testfixtures.starsector.strings;

import kmlib.testfixtures.reflection.DeclaredConstants;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reads the two halves a localisation guard holds together: the wording a mod ships, and the string
 * IDs the holder class names it by.
 *
 * <p>Nothing else joins them. A key a constant names but the file never declares resolves to
 * {@code [REDACTED]} on screen, and no suite sees it, because each hands its subject a resolver of
 * its own rather than reading the shipped file. The opposite drift is quieter still: a key nothing
 * names is wording that ships, is translated, and is never drawn.
 *
 * <p>Here rather than in each mod's suite because the guard is the same guard every time - every KM
 * mod ships one strings file and names its keys from one holder - and the walk below is the part
 * that can be subtly wrong. A regex that stopped matching some entries would leave both directions
 * passing over less of the file than they claim, in whichever repo was not updated.
 *
 * <p>The file is walked by hand rather than parsed, no JSON reader being on the test classpath. It
 * is flat - one key and its wording per line, inside a single category object, no escapes - so the
 * line shape below reads it exactly, and a file that stopped being flat would fail the walk rather
 * than quietly matching less of it.
 */
public final class ShippedStrings {

    // Where the engine reads a mod's localisation from, which is not a convention but the only path
    // it looks at. A caller cannot usefully name another, so this is stated once rather than taken
    // as an argument.
    private static final Path STRINGS_JSON = Path.of("data", "strings", "strings.json");

    // One entry as the file writes it: a key, its wording, and the comma every line but the last
    // carries. The wording is captured whole, including any trailing punctuation of its own.
    private static final Pattern ENTRY_LINE =
        Pattern.compile("^\\s*\"([a-z_]+)\"\\s*:\\s*\"(.*)\"\\s*,?\\s*$");

    // The constant a holder names its category with rather than one of the strings inside it. Every
    // KM holder spells it this way, and a holder that did not would simply have it counted among its
    // IDs - reported as a key the file never declares, which names the holder to fix.
    private static final String CATEGORY_CONSTANT = "CATEGORY";

    private ShippedStrings() {
    }

    /**
     * The shipped file's entries, in the order it declares them.
     *
     * <p>Read relative to the working directory, which is the mod's own repo root under Gradle - so
     * a suite calling this reads the file its build ships, not another mod's.
     *
     * @return key to wording, empty where the file declares nothing
     */
    public static Map<String, String> readStringsByKey() {

        var stringsByKey = new LinkedHashMap<String, String>();

        for (var line : readFileLines()) {
            var entry = ENTRY_LINE.matcher(line);

            if (entry.matches()) {
                stringsByKey.put(entry.group(1), entry.group(2));
            }
        }
        return stringsByKey;
    }

    /**
     * Every string ID a holder names, kept against the constant naming it so a failure says which
     * constant is at fault rather than only which key is missing.
     *
     * <p>The category constant names the category the keys sit in rather than one of them, so it is
     * dropped before the file is asked about it.
     *
     * @param holder the class declaring the mod's string IDs
     * @return constant name to string ID, in declaration order
     */
    public static Map<String, String> readStringIdsByConstantName(Class<?> holder) {

        var idsByConstantName = new LinkedHashMap<>(
            DeclaredConstants.readConstantsByName(holder, String.class));

        idsByConstantName.remove(CATEGORY_CONSTANT);

        return idsByConstantName;
    }

    private static Iterable<String> readFileLines() {
        try {
            return Files.readAllLines(STRINGS_JSON, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read " + STRINGS_JSON, exception);
        }
    }
}
