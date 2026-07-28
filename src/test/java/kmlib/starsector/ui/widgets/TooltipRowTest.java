package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.text.TextSpan;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TooltipRow}'s content model: the bare row a caller starts from carries none of the
 * optional parts, and each refinement adds exactly its own without disturbing what the row already
 * holds. The absences are the contract worth fixing - a caller never states them, so the bare row has
 * to be the plain crest-aligned, crest-less, marker-less, value-less line every refinement builds on.
 *
 * <p>Each refinement is pinned twice: once for the component it sets, and once for everything it must
 * leave alone. The second half is not redundant. A refinement rebuilds all eight components
 * positionally, three of which are same-typed neighbouring spans, so a refinement that wrote a value
 * into the marker slot would compile and would satisfy any assertion list that did not happen to name
 * both slots.
 */
class TooltipRowTest {
    private static final String CREST = "crest_a";
    private static final String OTHER_CREST = "crest_b";
    private static final String TEXT = "Hegemony";
    private static final String MARKER = "core territory";
    private static final String OTHER_MARKER = "contested";
    private static final String VALUE = "12";
    private static final String OTHER_VALUE = "34";
    private static final float INDENT = 14f;
    private static final float OTHER_INDENT = 28f;
    private static final float TOLERANCE = 0.001f;

    private static final TextSpan BLANK_SPAN = TextSpan.createBlank(Color.WHITE);

    // Pins a refinement to exactly the components it names: every other component must come through
    // untouched. One comparison rather than an enumeration of survivors, because an enumeration only
    // catches a component it thought to name - and the components most likely to be crossed are the
    // three spans, which an enumeration written around the refinement under test tends to skip.
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
                .carriesMarker(MARKER, Color.YELLOW)
                .carriesValue(VALUE, Color.GRAY)
                .indentsBy(INDENT);
    }

    // The canonical constructor reached by its spans alone. The row-level facts have no say in what it
    // rejects, and spelling all eight components per case would bury the one component under test.
    private static TooltipRow createRowWithSpans(
            TextSpan labelTextSpan,
            TextSpan markerTextSpan,
            TextSpan valueTextSpan) {

        return new TooltipRow(
                TooltipLineStyle.PARAGRAPH,
                TooltipLabelPlacement.ALIGNED_WITH_CRESTS,
                0f,
                false,
                null,
                labelTextSpan,
                markerTextSpan,
                valueTextSpan);
    }

    @Nested
    class Constructor {
        @Test
        void constructorRejectsANullLabelSpan() {
            assertThatThrownBy(() -> createRowWithSpans(null, BLANK_SPAN, BLANK_SPAN))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("labelTextSpan");
        }

        @Test
        void constructorRejectsANullMarkerSpan() {
            // An unfilled marker is a blank span, not a missing one, so a null here is a caller that
            // meant the blank and reached for the absence instead - caught while it is still on the
            // stack rather than inside the measurement that asks the marker whether it has text.
            assertThatThrownBy(() -> createRowWithSpans(BLANK_SPAN, null, BLANK_SPAN))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("markerTextSpan");
        }

        @Test
        void constructorRejectsANullValueSpan() {
            assertThatThrownBy(() -> createRowWithSpans(BLANK_SPAN, BLANK_SPAN, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("valueTextSpan");
        }

        @Test
        void constructorAcceptsAnAbsentCrestPath() {
            // The crest is the one genuinely optional part, so null stays its spelling of absence and
            // must not be swept up by the span checks beside it.
            assertThat(createRowWithSpans(BLANK_SPAN, BLANK_SPAN, BLANK_SPAN).hasCrest()).isFalse();
        }
    }

    @Nested
    class CreateRow {
        @Test
        void createRowCarriesTheLabelAndItsColour() {
            var row = buildBareRow();

            assertThat(row.labelTextSpan().text()).isEqualTo(TEXT);
            assertThat(row.labelTextSpan().colour()).isEqualTo(Color.WHITE);
        }

        @Test
        void createRowCarriesNoneOfTheOptionalParts() {
            var row = buildBareRow();

            assertThat(row.indent()).isCloseTo(0f, within(TOLERANCE));
            assertThat(row.crestSpritePath()).isNull();
            assertThat(row.markerTextSpan().hasText()).isFalse();
            assertThat(row.valueTextSpan().hasText()).isFalse();
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

        @Test
        void createRowColoursTheAbsentPartsWithTheLabel() {
            // Nothing draws in these colours on a bare row, but the spans must still carry one: a
            // refinement that sets only one part leaves the others to be measured, styled, and drawn by
            // the same path regardless, and that path reads a colour off every span it is handed.
            var row = buildBareRow();

            assertThat(row.markerTextSpan().colour()).isEqualTo(Color.WHITE);
            assertThat(row.valueTextSpan().colour()).isEqualTo(Color.WHITE);
        }
    }

    @Nested
    class CarriesCrest {
        @Test
        void carriesCrestSetsThePath() {
            assertThat(buildBareRow().carriesCrest(CREST).crestSpritePath()).isEqualTo(CREST);
        }

        @Test
        void carriesCrestChangesNothingElse() {
            assertRefinementChangesOnly(
                    buildRichRow().carriesCrest(OTHER_CREST),
                    buildRichRow(),
                    "crestSpritePath");
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
    }

    @Nested
    class CarriesValue {
        @Test
        void carriesValueSetsTheValueAndItsColour() {
            var row = buildBareRow().carriesValue(VALUE, Color.GRAY);

            assertThat(row.valueTextSpan().text()).isEqualTo(VALUE);
            assertThat(row.valueTextSpan().colour()).isEqualTo(Color.GRAY);
        }

        @Test
        void carriesValueChangesNothingElse() {
            assertRefinementChangesOnly(
                    buildRichRow().carriesValue(OTHER_VALUE, Color.CYAN),
                    buildRichRow(),
                    "valueTextSpan");
        }
    }

    @Nested
    class CarriesMarker {
        @Test
        void carriesMarkerSetsTheMarkerAndItsColour() {
            var row = buildBareRow().carriesMarker(MARKER, Color.YELLOW);

            assertThat(row.markerTextSpan().text()).isEqualTo(MARKER);
            assertThat(row.markerTextSpan().colour()).isEqualTo(Color.YELLOW);
        }

        @Test
        void carriesMarkerChangesNothingElse() {
            // The point of composing refinements: a marker cannot restate - or lose - the tier, crest,
            // and value the row was already built with.
            assertRefinementChangesOnly(
                    buildRichRow().carriesMarker(OTHER_MARKER, Color.CYAN),
                    buildRichRow(),
                    "markerTextSpan");
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
            // Centring changes where the line sits, not what it says, so the marker it may carry rides
            // along with the label rather than being dropped from the centred span.
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

    @Nested
    class HasMarker {
        @Test
        void hasMarkerIsFalseForABareRow() {
            assertThat(buildBareRow().hasMarker()).isFalse();
        }

        @Test
        void hasMarkerIsTrueForAMarkedRow() {
            assertThat(buildBareRow().carriesMarker(MARKER, Color.YELLOW).hasMarker()).isTrue();
        }

        @Test
        void hasMarkerIsFalseForABlankMarker() {
            // A caller that assembles a marker from parts and comes up with whitespace gets the
            // unmarked row it meant, rather than a gap reserved before nothing.
            assertThat(buildBareRow().carriesMarker(" ", Color.YELLOW).hasMarker()).isFalse();
        }
    }
}
