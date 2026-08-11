package kmlib.starsector.ui.text;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins how a label's runs compose, which is the rule every surface laying a label reads: the floor a
 * label is held to, that a run is added to what is there rather than replacing it, that the runs read as
 * one line when a surface draws them in one pass, and where they measure out to when drawn as several.
 *
 * <p>The offsets are the contract worth fixing hardest. A run starting one word space past the one before
 * it, that space being the drawing face's own, and a blank run being charged nothing are what make two
 * surfaces lay the same label identically - and a placement that disagreed with the width its host was
 * sized to is drift no draw call could catch. An image run is held to the same walk, since a crest set
 * among a line's words is spaced and anchored by the sentence rather than by a column.
 */
class LabelRunsTest {

    private static final Color RUN_COLOUR = new Color(200, 150, 50);
    private static final Color OTHER_RUN_COLOUR = new Color(50, 150, 200);

    // The line an image run squares itself off. A round number unlike any character count below, so a
    // width that came from the image cannot be mistaken for one that came from glyphs.
    private static final float LINE_HEIGHT = 20f;

    private static final String CREST_SPRITE_PATH = "graphics/factions/crest_hegemony.png";

    // Each glyph one unit wide, so a run's width is its character count and an expected offset can be
    // written as the literal it works out to rather than re-derived from the measurement under test.
    private static final StyledSpanMeasurer ONE_UNIT_PER_CHARACTER =
        textSpan -> textSpan.text().length();

    // What a face that sets its words far apart charges for a space, against the one unit it charges any
    // other glyph. A width no character count below can produce, so a measured gap says the space was
    // read off the face rather than assumed.
    private static final float WIDE_WORD_SPACE_WIDTH = 7f;

    // A face whose glyphs cost a unit each but whose space costs the width above - the shape that says
    // whether a caller standing a mark off a label read the face's own space or a number of its own.
    private static StyledSpanMeasurer createSpaceHeavyMeasurer() {
        return textSpan -> textSpan.text().isBlank()
            ? WIDE_WORD_SPACE_WIDTH * textSpan.text().length()
            : textSpan.text().length();
    }

    @Nested
    class AppendRun {

        @Test
        void appendRunPutsTheRunLast() {

            var runs = LabelRuns.appendRun(
                List.of(new TextSpan("Hegemony", RUN_COLOUR)),
                new TextSpan("(7)", OTHER_RUN_COLOUR));

            assertThat(runs)
                .containsExactly(
                    new TextSpan("Hegemony", RUN_COLOUR),
                    new TextSpan("(7)", OTHER_RUN_COLOUR));
        }

        @Test
        void appendRunPutsAnImageRunLast() {
            // The two kinds compose alike, so a caller sets a crest into a sentence the same way it
            // picks a word out in another colour.
            var runs = LabelRuns.appendRun(
                List.of(new TextSpan("Held by", RUN_COLOUR)),
                new ImageSpan(CREST_SPRITE_PATH));

            assertThat(runs)
                .containsExactly(
                    new TextSpan("Held by", RUN_COLOUR),
                    new ImageSpan(CREST_SPRITE_PATH));
        }

        @Test
        void appendRunLeavesTheCallersRunsAlone() {
            // A refinement hands its own list in and keeps holding it, so appending must not rewrite the
            // label of the value the caller started from.
            var callerRuns = new ArrayList<LabelRun>();
            callerRuns.add(new TextSpan("Hegemony", RUN_COLOUR));

            LabelRuns.appendRun(callerRuns, new TextSpan("(7)", OTHER_RUN_COLOUR));

            assertThat(callerRuns)
                .hasSize(1);
        }
    }

    @Nested
    class CopyRuns {

        @Test
        void copyRunsKeepsTheRunsInReadingOrder() {

            var runs = LabelRuns.copyRuns(List.of(
                new TextSpan("Hegemony", RUN_COLOUR),
                new TextSpan("(7)", OTHER_RUN_COLOUR)));

            assertThat(runs)
                .containsExactly(
                    new TextSpan("Hegemony", RUN_COLOUR),
                    new TextSpan("(7)", OTHER_RUN_COLOUR));
        }

        @Test
        void copyRunsDoesNotAliasTheCallersList() {

            var callerRuns = new ArrayList<LabelRun>();
            callerRuns.add(new TextSpan("Hegemony", RUN_COLOUR));

            var runs = LabelRuns.copyRuns(callerRuns);
            callerRuns.set(0, new TextSpan("Mutated", RUN_COLOUR));

            assertThat(runs)
                .containsExactly(new TextSpan("Hegemony", RUN_COLOUR));
        }

        @Test
        void copyRunsRejectsALabelWithNoRuns() {
            // Content with no runs is not a line; rejecting it here is what keeps the emptiness from
            // surfacing inside a measurement that cannot say which line was meant.
            assertThatThrownBy(() -> LabelRuns.copyRuns(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("labelRuns");
        }

        @Test
        void copyRunsRejectsAMissingRunList() {
            assertThatThrownBy(() -> LabelRuns.copyRuns(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("labelRuns");
        }

        @Test
        void copyRunsRejectsANullRun() {
            assertThatThrownBy(() -> LabelRuns.copyRuns(Arrays.asList(
                    new TextSpan("Hegemony", RUN_COLOUR),
                    null)))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class MeasureWordSpaceWidth {

        @Test
        void measureWordSpaceWidthChargesTheFacesOwnSpace() {
            // A space is one character, so this measurer charges it one unit - the same one the walk
            // below spends between two runs. Offered for a mark set between a label and what follows it,
            // which has to stand off by the very space the label parts its own runs by.
            var wordSpaceWidth = LabelRuns.measureWordSpaceWidth(ONE_UNIT_PER_CHARACTER);

            assertThat(wordSpaceWidth)
                .isEqualTo(1f);
        }

        @Test
        void measureWordSpaceWidthAnswersWhatTheRunWalkSpends() {
            // The two must not part: what a caller stands a mark off by is read from the same span the
            // offsets walk charges between runs, so a face that spaces its words widely spaces both.
            var wideSpaceMeasurer = createSpaceHeavyMeasurer();

            var offsets = LabelRuns.measureRunOffsets(
                List.of(
                    new TextSpan("AA", RUN_COLOUR),
                    new TextSpan("BBB", OTHER_RUN_COLOUR)),
                LINE_HEIGHT,
                wideSpaceMeasurer);

            // The second run opens at 2 + the 7-wide space this face sets, which is exactly what the
            // standalone measurement answers.
            assertThat(LabelRuns.measureWordSpaceWidth(wideSpaceMeasurer))
                .isEqualTo(7f);
            assertThat(offsets.runOffsetXs())
                .containsExactly(0f, 9f);
        }
    }

    @Nested
    class MeasureRunOffsets {

        @Test
        void measureRunOffsetsAnchorsASingleRunAtTheLabelsLeftEdge() {

            var offsets = LabelRuns.measureRunOffsets(
                List.of(new TextSpan("AA", RUN_COLOUR)),
                LINE_HEIGHT,
                ONE_UNIT_PER_CHARACTER);

            assertThat(offsets.runOffsetXs())
                .containsExactly(0f);
            assertThat(offsets.runsWidth())
                .isEqualTo(2f);
        }

        @Test
        void measureRunOffsetsStartsEachRunAWordGapPastTheOneBefore() {
            // The runs read as one sentence, so the second starts past the first plus the face's own
            // space, which this measurer charges as the one character it is: 2 + 1.
            var offsets = LabelRuns.measureRunOffsets(
                List.of(
                    new TextSpan("AA", RUN_COLOUR),
                    new TextSpan("BBB", OTHER_RUN_COLOUR)),
                LINE_HEIGHT,
                ONE_UNIT_PER_CHARACTER);

            assertThat(offsets.runOffsetXs())
                .containsExactly(0f, 3f);
            assertThat(offsets.runsWidth())
                .isEqualTo(6f);
        }

        @Test
        void measureRunOffsetsChargesAnImageRunItsLineHeight() {
            // An image squares off its line, so the label is charged 20 for the crest, the face's 1-wide
            // space, and 3 for the glyphs after it.
            var offsets = LabelRuns.measureRunOffsets(
                List.of(
                    new ImageSpan(CREST_SPRITE_PATH),
                    new TextSpan("BBB", RUN_COLOUR)),
                LINE_HEIGHT,
                ONE_UNIT_PER_CHARACTER);

            assertThat(offsets.runOffsetXs())
                .containsExactly(0f, 21f);
            assertThat(offsets.runsWidth())
                .isEqualTo(24f);
        }

        @Test
        void measureRunOffsetsSpacesAnImageRunFromTheWordsBeforeIt() {
            // The image is a run like any other, so it takes the same word space a second colour would
            // rather than butting against the glyphs: 2 + 1.
            var offsets = LabelRuns.measureRunOffsets(
                List.of(
                    new TextSpan("AA", RUN_COLOUR),
                    new ImageSpan(CREST_SPRITE_PATH)),
                LINE_HEIGHT,
                ONE_UNIT_PER_CHARACTER);

            assertThat(offsets.runOffsetXs())
                .containsExactly(0f, 3f);
            assertThat(offsets.runsWidth())
                .isEqualTo(23f);
        }

        @Test
        void measureRunOffsetsChargesABlankRunNeitherGapNorWidth() {
            // A caller assembling a run from parts and coming up blank gets the line it would have had
            // without it, rather than a gap reserved in front of no glyphs.
            var offsets = LabelRuns.measureRunOffsets(
                List.of(
                    new TextSpan("AA", RUN_COLOUR),
                    TextSpan.createBlank(OTHER_RUN_COLOUR),
                    new TextSpan("BBB", OTHER_RUN_COLOUR)),
                LINE_HEIGHT,
                ONE_UNIT_PER_CHARACTER);

            assertThat(offsets.runOffsetXs())
                .containsExactly(0f, 2f, 3f);
            assertThat(offsets.runsWidth())
                .isEqualTo(6f);
        }

        @Test
        void measureRunOffsetsTakesItsWordSpaceFromTheDrawingFace() {
            // The rule the whole measurement turns on: the space parting two runs is the face's own, so a
            // face with a wide space spaces its runs widely and one with a narrow space does not. Fixed at
            // one number instead, a stack whose lines draw at several sizes would space a footnote's runs
            // as far apart as a heading's, which reads as a column break rather than as a space.
            StyledSpanMeasurer wideSpacedFace =
                textSpan -> textSpan.text().equals(" ") ? 5d : textSpan.text().length();

            var offsets = LabelRuns.measureRunOffsets(
                List.of(
                    new TextSpan("AA", RUN_COLOUR),
                    new TextSpan("BBB", OTHER_RUN_COLOUR)),
                LINE_HEIGHT,
                wideSpacedFace);

            assertThat(offsets.runOffsetXs())
                .containsExactly(0f, 7f);
            assertThat(offsets.runsWidth())
                .isEqualTo(10f);
        }

        @Test
        void measureRunOffsetsChargesNoLeadingGapWhenTheFirstRunIsBlank() {
            // The gap is charged where one drawn run follows another, so a blank opening run leaves the
            // first drawn one flush at the label's left edge.
            var offsets = LabelRuns.measureRunOffsets(
                List.of(
                    TextSpan.createBlank(RUN_COLOUR),
                    new TextSpan("BBB", OTHER_RUN_COLOUR)),
                LINE_HEIGHT,
                ONE_UNIT_PER_CHARACTER);

            assertThat(offsets.runOffsetXs())
                .containsExactly(0f, 0f);
            assertThat(offsets.runsWidth())
                .isEqualTo(3f);
        }
    }

    @Nested
    class ResolveLineText {

        @Test
        void resolveLineTextJoinsTheRunsInReadingOrder() {
            // Parted by one space, spelled here because this form has no anchors to part them with -
            // the same rule the run-by-run form spends as geometry, so a label authored once reads
            // alike whichever way a surface lays it.
            var lineText = LabelRuns.resolveLineText(List.of(
                new TextSpan("Hegemony", RUN_COLOUR),
                new TextSpan("(7)", OTHER_RUN_COLOUR)));

            assertThat(lineText)
                .isEqualTo("Hegemony (7)");
        }

        @Test
        void resolveLineTextPassesOverARunWithNothingToDraw() {
            // The space is spent between two runs that draw, so a run assembled from parts and coming up
            // blank costs the line neither a space of its own nor a doubled one around it.
            var lineText = LabelRuns.resolveLineText(List.of(
                new TextSpan("Hegemony", RUN_COLOUR),
                TextSpan.createBlank(OTHER_RUN_COLOUR),
                new TextSpan("(7)", OTHER_RUN_COLOUR)));

            assertThat(lineText)
                .isEqualTo("Hegemony (7)");
        }

        @Test
        void resolveLineTextReadsASingleRunAsItself() {
            assertThat(LabelRuns.resolveLineText(List.of(new TextSpan("Hegemony", RUN_COLOUR))))
                .isEqualTo("Hegemony");
        }

        @Test
        void resolveLineTextSpacesTheRunsAsWidelyAsTheyArePlaced() {
            // The invariant the two forms stand or fall on. They part their runs by one rule written
            // twice - as a width added between two drawn runs, and as a character appended between them -
            // so nothing but a test holds them together, and a surface that measures by one and draws by
            // the other would space its words twice or not at all. Pinned on the measurer charging a unit
            // per character, where the joined line's own length is the width the placed form measures to.
            var labelRuns = List.<LabelRun>of(
                new TextSpan("Hegemony", RUN_COLOUR),
                new TextSpan("(7)", OTHER_RUN_COLOUR),
                new TextSpan("contested", RUN_COLOUR));

            var lineText = LabelRuns.resolveLineText(labelRuns);

            // Stated as the literal too, so the two forms cannot pass by being wrong together.
            assertThat(lineText)
                .isEqualTo("Hegemony (7) contested");
            assertThat((float) lineText.length())
                .isEqualTo(LabelRuns
                    .measureRunOffsets(labelRuns, LINE_HEIGHT, ONE_UNIT_PER_CHARACTER)
                    .runsWidth());
        }

        @Test
        void resolveLineTextReadsALabelOfBlankRunsAsNothing() {
            // Every run assembled from parts and coming up empty leaves no line at all - not a string of
            // the spaces that would have parted them, which a surface would then measure and centre.
            assertThat(LabelRuns.resolveLineText(List.of(
                    TextSpan.createBlank(RUN_COLOUR),
                    new TextSpan("   ", OTHER_RUN_COLOUR))))
                .isEmpty();
        }

        @Test
        void resolveLineTextPassesOverAnImageRunBetweenTwoWords() {
            // A crest set among the words contributes neither glyphs nor a space, so the words either
            // side of it close to the single space that parts them - where a space charged per run would
            // leave the line reading as though something had been dropped out of it.
            var lineText = LabelRuns.resolveLineText(List.of(
                new TextSpan("Hegemony", RUN_COLOUR),
                new ImageSpan(CREST_SPRITE_PATH),
                new TextSpan("(7)", OTHER_RUN_COLOUR)));

            assertThat(lineText)
                .isEqualTo("Hegemony (7)");
        }

        @Test
        void resolveLineTextLeavesOutAnImageRun() {
            // The line form is glyphs only, so a surface drawing a label in one pass gets its words and
            // is charged nothing for a crest it will not be laying out run by run - not even the space
            // that would part one, which would open the line on a blank the crest was meant to fill.
            var lineText = LabelRuns.resolveLineText(List.of(
                new ImageSpan(CREST_SPRITE_PATH),
                new TextSpan("Hegemony", RUN_COLOUR)));

            assertThat(lineText)
                .isEqualTo("Hegemony");
        }
    }
}
