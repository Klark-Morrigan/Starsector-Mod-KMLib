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

    /**
     * {@code text} less any word past its first {@code protectedWordCount}
     * that merely repeats, ignoring case, the word kept before it - the
     * stutter left where one phrase was appended to another already ending
     * on the word the second opens with.
     *
     * <p>The protected opening is the whole of the safety, and the reason
     * this is stated as a count rather than assumed: a repetition inside a
     * subject's own name is that name, and dropping it answers a name
     * nobody has. A caller therefore protects the words it holds
     * responsible for and exposes only what was appended to them; passing
     * zero protects nothing and puts every word of the subject in reach,
     * which is a scan almost nothing wants.
     *
     * <p>Text that lost nothing is answered as the very string given rather
     * than rejoined, so a caller can hand over a string it must not see
     * respaced and get it back untouched wherever there was no stutter to
     * drop. Where a word does go, what is left is rejoined on one space -
     * a string being repaired rather than preserved at that point.
     *
     * @param text               the text to weigh; null or blank is answered
     *                           as given, there being no words to weigh
     * @param protectedWordCount how many opening words are the subject's own
     *                           and so untouchable; zero or less protects
     *                           none of them
     * @return the text with the repeats dropped, or the very string given
     *         where none were
     */
    public static String dropAdjacentRepeatedWords(String text, int protectedWordCount) {

        var words = splitIntoWords(text);
        var keptWords = new ArrayList<String>();

        for (var index = 0; index < words.size(); index++) {
            
            var word = words.get(index);
            var isRepeat = index >= protectedWordCount
                && !keptWords.isEmpty()
                && word.equalsIgnoreCase(keptWords.get(keptWords.size() - 1));

            if (!isRepeat) {
                keptWords.add(word);
            }
        }
        if (keptWords.size() == words.size()) {
            return text;
        }
        return String.join(" ", keptWords);
    }
}
