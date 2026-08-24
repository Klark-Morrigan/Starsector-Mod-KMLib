package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Pins the fixed-set guarantee a colony set's construction makes: what it was built with is what
 * it reports, whatever happens to the list it was built from afterwards.
 * Each method's cases live in a {@link Nested} group so the suite reports as a per-method tree.
 *
 * <p>How a set is selected in the first place belongs to whichever reader selected it, and is
 * pinned by {@link SystemColoniesTest} and {@link HyperspaceColoniesTest}. What may be shown of
 * one is no part of the set at all - a consumer states its own projection over it - so nothing
 * here poses a visibility rule.
 */
final class ColoniesTest {

    @Nested
    class Construct {

        @Test
        void reads_absent_colonies_as_an_empty_set() {
            assertThat(new Colonies(null))
                .isEqualTo(Colonies.NONE);
        }

        @Test
        void keeps_the_colonies_it_was_built_with_when_the_source_list_changes_later() {

            var colonies = new ArrayList<Colony>();
            colonies.add(new Colony(mock(MarketAPI.class), true));

            var set = new Colonies(colonies);

            colonies.clear();

            assertThat(set.colonies())
                .hasSize(1);
        }

        @Test
        void rejects_an_attempt_to_change_the_colonies() {
            // A set is memoised for a whole pass and handed to every reader in it, so one reader
            // able to change it would be rewriting the system underneath all the others.
            var set = new Colonies(List.of(new Colony(mock(MarketAPI.class), true)));

            assertThatThrownBy(() -> set.colonies().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
