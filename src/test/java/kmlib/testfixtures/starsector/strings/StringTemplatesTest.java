package kmlib.testfixtures.starsector.strings;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the count against what {@code String.format} actually consumes, one edge per row. Each row is a
 * shape a shipped template can take; a counter that misreads one reports a contract a guard then holds a
 * call site to wrongly.
 */
final class StringTemplatesTest {

    @Nested
    class CountFormatArguments {

        // Pipe-delimited so a template may carry commas: the grouping flag is one of the shapes pinned.
        @ParameterizedTest(name = "\"{0}\" takes {1}")
        @CsvSource(delimiter = '|', value = {
            "plain wording              | 0",
            "100%% certain              | 0",
            "one line%nthe next         | 0",
            "%s and %d                  | 2",
            "%,.2f credits              | 1",
            "%-10s padded               | 1",
            "%1$s then %1$s again       | 1",
            "%2$s before %1$s           | 2",
            "%s beside %3$s             | 3",
            "%3$s beside %s %s %s %s    | 4",
            "%s then %<s again          | 1",
            "%tY                        | 1",
        })
        void countsWhatTheFormatterConsumes(String template, int expectedArguments) {

            assertThat(StringTemplates.countFormatArguments(template))
                .isEqualTo(expectedArguments);
        }
    }

    @Nested
    class ReadArgumentConversions {

        // Pipe-delimited so a template may carry commas; the expected slots are space-separated, an empty
        // cell meaning none.
        @ParameterizedTest(name = "\"{0}\" takes [{1}]")
        @CsvSource(delimiter = '|', value = {
            "plain wording                | ''",
            "100%% certain%n              | ''",
            "%s and %d                    | %1$s %2$d",
            "%2$d before %1$s             | %1$s %2$d",
            "%,.2f credits                | %1$f",
            "%S shouted                   | %1$s",
            "%1$s then %1$s again         | %1$s",
            "%s then %<s again            | %1$s",
            "%1$s read as %1$d            | %1$d %1$s",
            "%s beside %3$s               | %1$s %3$s",
        })
        void readsEachSlotByTheArgumentItTakes(String template, String expectedSlots) {

            var expectedConversions = expectedSlots.isEmpty()
                ? List.<String>of()
                : List.of(expectedSlots.split(" "));

            assertThat(StringTemplates.readArgumentConversions(template))
                .isEqualTo(expectedConversions);
        }
    }
}
