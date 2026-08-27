package kmlib.starsector.ui.widgets.tooltip;

import kmlib.starsector.ui.text.LabelRun;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.widgets.LabelledRow;
import kmlib.starsector.ui.widgets.RowSlot;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TooltipRow}'s content model: the bare line a caller starts from carries a one-run label and
 * none of the optional parts, and each refinement adds exactly its own without disturbing what the line
 * already holds. The absences are the contract worth fixing - a caller never states them, so the bare
 * line has to be the plain crest-aligned, crest-less, value-less one every refinement builds on.
 *
 * <p>Each refinement is pinned twice: once for the component it sets, and once for everything it must
 * leave alone. The second half is not redundant. A refinement rebuilds the whole line - its row-level
 * facts and its content - so one that dropped a part, or rebuilt it from the bare line rather than
 * carrying the one it was handed, would compile and would satisfy any assertion list that did not happen
 * to name it. What the content itself accepts is {@link LabelledRow}'s own contract, pinned there; what
 * is pinned here is which part of it each refinement touches.
 *
 * <p>That a centred line carries no crest and no value is not asserted anywhere, because it cannot be
 * written: {@link TooltipRow.CentredRow} holds a label and nothing else, so the pairing has no spelling
 * for a test to reject. What is pinned instead is that each kind offers only the refinements it can
 * honour - the crest, value, indent, and gutter refinements live on {@link TooltipRow.TableRow} alone.
 */
class TooltipRowTest {

    private static final String CREST = "crest_a";
    private static final String OTHER_CREST = "crest_b";
    private static final String TEXT = "Hegemony";
    private static final String RUN_TEXT = "core territory";
    private static final String OTHER_RUN_TEXT = "contested";
    private static final String VALUE = "12";
    private static final String OTHER_VALUE = "34";
    private static final float INDENT = 14f;
    private static final float OTHER_INDENT = 28f;
    private static final float TOLERANCE = 0.001f;
    private static final int IN_THE_BOXS_VOICE = 0;
    private static final int TWO_STEPS_UNDER = 2;

    // Pins a refinement to exactly the components it names: every other component must come through
    // untouched. One comparison rather than an enumeration of survivors, because an enumeration only
    // catches a component it thought to name.
    private static void assertRefinementChangesOnly(
            TooltipRow refined,
            TooltipRow original,
            String... changedComponents) {

        assertThat(refined)
            .usingRecursiveComparison()
            .ignoringFields(changedComponents)
            .isEqualTo(original);
    }

    private static TooltipRow.TableRow buildBareRow() {
        return TooltipRow.createRow(new TextSpan(TEXT, Color.WHITE));
    }

    // A line with every optional part already filled, so a refinement applied to it has something real to
    // preserve. Refinements pinned against a bare line would pass while dropping parts that are absent
    // there anyway.
    private static TooltipRow.TableRow buildRichRow() {
        return buildBareRow()
            .carriesCrest(CREST)
            .continuesWith(new TextSpan(RUN_TEXT, Color.YELLOW))
            .carriesValue(new TextSpan(VALUE, Color.GRAY))
            .indentsBy(INDENT);
    }

    private static TooltipRow.CentredRow buildBareCentredRow() {
        return TooltipRow.createCentredRow(new TextSpan(TEXT, Color.WHITE));
    }

    // A centred line carrying everything one can carry - a label of more than one run - so its own
    // refinements have something to preserve.
    private static TooltipRow.CentredRow buildRichCentredRow() {
        return buildBareCentredRow()
            .continuesWith(new TextSpan(RUN_TEXT, Color.YELLOW));
    }

    @Nested
    class Constructor {

        @Test
        void constructorRejectsATableRowWithNullContent() {
            // A line is its content plus how the box treats it, so a line with no content at all is not a
            // line - and a null would otherwise surface inside a measurement, well past the point that
            // could say which line was meant.
            assertThatThrownBy(() -> new TooltipRow.TableRow(
                    TooltipLineStyle.PARAGRAPH,
                    TooltipLabelPlacement.ALIGNED_WITH_CRESTS,
                    0f,
                    0,
                    null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("labelledRow");
        }

        @Test
        void constructorRejectsACentredRowWithNoLabelRuns() {
            // A centred line holds its label directly rather than through a labelled row, so the floor
            // that a line has a label has to hold here too - it is the same floor, not a second one.
            assertThatThrownBy(() -> new TooltipRow.CentredRow(
                    TooltipLineStyle.PARAGRAPH,
                    List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("labelRuns");
        }

        @Test
        void constructorCopiesACentredRowsLabelRuns() {
            // The line is a value, so a caller still holding the list it built must not be able to add a
            // run to a line already handed to a layout - which would size a box for runs and then draw
            // another. A centred line holds its runs directly, so it needs the copy a labelled row makes.
            var labelRuns = new ArrayList<LabelRun>();
            labelRuns.add(new TextSpan(TEXT, Color.WHITE));

            var centredRow = new TooltipRow.CentredRow(TooltipLineStyle.PARAGRAPH, labelRuns);

            labelRuns.add(new TextSpan(RUN_TEXT, Color.YELLOW));

            assertThat(centredRow.labelRuns())
                .hasSize(1);
        }

        @Test
        void constructorRejectsACentredRowWithANullLabel() {
            assertThatThrownBy(() -> new TooltipRow.CentredRow(
                    TooltipLineStyle.PARAGRAPH,
                    null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("labelRuns");
        }
    }

    @Nested
    class CreateRow {

        @Test
        void createRowCarriesTheLabelAsOneRun() {
            assertThat(buildBareRow().labelRuns())
                .containsExactly(new TextSpan(TEXT, Color.WHITE));
        }

        @Test
        void createRowCarriesNoneOfTheOptionalParts() {

            var row = buildBareRow();

            assertThat(row.indent())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(row.labelledRow().leadingRowSlot())
                .isEqualTo(RowSlot.EMPTY);
            assertThat(row.labelledRow().trailingRowSlot())
                .isEqualTo(RowSlot.EMPTY);
        }

        @Test
        void createRowAlignsItsLabelWithTheCrestedRows() {
            // An ordinary content line lines up with the box's other entries, so a caller that says
            // nothing about placement gets that - starting at the content edge instead is stated.
            assertThat(buildBareRow().labelPlacement())
                .isEqualTo(TooltipLabelPlacement.ALIGNED_WITH_CRESTS);
        }

        @Test
        void createRowReadsAsAParagraph() {
            // Most of a tooltip is its body, so a caller that says nothing about the kind of line it is
            // authoring gets a body line - which is what lets a whole existing body be built without
            // naming a kind at all.
            assertThat(buildBareRow().lineStyle())
                .isEqualTo(TooltipLineStyle.PARAGRAPH);
        }
    }

    @Nested
    class CreateCentredRow {

        @Test
        void createCentredRowCarriesTheLabelAsOneRun() {
            assertThat(buildBareCentredRow().labelRuns())
                .containsExactly(new TextSpan(TEXT, Color.WHITE));
        }

        @Test
        void createCentredRowReadsAsAParagraph() {
            // The same bare state a table row starts from, for the one look-facing fact the two kinds
            // share: a caller states a heading, and gets a body line by saying nothing.
            assertThat(buildBareCentredRow().lineStyle())
                .isEqualTo(TooltipLineStyle.PARAGRAPH);
        }
    }

    @Nested
    class CarriesCrest {
        @Test
        void carriesCrestLeadsTheRowWithThatImage() {
            assertThat(buildBareRow().carriesCrest(CREST).labelledRow().leadingRowSlot())
                .isEqualTo(new RowSlot.Image(CREST));
        }

        @Test
        void carriesCrestLeavesTheSlotUnfilledForAnAbsentPath() {
            // A caller resolving a crest a faction may simply not have hands the result straight over,
            // so a null is a crest-less line rather than an image slot holding nothing to load.
            assertThat(buildBareRow().carriesCrest(null).labelledRow().leadingRowSlot())
                .isEqualTo(RowSlot.EMPTY);
        }

        @Test
        void carriesCrestChangesNothingElse() {
            assertRefinementChangesOnly(
                buildRichRow().carriesCrest(OTHER_CREST),
                buildRichRow(),
                "labelledRow.leadingRowSlot");
        }
    }

    @Nested
    class CarriesValue {

        @Test
        void carriesValueTrailsTheRowWithThatRun() {

            var valueTextSpan = new TextSpan(VALUE, Color.GRAY);

            assertThat(buildBareRow().carriesValue(valueTextSpan).labelledRow().trailingRowSlot())
                .isEqualTo(new RowSlot.Text(valueTextSpan));
        }

        @Test
        void carriesValueFillsTheSlotWithABlankRunForABlankSpan() {
            // A caller assembling a value from parts and coming up empty said its line has a value, so
            // the slot is filled with a run that draws nothing rather than emptied - which is what a
            // line that never states a value holds. Neither is charged a column.
            var row = buildBareRow().carriesValue(TextSpan.createBlank(Color.GRAY));

            assertThat(row.labelledRow().trailingRowSlot())
                .isEqualTo(new RowSlot.Text(TextSpan.createBlank(Color.GRAY)));
        }

        @Test
        void carriesValueChangesNothingElse() {
            assertRefinementChangesOnly(
                buildRichRow().carriesValue(new TextSpan(OTHER_VALUE, Color.CYAN)),
                buildRichRow(),
                "labelledRow.trailingRowSlot");
        }
    }

    @Nested
    class CarriesValueRuns {

        @Test
        void carriesValueRunsTrailsTheRowWithThoseRuns() {
            // The runs fill the one trailing column together, so a value stating a finding and the
            // working behind it still aligns with the plain values of the rows around it.
            var valueTextSpans = List.of(
                new TextSpan(VALUE, Color.GRAY),
                new TextSpan(OTHER_VALUE, Color.CYAN));

            assertThat(buildBareRow().carriesValueRuns(valueTextSpans).labelledRow().trailingRowSlot())
                .isEqualTo(new RowSlot.TextRuns(valueTextSpans));
        }

        @Test
        void carriesValueRunsReplacesAValueAlreadyCarried() {
            // The two refinements fill one column, so a line cannot end up holding a single-run value
            // and a multi-run one at once - the last one stated is what the row carries.
            var row = buildBareRow()
                .carriesValue(new TextSpan(VALUE, Color.GRAY))
                .carriesValueRuns(List.of(new TextSpan(OTHER_VALUE, Color.CYAN)));

            assertThat(row.labelledRow().trailingRowSlot())
                .isEqualTo(new RowSlot.TextRuns(List.of(new TextSpan(OTHER_VALUE, Color.CYAN))));
        }

        @Test
        void carriesValueRunsChangesNothingElse() {
            assertRefinementChangesOnly(
                buildRichRow().carriesValueRuns(List.of(new TextSpan(OTHER_VALUE, Color.CYAN))),
                buildRichRow(),
                "labelledRow.trailingRowSlot");
        }
    }

    @Nested
    class ContinuesWith {

        @Test
        void continuesWithAppendsTheRun() {
            assertThat(buildBareRow()
                    .continuesWith(new TextSpan(RUN_TEXT, Color.YELLOW))
                    .labelRuns())
                .containsExactly(
                    new TextSpan(TEXT, Color.WHITE),
                    new TextSpan(RUN_TEXT, Color.YELLOW));
        }

        @Test
        void continuesWithAppendsTheRunOnACentredRow() {
            // A centred line reads in more than one colour exactly as a table row does: the runs are the
            // label, and the label is the one thing the two kinds carry alike.
            assertThat(buildBareCentredRow()
                    .continuesWith(new TextSpan(RUN_TEXT, Color.YELLOW))
                    .labelRuns())
                .containsExactly(
                    new TextSpan(TEXT, Color.WHITE),
                    new TextSpan(RUN_TEXT, Color.YELLOW));
        }

        @Test
        void continuesWithChangesNothingElse() {
            // The point of composing refinements: a run cannot restate - or lose - the tier, crest, and
            // value the line was already built with.
            assertRefinementChangesOnly(
                buildRichRow().continuesWith(new TextSpan(OTHER_RUN_TEXT, Color.CYAN)),
                buildRichRow(),
                "labelledRow.labelRuns");
        }

        @Test
        void continuesWithChangesNothingElseOnACentredRow() {
            assertRefinementChangesOnly(
                buildRichCentredRow().continuesWith(new TextSpan(OTHER_RUN_TEXT, Color.CYAN)),
                buildRichCentredRow(),
                "labelRuns");
        }
    }

    @Nested
    class IndentsBy {

        @Test
        void indentsBySetsTheInset() {

            var row = buildBareRow().indentsBy(INDENT);

            assertThat(row.indent())
                .isCloseTo(INDENT, within(TOLERANCE));
        }

        @Test
        void indentsByChangesNothingElse() {
            assertRefinementChangesOnly(
                buildRichRow().indentsBy(OTHER_INDENT),
                buildRichRow(),
                "indent");
        }
    }

    @Nested
    class ClearsCrestColumn {

        @Test
        void clearsCrestColumnStartsTheLabelAtTheContentEdge() {

            var row = buildBareRow().clearsCrestColumn();

            assertThat(row.labelPlacement())
                .isEqualTo(TooltipLabelPlacement.AT_CONTENT_EDGE);
        }

        @Test
        void clearsCrestColumnChangesNothingElse() {
            // Only the crest gutter is cleared. A title still carries a trailing value the way the rows
            // it heads do, so the value column is not part of what the placement decides.
            assertRefinementChangesOnly(
                buildRichRow().clearsCrestColumn(),
                buildRichRow(),
                "labelPlacement");
        }
    }

    @Nested
    class ReadsAs {

        @Test
        void readsAsSetsTheKindOfLine() {

            var row = buildBareRow().readsAs(TooltipLineStyle.HEADER);

            assertThat(row.lineStyle())
                .isEqualTo(TooltipLineStyle.HEADER);
        }

        @Test
        void readsAsSetsTheKindOfLineOnACentredRow() {

            var centredRow = buildBareCentredRow().readsAs(TooltipLineStyle.HEADER);

            assertThat(centredRow.lineStyle())
                .isEqualTo(TooltipLineStyle.HEADER);
        }

        @Test
        void readsAsChangesNothingElse() {
            // A heading is still a line: naming its kind must not disturb the content or the placement it
            // was already built with, since the kind decides only how it is drawn.
            assertRefinementChangesOnly(
                buildRichRow().readsAs(TooltipLineStyle.HEADER),
                buildRichRow(),
                "lineStyle");
        }

        @Test
        void readsAsChangesNothingElseOnACentredRow() {
            assertRefinementChangesOnly(
                buildRichCentredRow().readsAs(TooltipLineStyle.HEADER),
                buildRichCentredRow(),
                "lineStyle");
        }
    }

    @Nested
    class SubordinationLevel {

        @Test
        void subordinationLevelAnswersTheStepsATableRowWasPutUnder() {
            assertThat(buildBareRow().subordinatedAt(TWO_STEPS_UNDER).subordinationLevel())
                .isEqualTo(TWO_STEPS_UNDER);
        }

        @Test
        void subordinationLevelAnswersTheBoxsOwnVoiceForACentredRow() {
            // A centred line has left the table and stands under nothing in it, however deep the table
            // beside it goes - asked of the interface, so a measurer and a renderer resolving its look
            // cannot each answer the question their own way.
            assertThat(buildBareCentredRow().subordinationLevel())
                .isEqualTo(IN_THE_BOXS_VOICE);
        }

        @Test
        void subordinationLevelAnswersTheBoxsOwnVoiceForAnUnplacedTableRow() {
            assertThat(buildBareRow().subordinationLevel())
                .isEqualTo(IN_THE_BOXS_VOICE);
        }
    }
}
