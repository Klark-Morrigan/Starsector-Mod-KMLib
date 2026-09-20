package kmlib.starsector.markets.colonies;

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
        void readsAbsentColoniesAsAnEmptySet() {
            assertThat(new Colonies(null))
                .isEqualTo(Colonies.NONE);
        }

        @Test
        void keepsTheColoniesItWasBuiltWithWhenTheSourceListChangesLater() {

            var colonies = new ArrayList<Colony>();
            colonies.add(new Colony(mock(MarketAPI.class), true));

            var set = new Colonies(colonies);

            colonies.clear();

            assertThat(set.colonies())
                .hasSize(1);
        }

        @Test
        void rejectsAnAttemptToChangeTheColonies() {
            // A set is memoised for a whole pass and handed to every reader in it, so one reader
            // able to change it would be rewriting the system underneath all the others.
            var set = new Colonies(List.of(new Colony(mock(MarketAPI.class), true)));

            assertThatThrownBy(() -> set.colonies().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class SelectColonies {

        @Test
        void keepsTheSetsOwnOrderRatherThanGroupingThePassingColonies() {
            // A caller mirroring vanilla's tie rules settles a contest on whichever colony the set
            // reaches first, so a selection that regrouped them would resolve a different winner.
            var first = buildListedColony();
            var skipped = buildListedColony();
            var last = buildListedColony();

            var set = new Colonies(List.of(first, skipped, last));

            assertThat(set.selectColonies(colony -> colony != skipped))
                .containsExactly(first, last);
        }

        @Test
        void yieldsNothingWhereNoColonyPasses() {
            assertThat(new Colonies(List.of(buildListedColony())).selectColonies(colony -> false))
                .isEmpty();
        }

        @Test
        void yieldsNothingForAnUnstatedTest() {
            // An absent test is no grounds for reporting anybody, which is the direction that
            // withholds rather than the one that invents a colony nobody asked to be shown.
            assertThat(new Colonies(List.of(buildListedColony())).selectColonies(null))
                .isEmpty();
        }

        @Test
        void rejectsAnAttemptToChangeTheSelection() {

            var selected = new Colonies(List.of(buildListedColony()))
                .selectColonies(colony -> true);

            assertThatThrownBy(selected::clear)
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class HasAnyColony {

        @Test
        void agreesWithTheSelectionItAsksTheEmptinessOf() {
            // The claim the short-circuiting read rests on: not materialising the list must not
            // change the answer, whichever way the test falls.
            var set = new Colonies(List.of(buildListedColony()));

            assertThat(set.hasAnyColony(colony -> true))
                .isTrue();
            assertThat(set.hasAnyColony(colony -> false))
                .isFalse();
        }

        @Test
        void answersFalseForAPlaceWithNobodyInIt() {
            assertThat(Colonies.NONE.hasAnyColony(colony -> true))
                .isFalse();
        }

        @Test
        void answersFalseForAnUnstatedTest() {
            assertThat(new Colonies(List.of(buildListedColony())).hasAnyColony(null))
                .isFalse();
        }
    }

    // A colony the economy lists, which is what a case poses where nothing about the colony itself
    // is the subject - every case here is about the walk rather than about what it walks.
    private static Colony buildListedColony() {
        return new Colony(mock(MarketAPI.class), true);
    }
}
