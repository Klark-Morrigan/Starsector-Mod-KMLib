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
 * <p>The offsets are the contract worth fixing hardest. A run starting a word gap past the one before it
 * and a blank run being charged nothing are what make two surfaces lay the same label identically - and a
 * placement that disagreed with the width its host was sized to is drift no draw call could catch. An
 * image run is held to the same walk, since a crest set among a line's words is spaced and anchored by
 * the sentence rather than by a column.
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

    @Nested
    class AppendRun {

        @Test
        void appendRunPutsTheRunLast() {

            var runs = LabelRuns.appendRun(
                List.of(new TextSpan("Hegemony", RUN_COLOUR)),
                new TextSpan(" (7)", OTHER_RUN_COLOUR));

            assertThat(runs)
                .containsExactly(
                    new TextSpan("Hegemony", RUN_COLOUR),
                    new TextSpan(" (7)", OTHER_RUN_COLOUR));
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

            LabelRuns.appendRun(callerRuns, new TextSpan(" (7)", OTHER_RUN_COLOUR));

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
                new TextSpan(" (7)", OTHER_RUN_COLOUR)));

            assertThat(runs)
                .containsExactly(
                    new TextSpan("Hegemony", RUN_COLOUR),
                    new TextSpan(" (7)", OTHER_RUN_COLOUR));
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
            // The runs read as one sentence, so the second starts past the first plus the gap: 2 + 6.
            var offsets = LabelRuns.measureRunOffsets(
                List.of(
                    new TextSpan("AA", RUN_COLOUR),
                    new TextSpan("BBB", OTHER_RUN_COLOUR)),
                LINE_HEIGHT,
                ONE_UNIT_PER_CHARACTER);

            assertThat(offsets.runOffsetXs())
                .containsExactly(0f, 8f);
            assertThat(offsets.runsWidth())
                .isEqualTo(11f);
        }

        @Test
        void measureRunOffsetsChargesAnImageRunItsLineHeight() {
            // An image squares off its line, so the label is charged 20 for the crest, the 6-unit word
            // gap, and 3 for the glyphs after it.
            var offsets = LabelRuns.measureRunOffsets(
                List.of(
                    new ImageSpan(CREST_SPRITE_PATH),
                    new TextSpan("BBB", RUN_COLOUR)),
                LINE_HEIGHT,
                ONE_UNIT_PER_CHARACTER);

            assertThat(offsets.runOffsetXs())
                .containsExactly(0f, 26f);
            assertThat(offsets.runsWidth())
                .isEqualTo(29f);
        }

        @Test
        void measureRunOffsetsSpacesAnImageRunFromTheWordsBeforeIt() {
            // The image is a run like any other, so it takes the same word gap a second colour would
            // rather than butting against the glyphs: 2 + 6.
            var offsets = LabelRuns.measureRunOffsets(
                List.of(
                    new TextSpan("AA", RUN_COLOUR),
                    new ImageSpan(CREST_SPRITE_PATH)),
                LINE_HEIGHT,
                ONE_UNIT_PER_CHARACTER);

            assertThat(offsets.runOffsetXs())
                .containsExactly(0f, 8f);
            assertThat(offsets.runsWidth())
                .isEqualTo(28f);
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
                .containsExactly(0f, 2f, 8f);
            assertThat(offsets.runsWidth())
                .isEqualTo(11f);
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

            var lineText = LabelRuns.resolveLineText(List.of(
                new TextSpan("Hegemony", RUN_COLOUR),
                new TextSpan(" (7)", OTHER_RUN_COLOUR)));

            assertThat(lineText)
                .isEqualTo("Hegemony (7)");
        }

        @Test
        void resolveLineTextReadsASingleRunAsItself() {
            assertThat(LabelRuns.resolveLineText(List.of(new TextSpan("Hegemony", RUN_COLOUR))))
                .isEqualTo("Hegemony");
        }

        @Test
        void resolveLineTextLeavesOutAnImageRun() {
            // The line form is glyphs only, so a surface drawing a label in one pass gets its words and
            // is charged nothing for a crest it will not be laying out run by run.
            var lineText = LabelRuns.resolveLineText(List.of(
                new ImageSpan(CREST_SPRITE_PATH),
                new TextSpan("Hegemony", RUN_COLOUR)));

            assertThat(lineText)
                .isEqualTo("Hegemony");
        }
    }
}
