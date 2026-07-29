package kmlib.starsector.strings;

import com.fs.starfarer.api.Global;

import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Objects;

/**
 * Defensive wrapper around Starsector's
 * {@link com.fs.starfarer.api.SettingsAPI#getString(String, String)}
 * for mods that want a uniform "fetch and forget" entry point with
 * loud failure on missing or malformed strings.
 *
 * <p>Three failure modes collapse to the same {@link #REDACTED}
 * sentinel so missing localisation shows up visibly during playtest
 * rather than rendering as an empty label:
 * <ul>
 *   <li>The settings call returns {@code null}, an empty string, or
 *       whitespace.</li>
 *   <li>The settings call throws (e.g. the category is not registered
 *       in any loaded mod's {@code data/strings/strings.json}).</li>
 *   <li>{@link #format(String, String, Object...)} sees an
 *       {@link IllegalFormatException} because the template and the
 *       args drifted apart.</li>
 * </ul>
 *
 * <p>The public API is static and takes {@code (category, key)} on
 * every call. Mods that want category-bound ergonomics keep a small
 * constants holder of their own (string ids + the category id) and
 * pass both through; that avoids leaking per-mod knowledge into KMLib
 * while still letting each call site reference typed key constants.
 */
public final class StarsectorStrings {

    /** Visible sentinel returned whenever a string lookup or format
     *  fails. Chosen to be loud on screen rather than to swallow
     *  silently - localisation bugs surface in playtest instead of
     *  shipping as blank text. */
    public static final String REDACTED = "[REDACTED]";

    private StarsectorStrings() {
    }

    /**
     * Returns the localised string registered under {@code (category,
     * key)} in {@code data/strings/strings.json}, or {@link #REDACTED}
     * when the settings call returns blank or throws.
     */
    public static String get(String category, String key) {
        return get(category, key, StarsectorStrings::fromSettings);
    }

    /**
     * Returns the localised string formatted against {@code args}
     * using {@link Locale#ROOT}, or {@link #REDACTED} when the lookup
     * or the format call fails. The root locale is forced so the
     * formatted text does not pick up the JVM default locale's
     * separators - Starsector strings are written assuming "1,234.5"
     * style numerics.
     */
    public static String format(String category, String key, Object... args) {
        return format(category, key, StarsectorStrings::fromSettings, args);
    }

    /**
     * Package-private overload taking an explicit {@link StringSource}.
     * The public {@link #get(String, String)} entry point pins the
     * source to the live {@code SettingsAPI}; callers that already
     * hold a resolver use this overload to avoid the extra hop.
     */
    static String get(String category, String key, StringSource source) {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(source, "source");

        try {
            String value = source.get(category, key);
            if (!hasText(value)) {
                return REDACTED;
            }
            return value;
        } catch (RuntimeException exception) {
            return REDACTED;
        }
    }

    static String format(String category, String key, StringSource source, Object... args) {
        String template = get(category, key, source);
        try {
            return String.format(Locale.ROOT, template, args);
        } catch (IllegalFormatException exception) {
            return REDACTED;
        }
    }

    private static String fromSettings(String category, String key) {
        return Global.getSettings().getString(category, key);
    }

    /** Resolver hook for the package-private overloads that take an
     *  explicit source. */
    @FunctionalInterface
    interface StringSource {
        String get(String category, String key);
    }

    /** Local copy of "text is non-null and contains non-whitespace" so
     *  KMLib does not have to depend on a consumer's util module just
     *  to share one predicate. */
    private static boolean hasText(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
