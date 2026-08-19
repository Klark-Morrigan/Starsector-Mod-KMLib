package kmlib.starsector.colonies;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link ColonyVisibility#BASE_FOG}, the rule a caller stating none of its own is read as.
 * The cases live in a {@link Nested} group so the suite reports as a per-member tree.
 *
 * <p>Worth pinning although the record itself holds no logic: each of the three flags is
 * load-bearing in a direction nothing else would catch. A reveal set here by mistake would put
 * every undiscovered colony in the sector on the map, and a gate set here would hold back a
 * derelict nobody asked to hide - both silently, and both under the name the whole library
 * falls back to.
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
        void holds_back_neither_abandoned_stations_nor_hidden_colonies() {
            // A gate nobody asked for must not appear out of an unstated argument, so the
            // fall-back rule adds nothing to the fog in either direction.
            assertThat(ColonyVisibility.BASE_FOG.shouldGateAbandonedStations())
                .isFalse();
            assertThat(ColonyVisibility.BASE_FOG.shouldGateHiddenColonies())
                .isFalse();
        }
    }
}
