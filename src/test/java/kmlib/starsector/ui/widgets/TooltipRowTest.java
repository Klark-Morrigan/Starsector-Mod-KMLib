package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.text.TextSpan;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TooltipRow}'s content model: the bare row a caller starts from carries a one-run label
 * and none of the optional parts, and each refinement adds exactly its own without disturbing what the
 * row already holds. The absences are the contract worth fixing - a caller never states them, so the
 * bare row has to be the plain crest-aligned, crest-less, value-less line every refinement builds on.
 *
 * <p>Each refinement is pinned twice: once for the component it sets, and once for everything it must
 * leave alone. The second half is not redundant. A refinement rebuilds the whole row - its four
 * row-level facts and its content - so one that dropped a part, or rebuilt it from the bare row rather
 * than carrying the one it was handed, would compile and would satisfy any assertion list that did not
 * happen to name it. What the content itself accepts is {@link LabelledRow}'s own contract, pinned
 * there; what is pinned here is which part of it each refinement of a tooltip line touches.
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

    private static TooltipRow buildBareRow() {
        return TooltipRow.createRow(TEXT, Color.WHITE);
    }

    // A row with every optional part already filled, so a refinement applied to it has something real to
    // preserve. Refinements pinned against a bare row would pass while dropping parts that are absent
    // there anyway.
    private static TooltipRow buildRichRow() {
        return buildBareRow()
                .carriesCrest(CREST)
                .continuesWith(RUN_TEXT, Color.YELLOW)
                .carriesValue(VALUE, Color.GRAY)
                .indentsBy(INDENT);
    }

    @Nested
    class Constructor {
        @Test
        void constructorRejectsANullContent() {
            // A row is its content plus how the box treats it, so a row with no content at all is not a
            // row - and a null would otherwise surface inside a measurement, well past the point that
            // could say which row was meant.
            assertThatThrownBy(() -> new TooltipRow(
                    TooltipLineStyle.PARAGRAPH,
                    TooltipLabelPlacement.ALIGNED_WITH_CRESTS,
                    0f,
                    false,
                    null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("labelledRow");
        }
    }

    @Nested
    class CreateRow {
        @Test
        void createRowCarriesTheLabelAsOneRun() {
            var labelTextSpans = buildBareRow().labelledRow().labelTextSpans();

            assertThat(labelTextSpans).hasSize(1);
            assertThat(labelTextSpans.get(0).text()).isEqualTo(TEXT);
            assertThat(labelTextSpans.get(0).colour()).isEqualTo(Color.WHITE);
        }

        @Test
        void createRowCarriesNoneOfTheOptionalParts() {
            var row = buildBareRow();

            assertThat(row.indent()).isCloseTo(0f, within(TOLERANCE));
            assertThat(row.labelledRow().leadingRowSlot()).isEqualTo(RowSlot.EMPTY);
            assertThat(row.labelledRow().trailingRowSlot()).isEqualTo(RowSlot.EMPTY);
            assertThat(row.hasSectionBreak()).isFalse();
            assertThat(row.hasCrest()).isFalse();
        }

        @Test
        void createRowAlignsItsLabelWithTheCrestedRows() {
            // An ordinary content row lines up with the box's other entries, so a caller that says
            // nothing about placement gets that - starting at the content edge instead, or centring as a
            // standalone span, is each stated.
            assertThat(buildBareRow().labelPlacement())
                    .isEqualTo(TooltipLabelPlacement.ALIGNED_WITH_CRESTS);
        }

        @Test
        void createRowReadsAsAParagraph() {
            // Most of a tooltip is its body, so a caller that says nothing about the kind of line it is
            // authoring gets a body line - which is what lets a whole existing body be built without
            // naming a kind at all.
            assertThat(buildBareRow().lineStyle()).isEqualTo(TooltipLineStyle.PARAGRAPH);
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
            // so a null is a crest-less row rather than an image slot holding nothing to load.
            var row = buildBareRow().carriesCrest(null);

            assertThat(row.labelledRow().leadingRowSlot()).isEqualTo(RowSlot.EMPTY);
            assertThat(row.hasCrest()).isFalse();
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
    class HasCrest {
        @Test
        void hasCrestIsFalseForABareRow() {
            assertThat(buildBareRow().hasCrest()).isFalse();
        }

        @Test
        void hasCrestIsTrueForACrestedRow() {
            assertThat(buildBareRow().carriesCrest(CREST).hasCrest()).isTrue();
        }

        @Test
        void hasCrestIsFalseForALeadingSlotHoldingSomethingOtherThanAnImage() {
            // A crest is an image in the leading column, not merely something in it: a row leading with
            // a tick has nothing to hang in the crest square, and the draw that paints one reads the
            // slot the same way rather than trusting that a filled slot must be a crest.
            var ticked = new TooltipRow(
                    TooltipLineStyle.PARAGRAPH,
                    TooltipLabelPlacement.ALIGNED_WITH_CRESTS,
                    0f,
                    false,
                    LabelledRow.createRow(TEXT, Color.WHITE).leadsWith(new RowSlot.Tick(true)));

            assertThat(ticked.hasCrest()).isFalse();
        }
    }

    @Nested
    class CarriesValue {
        @Test
        void carriesValueTrailsTheRowWithThatRun() {
            assertThat(buildBareRow().carriesValue(VALUE, Color.GRAY).labelledRow().trailingRowSlot())
                    .isEqualTo(new RowSlot.Text(new TextSpan(VALUE, Color.GRAY)));
        }

        @Test
        void carriesValueFillsTheSlotWithABlankRunForBlankText() {
            // A caller assembling a value from parts and coming up empty said its row has a value, so
            // the slot is filled with a run that draws nothing rather than emptied - which is what a
            // row that never states a value holds. Neither is charged a column.
            var row = buildBareRow().carriesValue("", Color.GRAY);

            assertThat(row.labelledRow().trailingRowSlot())
                    .isEqualTo(new RowSlot.Text(TextSpan.createBlank(Color.GRAY)));
        }

        @Test
        void carriesValueChangesNothingElse() {
            assertRefinementChangesOnly(
                    buildRichRow().carriesValue(OTHER_VALUE, Color.CYAN),
                    buildRichRow(),
                    "labelledRow.trailingRowSlot");
        }
    }

    @Nested
    class ContinuesWith {
        @Test
        void continuesWithAppendsTheRunAndItsColour() {
            var labelTextSpans = buildBareRow()
                    .continuesWith(RUN_TEXT, Color.YELLOW)
                    .labelledRow()
                    .labelTextSpans();

            assertThat(labelTextSpans).extracting(TextSpan::text).containsExactly(TEXT, RUN_TEXT);
            assertThat(labelTextSpans.get(1).colour()).isEqualTo(Color.YELLOW);
        }

        @Test
        void continuesWithChangesNothingElse() {
            // The point of composing refinements: a run cannot restate - or lose - the tier, crest, and
            // value the row was already built with.
            assertRefinementChangesOnly(
                    buildRichRow().continuesWith(OTHER_RUN_TEXT, Color.CYAN),
                    buildRichRow(),
                    "labelledRow.labelTextSpans");
        }
    }

    @Nested
    class IndentsBy {
        @Test
        void indentsBySetsTheInset() {
            var row = buildBareRow().indentsBy(INDENT);

            assertThat(row.indent()).isCloseTo(INDENT, within(TOLERANCE));
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
    class OpensSection {
        @Test
        void opensSectionMarksTheRowAsStartingABlock() {
            var row = buildBareRow().opensSection();

            assertThat(row.hasSectionBreak()).isTrue();
        }

        @Test
        void opensSectionChangesNothingElse() {
            assertRefinementChangesOnly(
                    buildRichRow().opensSection(),
                    buildRichRow(),
                    "hasSectionBreak");
        }
    }

    @Nested
    class ClearsCrestColumn {
        @Test
        void clearsCrestColumnStartsTheLabelAtTheContentEdge() {
            var row = buildBareRow().clearsCrestColumn();

            assertThat(row.labelPlacement()).isEqualTo(TooltipLabelPlacement.AT_CONTENT_EDGE);
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
    class Centred {
        @Test
        void centredLaysTheLabelAsAStandaloneCentredSpan() {
            var row = buildBareRow().centred();

            assertThat(row.labelPlacement()).isEqualTo(TooltipLabelPlacement.CENTRED);
        }

        @Test
        void centredChangesNothingElse() {
            // Centring changes where the line sits, not what it says, so every run of the label rides
            // along rather than being dropped from the centred span.
            assertRefinementChangesOnly(
                    buildRichRow().centred(),
                    buildRichRow(),
                    "labelPlacement");
        }

        @Test
        void centredReplacesAFlushPlacementRatherThanCompoundingWithIt() {
            // One placement, so the last one stated wins: a centred span is already clear of the columns,
            // and there is no state in which a row is both an edge-flush line and a centred one.
            var row = buildBareRow().clearsCrestColumn().centred();

            assertThat(row.labelPlacement()).isEqualTo(TooltipLabelPlacement.CENTRED);
        }
    }

    @Nested
    class ReadsAs {
        @Test
        void readsAsSetsTheKindOfLine() {
            var row = buildBareRow().readsAs(TooltipLineStyle.HEADER);

            assertThat(row.lineStyle()).isEqualTo(TooltipLineStyle.HEADER);
        }

        @Test
        void readsAsChangesNothingElse() {
            // A heading is still a row: naming its kind must not disturb the content or the placement it
            // was already built with, since the kind decides only how it is drawn.
            assertRefinementChangesOnly(
                    buildRichRow().centred().readsAs(TooltipLineStyle.HEADER),
                    buildRichRow().centred(),
                    "lineStyle");
        }
    }
}
