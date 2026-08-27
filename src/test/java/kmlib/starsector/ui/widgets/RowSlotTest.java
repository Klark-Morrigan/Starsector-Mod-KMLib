package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.text.StyledSpanMeasurer;
import kmlib.starsector.ui.text.TextSpan;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins how wide each kind of {@link RowSlot} draws, because that width is what a stack of rows reserves
 * its shared columns from: a slot that under-reports leaves the label it flanks drawn over it, and one
 * that over-reports opens a gutter no content fills. The square kinds scale with the line so a stack
 * drawn at any size stays even, a run of text is worth whatever the line's own face measures it at, a
 * value drawn in several runs is worth those runs and the gaps they stand apart by, and the two
 * spellings of nothing-to-draw - an unfilled slot and a blank run - are charged alike.
 */
class RowSlotTest {

    private static final float LINE_HEIGHT = 20f;
    private static final float TALLER_LINE_HEIGHT = 40f;
    private static final Color SLOT_COLOUR = new Color(200, 150, 50);
    private static final Color OTHER_SLOT_COLOUR = new Color(140, 140, 140);

    // The word space two drawn runs of one value stand apart by: the drawing face's own, which the
    // stand-in measurer below charges as the one character it is. Restated here rather than read from
    // the rule that applies it: read from the subject the expectation would follow a change to the
    // spacing rather than catching it.
    private static final float WORD_SPACE = 1f;

    // A measurement no slot under test is allowed to spend. Handed to the kinds whose width is geometry
    // rather than glyphs, so "it never asks" is pinned as a contract rather than left as something that
    // happens to be true of today's arithmetic - a container may have nothing but a stub to hand over.
    private static final StyledSpanMeasurer UNSPENDABLE_MEASURER = textSpan -> {
        throw new AssertionError("a slot whose width is geometry must not measure text");
    };

    // The stand-in face charges one unit per character, so a measured width reads straight off the
    // string under test and says plainly that the bound measurement was the thing consulted.
    private static double measureSpanWidth(TextSpan textSpan) {
        return textSpan.text().length();
    }

    private static float computeWidth(RowSlot rowSlot, float lineHeight) {
        StyledSpanMeasurer spanMeasurer = RowSlotTest::measureSpanWidth;
        return rowSlot.computeWidth(lineHeight, spanMeasurer);
    }

    @Nested
    class ComputeWidth {
        @Test
        void computeWidthSquaresAnImageOffTheLineHeight() {
            // An image hangs as tall as its line, so it sits level with the label beside it whatever
            // face that label draws in.
            assertThat(computeWidth(new RowSlot.Image("crest_a"), LINE_HEIGHT))
                .isEqualTo(LINE_HEIGHT);
        }

        @Test
        void computeWidthSquaresATickOffTheLineHeight() {
            // Matching the image's width is what lets a row leading with a tick lay its label exactly
            // where a crested row lays its own.
            assertThat(computeWidth(new RowSlot.Tick(true), LINE_HEIGHT))
                .isEqualTo(computeWidth(new RowSlot.Image("crest_a"), LINE_HEIGHT));
        }

        @Test
        void computeWidthIsUnchangedByWhetherATickIsTicked() {
            // A ticked box and a clear one occupy the same column, so a list cannot shift as its
            // options are ticked.
            assertThat(computeWidth(new RowSlot.Tick(false), LINE_HEIGHT))
                .isEqualTo(computeWidth(new RowSlot.Tick(true), LINE_HEIGHT));
        }

        @Test
        void computeWidthTakesATriangleFromTheDrawnTriangleSlot() {
            // The reserved width is the width the marker is drawn at, so the column and the shape in it
            // cannot disagree - and it stays narrower than the line, reading as a compact marker.
            assertThat(computeWidth(new RowSlot.Triangle(TriangleDirection.UP), LINE_HEIGHT))
                .isEqualTo(IconLabelRow.computeDirectionTriangleSlotWidth(LINE_HEIGHT))
                .isLessThan(LINE_HEIGHT);
        }

        @Test
        void computeWidthIsUnchangedByWhichWayATrianglePoints() {
            assertThat(computeWidth(new RowSlot.Triangle(TriangleDirection.DOWN), LINE_HEIGHT))
                .isEqualTo(computeWidth(new RowSlot.Triangle(TriangleDirection.UP), LINE_HEIGHT));
        }

        @Test
        void computeWidthMeasuresARunOfTextThroughTheBoundMeasurement() {
            // Four characters at one unit each: the slot spends the measurement it was handed rather
            // than deriving a width from the line it sits on.
            assertThat(computeWidth(new RowSlot.Text(new TextSpan("9999", SLOT_COLOUR)), LINE_HEIGHT))
                .isEqualTo(4f);
        }

        @Test
        void computeWidthChargesNothingForABlankRunOfText() {
            // A caller that assembled a run from parts and came up empty gets the column it would have
            // had without the run, rather than a gap held open in front of no glyphs.
            assertThat(computeWidth(new RowSlot.Text(TextSpan.createBlank(SLOT_COLOUR)), LINE_HEIGHT))
                .isEqualTo(RowSlot.NO_WIDTH);
        }

        @Test
        void computeWidthChargesNothingForAWhitespaceOnlyRunOfText() {
            // Which runs read as nothing-to-draw is TextSpan's rule, so a run of separators is charged
            // as blank here too rather than as the width its spaces happen to measure.
            assertThat(computeWidth(new RowSlot.Text(new TextSpan("   ", SLOT_COLOUR)), LINE_HEIGHT))
                .isEqualTo(RowSlot.NO_WIDTH);
        }

        @Test
        void computeWidthSumsTheRunsOfAValueDrawnInSeveral() {
            // Four characters, the word space, then two more: the column reserves room for the space the
            // runs are drawn apart by, or the second run would be painted past the edge it was sized to.
            assertThat(computeWidth(
                    new RowSlot.TextRuns(List.of(
                        new TextSpan("9999", SLOT_COLOUR),
                        new TextSpan("12", OTHER_SLOT_COLOUR))),
                    LINE_HEIGHT))
                .isEqualTo(4f + WORD_SPACE + 2f);
        }

        @Test
        void computeWidthChargesAValueOfOneRunWhatTheSingleRunKindIsCharged() {
            // A value picked out in two colours and a plain one must be priced by one rule, or a stack
            // mixing them would reserve two different columns for the same glyphs.
            assertThat(computeWidth(
                    new RowSlot.TextRuns(List.of(new TextSpan("9999", SLOT_COLOUR))),
                    LINE_HEIGHT))
                .isEqualTo(computeWidth(
                    new RowSlot.Text(new TextSpan("9999", SLOT_COLOUR)),
                    LINE_HEIGHT));
        }

        @Test
        void computeWidthChargesNothingForAValueWhoseRunsAreAllBlank() {
            // Neither the glyphs nor the gap between them: a caller that assembled every run from parts
            // and came up empty gets the column collapsed, exactly as the single-run kind does.
            assertThat(computeWidth(
                    new RowSlot.TextRuns(List.of(
                        TextSpan.createBlank(SLOT_COLOUR),
                        new TextSpan("   ", OTHER_SLOT_COLOUR))),
                    LINE_HEIGHT))
                .isEqualTo(RowSlot.NO_WIDTH);
        }

        @Test
        void computeWidthChargesNoGapInFrontOfABlankRunOfAValue() {
            // A value whose working came out empty measures as the finding alone, rather than as the
            // finding held one gap in from the column it is aligned to.
            assertThat(computeWidth(
                    new RowSlot.TextRuns(List.of(
                        TextSpan.createBlank(OTHER_SLOT_COLOUR),
                        new TextSpan("12", SLOT_COLOUR))),
                    LINE_HEIGHT))
                .isEqualTo(2f);
        }

        @Test
        void computeWidthChargesNothingForAnUnfilledSlot() {
            assertThat(computeWidth(RowSlot.EMPTY, LINE_HEIGHT))
                .isEqualTo(RowSlot.NO_WIDTH);
        }

        @Test
        void computeWidthMeasuresNoTextForTheKindsSizedOffGeometry() {
            // An image, a tick, and a triangle answer from the line alone, so a container with no text
            // measurement to hand - or one bound to another face - still gets their widths.
            assertThat(new RowSlot.Image("crest_a").computeWidth(LINE_HEIGHT, UNSPENDABLE_MEASURER))
                .isEqualTo(LINE_HEIGHT);
            assertThat(new RowSlot.Tick(true).computeWidth(LINE_HEIGHT, UNSPENDABLE_MEASURER))
                .isEqualTo(LINE_HEIGHT);
            assertThat(new RowSlot.Triangle(TriangleDirection.UP)
                    .computeWidth(LINE_HEIGHT, UNSPENDABLE_MEASURER))
                .isEqualTo(IconLabelRow.computeDirectionTriangleSlotWidth(LINE_HEIGHT));
            assertThat(RowSlot.EMPTY.computeWidth(LINE_HEIGHT, UNSPENDABLE_MEASURER))
                .isEqualTo(RowSlot.NO_WIDTH);
        }

        @Test
        void computeWidthScalesTheSquareAndTriangleKindsWithTheLineHeight() {
            // Every kind whose width is geometry rather than glyphs grows with the line, so a stack
            // drawn larger keeps the same proportions rather than shrinking its slots.
            assertThat(computeWidth(new RowSlot.Image("crest_a"), TALLER_LINE_HEIGHT))
                .isGreaterThan(computeWidth(new RowSlot.Image("crest_a"), LINE_HEIGHT));
            assertThat(computeWidth(new RowSlot.Tick(true), TALLER_LINE_HEIGHT))
                .isGreaterThan(computeWidth(new RowSlot.Tick(true), LINE_HEIGHT));
            assertThat(computeWidth(new RowSlot.Triangle(TriangleDirection.UP), TALLER_LINE_HEIGHT))
                .isGreaterThan(computeWidth(new RowSlot.Triangle(TriangleDirection.UP), LINE_HEIGHT));
        }

        @Test
        void computeWidthLeavesARunOfTextUnchangedByTheLineHeight() {
            // Text is as wide as the face it was measured in, and that face is already bound into the
            // measurement - so a taller line does not silently re-price a run measured on another face.
            var textSlot = new RowSlot.Text(new TextSpan("9999", SLOT_COLOUR));

            assertThat(computeWidth(textSlot, TALLER_LINE_HEIGHT))
                .isEqualTo(computeWidth(textSlot, LINE_HEIGHT));
        }
    }

    @Nested
    class IsFilled {
        @Test
        void isFilledIsTrueForTheKindsThatAlwaysDrawSomething() {
            // An image, a tick, and a triangle each draw whatever state they carry, so a row holding
            // one fills that flank and the label beside it starts past the reserved column.
            assertThat(new RowSlot.Image("crest_a").isFilled())
                .isTrue();
            assertThat(new RowSlot.Tick(false).isFilled())
                .isTrue();
            assertThat(new RowSlot.Triangle(TriangleDirection.DOWN).isFilled())
                .isTrue();
        }

        @Test
        void isFilledIsTrueForARunOfTextWithGlyphs() {
            assertThat(new RowSlot.Text(new TextSpan("12", SLOT_COLOUR)).isFilled())
                .isTrue();
        }

        @Test
        void isFilledIsTrueForAValueWhereAnyRunHasGlyphs() {
            // A value stating only its finding, its working having come out empty, still fills its
            // column - the row draws something there and the label must stay clear of it.
            assertThat(new RowSlot.TextRuns(List.of(
                        TextSpan.createBlank(OTHER_SLOT_COLOUR),
                        new TextSpan("12", SLOT_COLOUR)))
                    .isFilled())
                .isTrue();
        }

        @Test
        void isFilledIsFalseForAValueWhoseRunsAreAllBlank() {
            assertThat(new RowSlot.TextRuns(List.of(
                        TextSpan.createBlank(SLOT_COLOUR),
                        new TextSpan("   ", OTHER_SLOT_COLOUR)))
                    .isFilled())
                .isFalse();
        }

        @Test
        void isFilledIsFalseForAnUnfilledSlot() {
            assertThat(RowSlot.EMPTY.isFilled())
                .isFalse();
        }

        @Test
        void isFilledIsFalseForABlankRunOfText() {
            // A caller that assembled a value from parts and came up empty said its row has a value;
            // the row is charged the absence it actually draws rather than a gutter in front of no
            // glyphs.
            assertThat(new RowSlot.Text(TextSpan.createBlank(SLOT_COLOUR)).isFilled())
                .isFalse();
        }

        @Test
        void isFilledIsFalseForAWhitespaceOnlyRunOfText() {
            assertThat(new RowSlot.Text(new TextSpan("   ", SLOT_COLOUR)).isFilled())
                .isFalse();
        }

        @Test
        void isFilledAgreesWithWhetherTheSlotIsChargedAWidth() {
            // The two readings of "is anything here" must not part company: a slot a container skips
            // laying out is exactly one it would have reserved nothing for.
            assertThat(new RowSlot.Text(TextSpan.createBlank(SLOT_COLOUR)).isFilled())
                .isFalse();
            assertThat(computeWidth(new RowSlot.Text(TextSpan.createBlank(SLOT_COLOUR)), LINE_HEIGHT))
                .isEqualTo(RowSlot.NO_WIDTH);
        }
    }

    @Nested
    class Equals {
        @Test
        void equalsMatchesTheSharedEmptySlotWithOneBuiltByHand() {
            // The shared constant is a convenience, not an identity: a row holding a hand-built empty
            // slot must compare equal to one holding EMPTY, or two rows that carry nothing would differ.
            assertThat(RowSlot.EMPTY)
                .isEqualTo(new RowSlot.Empty());
        }
    }

    @Nested
    class Constructor {
        @Test
        void constructorRejectsAnImageWithNoPath() {
            // A slot holding no image is RowSlot.EMPTY; a null path would otherwise surface at the
            // texture lookup inside a draw call.
            assertThatThrownBy(() -> new RowSlot.Image(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("spritePath");
        }

        @Test
        void constructorLeavesAnImageUntintedWhenNoColourIsStated() {
            // The path-only form is what almost every row builds, so it must mean "as authored"
            // rather than some stand-in shade: a null tint is the no-op multiply the draw already
            // treats as drawing the texture's own pixels.
            assertThat(new RowSlot.Image("crest_a").tintColour())
                .isNull();
        }

        @Test
        void constructorCarriesAStatedImageTint() {
            // A row that recedes states the shade its mark is multiplied by, and the slot carries it
            // through to the draw rather than the two ends agreeing on it separately.
            assertThat(new RowSlot.Image("crest_a", SLOT_COLOUR).tintColour())
                .isEqualTo(SLOT_COLOUR);
        }

        @Test
        void constructorRejectsATextSlotWithNoSpan() {
            assertThatThrownBy(() -> new RowSlot.Text(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("textSpan");
        }

        @Test
        void constructorRejectsAMultiRunValueWithNoRuns() {
            // A slot holding no value is RowSlot.EMPTY and a value that draws nothing is a blank run,
            // so an empty list spells neither absence and would reach the layout as a value of nothing.
            assertThatThrownBy(() -> new RowSlot.TextRuns(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("textSpans");
        }

        @Test
        void constructorCopiesAMultiRunValuesRuns() {
            // The slot is a value, so a caller still holding the list it built must not be able to add
            // a run to a slot already handed to a layout - which would reserve a column for the runs it
            // measured and then draw another one past its edge.
            var textSpans = new ArrayList<TextSpan>();

            textSpans.add(new TextSpan("12", SLOT_COLOUR));

            var rowSlot = new RowSlot.TextRuns(textSpans);

            textSpans.add(new TextSpan("34", OTHER_SLOT_COLOUR));

            assertThat(rowSlot.textSpans())
                .hasSize(1);
        }

        @Test
        void constructorRejectsAMultiRunValueWithNoRunList() {
            assertThatThrownBy(() -> new RowSlot.TextRuns(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void constructorRejectsATriangleWithNoDirection() {
            assertThatThrownBy(() -> new RowSlot.Triangle(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("triangleDirection");
        }
    }
}
