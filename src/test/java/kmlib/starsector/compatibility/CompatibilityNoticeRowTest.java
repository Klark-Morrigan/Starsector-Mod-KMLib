package kmlib.starsector.compatibility;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Covers the guard that stands in for a type system this value has none of, and the one thing it
 * does with what it holds.
 *
 * <p>Both components are strings, so the compiler cannot tell the wording from what fills it. What
 * separates them is the slot: a template carries one and a value does not, which is what turns a
 * transposed pair into a refusal rather than a row that merely reads oddly.
 */
final class CompatibilityNoticeRowTest {

    private static final String ROW_TEMPLATE = "    Installed:  %s";

    private static final String ROW_VALUE = "0.9.1";

    @Nested
    class Constructor {

        @Test
        void refusesARowWithNoTemplate() {

            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CompatibilityNoticeRow(" ", ROW_VALUE));
        }

        @Test
        void refusesARowWithNoValue() {

            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CompatibilityNoticeRow(ROW_TEMPLATE, " "));
        }

        @Test
        void refusesAValueWhereTheTemplateBelongs() {

            // The transposition that matters: the pair the wrong way round would render as the
            // value alone, with the label it was meant to carry lost.
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CompatibilityNoticeRow(ROW_VALUE, ROW_TEMPLATE));
        }
    }

    @Nested
    class FillRow {

        @Test
        void putsTheValueInTheTemplatesSlot() {

            assertThat(new CompatibilityNoticeRow(ROW_TEMPLATE, ROW_VALUE).fillRow())
                .isEqualTo("    Installed:  0.9.1");
        }

        @Test
        void leavesAValueCarryingItsOwnPunctuationAlone() {

            // A consuming mod's sentence lands in a slot like any other value, and nothing in it is
            // read as wording - a percent sign in one would otherwise be taken for a second slot.
            var row = new CompatibilityNoticeRow("    Effect:     %s", "Fuel use is up 50%.");

            assertThat(row.fillRow())
                .isEqualTo("    Effect:     Fuel use is up 50%.");
        }
    }
}
