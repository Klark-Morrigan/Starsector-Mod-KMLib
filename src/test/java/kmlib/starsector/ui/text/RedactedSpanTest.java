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
 * the room its blocks need, it lays one block per word where the words stood, and it fills them at the
 * weight of the line rather than at the colour it was handed. All four matter downstream without being
 * checked there - a run that came out narrower than its blocks would have them overrun the words after
 * it, a word charged room but left unpainted would show as a gap in the middle of a redaction, a fill
 * taken straight off the line's colour shouts over the words either side, and a shape the constructor let
 * through reports as a failure inside a draw call rather than at whoever derived it.
 */
class RedactedSpanTest {

    private static final Color REDACTION_COLOUR = new Color(180, 180, 180);

    // The same colour part-transparent, for the case about what the fill does to alpha. Its own value
    // rather than the one above with an alpha argument, so a case cannot read as being about the shade.
    private static final int PART_ALPHA = 120;
    private static final Color PART_ALPHA_REDACTION_COLOUR = new Color(180, 180, 180, PART_ALPHA);

    // The strengths the cases are posed at: a tenth off, both ends of the range, and one past each end
    // for the readings that are held rather than computed. A tenth rather than the shipped weight
    // because the shipped weight has a case of its own, and a case standing for "some strength" that
    // happened to be the default would go on passing if the arithmetic stopped reading its argument.
    private static final float TENTH_STRENGTH = 0.1f;
    private static final float NO_STRENGTH = 0f;
    private static final float FULL_STRENGTH = 1f;
    private static final float STRENGTH_PAST_FULL = 1.4f;
    private static final float NEGATIVE_STRENGTH = -0.5f;

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
    class CanBeLeftOutOfLineText {

        @Test
        void canBeLeftOutOfLineTextIsFalse() {
            // The blocks are the only way the run says anything, so a line that drops them reads as
            // though no name had been there - which is why the flattened form refuses one outright
            // rather than handing back a shortened line nothing downstream can tell is short.
            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR)
                .canBeLeftOutOfLineText())
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

    @Nested
    class ResolveBlockFillColour {

        @Test
        void resolveBlockFillColourSinksTheLineColourByTheStrengthGiven() {
            // A block covers its whole band where the glyphs it replaces cover a fraction of theirs, so
            // filling it in the line's own colour lands several times the area of it on screen and the
            // redaction shouts over the words either side. A tenth taken off is a tenth of every channel.
            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR)
                .resolveBlockFillColour(TENTH_STRENGTH))
                .isEqualTo(new Color(162, 162, 162));
        }

        @Test
        void resolveBlockFillColourFillsInTheLinesOwnColourAtNoStrength() {
            // The near end of a host's slider, where the player has asked for no correction at all - so
            // the blocks draw in exactly the colour the words either side of them do.
            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR)
                .resolveBlockFillColour(NO_STRENGTH))
                .isEqualTo(new Color(180, 180, 180));
        }

        @Test
        void resolveBlockFillColourFillsBlackAtFullStrength() {

            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR)
                .resolveBlockFillColour(FULL_STRENGTH))
                .isEqualTo(new Color(0, 0, 0));
        }

        @Test
        void resolveBlockFillColourHoldsAStrengthPastFullAtBlack() {
            // Past the range the strength means anything over. The channels would clamp on their own, but
            // holding it here is what keeps the reading the caller asked for - a host handing over more
            // than the whole colour gets black rather than a colour arrived at by some other route.
            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR)
                .resolveBlockFillColour(STRENGTH_PAST_FULL))
                .isEqualTo(new Color(0, 0, 0));
        }

        @Test
        void resolveBlockFillColourHoldsANegativeStrengthAtNoDarkening() {
            // The other end: a strength below nothing would brighten the blocks past the line they stand
            // in, which is the one thing the correction exists to prevent.
            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR)
                .resolveBlockFillColour(NEGATIVE_STRENGTH))
                .isEqualTo(new Color(180, 180, 180));
        }

        @Test
        void resolveBlockFillColourKeepsTheLineColoursOwnAlpha() {
            // The correction is a weight one, and fading is the drawing surface's to decide - a run that
            // also thinned itself would fade twice over in a box already drawing at part opacity.
            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, PART_ALPHA_REDACTION_COLOUR)
                .resolveBlockFillColour(RedactedSpan.TEXT_WEIGHT_DARKENING_STRENGTH)
                .getAlpha())
                .isEqualTo(PART_ALPHA);
        }

        @Test
        void resolveBlockFillColourSinksTheShippedShareAtTheStandardStrength() {
            // The weight the library ships, restated as the colour it lands on so a change to the finding
            // shows up here as a changed expectation rather than passing silently.
            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR)
                .resolveBlockFillColour(RedactedSpan.TEXT_WEIGHT_DARKENING_STRENGTH))
                .isEqualTo(new Color(108, 108, 108));
        }

        @Test
        void resolveBlockFillColourLeavesTheRunsOwnColourAsTheLineWroteIt() {
            // The line's colour is what the run was handed and what the words either side of it draw in;
            // the fill is a reading taken off it. Kept apart so a surface asking for one cannot be given
            // the other, and so the two never drift into disagreeing about what colour the line is.
            assertThat(new RedactedSpan(TWO_WORD_NAME_LENGTHS, REDACTION_COLOUR).colour())
                .isEqualTo(new Color(180, 180, 180));
        }
    }
}
