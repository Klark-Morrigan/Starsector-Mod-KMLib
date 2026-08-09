package kmlib.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic, Starsector-agnostic reads over strings shared across the
 * KMLib jar. Lives in its own package (not {@code kmlib.starsector.*})
 * because the helpers here are pure text utilities - they have no
 * dependency on Starsector's API surface and are reused by any KMLib
 * code, Starsector-related or not.
 *
 * <p>Sibling concept to
 * {@link kmlib.starsector.strings.StarsectorStrings}, which is the
 * defensive wrapper around Starsector's {@code settings.json}
 * localisation lookups; this class is purely string-manipulation.
 */
public final class KmlibStrings {

    private KmlibStrings() {
    }

    /**
     * Returns {@code true} when {@code value} is non-null and contains
     * at least one non-whitespace character. Lives here so KMLib does
     * not depend on a util module (Apache Commons, Guava, Spring) just
     * for one predicate; multiple internal callers ({@code
     * StarsectorStrings}, {@code StarsectorPlayerFactionResolver},
     * future consumers) share one implementation.
     */
    public static boolean hasText(String value) {
        if (value == null) {
            return false;
        }
        for (var i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The whitespace-parted words of {@code text}, in the order they are
     * written and with empties dropped, so leading, trailing or doubled
     * spacing changes nothing about the answer.
     *
     * <p>Empties are dropped rather than reported because a caller counting
     * or walking words means the words: a count shifted by how the string
     * happened to be spaced is a count of something else. A caller that
     * genuinely needs to know a string was blank tests it with
     * {@link #hasText} first, which states that question directly.
     *
     * @param text the string to part; null or blank yields an empty list
     * @return the words, never null
     */
    public static List<String> splitIntoWords(String text) {
        var words = new ArrayList<String>();
        if (!hasText(text)) {
            return words;
        }
        for (var word : text.trim().split("\\s+")) {
            words.add(word);
        }
        return words;
    }
}
