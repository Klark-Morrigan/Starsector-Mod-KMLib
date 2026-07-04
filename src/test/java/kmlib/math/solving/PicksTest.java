package kmlib.math.solving;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link Picks#pickHigher}: the candidate wins when it scores higher, the
 * incumbent survives a tie or a null candidate, a null incumbent never blocks a real
 * candidate, and two nulls stay null.
 */
final class PicksTest {

    @Nested
    class PickHigher {
        @Test
        void pickHigherReturnsTheCandidateWhenItScoresHigher() {
            assertThat(Picks.pickHigher("current", "candidate",
                    value -> value.equals("candidate") ? 2.0 : 1.0)).isEqualTo("candidate");
        }

        @Test
        void pickHigherKeepsTheCurrentOnATie() {
            // First seen wins a tie - the deterministic pick a repeated search needs.
            assertThat(Picks.pickHigher("current", "candidate", value -> 1.0))
                    .isEqualTo("current");
        }

        @Test
        void pickHigherReturnsTheCandidateWhenCurrentIsNull() {
            assertThat(Picks.<String>pickHigher(null, "candidate", value -> 1.0))
                    .isEqualTo("candidate");
        }

        @Test
        void pickHigherKeepsTheCurrentWhenCandidateIsNull() {
            assertThat(Picks.pickHigher("current", null, value -> 1.0)).isEqualTo("current");
        }

        @Test
        void pickHigherReturnsNullWhenBothAreNull() {
            assertThat(Picks.<String>pickHigher(null, null, value -> 1.0)).isNull();
        }
    }
}
