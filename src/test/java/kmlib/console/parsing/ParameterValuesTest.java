package kmlib.console.parsing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins {@link ParameterValues#positiveWholeNumber}: a whole number above zero is taken as given,
 * and everything else is refused with the clause the command supplied - a count of rows is a
 * request for some, so zero and below ask for nothing rather than for all.
 */
final class ParameterValuesTest {

    private static final String EXPECTED = "a whole number of rows above zero";

    @Nested
    class PositiveWholeNumber {

        @Test
        void parsesAWholeNumberAboveZero() throws ValueParseException {

            assertThat(ParameterValues.positiveWholeNumber(EXPECTED).parseValue("20"))
                .isEqualTo(20);
        }

        @Test
        void rejectsZeroWithTheSuppliedClause() {

            assertThatThrownBy(() -> ParameterValues.positiveWholeNumber(EXPECTED).parseValue("0"))
                .isInstanceOf(ValueParseException.class)
                .hasMessage(EXPECTED);
        }

        @Test
        void rejectsANegativeNumberWithTheSuppliedClause() {

            assertThatThrownBy(() -> ParameterValues.positiveWholeNumber(EXPECTED).parseValue("-3"))
                .isInstanceOf(ValueParseException.class)
                .hasMessage(EXPECTED);
        }

        @Test
        void rejectsSomethingThatIsNotANumberWithTheSuppliedClause() {

            assertThatThrownBy(() ->
                ParameterValues.positiveWholeNumber(EXPECTED).parseValue("many"))
                .isInstanceOf(ValueParseException.class)
                .hasMessage(EXPECTED);
        }
    }
}
