package kmlib.starsector.ui.text;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins {@link RedactedSpan}'s contracts: it holds counts and never the name they came from, it is charged
 * the room its blocks need, and it lays one block per word where the words stood. All three matter
 * downstream without being checked there - a run that came out narrower than its blocks would have them
 * overrun the words after it, a word charged room but left unpainted would show as a gap in the middle of
 * a redaction, and a shape the constructor let through reports as a failure inside a draw call rather
 * than at whoever derived it.
 */
class RedactedSpanTest {

    private static final Color REDACTION_COLOUR = new Color(180, 180, 180);

    // A name of two words, seven characters and five, as the counts a line carries in place of it.
    private static final List<Integer> TWO_WORD_NAME_LENGTHS = List.of(7, 5);

    // Charges one unit per character, so a width that came from anywhere but the character counts - the
    // real name's glyphs, a fixed block size - would be visible in the assertions below. The word space
    // the label parts its runs by is one character wide under it, like any other single character.
    private static final StyledSpanMeasurer ONE_UNIT_PER_CHARACTER =
        textSpan -> textSpan.text().length();

    private static final float LINE_HEIGHT = 20f;

    private static final float BAR_LEFT_X = 100f;

    private static final float BAR_BOTTOM_Y = 40f;

    @Nested
    class Constructor {

        @Test
        void constructorKeepsTheWordLengthsItWasGiven() {

            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR).wordLengths())
                .containsExactly(7, 5);
        }

        @Test
        void constructorCopiesTheWordLengths() {
            // The counts are derived where the line is built, often from a list that goes on being
            // assembled - a run reading a caller's live list would redact a different shape later in the
            // same frame.
            var wordLengths = new ArrayList<>(List.of(7));
            var redactedSpan = new RedactedSpan(wordLengths, REDACTION_COLOUR);

            wordLengths.add(5);

            assertThat(redactedSpan.wordLengths())
                .containsExactly(7);
        }

        @Test
        void constructorRejectsNullWordLengths() {

            assertThatThrownBy(() -> new RedactedSpan(null, REDACTION_COLOUR))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("wordLengths");
        }

        @Test
        void constructorRejectsANullColour() {

            assertThatThrownBy(() -> new RedactedSpan(TWO_WORD_NAME_LENGTHS, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("colour");
        }

        @Test
        void constructorRejectsANegativeWordLength() {
            // No name has a word of fewer than no characters, so a negative count is a mistake in
            // whatever derived it rather than a redaction of some other shape.
            assertThatThrownBy(() -> new RedactedSpan(List.of(-3), REDACTION_COLOUR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("wordLengths");
        }

        @Test
        void constructorBuildsAWordOfItsOwn() {
            // A redaction stands where a name stood, so it keeps the sentence's own space clear of the
            // words either side of it rather than butting against them.
            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR).isJoinedToPreviousRun())
                .isFalse();
        }
    }

    @Nested
    class ComputeWidth {

        @Test
        void computeWidthChargesEachWordItsCharactersAndOneGapBetweenThem() {
            // Seven characters, the face's own word space, then five - the same rule the label parts its
            // other runs by, so the blocks stand where the words they replace stood.
            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR)
                .computeWidth(LINE_HEIGHT, ONE_UNIT_PER_CHARACTER))
                .isEqualTo(13f);
        }

        @Test
        void computeWidthChargesASingleWordNoGap() {

            assertThat(new RedactedSpan(List.of(7), REDACTION_COLOUR)
                .computeWidth(LINE_HEIGHT, ONE_UNIT_PER_CHARACTER))
                .isEqualTo(7f);
        }

        @Test
        void computeWidthChargesNothingForAWordOfNoCharacters() {
            // A word that ran to nothing is charged neither block nor gap, the reading a blank run of
            // text gets - so a redaction assembled from parts and coming up empty gets the line it would
            // have had without it.
            assertThat(new RedactedSpan(List.of(0), REDACTION_COLOUR)
                .computeWidth(LINE_HEIGHT, ONE_UNIT_PER_CHARACTER))
                .isEqualTo(0f);
        }

        @Test
        void computeWidthChargesNothingForNoWordsAtAll() {

            assertThat(new RedactedSpan(List.of(), REDACTION_COLOUR)
                .computeWidth(LINE_HEIGHT, ONE_UNIT_PER_CHARACTER))
                .isEqualTo(0f);
        }

        @Test
        void computeWidthIsUnchangedByTheLineHeight() {
            // A block stands as wide as the characters it replaces, so what squares an image off its line
            // has no bearing here.
            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR)
                .computeWidth(32f, ONE_UNIT_PER_CHARACTER))
                .isEqualTo(13f);
        }
    }

    @Nested
    class HasContent {

        @Test
        void hasContentIsTrueForAWordWithCharacters() {

            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR).hasContent())
                .isTrue();
        }

        @Test
        void hasContentIsFalseForNoWordsAtAll() {

            assertThat(new RedactedSpan(List.of(), REDACTION_COLOUR).hasContent())
                .isFalse();
        }

        @Test
        void hasContentIsFalseWhereEveryWordRanToNothing() {
            // Answered without a face, like every run, and by the same reading the walk charges the run
            // nothing by - so a line cannot reserve room for a redaction that draws no block.
            assertThat(new RedactedSpan(List.of(0, 0), REDACTION_COLOUR).hasContent())
                .isFalse();
        }
    }

    @Nested
    class LayOutWordBars {

        @Test
        void layOutWordBarsPlacesOneBlockPerWordAcrossTheGapBetweenThem() {
            // Seven characters from the run's left edge, then the word space, then five - the blocks fill
            // exactly the stretch the width above charged room for.
            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR)
                .layOutWordBars(BAR_LEFT_X, BAR_BOTTOM_Y, LINE_HEIGHT, ONE_UNIT_PER_CHARACTER))
                .containsExactly(
                    new Rectangle(100f, 40f, 7f, 20f),
                    new Rectangle(108f, 40f, 5f, 20f));
        }

        @Test
        void layOutWordBarsStandsEveryBlockInTheBandItWasGiven() {
            // Where the line's foot sits and how tall it stands are the drawing surface's own - a box
            // anchoring its rows by their tops and a strip centring them on a row stand the same run on
            // two different footings.
            assertThat(new RedactedSpan(List.of(7), REDACTION_COLOUR)
                .layOutWordBars(BAR_LEFT_X, 12f, 32f, ONE_UNIT_PER_CHARACTER))
                .containsExactly(new Rectangle(100f, 12f, 7f, 32f));
        }

        @Test
        void layOutWordBarsLeavesOutAWordOfNoCharacters() {
            // The word was charged no room, so painting a block for it would put one where the
            // measurement reserved none.
            assertThat(new RedactedSpan(List.of(0, 5), REDACTION_COLOUR)
                .layOutWordBars(BAR_LEFT_X, BAR_BOTTOM_Y, LINE_HEIGHT, ONE_UNIT_PER_CHARACTER))
                .containsExactly(new Rectangle(100f, 40f, 5f, 20f));
        }

        @Test
        void layOutWordBarsDrawsNothingForNoWordsAtAll() {

            assertThat(new RedactedSpan(List.of(), REDACTION_COLOUR)
                .layOutWordBars(BAR_LEFT_X, BAR_BOTTOM_Y, LINE_HEIGHT, ONE_UNIT_PER_CHARACTER))
                .isEmpty();
        }
    }

    @Nested
    class PaintRun {

        @Test
        void paintRunHandsItselfToThePaintersRedactionMethod() {

            var redactedSpan = new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR);
            var labelRunPainterFake = new LabelRunPainterFake();

            redactedSpan.paintRun(labelRunPainterFake, 64f);

            assertThat(labelRunPainterFake.getPaintedRun())
                .isSameAs(redactedSpan);
            assertThat(labelRunPainterFake.getPaintedRunX())
                .isEqualTo(64f);
        }
    }
}
