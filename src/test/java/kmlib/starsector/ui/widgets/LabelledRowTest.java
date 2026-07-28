package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.text.TextSpan;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the shared row content: the bare content a caller starts from is a one-run label with neither
 * flank filled, each refinement adds exactly its own without disturbing what is already there, and the
 * label is a floor the constructor refuses to go below. The absences are the contract worth fixing - a
 * caller never states them, so the bare content has to be what every refinement builds on.
 */
class LabelledRowTest {
    private static final String TEXT = "Hegemony";
    private static final String RUN_TEXT = "core territory";
    private static final String OTHER_RUN_TEXT = "contested";
    private static final String CREST = "crest_a";
    private static final String VALUE = "12";

    private static final TextSpan BLANK_SPAN = TextSpan.createBlank(Color.WHITE);
    private static final RowSlot CREST_SLOT = new RowSlot.Image(CREST);
    private static final RowSlot VALUE_SLOT = new RowSlot.Text(new TextSpan(VALUE, Color.GRAY));

    // Pins a refinement to exactly the components it names: every other component must come through
    // untouched. One comparison rather than an enumeration of survivors, because an enumeration only
    // catches a component it thought to name.
    private static void assertRefinementChangesOnly(
            LabelledRow refined,
            LabelledRow original,
            String... changedComponents) {

        assertThat(refined)
                .usingRecursiveComparison()
                .ignoringFields(changedComponents)
                .isEqualTo(original);
    }

    private static LabelledRow buildBareRow() {
        return LabelledRow.createRow(TEXT, Color.WHITE);
    }

    // Content with both flanks filled and a second run on the label, so a refinement applied to it has
    // something real to preserve. Refinements pinned against bare content would pass while dropping
    // parts that are absent there anyway.
    private static LabelledRow buildRichRow() {
        return buildBareRow()
                .leadsWith(CREST_SLOT)
                .continuesWith(RUN_TEXT, Color.YELLOW)
                .trailsWith(VALUE_SLOT);
    }

    @Nested
    class Constructor {
        @Test
        void constructorRejectsANullLabelRunList() {
            assertThatThrownBy(() -> new LabelledRow(RowSlot.EMPTY, null, RowSlot.EMPTY))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("labelTextSpans");
        }

        @Test
        void constructorRejectsALabelWithNoRuns() {
            // A row is a label with things around it, so a label of no runs is not a row at all - the
            // floor that stops the model dissolving into a bag of optional parts with no centre.
            assertThatThrownBy(() -> new LabelledRow(RowSlot.EMPTY, List.of(), RowSlot.EMPTY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("labelTextSpans");
        }

        @Test
        void constructorRejectsANullLabelRun() {
            // An unfilled run is a blank span, not a missing one, so a null here is a caller that meant
            // the blank and reached for the absence instead - caught while it is still on the stack
            // rather than inside the measurement that asks each run whether it has text.
            assertThatThrownBy(() -> new LabelledRow(
                    RowSlot.EMPTY,
                    Arrays.asList(BLANK_SPAN, null),
                    RowSlot.EMPTY))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void constructorRejectsANullLeadingSlot() {
            // A row that leads with nothing holds RowSlot.EMPTY, so a null would be a second spelling of
            // an absence the slot set already spells once.
            assertThatThrownBy(() -> new LabelledRow(null, List.of(BLANK_SPAN), RowSlot.EMPTY))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("leadingRowSlot");
        }

        @Test
        void constructorRejectsANullTrailingSlot() {
            assertThatThrownBy(() -> new LabelledRow(RowSlot.EMPTY, List.of(BLANK_SPAN), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("trailingRowSlot");
        }

        @Test
        void constructorCopiesTheLabelRuns() {
            // The content is a value, so a caller still holding the list it built must not be able to
            // add a run to content already handed to a layout - which would size a box for runs and
            // then draw another.
            var labelTextSpans = new ArrayList<TextSpan>();
            labelTextSpans.add(new TextSpan(TEXT, Color.WHITE));
            var labelledRow = new LabelledRow(RowSlot.EMPTY, labelTextSpans, RowSlot.EMPTY);

            labelTextSpans.add(new TextSpan(RUN_TEXT, Color.YELLOW));

            assertThat(labelledRow.labelTextSpans()).hasSize(1);
        }
    }

    @Nested
    class CreateRow {
        @Test
        void createRowCarriesTheLabelAsOneRun() {
            var labelledRow = buildBareRow();

            assertThat(labelledRow.labelTextSpans()).hasSize(1);
            assertThat(labelledRow.labelTextSpans().get(0).text()).isEqualTo(TEXT);
            assertThat(labelledRow.labelTextSpans().get(0).colour()).isEqualTo(Color.WHITE);
        }

        @Test
        void createRowLeavesBothFlanksUnfilled() {
            // A caller states what its row has, so the bare content is a label and nothing else - and
            // the unfilled flanks are slots rather than nulls, so whatever measures them needs no branch.
            var labelledRow = buildBareRow();

            assertThat(labelledRow.leadingRowSlot()).isEqualTo(RowSlot.EMPTY);
            assertThat(labelledRow.trailingRowSlot()).isEqualTo(RowSlot.EMPTY);
        }
    }

    @Nested
    class LeadsWith {
        @Test
        void leadsWithSetsTheLeadingSlot() {
            assertThat(buildBareRow().leadsWith(CREST_SLOT).leadingRowSlot()).isEqualTo(CREST_SLOT);
        }

        @Test
        void leadsWithChangesNothingElse() {
            assertRefinementChangesOnly(
                    buildRichRow().leadsWith(new RowSlot.Tick(true)),
                    buildRichRow(),
                    "leadingRowSlot");
        }
    }

    @Nested
    class ContinuesWith {
        @Test
        void continuesWithAppendsTheRunAndItsColour() {
            var labelledRow = buildBareRow().continuesWith(RUN_TEXT, Color.YELLOW);

            assertThat(labelledRow.labelTextSpans()).hasSize(2);
            assertThat(labelledRow.labelTextSpans().get(1).text()).isEqualTo(RUN_TEXT);
            assertThat(labelledRow.labelTextSpans().get(1).colour()).isEqualTo(Color.YELLOW);
        }

        @Test
        void continuesWithKeepsTheRunsAlreadyOnTheLabel() {
            // The label is a sentence, so a run is added to what is there rather than replacing it -
            // otherwise a second colour would cost the caller the first.
            var labelledRow = buildBareRow().continuesWith(RUN_TEXT, Color.YELLOW);

            assertThat(labelledRow.labelTextSpans().get(0).text()).isEqualTo(TEXT);
            assertThat(labelledRow.labelTextSpans().get(0).colour()).isEqualTo(Color.WHITE);
        }

        @Test
        void continuesWithAppliedTwiceLaysThreeRunsInOrder() {
            // The point of runs over a fixed second slot: a third colour on one line costs the model
            // nothing, and the runs stay in the order they were written.
            var labelledRow = buildBareRow()
                    .continuesWith(RUN_TEXT, Color.YELLOW)
                    .continuesWith(OTHER_RUN_TEXT, Color.CYAN);

            assertThat(labelledRow.labelTextSpans())
                    .extracting(TextSpan::text)
                    .containsExactly(TEXT, RUN_TEXT, OTHER_RUN_TEXT);
        }

        @Test
        void continuesWithChangesNothingElse() {
            assertRefinementChangesOnly(
                    buildRichRow().continuesWith(OTHER_RUN_TEXT, Color.CYAN),
                    buildRichRow(),
                    "labelTextSpans");
        }
    }

    @Nested
    class TrailsWith {
        @Test
        void trailsWithSetsTheTrailingSlot() {
            assertThat(buildBareRow().trailsWith(VALUE_SLOT).trailingRowSlot()).isEqualTo(VALUE_SLOT);
        }

        @Test
        void trailsWithChangesNothingElse() {
            assertRefinementChangesOnly(
                    buildRichRow().trailsWith(RowSlot.EMPTY),
                    buildRichRow(),
                    "trailingRowSlot");
        }
    }
}
