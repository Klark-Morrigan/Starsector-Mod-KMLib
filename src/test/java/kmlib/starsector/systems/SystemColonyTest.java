package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link SystemColony#isHidden} and {@link SystemColony#isKnownToPlayer},
 * the two facts a colony answers off its own market rather than storing beside it. Each
 * method's cases live in a {@link Nested} group so the suite reports as a per-method tree; the
 * shared mock builders stay on the outer class.
 */
final class SystemColonyTest {

    @Nested
    class IsHidden {

        @Test
        void reports_a_concealed_market_as_hidden() {

            var colony = new SystemColony(buildMarket(true, false), true);

            assertThat(colony.isHidden())
                .isTrue();
        }

        @Test
        void reports_a_publicly_listed_market_as_not_hidden() {

            var colony = new SystemColony(buildMarket(false, false), true);

            assertThat(colony.isHidden())
                .isFalse();
        }
    }

    @Nested
    class IsKnownToPlayer {

        @Test
        void reports_a_concealed_colony_on_a_found_entity_as_known() {
            // A raided pirate base: permanently hidden, and perfectly well known. Concealment is
            // not the fog, which is why the colony asks the market rather than reading isHidden.
            var colony = new SystemColony(buildMarket(true, false), true);

            assertThat(colony.isKnownToPlayer())
                .isTrue();
        }

        @Test
        void reports_a_concealed_colony_on_an_unfound_entity_as_unknown() {

            var colony = new SystemColony(buildMarket(true, true), true);

            assertThat(colony.isKnownToPlayer())
                .isFalse();
        }

        @Test
        void reports_an_open_colony_on_an_unfound_entity_as_known() {
            // Surfaced by a story reveal ahead of a fleet reaching it: public knowledge, listed
            // on the star's own map tooltip, so the discovery arm alone must not fog it out.
            var colony = new SystemColony(buildMarket(false, true), true);

            assertThat(colony.isKnownToPlayer())
                .isTrue();
        }
    }

    // A market wired for the two axes the reads above run on, which are independent of each
    // other: whether it is publicly listed, and whether its entity has been found.
    private static MarketAPI buildMarket(boolean isHidden, boolean isEntityDiscoverable) {

        // The entity finishes its own stubbing before the market's opens, so the two do not nest
        // into an unfinished-stubbing error.
        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.isDiscoverable())
            .thenReturn(isEntityDiscoverable);

        var marketMock = mock(MarketAPI.class);

        when(marketMock.isHidden())
            .thenReturn(isHidden);
        when(marketMock.getPrimaryEntity())
            .thenReturn(entityMock);

        return marketMock;
    }
}
