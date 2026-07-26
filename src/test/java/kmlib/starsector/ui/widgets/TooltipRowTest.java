package kmlib.starsector.ui.widgets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TooltipRow}'s tier factories: the inset each names, and that neither touches the rest of
 * the row. The flush factory exists so a caller never spells out a zero indent, so the zero it produces
 * is the contract worth fixing.
 */
class TooltipRowTest {
    private static final String CREST = "crest_a";
    private static final String TEXT = "Hegemony";
    private static final String VALUE = "12";
    private static final float INDENT = 14f;
    private static final float TOLERANCE = 0.001f;

    @Nested
    class CreateFlushRow {
        @Test
        void createFlushRowSitsAtZeroIndent() {
            var row = TooltipRow.createFlushRow(CREST, TEXT, Color.WHITE, VALUE, Color.GRAY);

            assertThat(row.indent()).isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void createFlushRowCarriesTheContentThrough() {
            var row = TooltipRow.createFlushRow(CREST, TEXT, Color.WHITE, VALUE, Color.GRAY);

            assertThat(row.crestSpritePath()).isEqualTo(CREST);
            assertThat(row.text()).isEqualTo(TEXT);
            assertThat(row.textColor()).isEqualTo(Color.WHITE);
            assertThat(row.value()).isEqualTo(VALUE);
            assertThat(row.valueColor()).isEqualTo(Color.GRAY);
        }

        @Test
        void createFlushRowKeepsACrestlessRowCrestless() {
            var row = TooltipRow.createFlushRow(null, TEXT, Color.WHITE, "", Color.GRAY);

            assertThat(row.crestSpritePath()).isNull();
            assertThat(row.value()).isEmpty();
        }
    }

    @Nested
    class CreateIndentedRow {
        @Test
        void createIndentedRowSitsAtTheGivenIndent() {
            var row = TooltipRow.createIndentedRow(
                    INDENT, CREST, TEXT, Color.LIGHT_GRAY, VALUE, Color.GRAY);

            assertThat(row.indent()).isCloseTo(INDENT, within(TOLERANCE));
        }

        @Test
        void createIndentedRowCarriesTheContentThrough() {
            var row = TooltipRow.createIndentedRow(
                    INDENT, CREST, TEXT, Color.LIGHT_GRAY, VALUE, Color.GRAY);

            assertThat(row.crestSpritePath()).isEqualTo(CREST);
            assertThat(row.text()).isEqualTo(TEXT);
            assertThat(row.textColor()).isEqualTo(Color.LIGHT_GRAY);
            assertThat(row.value()).isEqualTo(VALUE);
            assertThat(row.valueColor()).isEqualTo(Color.GRAY);
        }
    }
}
