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
 * to be the plain flush, crest-less, marker-less, value-less line every refinement builds on.
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

            assertThat(row.text()).isEqualTo(TEXT);
            assertThat(row.textColor()).isEqualTo(Color.WHITE);
        }

        @Test
        void createRowCarriesNoneOfTheOptionalParts() {
            var row = bareRow();

            assertThat(row.indent()).isCloseTo(0f, within(TOLERANCE));
            assertThat(row.crestSpritePath()).isNull();
            assertThat(row.marker()).isEmpty();
            assertThat(row.value()).isEmpty();
            assertThat(row.hasSectionBreak()).isFalse();
            assertThat(row.isOutsideCrestColumn()).isFalse();
            assertThat(row.isLabelCentred()).isFalse();
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
            // Nothing draws in these colours on a bare row, but they must not be null: a refinement
            // that sets only one part leaves the others' colours to be read by the renderer regardless.
            var row = bareRow();

            assertThat(row.markerColor()).isEqualTo(Color.WHITE);
            assertThat(row.valueColor()).isEqualTo(Color.WHITE);
        }
    }

    @Nested
    class CarriesCrest {
        @Test
        void carriesCrestSetsThePathAndKeepsTheRest() {
            var row = bareRow().carriesCrest(CREST);

            assertThat(row.crestSpritePath()).isEqualTo(CREST);
            assertThat(row.text()).isEqualTo(TEXT);
            assertThat(row.value()).isEmpty();
        }
    }

    @Nested
    class CarriesValue {
        @Test
        void carriesValueSetsTheValueAndItsColour() {
            var row = bareRow().carriesValue(VALUE, Color.GRAY);

            assertThat(row.value()).isEqualTo(VALUE);
            assertThat(row.valueColor()).isEqualTo(Color.GRAY);
        }
    }

    @Nested
    class CarriesMarker {
        @Test
        void carriesMarkerSetsTheMarkerAndItsColour() {
            var row = bareRow().carriesMarker(MARKER, Color.YELLOW);

            assertThat(row.marker()).isEqualTo(MARKER);
            assertThat(row.markerColor()).isEqualTo(Color.YELLOW);
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
            assertThat(row.text()).isEqualTo(TEXT);
            assertThat(row.value()).isEqualTo(VALUE);
            assertThat(row.valueColor()).isEqualTo(Color.GRAY);
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
        void clearsCrestColumnMarksTheRowAsLayingFlush() {
            var row = bareRow().clearsCrestColumn();

            assertThat(row.isOutsideCrestColumn()).isTrue();
        }
    }

    @Nested
    class Centred {
        @Test
        void centredMarksTheRowAsAStandaloneCentredLine() {
            var row = bareRow().centred();

            assertThat(row.isLabelCentred()).isTrue();
        }

        @Test
        void centredKeepsTheLabelAndItsMarker() {
            // Centring changes where the line sits, not what it says, so the marker it may carry rides
            // along with the label rather than being dropped from the centred span.
            var row = bareRow()
                    .carriesMarker(MARKER, Color.YELLOW)
                    .centred();

            assertThat(row.text()).isEqualTo(TEXT);
            assertThat(row.marker()).isEqualTo(MARKER);
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

            assertThat(row.text()).isEqualTo(TEXT);
            assertThat(row.marker()).isEqualTo(MARKER);
            assertThat(row.isLabelCentred()).isTrue();
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
