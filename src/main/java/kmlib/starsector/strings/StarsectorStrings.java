package kmlib.starsector.strings;

import com.fs.starfarer.api.Global;

import kmlib.text.KmlibStrings;

import org.apache.log4j.Logger;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Defensive wrapper around Starsector's {@link com.fs.starfarer.api.SettingsAPI#getString(String, String)}:
 * a uniform "fetch and forget" entry point that fails loudly on a missing or malformed string.
 *
 * <p>Three failure modes collapse to the same {@link #REDACTED} sentinel, so missing localisation
 * shows up visibly during playtest rather than rendering as an empty label:
 * <ul>
 *   <li>The settings call returns {@code null}, an empty string, or whitespace.</li>
 *   <li>The settings call throws - the category is not registered in any loaded mod's
 *       {@code data/strings/strings.json}, say.</li>
 *   <li>{@link #format(String, String, Object...)} sees an {@link IllegalFormatException} because
 *       the template and the args drifted apart.</li>
 * </ul>
 *
 * <p>The public API is static and takes {@code (category, key)} on every call. A mod that wants
 * category-bound ergonomics keeps a small constants holder of its own - the string IDs beside the
 * category ID - and passes both through; that keeps per-mod knowledge out of this class while
 * still letting each call site name a typed key constant.
 */
public final class StarsectorStrings {

    /**
     * Visible sentinel returned whenever a string lookup or format fails. Loud on screen rather
     * than silently swallowed, so a localisation bug surfaces in playtest instead of shipping as
     * blank text.
     */
    public static final String REDACTED = "[REDACTED]";

    // Asked of log4j directly rather than of the game: the same logger under the same name, and one a
    // class this widely read cannot be handed null by a suite that first loads it under a stood-in game.
    private static final Logger LOG = Logger.getLogger(StarsectorStrings.class);

    // Where a mod's strings sit in its own folder, as the game loads them.
    private static final String STRINGS_PATH = "data/strings/strings.json";

    private StarsectorStrings() {
    }

    /**
     * The localised string registered under {@code (category, key)} in
     * {@code data/strings/strings.json}, or {@link #REDACTED} when the settings call returns blank
     * or throws.
     *
     * @param category the strings.json category the key sits in
     * @param key      the string ID inside that category
     * @return the wording, or the sentinel
     */
    public static String get(String category, String key) {
        return get(category, key, StarsectorStrings::fromSettings);
    }

    /**
     * The localised string formatted against {@code args} using {@link Locale#ROOT}, or
     * {@link #REDACTED} when the lookup or the format call fails.
     *
     * <p>The root locale is forced so the formatted text does not pick up the JVM default locale's
     * separators - Starsector strings are written assuming "1,234.5" style numerics.
     *
     * @param category the strings.json category the key sits in
     * @param key      the string ID of the template inside that category
     * @param args     what fills the template's slots, in order
     * @return the filled template, or the sentinel
     */
    public static String format(String category, String key, Object... args) {
        return format(category, key, StarsectorStrings::fromSettings, args);
    }

    /**
     * Every non-blank string one mod ships in one category of its {@code data/strings/strings.json},
     * read off that mod's own file as the game loads it - for a caller that has to know everything its
     * text may say, such as settling the face that text is drawn in.
     *
     * <p>Empty rather than failing where the file or the category cannot be read, and said in the log
     * on each such read, since a caller settling on what it read would otherwise act on nothing with
     * no trace of why.
     *
     * @param modId    the mod whose file is read
     * @param category the category read out of it
     * @return the category's strings, in the file's order
     */
    public static List<String> listCategoryStrings(String modId, String category) {

        try {
            var categoryObject = Global.getSettings().loadJSON(STRINGS_PATH, modId).getJSONObject(category);
            var strings = new ArrayList<String>();
            var keys = categoryObject.keys();

            while (keys.hasNext()) {

                var text = categoryObject.optString((String) keys.next());

                if (KmlibStrings.hasText(text)) {
                    strings.add(text);
                }
            }
            return strings;

        } catch (IOException | JSONException | RuntimeException readFailure) {

            LOG.warn("The strings of category '" + category + "' in " + modId + "'s " + STRINGS_PATH
                + " could not be read", readFailure);
            return List.of();
        }
    }

    /**
     * {@link #get(String, String)} against an explicit {@link StringSource}. The public entry point
     * pins the source to the live {@code SettingsAPI}; a caller that already holds a resolver uses
     * this one to avoid the extra hop.
     *
     * @param category the strings.json category the key sits in
     * @param key      the string ID inside that category
     * @param source   what answers the lookup
     * @return the wording, or the sentinel
     */
    static String get(String category, String key, StringSource source) {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(source, "source");

        try {
            var value = source.get(category, key);
            if (!KmlibStrings.hasText(value)) {
                return REDACTED;
            }
            return value;
        } catch (RuntimeException exception) {
            return REDACTED;
        }
    }

    /**
     * {@link #format(String, String, Object...)} against an explicit {@link StringSource}, on the
     * same grounds as the explicit-source {@code get}.
     *
     * @param category the strings.json category the key sits in
     * @param key      the string ID of the template inside that category
     * @param source   what answers the lookup
     * @param args     what fills the template's slots, in order
     * @return the filled template, or the sentinel
     */
    static String format(String category, String key, StringSource source, Object... args) {
        var template = get(category, key, source);
        try {
            return String.format(Locale.ROOT, template, args);
        } catch (IllegalFormatException exception) {
            return REDACTED;
        }
    }

    private static String fromSettings(String category, String key) {
        return Global.getSettings().getString(category, key);
    }

    /** What answers a {@code (category, key)} lookup, for the entry points that take one explicitly. */
    @FunctionalInterface
    interface StringSource {
        String get(String category, String key);
    }
}
