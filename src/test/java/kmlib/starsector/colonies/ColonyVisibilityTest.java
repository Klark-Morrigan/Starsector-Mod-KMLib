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
        void admits_no_ruin_the_player_has_not_surveyed() {

            assertThat(ColonyVisibility.BASE_FOG.shouldIncludeUnsurveyedDeadWorlds())
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
            assertThat(new ColonyVisibility(Set.of(), null).revelationGates())
                .isEmpty();
        }

        @Test
        void reads_absent_reveals_as_the_fog_entire() {
            // The direction that matters: an unstated set must not lift a fog arm, or a missing
            // argument would put every undiscovered colony in the sector on the map.
            var rule = new ColonyVisibility(null, Set.of());

            assertThat(rule.shouldIncludeUndiscoveredMarkets())
                .isFalse();
            assertThat(rule.shouldIncludeUnsurveyedDeadWorlds())
                .isFalse();
        }

        @Test
        void reads_each_reveal_as_the_one_arm_it_names() {
            // The whole of what parts one toggle from the next: each answers for its own arm and
            // says nothing about the other's.
            var rule = new ColonyVisibility(
                Set.of(VisibilityReveal.UNSURVEYED_DEAD_WORLDS),
                Set.of());

            assertThat(rule.shouldIncludeUnsurveyedDeadWorlds())
                .isTrue();
            assertThat(rule.shouldIncludeUndiscoveredMarkets())
                .isFalse();
        }

        @Test
        void keeps_the_gates_it_was_built_with_when_the_source_set_changes_later() {

            var gates = new HashSet<RevelationGate>();
            gates.add(RevelationGate.SPACE_DERELICTS);

            var rule = new ColonyVisibility(Set.of(), gates);

            gates.clear();

            assertThat(rule.revelationGates())
                .containsExactly(RevelationGate.SPACE_DERELICTS);
        }

        @Test
        void keeps_the_reveals_it_was_built_with_when_the_source_set_changes_later() {

            var reveals = new HashSet<VisibilityReveal>();
            reveals.add(VisibilityReveal.UNDISCOVERED_MARKETS);

            var rule = new ColonyVisibility(reveals, Set.of());

            reveals.clear();

            assertThat(rule.shouldIncludeUndiscoveredMarkets())
                .isTrue();
        }

        @Test
        void rejects_an_attempt_to_change_the_gates() {
            // A rule is resolved once for a whole render pass and handed to every reader in it,
            // so one reader able to change it would be re-fogging the map underneath the others.
            var rule = new ColonyVisibility(
                Set.of(),
                Set.of(RevelationGate.HIDDEN_COLONIES));

            assertThatThrownBy(() -> rule.revelationGates().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void rejects_an_attempt_to_change_the_reveals() {

            var rule = new ColonyVisibility(
                Set.of(VisibilityReveal.UNDISCOVERED_MARKETS),
                Set.of());

            assertThatThrownBy(() -> rule.reveals().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
