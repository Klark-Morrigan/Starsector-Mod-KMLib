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

    /**
     * What {@link #findWholeWordIndex} answers where the text does not say the word at all. Named
     * rather than left as a bare negative index, so a caller reads the absence as one rather than
     * comparing against a literal whose meaning is only in this method's contract.
     */
    public static final int NO_WORD_MATCH = -1;

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
     * Where {@code text} first says {@code word} as a word of its own, ignoring case, or
     * {@link #NO_WORD_MATCH} where it never does.
     *
     * <p>A whole word rather than a bare containment: what parts one word from another is anything
     * that is not a letter or a digit, and the ends of the text itself. So a hyphen counts as plainly
     * as a space does, and a word that merely opens a longer one does not match at all - searching
     * <em>abandoned</em> finds nothing in <em>Abandonedium</em>.
     *
     * <p>Matched against the text as it is spelled rather than against a folded copy, so the position
     * answered indexes the string the caller holds. A copy folded to one case is free to come out a
     * different length, which would slide the answer along the text by however much it moved - and a
     * caller picking a stretch out by that position would then pick out the wrong characters.
     *
     * @param text the text to search; null or empty says the word nowhere
     * @param word the word to look for; null or empty is said nowhere
     * @return the position the word starts at, or {@link #NO_WORD_MATCH}
     */
    public static int findWholeWordIndex(String text, String word) {

        if (!hasText(text) || !hasText(word)) {
            return NO_WORD_MATCH;
        }
        for (var index = 0; index + word.length() <= text.length(); index++) {

            if (text.regionMatches(true, index, word, 0, word.length())
                    && isWholeWordAt(text, index, word.length())) {
                return index;
            }
        }
        return NO_WORD_MATCH;
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

    // Whether the stretch found at that position stands alone rather than opening or closing a
    // longer word.
    private static boolean isWholeWordAt(String text, int matchIndex, int wordLength) {
        return isWordBoundaryAt(text, matchIndex - 1)
            && isWordBoundaryAt(text, matchIndex + wordLength);
    }

    // Whether that position parts one word from another - anything that is not a letter or a digit,
    // and the ends of the text itself.
    private static boolean isWordBoundaryAt(String text, int index) {
        return index < 0
            || index >= text.length()
            || !Character.isLetterOrDigit(text.charAt(index));
    }
}
