package kmlib.text;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KmlibStringsTest {

    @Nested
    class HasText {

        @Test
        void hasTextIsFalseOnNull() {
            assertThat(KmlibStrings.hasText(null))
                .isFalse();
        }

        @Test
        void hasTextIsFalseOnEmpty() {
            assertThat(KmlibStrings.hasText(""))
                .isFalse();
        }

        @Test
        void hasTextIsFalseOnWhitespaceOnly() {
            // Mix of space, tab, newline so the per-char loop runs over
            // each whitespace flavour the predicate is meant to dismiss.
            assertThat(KmlibStrings.hasText(" \t\n "))
                .isFalse();
        }

        @Test
        void hasTextIsTrueOnPlainText() {
            assertThat(KmlibStrings.hasText("Independent"))
                .isTrue();
        }

        @Test
        void hasTextIsTrueOnLeadingAndTrailingWhitespace() {
            // The predicate accepts any string containing at least one
            // non-whitespace character - it is not a "trim then check"
            // wrapper, so surrounding whitespace stays in the input.
            assertThat(KmlibStrings.hasText("  word  "))
                .isTrue();
        }
    }

    @Nested
    class SplitIntoWords {

        @Test
        void splitIntoWordsPartsASentenceIntoItsWordsInOrder() {
            assertThat(KmlibStrings.splitIntoWords("Penelope's Star System"))
                .containsExactly("Penelope's", "Star", "System");
        }

        @Test
        void splitIntoWordsIgnoresSurroundingAndDoubledSpacing() {
            // The reason the answer is words rather than pieces: a caller counting or walking
            // them means the words, so how the string happened to be spaced changes nothing.
            assertThat(KmlibStrings.splitIntoWords("  Penelope's   Star \t System \n"))
                .containsExactly("Penelope's", "Star", "System");
        }

        @Test
        void splitIntoWordsPartsAWhitespaceFlavourOtherThanTheSpace() {
            assertThat(KmlibStrings.splitIntoWords("first\tsecond\nthird"))
                .containsExactly("first", "second", "third");
        }

        @Test
        void splitIntoWordsAnswersOneWordForAStringWithoutSpacing() {
            assertThat(KmlibStrings.splitIntoWords("Galatia"))
                .containsExactly("Galatia");
        }

        @Test
        void splitIntoWordsIsEmptyOnWhitespaceOnly() {
            // No words were written, so none are reported - rather than the one empty piece a
            // bare split answers, which would count as a word everywhere the count is read.
            assertThat(KmlibStrings.splitIntoWords(" \t\n "))
                .isEmpty();
        }

        @Test
        void splitIntoWordsIsEmptyOnEmpty() {
            assertThat(KmlibStrings.splitIntoWords(""))
                .isEmpty();
        }

        @Test
        void splitIntoWordsIsEmptyOnNull() {
            assertThat(KmlibStrings.splitIntoWords(null))
                .isEmpty();
        }
    }

    @Nested
    class FindWholeWordIndex {

        @Test
        void findWholeWordIndexAnswersWhereTheTextOpensOnTheWord() {
            assertThat(KmlibStrings.findWholeWordIndex("Abandoned Station", "abandoned"))
                .isZero();
        }

        @Test
        void findWholeWordIndexAnswersWhereTheWordSitsInTheMiddle() {
            assertThat(KmlibStrings.findWholeWordIndex("Old Abandoned Yards", "abandoned"))
                .isEqualTo(4);
        }

        @Test
        void findWholeWordIndexTakesAHyphenAsPartingTwoWords() {
            // Anything that is not a letter or a digit parts one word from another, so a hyphenated
            // name says the word as plainly as the spaced form does.
            assertThat(KmlibStrings.findWholeWordIndex("Abandoned-Station", "abandoned"))
                .isZero();
        }

        @Test
        void findWholeWordIndexPassesOverAWordMerelyOpeningALongerOne() {
            // The whole reason this is not a containment: a caller picking a stretch out by the
            // answer would otherwise pick out the first nine characters of a word nobody wrote.
            assertThat(KmlibStrings.findWholeWordIndex("Abandonedium", "abandoned"))
                .isEqualTo(KmlibStrings.NO_WORD_MATCH);
        }

        @Test
        void findWholeWordIndexPassesOverAWordMerelyClosingALongerOne() {
            assertThat(KmlibStrings.findWholeWordIndex("Unabandoned", "abandoned"))
                .isEqualTo(KmlibStrings.NO_WORD_MATCH);
        }

        @Test
        void findWholeWordIndexAnswersTheFirstOfSeveralOccurrences() {
            assertThat(KmlibStrings.findWholeWordIndex("Abandoned Abandoned Yards", "abandoned"))
                .isZero();
        }

        @Test
        void findWholeWordIndexPassesOverAPartialMatchAheadOfAWholeOne() {
            // The search does not stop at the first place the characters appear: a longer word
            // carrying them is stepped over and the standalone one past it still answers.
            assertThat(KmlibStrings.findWholeWordIndex("Abandonedium Abandoned", "abandoned"))
                .isEqualTo(13);
        }

        @Test
        void findWholeWordIndexAnswersTheTextsOwnPositionWhateverCaseEitherIsIn() {
            // Matched against the text as it is spelled rather than a folded copy, so the position
            // indexes the string the caller holds.
            assertThat(KmlibStrings.findWholeWordIndex("ABANDONED STATION", "abandoned"))
                .isZero();
        }

        @Test
        void findWholeWordIndexAnswersAWordTheWholeTextConsistsOf() {
            assertThat(KmlibStrings.findWholeWordIndex("abandoned", "abandoned"))
                .isZero();
        }

        @Test
        void findWholeWordIndexSaysNothingIsFoundInAbsentText() {
            assertThat(KmlibStrings.findWholeWordIndex(null, "abandoned"))
                .isEqualTo(KmlibStrings.NO_WORD_MATCH);
        }

        @Test
        void findWholeWordIndexSaysAnAbsentWordIsSaidNowhere() {
            // An empty word matches at every position of every string, which is an answer no caller
            // could act on - so it is refused rather than answered at nought.
            assertThat(KmlibStrings.findWholeWordIndex("Abandoned Station", ""))
                .isEqualTo(KmlibStrings.NO_WORD_MATCH);
        }
    }

    @Nested
    class DropAdjacentRepeatedWords {

        // A subject of two words with a third appended that opens on the word the subject ends on -
        // the stutter the whole read exists for, stated once so the cases differ only in what they
        // are about.
        private static final int TWO_WORD_SUBJECT = 2;

        @Test
        void dropAdjacentRepeatedWordsDropsAWordRepeatingTheOneBeforeIt() {
            assertThat(KmlibStrings.dropAdjacentRepeatedWords(
                    "Penelope's Star Star System",
                    TWO_WORD_SUBJECT))
                .isEqualTo("Penelope's Star System");
        }

        @Test
        void dropAdjacentRepeatedWordsKeepsARepeatInsideTheProtectedOpening() {
            // The case the protection exists for: the repeat is the subject's own, so dropping it
            // would answer a name nobody has.
            assertThat(KmlibStrings.dropAdjacentRepeatedWords("Ko Ko Star System", TWO_WORD_SUBJECT))
                .isEqualTo("Ko Ko Star System");
        }

        @Test
        void dropAdjacentRepeatedWordsMatchesARepeatIgnoringCase() {
            // The two phrases were written apart, so the one that repeats need not be capitalised
            // as the word it repeats was.
            assertThat(KmlibStrings.dropAdjacentRepeatedWords(
                    "Penelope's Star STAR System",
                    TWO_WORD_SUBJECT))
                .isEqualTo("Penelope's Star System");
        }

        @Test
        void dropAdjacentRepeatedWordsPutsEveryWordInReachOfAProtectionOfNone() {
            // The hazard the count is required for, pinned rather than left to a caller to find:
            // unprotected, the walk eats the subject's own repetition too.
            assertThat(KmlibStrings.dropAdjacentRepeatedWords("Ko Ko Star System", 0))
                .isEqualTo("Ko Star System");
        }

        @Test
        void dropAdjacentRepeatedWordsPutsEveryWordInReachOfANegativeProtection() {
            // A count below zero protects no more than none does, rather than counting
            // back from the end - there is no "protect all but the last" reading here.
            assertThat(KmlibStrings.dropAdjacentRepeatedWords("Ko Ko Star System", -1))
                .isEqualTo("Ko Star System");
        }

        @Test
        void dropAdjacentRepeatedWordsAnswersUnchangedTextAsTheVeryStringGiven() {
            // A caller handing over something it must not see respaced gets it back untouched
            // wherever there was no stutter to drop.
            var text = "Galatia  Star System";

            assertThat(KmlibStrings.dropAdjacentRepeatedWords(text, 1))
                .isSameAs(text);
        }

        @Test
        void dropAdjacentRepeatedWordsRejoinsWhatIsLeftOnOneSpace() {
            // The other half of that rule: once a word goes, the text is being repaired rather than
            // preserved, so whatever spacing it was written with does not survive.
            assertThat(KmlibStrings.dropAdjacentRepeatedWords(
                    "Penelope's  Star   Star System",
                    TWO_WORD_SUBJECT))
                .isEqualTo("Penelope's Star System");
        }

        @Test
        void dropAdjacentRepeatedWordsDropsEveryWordOfARunPastTheProtection() {
            assertThat(KmlibStrings.dropAdjacentRepeatedWords(
                    "Penelope's Star Star Star System",
                    TWO_WORD_SUBJECT))
                .isEqualTo("Penelope's Star System");
        }

        @Test
        void dropAdjacentRepeatedWordsAnswersBlankTextAsGiven() {
            assertThat(KmlibStrings.dropAdjacentRepeatedWords("  ", TWO_WORD_SUBJECT))
                .isEqualTo("  ");
        }

        @Test
        void dropAdjacentRepeatedWordsAnswersNullAsGiven() {
            assertThat(KmlibStrings.dropAdjacentRepeatedWords(null, TWO_WORD_SUBJECT))
                .isNull();
        }
    }
}
