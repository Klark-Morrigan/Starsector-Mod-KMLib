package kmlib.starsector.colonies;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins {@link ColonyVisibility#BASE_FOG}, the rule a caller stating none of its own is read as,
 * and the fixed-rule guarantee its construction makes. Each member's cases live in a
 * {@link Nested} group so the suite reports as a per-member tree.
 *
 * <p>Worth pinning although the record itself holds no logic: everything it carries is
 * load-bearing in a direction nothing else would catch. A reveal set here by mistake would put
 * every undiscovered colony in the sector on the map, and a gate set here would hold back a
 * derelict nobody asked to hide - both silently, and both under the name the whole library falls
 * back to.
 */
final class ColonyVisibilityTest {

    @Nested
    class BaseFog {

        @Test
        void admits_nothing_the_player_has_not_found() {

            assertThat(ColonyVisibility.BASE_FOG.shouldIncludeUndiscoveredMarkets())
                .isFalse();
        }

        @Test
        void holds_back_nothing_beyond_the_fog() {
            // A gate nobody asked for must not appear out of an unstated argument, so the
            // fall-back rule adds nothing to the fog in any direction.
            assertThat(ColonyVisibility.BASE_FOG.revelationGates())
                .isEmpty();
        }
    }

    @Nested
    class Construct {

        @Test
        void reads_absent_gates_as_no_gates_at_all() {
            assertThat(new ColonyVisibility(false, null).revelationGates())
                .isEmpty();
        }

        @Test
        void keeps_the_gates_it_was_built_with_when_the_source_set_changes_later() {

            var gates = new HashSet<RevelationGate>();
            gates.add(RevelationGate.SPACE_DERELICTS);

            var rule = new ColonyVisibility(false, gates);

            gates.clear();

            assertThat(rule.revelationGates())
                .containsExactly(RevelationGate.SPACE_DERELICTS);
        }

        @Test
        void rejects_an_attempt_to_change_the_gates() {
            // A rule is resolved once for a whole render pass and handed to every reader in it,
            // so one reader able to change it would be re-fogging the map underneath the others.
            var rule = new ColonyVisibility(false, Set.of(RevelationGate.HIDDEN_COLONIES));

            assertThatThrownBy(() -> rule.revelationGates().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
