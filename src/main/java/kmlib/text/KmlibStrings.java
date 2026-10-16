package kmlib.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Pure text utilities shared across the KMLib jar. They read no Starsector API, so they live outside
 * {@code kmlib.starsector.*} and serve any KMLib code.
 *
 * <p>Every read here parts text from whitespace by the one rule {@link #hasText} uses:
 * {@link Character#isWhitespace}. A second rule would let two reads disagree about the same value.
 */
public final class KmlibStrings {

    // What closes each initial of an abbreviated name, so the short form reads as one - a run of
    // bare capitals is a word in its own right rather than a name stood in for.
    private static final char INITIAL_TERMINATOR = '.';

    private static final String PARENTHETICAL_CLOSER = ")";

    private static final String PARENTHETICAL_OPENER = " (";

    // The \p{javaWhitespace} class is Character.isWhitespace, the rule hasText reads. The plain \s class
    // is ASCII only, so it would not part words on a CJK ideographic space (U+3000).
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\p{javaWhitespace}+");

    private static final String WORD_SEPARATOR = " ";

    /**
     * What {@link #findWholeWordIndex} answers where the text does not say the word at all. Named
     * rather than left as a bare negative index, so a caller reads the absence as one rather than
     * comparing against a literal whose meaning is only in this method's contract.
     */
    public static final int NO_WORD_MATCH = -1;

    private KmlibStrings() {
    }

    /**
     * Returns {@code true} when {@code value} is non-null and contains at least one non-whitespace
     * character. Lives here so KMLib does not depend on a util module (Apache Commons, Guava, Spring)
     * for one predicate.
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
     * {@code value} itself where it {@link #hasText has text}, else an {@link IllegalArgumentException}
     * carrying {@code message}.
     *
     * <p>The blank-rejecting counterpart of {@link java.util.Objects#requireNonNull}, for a value
     * type whose components are sentences or names: an empty one passes a null check and renders as
     * a hole in the text, which is the failure the check was there to stop.
     *
     * @param value   the string a caller requires text in
     * @param message what a blank costs, for the exception
     * @return {@code value}, so the check reads inline in a constructor
     */
    public static String requireText(String value, String message) {

        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * {@code value} with its surrounding whitespace stripped, or {@code null} where nothing is left.
     *
     * <p>The game and the mods around it report a missing name three ways - null, empty, and a cell
     * of spaces - and none of them means anything different to a caller. This answers all three as
     * the one absence, so two sites cannot disagree about the same value.
     *
     * @param value the text to normalise; may be null
     * @return the stripped text, or {@code null} where it is blank
     */
    public static String normaliseText(String value) {

        if (!hasText(value)) {
            return null;
        }
        return value.strip();
    }

    /**
     * {@link #normaliseText} handed back as an {@link Optional}, for a caller that works in them.
     *
     * @param value the text to normalise; may be null
     * @return the stripped text, or empty where it is blank
     */
    public static Optional<String> convertToOptionalText(String value) {
        return Optional.ofNullable(normaliseText(value));
    }

    /**
     * {@link #normaliseText} with the absence answered as {@code ""}, for a caller building a string
     * that would otherwise print a null.
     *
     * @param value the text to normalise; may be null
     * @return the stripped text, or {@code ""} where it is blank
     */
    public static String getTextOrEmpty(String value) {
        return Objects.requireNonNullElse(normaliseText(value), "");
    }

    /**
     * The {@link #normaliseText normalised} text {@code readText} answers for each of {@code items},
     * in their order, a null or blank answer left out. This is the one loop every read of "what may
     * this say" over a listing would otherwise write for itself.
     *
     * @param items    what to read the text off
     * @param readText reads one item's text, which may be null or blank
     * @param <T>      the kind of item
     * @return each item's stripped text where it has some, never null
     */
    public static <T> List<String> collectTexts(Iterable<? extends T> items, Function<? super T, String> readText) {

        var texts = new ArrayList<String>();

        for (var item : items) {

            var text = normaliseText(readText.apply(item));
            if (text != null) {
                texts.add(text);
            }
        }

        return texts;
    }

    /**
     * The whitespace-parted words of {@code text}, in the order they are written, with empties
     * dropped. Leading, trailing or doubled spacing changes nothing about the answer.
     *
     * <p>Empties are dropped because a caller counting or walking words means the words. A count
     * shifted by how the string happened to be spaced is a count of something else. A caller that
     * needs to know a string was blank asks {@link #hasText} first.
     *
     * @param text the string to part; null or blank yields an empty list
     * @return the words, never null
     */
    public static List<String> splitIntoWords(String text) {

        if (!hasText(text)) {
            return new ArrayList<>();
        }

        return new ArrayList<>(Arrays.asList(WHITESPACE_RUN.split(text.strip())));
    }

    /**
     * The initials of {@code text}'s words, each raised to upper case and closed by a full stop, so
     * "Church of Galactic Redemption" reads as "C.O.G.R.".
     *
     * <p>Every word contributes, the small joining ones included. A reader matches a short form back
     * to its name by counting letters against words. Which words are small enough to drop is a
     * judgement no two names would answer alike.
     *
     * <p>Whitespace is the only parting ({@link #splitIntoWords}), so a hyphenated word contributes
     * the one initial it opens with.
     *
     * @param text the name to abbreviate; text with no words in it abbreviates to nothing
     * @return the initials, never null
     */
    public static String abbreviateToInitials(String text) {

        var initials = new StringBuilder();

        for (var word : splitIntoWords(text)) {

            initials
                .append(Character.toUpperCase(word.charAt(0)))
                .append(INITIAL_TERMINATOR);
        }

        return initials.toString();
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
     * answered indexes the string the caller holds. A copy folded to one case can come out a different
     * length, which would slide the answer along the text.
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
     * {@code text} less any word past its first {@code protectedWordCount} that repeats, ignoring
     * case, the word kept before it. That is the stutter left where one phrase was appended to another
     * already ending on the word the second opens with.
     *
     * <p>The protected opening is what makes this safe. A repetition inside a subject's own name is
     * that name, and dropping it answers a name nobody has. A caller protects the words it holds
     * responsible for and exposes only what was appended. Zero protects nothing.
     *
     * <p>Text that lost nothing is answered as the very string given, so its spacing survives. Where
     * a word does go, what is left is rejoined on one space.
     *
     * @param text               the text to weigh; null or blank is answered as given
     * @param protectedWordCount how many opening words are the subject's own and so untouchable; zero
     *                           or less protects none of them
     * @return the text with the repeats dropped, or the very string given where none were
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
        return String.join(WORD_SEPARATOR, keptWords);
    }

    /**
     * {@code main} and {@code parenthetical} joined as "main (parenthetical)", or whichever of the two
     * {@link #hasText has text} where only one does.
     *
     * <p>A missing half drops out rather than leaving its slot behind, so a caller never prints an
     * empty pair of brackets or a bracketed remark with nothing before it.
     *
     * @param main          the leading text; null or blank is absent
     * @param parenthetical the remark set in brackets after it; null or blank is absent
     * @return the joined text, or empty where neither half has text
     */
    public static Optional<String> joinWithParenthetical(String main, String parenthetical) {

        var hasMain = hasText(main);
        var hasParenthetical = hasText(parenthetical);

        if (hasMain && hasParenthetical) {
            return Optional.of(main + PARENTHETICAL_OPENER + parenthetical + PARENTHETICAL_CLOSER);
        }

        if (hasMain) {
            return Optional.of(main);
        }

        if (hasParenthetical) {
            return Optional.of(parenthetical);
        }

        return Optional.empty();
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
