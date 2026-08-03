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
 * placement that disagreed with the width its host was sized to is drift no draw call could catch.
 */
class LabelRunsTest {

    private static final Color RUN_COLOUR = new Color(200, 150, 50);
    private static final Color OTHER_RUN_COLOUR = new Color(50, 150, 200);

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
        void appendRunLeavesTheCallersRunsAlone() {
            // A refinement hands its own list in and keeps holding it, so appending must not rewrite the
            // label of the value the caller started from.
            var callerRuns = new ArrayList<TextSpan>();
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

            var callerRuns = new ArrayList<TextSpan>();
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
                .hasMessageContaining("labelTextSpans");
        }

        @Test
        void copyRunsRejectsAMissingRunList() {
            assertThatThrownBy(() -> LabelRuns.copyRuns(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("labelTextSpans");
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
                ONE_UNIT_PER_CHARACTER);

            assertThat(offsets.runOffsetXs())
                .containsExactly(0f, 8f);
            assertThat(offsets.runsWidth())
                .isEqualTo(11f);
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
    }
}
