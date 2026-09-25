package kmlib.testfixtures.starsector.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * The JSON files a mod ships, read the way the engine reads them, plus the shape checks a reading of
 * one states its fields through.
 *
 * <p>The engine strips {@code #} comments line by line and hands the rest to the org.json in the
 * game's {@code json.jar}, which is lenient: trailing commas, unquoted keys and single-quoted strings
 * all parse, and vanilla's own files lean on the comments. A stricter reader would fail files the game
 * loads; a line walk would pass files it refuses. So the parse here is the game's own parser behind a
 * copy of the engine's comment strip, which cannot be called directly - it lives in an obfuscated class
 * the test JVM will not load.
 *
 * <p>Refused as the game refuses them: a duplicated key, which org.json reports rather than keeping
 * either value, and a byte-order mark, which the game meets as the document's first character and
 * reports only as "must begin with '{'". Refused beyond the game: text that is not valid UTF-8, which
 * the game would draw as replacement characters, and a JSON null, which no shipped file has a use for.
 *
 * <p>Objects come back as maps sorted by key. The game's parser keeps no member order, so nothing the
 * game reads can depend on one, and a sorted map makes every reading and every failure deterministic.
 * No org.json type crosses the boundary, so a consumer reads its files without {@code json.jar} on its
 * own test compile classpath.
 */
public final class ShippedJson {

    // UTF-8's byte-order mark, which the game's parser reads as the document's first character.
    private static final byte[] UTF8_BYTE_ORDER_MARK = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    // What starts a comment in a shipped JSON file, as the engine's loader strips it.
    private static final char COMMENT_MARKER = '#';

    // Joins keys in a failure's location. Not a dot, because keys carry dots - a strings key rarely,
    // a file name always.
    private static final String MEMBER_SEPARATOR = " > ";

    private ShippedJson() {
        // fixture of static readers, no instances.
    }

    /**
     * Builds a value read out of a file, naming the file when the value refuses what was read. A value
     * type states what is wrong with its arguments; only the reading knows which file they came from,
     * and the file is what a reader has to open to fix it.
     *
     * @param location          the file, or the member of it, the arguments were read from
     * @param valueConstructor  builds the value, throwing {@link IllegalArgumentException} on a refusal
     * @param <T>               the value's type
     * @return the value
     */
    public static <T> T constructValueAt(String location, Supplier<T> valueConstructor) {
        try {
            return valueConstructor.get();

        } catch (IllegalArgumentException illegalArgumentException) {

            throw new AssertionError(
                location + ": " + illegalArgumentException.getMessage(),
                illegalArgumentException);
        }
    }

    /**
     * Names one element of an array for a failure message.
     *
     * @param location the file or member holding the array
     * @param index    the element's position
     * @return the element's location
     */
    public static String locateElement(String location, int index) {
        return location + "[" + index + "]";
    }

    /**
     * Names one member of an object for a failure message.
     *
     * @param location the file or member holding it
     * @param key      the member's key
     * @return the member's location
     */
    public static String locateMember(String location, String key) {
        return location + MEMBER_SEPARATOR + key;
    }

    /**
     * Reads a file holding one JSON object, as the engine reads it.
     *
     * @param file the file to read
     * @return its members by key, sorted
     */
    public static Map<String, Object> readObjectFile(Path file) {

        var location = file.toString();

        try {
            return convertObject(new JSONObject(stripComments(readFileText(file))), location);

        } catch (JSONException jsonException) {

            // The engine's own message, with the file it would have named in the log.
            throw new AssertionError(
                location + " does not parse as the game reads it: " + jsonException.getMessage(),
                jsonException);
        }
    }

    /**
     * Narrows a member to a JSON array.
     *
     * @param value    the member, null where the key is absent
     * @param location the file and member path a failure names
     * @return the array's elements, in file order
     */
    @SuppressWarnings("unchecked")
    public static List<Object> requireList(Object value, String location) {

        requirePresent(value, location);

        if (!(value instanceof List)) {
            throw new AssertionError(location + " must be a JSON array");
        }
        return (List<Object>) value;
    }

    /**
     * Narrows a member to a JSON object.
     *
     * @param value    the member, null where the key is absent
     * @param location the file and member path a failure names
     * @return the object's members
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> requireObject(Object value, String location) {

        requirePresent(value, location);

        if (!(value instanceof Map)) {
            throw new AssertionError(location + " must be a JSON object");
        }
        return (Map<String, Object>) value;
    }

    /**
     * Fails an object carrying a key its reading has no field for. A misspelt key would otherwise read
     * as an absent one, and every field that may be absent would silently take its fallback.
     *
     * @param object      the object to check
     * @param allowedKeys every key its reading knows
     * @param location    the file and member path a failure names
     */
    public static void requireOnlyKeys(Map<String, Object> object, Set<String> allowedKeys, String location) {

        var unknownKeys = new TreeSet<>(object.keySet());

        unknownKeys.removeAll(allowedKeys);

        if (!unknownKeys.isEmpty()) {

            throw new AssertionError(
                location
                    + " carries unknown keys " + unknownKeys
                    + "; known keys are " + new TreeSet<>(allowedKeys));
        }
    }

    /**
     * Narrows a member to a string, empty included.
     *
     * @param value    the member, null where the key is absent
     * @param location the file and member path a failure names
     * @return the string
     */
    public static String requireString(Object value, String location) {

        requirePresent(value, location);

        if (!(value instanceof String)) {
            throw new AssertionError(location + " must be a JSON string");
        }
        return (String) value;
    }

    /**
     * The engine's comment strip, character for character: a {@code #} outside double quotes drops the
     * rest of its line, and a line break ends both the comment and any open quote. Carriage returns are
     * dropped and newlines kept, as the engine does. Its quote tracking toggles on every double quote,
     * escaped ones included, and so does this - a {@code #} after an escaped quote is treated exactly
     * as the game treats it.
     *
     * @param text the file's text
     * @return what the engine hands its parser
     */
    static String stripComments(String text) {

        var stripped = new StringBuilder(text.length());
        var isInComment = false;
        var isInQuote = false;

        for (var index = 0; index < text.length(); index++) {

            var character = text.charAt(index);

            if (character == '"') {

                isInQuote = !isInQuote;
            }
            if (character == '\n' || character == '\r') {

                isInComment = false;
                isInQuote = false;

                if (character == '\n') {
                    stripped.append('\n');
                }
            } else if (character == COMMENT_MARKER && !isInQuote) {

                isInComment = true;

            } else if (!isInComment) {

                stripped.append(character);
            }
        }
        return stripped.toString();
    }

    private static List<Object> convertArray(JSONArray array, String location) throws JSONException {

        var elements = new ArrayList<>();

        for (var index = 0; index < array.length(); index++) {
            elements.add(convertValue(array.get(index), locateElement(location, index)));
        }
        return Collections.unmodifiableList(elements);
    }

    private static Map<String, Object> convertObject(JSONObject object, String location) throws JSONException {

        var members = new TreeMap<String, Object>();

        for (Iterator<?> keys = object.keys(); keys.hasNext(); ) {

            var key = (String) keys.next();

            members.put(key, convertValue(object.get(key), locateMember(location, key)));
        }
        return Collections.unmodifiableMap(members);
    }

    // Everything org.json hands back, as the plain Java values a caller can hold without naming it.
    private static Object convertValue(Object value, String location) throws JSONException {

        if (value instanceof JSONObject) {
            return convertObject((JSONObject) value, location);
        }
        if (value instanceof JSONArray) {
            return convertArray((JSONArray) value, location);
        }
        if (JSONObject.NULL.equals(value)) {
            throw new AssertionError(location + " is null, which no shipped file uses");
        }
        return value;
    }

    private static String readFileText(Path file) {

        try {
            var bytes = Files.readAllBytes(file);

            if (startsWithByteOrderMark(bytes)) {
                throw new AssertionError(file + " starts with a UTF-8 byte-order mark, which the game refuses");
            }

            // Decoded strictly, so a file saved in a legacy code page fails here rather than reading as
            // replacement characters.
            return StandardCharsets.UTF_8
                .newDecoder()
                .decode(ByteBuffer.wrap(bytes))
                .toString();

        } catch (CharacterCodingException characterCodingException) {

            throw new AssertionError(
                file + " is not valid UTF-8",
                characterCodingException);

        } catch (IOException ioException) {

            // Surfaced rather than swallowed: the file is shipped data, so a read failure means the
            // reading is looking in the wrong place.
            throw new UncheckedIOException(
                "Could not read " + file.toAbsolutePath(),
                ioException);
        }
    }

    // Absent and wrongly typed are told apart, since the fix for each is a different edit.
    private static void requirePresent(Object value, String location) {

        if (value == null) {
            throw new AssertionError(location + " is missing");
        }
    }

    private static boolean startsWithByteOrderMark(byte[] bytes) {

        if (bytes.length < UTF8_BYTE_ORDER_MARK.length) {
            return false;
        }
        for (var index = 0; index < UTF8_BYTE_ORDER_MARK.length; index++) {
            if (bytes[index] != UTF8_BYTE_ORDER_MARK[index]) {
                return false;
            }
        }
        return true;
    }
}
