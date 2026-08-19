package kmlib.starsector.colonies;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link ColonyKind#resolveKind} - which kind of place a market stands for.
 * The cases live in a {@link Nested} group so the suite reports as a per-method tree, and the
 * markets they are posed against are {@link ColonyMarketFixture}'s.
 *
 * <p>What makes a market an abandoned station is the market read's own contract and is pinned
 * with it; what this adds is that the classification routes to the right kind, and that anything
 * unrecognised falls to the ordinary one rather than being erased.
 */
final class ColonyKindTest {

    @Nested
    class ResolveKind {

        @Test
        void reads_a_derelict_station_as_an_abandoned_station() {

            var derelict = ColonyMarketFixture.buildDerelictStation("neutral");

            assertThat(ColonyKind.resolveKind(derelict))
                .isEqualTo(ColonyKind.ABANDONED_STATION);
        }

        @Test
        void reads_an_ordinary_colony_as_a_colony() {

            var colony = ColonyMarketFixture.buildVisibleColony("hegemony");

            assertThat(ColonyKind.resolveKind(colony))
                .isEqualTo(ColonyKind.COLONY);
        }

        @Test
        void reads_a_null_market_as_a_colony() {
            // The default arm, posed at its extreme: nothing at all to read still yields the kind
            // that keeps a place on the map, because misfiling a settlement as a hulk erases it.
            assertThat(ColonyKind.resolveKind(null))
                .isEqualTo(ColonyKind.COLONY);
        }
    }
}
