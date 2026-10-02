package kmlib.text;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KmlibStringsTests {

    @Nested
    class HasText {

        @Test
        void isFalseOnNull() {
            assertThat(KmlibStrings.hasText(null))
                .isFalse();
        }

        @Test
        void isFalseOnEmpty() {
            assertThat(KmlibStrings.hasText(""))
                .isFalse();
        }

        @Test
        void isFalseOnWhitespaceOnly() {
            // Mix of space, tab, newline so the per-char loop runs over
            // each whitespace flavour the predicate is meant to dismiss.
            assertThat(KmlibStrings.hasText(" \t\n "))
                .isFalse();
        }

        @Test
        void isTrueOnPlainText() {
            assertThat(KmlibStrings.hasText("Independent"))
                .isTrue();
        }

        @Test
        void isTrueOnLeadingAndTrailingWhitespace() {
            // The predicate accepts any string containing at least one
            // non-whitespace character - it is not a "trim then check"
            // wrapper, so surrounding whitespace stays in the input.
            assertThat(KmlibStrings.hasText("  word  "))
                .isTrue();
        }
    }

    @Nested
    class RequireText {

        @Test
        void returnsTheValueItWasGiven() {
            assertThat(KmlibStrings.requireText("Fast Rendering", "unused"))
                .isEqualTo("Fast Rendering");
        }

        @Test
        void rejectsABlankWithTheGivenMessage() {
            // Blank rather than null: the case a null check lets through, and the reason the helper
            // exists beside Objects.requireNonNull.
            assertThatThrownBy(() -> KmlibStrings.requireText("   ", "A name with no text in it"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A name with no text in it");
        }
    }

    @Nested
    class CollectTexts {

        @Test
        void keepsEachItemsTextInOrderLeavingNullsAndBlanksOut() {
            // A blank or absent name is nothing a caller could draw, so it is not text the caller holds.
            var names = Arrays.asList("Corvus", null, "  ", "Askonia");

            assertThat(KmlibStrings.collectTexts(names, name -> name))
                .containsExactly("Corvus", "Askonia");
        }

        @Test
        void readsTheTextOffEachItemThroughTheReaderGiven() {

            assertThat(KmlibStrings.collectTexts(List.of(1, 22), number -> "#" + number))
                .containsExactly("#1", "#22");
        }
    }

    @Nested
    class SplitIntoWords {

        @Test
        void partsASentenceIntoItsWordsInOrder() {
            assertThat(KmlibStrings.splitIntoWords("Penelope's Star System"))
                .containsExactly("Penelope's", "Star", "System");
        }

        @Test
        void ignoresSurroundingAndDoubledSpacing() {
            // The reason the answer is words rather than pieces: a caller counting or walking
            // them means the words, so how the string happened to be spaced changes nothing.
            assertThat(KmlibStrings.splitIntoWords("  Penelope's   Star \t System \n"))
                .containsExactly("Penelope's", "Star", "System");
        }

        @Test
        void partsAWhitespaceFlavourOtherThanTheSpace() {
            assertThat(KmlibStrings.splitIntoWords("first\tsecond\nthird"))
                .containsExactly("first", "second", "third");
        }

        @Test
        void answersOneWordForAStringWithoutSpacing() {
            assertThat(KmlibStrings.splitIntoWords("Galatia"))
                .containsExactly("Galatia");
        }

        @Test
        void isEmptyOnWhitespaceOnly() {
            // No words were written, so none are reported - rather than the one empty piece a
            // bare split answers, which would count as a word everywhere the count is read.
            assertThat(KmlibStrings.splitIntoWords(" \t\n "))
                .isEmpty();
        }

        @Test
        void isEmptyOnEmpty() {
            assertThat(KmlibStrings.splitIntoWords(""))
                .isEmpty();
        }

        @Test
        void isEmptyOnNull() {
            assertThat(KmlibStrings.splitIntoWords(null))
                .isEmpty();
        }
    }

    @Nested
    class AbbreviateToInitials {

        @Test
        void takesTheInitialOfEveryWordIncludingTheSmallOnes() {
            // A reader matching the short form back counts its letters off against the words, so a
            // joining word quietly passed over would leave them one letter short of the name.
            assertThat(KmlibStrings.abbreviateToInitials("Church of Galactic Redemption"))
                .isEqualTo("C.O.G.R.");
        }

        @Test
        void raisesALowerCaseInitial() {
            assertThat(KmlibStrings.abbreviateToInitials("church of redemption"))
                .isEqualTo("C.O.R.");
        }

        @Test
        void readsAHyphenatedWordAsTheOneWordItIsWritten() {
            // Whitespace is the only parting, so nothing here has to decide whether a hyphen joins
            // two names or spells one.
            assertThat(KmlibStrings.abbreviateToInitials("Tri-Tachyon Concord"))
                .isEqualTo("T.C.");
        }

        @Test
        void answersOneInitialForOneWord() {
            assertThat(KmlibStrings.abbreviateToInitials("Galatia"))
                .isEqualTo("G.");
        }

        @Test
        void isEmptyForTextWithNoWordsInIt() {
            assertThat(KmlibStrings.abbreviateToInitials(" \t\n "))
                .isEmpty();
        }

        @Test
        void isEmptyForNull() {
            assertThat(KmlibStrings.abbreviateToInitials(null))
                .isEmpty();
        }
    }

    @Nested
    class FindWholeWordIndex {

        @Test
        void answersWhereTheTextOpensOnTheWord() {
            assertThat(KmlibStrings.findWholeWordIndex("Abandoned Station", "abandoned"))
                .isZero();
        }

        @Test
        void answersWhereTheWordSitsInTheMiddle() {
            assertThat(KmlibStrings.findWholeWordIndex("Old Abandoned Yards", "abandoned"))
                .isEqualTo(4);
        }

        @Test
        void takesAHyphenAsPartingTwoWords() {
            // Anything that is not a letter or a digit parts one word from another, so a hyphenated
            // name says the word as plainly as the spaced form does.
            assertThat(KmlibStrings.findWholeWordIndex("Abandoned-Station", "abandoned"))
                .isZero();
        }

        @Test
        void passesOverAWordMerelyOpeningALongerOne() {
            // The whole reason this is not a containment: a caller picking a stretch out by the
            // answer would otherwise pick out the first nine characters of a word nobody wrote.
            assertThat(KmlibStrings.findWholeWordIndex("Abandonedium", "abandoned"))
                .isEqualTo(KmlibStrings.NO_WORD_MATCH);
        }

        @Test
        void passesOverAWordMerelyClosingALongerOne() {
            assertThat(KmlibStrings.findWholeWordIndex("Unabandoned", "abandoned"))
                .isEqualTo(KmlibStrings.NO_WORD_MATCH);
        }

        @Test
        void answersTheFirstOfSeveralOccurrences() {
            assertThat(KmlibStrings.findWholeWordIndex("Abandoned Abandoned Yards", "abandoned"))
                .isZero();
        }

        @Test
        void passesOverAPartialMatchAheadOfAWholeOne() {
            // The search does not stop at the first place the characters appear: a longer word
            // carrying them is stepped over and the standalone one past it still answers.
            assertThat(KmlibStrings.findWholeWordIndex("Abandonedium Abandoned", "abandoned"))
                .isEqualTo(13);
        }

        @Test
        void answersTheTextsOwnPositionWhateverCaseEitherIsIn() {
            // Matched against the text as it is spelled rather than a folded copy, so the position
            // indexes the string the caller holds.
            assertThat(KmlibStrings.findWholeWordIndex("ABANDONED STATION", "abandoned"))
                .isZero();
        }

        @Test
        void answersAWordTheWholeTextConsistsOf() {
            assertThat(KmlibStrings.findWholeWordIndex("abandoned", "abandoned"))
                .isZero();
        }

        @Test
        void saysNothingIsFoundInAbsentText() {
            assertThat(KmlibStrings.findWholeWordIndex(null, "abandoned"))
                .isEqualTo(KmlibStrings.NO_WORD_MATCH);
        }

        @Test
        void saysAnAbsentWordIsSaidNowhere() {
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
        void dropsAWordRepeatingTheOneBeforeIt() {
            assertThat(KmlibStrings.dropAdjacentRepeatedWords(
                    "Penelope's Star Star System",
                    TWO_WORD_SUBJECT))
                .isEqualTo("Penelope's Star System");
        }

        @Test
        void keepsARepeatInsideTheProtectedOpening() {
            // The case the protection exists for: the repeat is the subject's own, so dropping it
            // would answer a name nobody has.
            assertThat(KmlibStrings.dropAdjacentRepeatedWords("Ko Ko Star System", TWO_WORD_SUBJECT))
                .isEqualTo("Ko Ko Star System");
        }

        @Test
        void matchesARepeatIgnoringCase() {
            // The two phrases were written apart, so the one that repeats need not be capitalised
            // as the word it repeats was.
            assertThat(KmlibStrings.dropAdjacentRepeatedWords(
                    "Penelope's Star STAR System",
                    TWO_WORD_SUBJECT))
                .isEqualTo("Penelope's Star System");
        }

        @Test
        void putsEveryWordInReachOfAProtectionOfNone() {
            // The hazard the count is required for, pinned rather than left to a caller to find:
            // unprotected, the walk eats the subject's own repetition too.
            assertThat(KmlibStrings.dropAdjacentRepeatedWords("Ko Ko Star System", 0))
                .isEqualTo("Ko Star System");
        }

        @Test
        void putsEveryWordInReachOfANegativeProtection() {
            // A count below zero protects no more than none does, rather than counting
            // back from the end - there is no "protect all but the last" reading here.
            assertThat(KmlibStrings.dropAdjacentRepeatedWords("Ko Ko Star System", -1))
                .isEqualTo("Ko Star System");
        }

        @Test
        void answersUnchangedTextAsTheVeryStringGiven() {
            // A caller handing over something it must not see respaced gets it back untouched
            // wherever there was no stutter to drop.
            var text = "Galatia  Star System";

            assertThat(KmlibStrings.dropAdjacentRepeatedWords(text, 1))
                .isSameAs(text);
        }

        @Test
        void rejoinsWhatIsLeftOnOneSpace() {
            // The other half of that rule: once a word goes, the text is being repaired rather than
            // preserved, so whatever spacing it was written with does not survive.
            assertThat(KmlibStrings.dropAdjacentRepeatedWords(
                    "Penelope's  Star   Star System",
                    TWO_WORD_SUBJECT))
                .isEqualTo("Penelope's Star System");
        }

        @Test
        void dropsEveryWordOfARunPastTheProtection() {
            assertThat(KmlibStrings.dropAdjacentRepeatedWords(
                    "Penelope's Star Star Star System",
                    TWO_WORD_SUBJECT))
                .isEqualTo("Penelope's Star System");
        }

        @Test
        void answersBlankTextAsGiven() {
            assertThat(KmlibStrings.dropAdjacentRepeatedWords("  ", TWO_WORD_SUBJECT))
                .isEqualTo("  ");
        }

        @Test
        void answersNullAsGiven() {
            assertThat(KmlibStrings.dropAdjacentRepeatedWords(null, TWO_WORD_SUBJECT))
                .isNull();
        }
    }
}
