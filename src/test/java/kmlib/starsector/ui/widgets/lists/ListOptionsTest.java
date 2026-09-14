package kmlib.starsector.ui.widgets.lists;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the bounds rule the picker and its two selectors read a reported index through: the options
 * the list was laid from and nothing either side of them. Read here as well as through each
 * caller's own suite, since what those pin is what a control does about a stray index and this is
 * which indices are stray at all.
 */
final class ListOptionsTest {

    // Three options stand in for whatever a control was laid out from - items, sort modes, column
    // counts - the rule reading their count and nothing about them.
    private static final List<String> OPTIONS = List.of("first", "second", "third");

    @Nested
    class IsOptionAt {

        @Test
        void isOptionAtAnswersTrueForEveryOptionTheListWasLaidFrom() {
            // Both ends included: the last option is as real as the first, and a bound off by one
            // would drop the row a list most often ends on.
            assertThat(ListOptions.isOptionAt(OPTIONS, 0))
                .isTrue();
            assertThat(ListOptions.isOptionAt(OPTIONS, 2))
                .isTrue();
        }

        @Test
        void isOptionAtAnswersFalsePastTheLastOption() {
            // The near miss and a far one alike: one past the end names no option, which is the read
            // that would run off the list.
            assertThat(ListOptions.isOptionAt(OPTIONS, OPTIONS.size()))
                .isFalse();
            assertThat(ListOptions.isOptionAt(OPTIONS, 99))
                .isFalse();
        }

        @Test
        void isOptionAtAnswersFalseBelowTheFirstOption() {
            // The other end, which a bounds test written against the list's length alone would let
            // through - a sentinel carried as a negative arrives here like any other index.
            assertThat(ListOptions.isOptionAt(OPTIONS, -1))
                .isFalse();
        }

        @Test
        void isOptionAtAnswersFalseForAListWithNoOptions() {
            // A control laid from nothing has no real options, so every reading against it is a miss
            // rather than a first row that happens to be absent.
            assertThat(ListOptions.isOptionAt(List.of(), 0))
                .isFalse();
        }
    }
}
