package kmlib.starsector.ui.widgets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TooltipRow}'s content model: the bare row a caller starts from carries none of the
 * optional parts, and each refinement adds exactly its own without disturbing what the row already
 * holds. The absences are the contract worth fixing - a caller never states them, so the bare row has
 * to be the plain crest-aligned, crest-less, marker-less, value-less line every refinement builds on.
 */
class TooltipRowTest {
    private static final String CREST = "crest_a";
    private static final String TEXT = "Hegemony";
    private static final String MARKER = "core territory";
    private static final String VALUE = "12";
    private static final float INDENT = 14f;
    private static final float TOLERANCE = 0.001f;

    private static TooltipRow bareRow() {
        return TooltipRow.createRow(TEXT, Color.WHITE);
    }

    @Nested
    class CreateRow {
        @Test
        void createRowCarriesTheLabelAndItsColour() {
            var row = bareRow();

            assertThat(row.labelTextSpan().text()).isEqualTo(TEXT);
            assertThat(row.labelTextSpan().colour()).isEqualTo(Color.WHITE);
        }

        @Test
        void createRowCarriesNoneOfTheOptionalParts() {
            var row = bareRow();

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
            assertThat(bareRow().labelPlacement()).isEqualTo(TooltipLabelPlacement.ALIGNED_WITH_CRESTS);
        }

        @Test
        void createRowReadsAsAParagraph() {
            // Most of a tooltip is its body, so a caller that says nothing about the kind of line it is
            // authoring gets a body line - which is what lets a whole existing body be built without
            // naming a kind at all.
            assertThat(bareRow().lineStyle()).isEqualTo(TooltipLineStyle.PARAGRAPH);
        }

        @Test
        void createRowColoursTheAbsentPartsWithTheLabel() {
            // Nothing draws in these colours on a bare row, but the spans must still carry one: a
            // refinement that sets only one part leaves the others to be measured, styled, and drawn by
            // the same path regardless, and that path reads a colour off every span it is handed.
            var row = bareRow();

            assertThat(row.markerTextSpan().colour()).isEqualTo(Color.WHITE);
            assertThat(row.valueTextSpan().colour()).isEqualTo(Color.WHITE);
        }
    }

    @Nested
    class CarriesCrest {
        @Test
        void carriesCrestSetsThePathAndKeepsTheRest() {
            var row = bareRow().carriesCrest(CREST);

            assertThat(row.crestSpritePath()).isEqualTo(CREST);
            assertThat(row.labelTextSpan().text()).isEqualTo(TEXT);
            assertThat(row.valueTextSpan().hasText()).isFalse();
        }
    }

    @Nested
    class HasCrest {
        @Test
        void hasCrestIsFalseForABareRow() {
            assertThat(bareRow().hasCrest()).isFalse();
        }

        @Test
        void hasCrestIsTrueForACrestedRow() {
            assertThat(bareRow().carriesCrest(CREST).hasCrest()).isTrue();
        }
    }

    @Nested
    class CarriesValue {
        @Test
        void carriesValueSetsTheValueAndItsColour() {
            var row = bareRow().carriesValue(VALUE, Color.GRAY);

            assertThat(row.valueTextSpan().text()).isEqualTo(VALUE);
            assertThat(row.valueTextSpan().colour()).isEqualTo(Color.GRAY);
        }
    }

    @Nested
    class CarriesMarker {
        @Test
        void carriesMarkerSetsTheMarkerAndItsColour() {
            var row = bareRow().carriesMarker(MARKER, Color.YELLOW);

            assertThat(row.markerTextSpan().text()).isEqualTo(MARKER);
            assertThat(row.markerTextSpan().colour()).isEqualTo(Color.YELLOW);
        }

        @Test
        void carriesMarkerKeepsTheRestOfTheRow() {
            // The point of composing refinements: a marker cannot restate - or lose - the tier, crest,
            // and value the row was already built with.
            var row = bareRow()
                    .carriesCrest(CREST)
                    .carriesValue(VALUE, Color.GRAY)
                    .indentsBy(INDENT)
                    .carriesMarker(MARKER, Color.YELLOW);

            assertThat(row.indent()).isCloseTo(INDENT, within(TOLERANCE));
            assertThat(row.crestSpritePath()).isEqualTo(CREST);
            assertThat(row.labelTextSpan().text()).isEqualTo(TEXT);
            assertThat(row.valueTextSpan().text()).isEqualTo(VALUE);
            assertThat(row.valueTextSpan().colour()).isEqualTo(Color.GRAY);
        }
    }

    @Nested
    class IndentsBy {
        @Test
        void indentsBySetsTheInset() {
            var row = bareRow().indentsBy(INDENT);

            assertThat(row.indent()).isCloseTo(INDENT, within(TOLERANCE));
        }
    }

    @Nested
    class OpensSection {
        @Test
        void opensSectionMarksTheRowAsStartingABlock() {
            var row = bareRow().opensSection();

            assertThat(row.hasSectionBreak()).isTrue();
        }
    }

    @Nested
    class ClearsCrestColumn {
        @Test
        void clearsCrestColumnStartsTheLabelAtTheContentEdge() {
            var row = bareRow().clearsCrestColumn();

            assertThat(row.labelPlacement()).isEqualTo(TooltipLabelPlacement.AT_CONTENT_EDGE);
        }

        @Test
        void clearsCrestColumnKeepsTheValueColumn() {
            // Only the crest gutter is cleared. A title still carries a trailing value the way the rows
            // it heads do, so the value column is not part of what the placement decides.
            var row = bareRow()
                    .carriesValue(VALUE, Color.GRAY)
                    .clearsCrestColumn();

            assertThat(row.valueTextSpan().text()).isEqualTo(VALUE);
        }
    }

    @Nested
    class Centred {
        @Test
        void centredLaysTheLabelAsAStandaloneCentredSpan() {
            var row = bareRow().centred();

            assertThat(row.labelPlacement()).isEqualTo(TooltipLabelPlacement.CENTRED);
        }

        @Test
        void centredKeepsTheLabelAndItsMarker() {
            // Centring changes where the line sits, not what it says, so the marker it may carry rides
            // along with the label rather than being dropped from the centred span.
            var row = bareRow()
                    .carriesMarker(MARKER, Color.YELLOW)
                    .centred();

            assertThat(row.labelTextSpan().text()).isEqualTo(TEXT);
            assertThat(row.markerTextSpan().text()).isEqualTo(MARKER);
        }

        @Test
        void centredReplacesAFlushPlacementRatherThanCompoundingWithIt() {
            // One placement, so the last one stated wins: a centred span is already clear of the columns,
            // and there is no state in which a row is both an edge-flush line and a centred one.
            var row = bareRow().clearsCrestColumn().centred();

            assertThat(row.labelPlacement()).isEqualTo(TooltipLabelPlacement.CENTRED);
        }
    }

    @Nested
    class ReadsAs {
        @Test
        void readsAsSetsTheKindOfLine() {
            var row = bareRow().readsAs(TooltipLineStyle.HEADER);

            assertThat(row.lineStyle()).isEqualTo(TooltipLineStyle.HEADER);
        }

        @Test
        void readsAsKeepsTheRestOfTheRow() {
            // A heading is still a row: naming its kind must not disturb the content or the placement it
            // was already built with, since the kind decides only how it is drawn.
            var row = bareRow()
                    .centred()
                    .carriesMarker(MARKER, Color.YELLOW)
                    .readsAs(TooltipLineStyle.HEADER);

            assertThat(row.labelTextSpan().text()).isEqualTo(TEXT);
            assertThat(row.markerTextSpan().text()).isEqualTo(MARKER);
            assertThat(row.labelPlacement()).isEqualTo(TooltipLabelPlacement.CENTRED);
        }
    }

    @Nested
    class HasMarker {
        @Test
        void hasMarkerIsFalseForABareRow() {
            assertThat(bareRow().hasMarker()).isFalse();
        }

        @Test
        void hasMarkerIsTrueForAMarkedRow() {
            assertThat(bareRow().carriesMarker(MARKER, Color.YELLOW).hasMarker()).isTrue();
        }

        @Test
        void hasMarkerIsFalseForABlankMarker() {
            // A caller that assembles a marker from parts and comes up with whitespace gets the
            // unmarked row it meant, rather than a gap reserved before nothing.
            assertThat(bareRow().carriesMarker(" ", Color.YELLOW).hasMarker()).isFalse();
        }
    }
}
