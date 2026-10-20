package kmlib.mods.console.commands.parsing;

import kmlib.testfixtures.starsector.strings.KmlibStringsSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins {@link ParameterValues#positiveWholeNumber}: a whole number above zero is taken as given,
 * and everything else is refused with the clause the command supplied - a count of rows is a
 * request for some, so zero and below ask for nothing rather than for all.
 *
 * <p>Also pins {@link ParameterValues#oneOfKeywords}: a word reads as the value it stands for
 * whatever its case, and any other word is refused with the words listed in their given order.
 */
final class ParameterValuesTests {

    private static final String EXPECTED = "a whole number of rows above zero";

    // Listed out of alphabetical order, so a refusal that sorted the words would show it.
    private static final Map<String, Integer> VALUES_BY_KEYWORD = createValuesByKeyword();

    private static Map<String, Integer> createValuesByKeyword() {

        var valuesByKeyword = new LinkedHashMap<String, Integer>();

        valuesByKeyword.put("walks", 3);
        valuesByKeyword.put("flat", 2);

        return valuesByKeyword;
    }

    @BeforeEach
    void installStrings() {
        KmlibStringsSettingsFake.installSettings();
    }

    @AfterEach
    void clearStrings() {
        KmlibStringsSettingsFake.clearSettings();
    }

    @Nested
    class OneOfKeywords {

        @Test
        void readsAWordAsTheValueItStandsForWhateverItsCase() throws ValueParseException {

            assertThat(ParameterValues.oneOfKeywords(VALUES_BY_KEYWORD).parseValue("Walks"))
                .isEqualTo(3);
        }

        @Test
        void rejectsAnyOtherWordListingTheWordsInTheirGivenOrder() {

            assertThatThrownBy(() -> ParameterValues.oneOfKeywords(VALUES_BY_KEYWORD).parseValue("tree"))
                .isInstanceOf(ValueParseException.class)
                .hasMessage("one of walks, flat");
        }
    }

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
