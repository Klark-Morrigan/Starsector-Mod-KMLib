package kmlib.testfixtures.starsector.strings;

import kmlib.testfixtures.reflection.DeclaredConstants;
import kmlib.testfixtures.starsector.json.ShippedJson;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static kmlib.testfixtures.starsector.json.ShippedJson.locateMember;
import static kmlib.testfixtures.starsector.json.ShippedJson.requireObject;
import static kmlib.testfixtures.starsector.json.ShippedJson.requireString;

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
 * mod ships one strings file and names its keys from one holder. The file is parsed through
 * {@link ShippedJson}, the engine's own reading, so a file the game loads reads here in full and a
 * file it refuses - a key declared twice, above all - fails here too.
 */
public final class ShippedStrings {

    /**
     * Where the engine reads a mod's strings from, which is not a convention but the only path it looks
     * at - so the no-argument readings take no path.
     */
    public static final Path STRINGS_JSON = Path.of("data", "strings", "strings.json");

    // The constant a holder names its category with rather than one of the strings inside it. Every
    // KM holder spells it this way, and a holder that did not would simply have it counted among its
    // IDs - reported as a key the file never declares, which names the holder to fix.
    private static final String CATEGORY_CONSTANT = "CATEGORY";

    private ShippedStrings() {
    }

    /**
     * Any strings file's wording, by category and then by key, both sorted. The engine merges the file
     * category by category and key by key, so neither order carries meaning.
     *
     * @param stringsFile the strings file to read
     * @return key to wording, per category
     */
    public static Map<String, Map<String, String>> readStringsByCategory(Path stringsFile) {

        var location = stringsFile.toString();
        var stringsByKeyByCategory = new TreeMap<String, Map<String, String>>();

        ShippedJson.readObjectFile(stringsFile).forEach((category, categoryValue) -> {

            var categoryLocation = locateMember(location, category);
            var stringsByKey = new TreeMap<String, String>();

            requireObject(categoryValue, categoryLocation).forEach((key, wording) ->
                stringsByKey.put(key, requireString(wording, locateMember(categoryLocation, key))));

            stringsByKeyByCategory.put(category, Collections.unmodifiableMap(stringsByKey));
        });
        return Collections.unmodifiableMap(stringsByKeyByCategory);
    }

    /**
     * The shipped file's entries, every category's keys together, sorted.
     *
     * <p>Read relative to the working directory, which is the mod's own repo root under Gradle - so
     * a suite calling this reads the file its build ships, not another mod's.
     *
     * @return key to wording, empty where the file declares nothing
     */
    public static Map<String, String> readStringsByKey() {
        return readStringsByKey(STRINGS_JSON);
    }

    /**
     * Any strings file's entries, every category's keys together, sorted.
     *
     * @param stringsFile the strings file to read
     * @return key to wording, empty where the file declares nothing
     */
    public static Map<String, String> readStringsByKey(Path stringsFile) {

        var stringsByKey = new TreeMap<String, String>();

        readStringsByCategory(stringsFile).forEach((category, categoryStrings) ->
            categoryStrings.forEach((key, wording) -> {

                // Held apart by category in the file but flattened here, so one key in two
                // categories would leave the guard comparing against whichever came last.
                if (stringsByKey.put(key, wording) != null) {

                    throw new AssertionError(
                        stringsFile + " declares \"" + key + "\" in more than one category");
                }
            }));
        return Collections.unmodifiableMap(stringsByKey);
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
}
