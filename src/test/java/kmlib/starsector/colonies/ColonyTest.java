package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link Colony#isHidden}, {@link Colony#isDiscoveredByPlayer} and
 * {@link Colony#readOwnerId} - the facts a colony answers about itself off its own market rather
 * than storing beside it. Each method's cases live in a {@link Nested} group so the suite reports
 * as a per-method tree; the shared mock builders stay on the outer class.
 */
final class ColonyTest {

    @Nested
    class IsHidden {

        @Test
        void reports_a_concealed_market_as_hidden() {

            var colony = new Colony(buildMarket(true, false), true);

            assertThat(colony.isHidden())
                .isTrue();
        }

        @Test
        void reports_a_publicly_listed_market_as_not_hidden() {

            var colony = new Colony(buildMarket(false, false), true);

            assertThat(colony.isHidden())
                .isFalse();
        }
    }

    @Nested
    class IsDiscoveredByPlayer {

        @Test
        void reports_a_concealed_colony_on_a_found_entity_as_found() {
            // A raided pirate base: permanently hidden, and perfectly well found. Concealment is
            // not the fog, which is why the colony asks the market rather than reading isHidden.
            var colony = new Colony(buildMarket(true, false), true);

            assertThat(colony.isDiscoveredByPlayer())
                .isTrue();
        }

        @Test
        void reports_a_concealed_colony_on_an_undiscovered_entity_as_undiscovered() {

            var colony = new Colony(buildMarket(true, true), true);

            assertThat(colony.isDiscoveredByPlayer())
                .isFalse();
        }

        @Test
        void reports_an_open_colony_on_an_undiscovered_entity_as_undiscovered() {
            // The case concealment and the fog part company on. Being publicly listed is not
            // being seen: a derelict station declares itself to an economy the player has no
            // sight of, so listing alone must not carry a colony past the fog.
            var colony = new Colony(buildMarket(false, true), true);

            assertThat(colony.isDiscoveredByPlayer())
                .isFalse();
        }
    }

    @Nested
    class ReadOwnerId {

        @Test
        void reports_the_faction_id_the_market_carries() {
            // Read off the market's own id rather than its faction object, which is what an
            // ownership change writes - so a market answering one and not the other answers here.
            var marketMock = mock(MarketAPI.class);

            when(marketMock.getFactionId())
                .thenReturn("pirates");

            assertThat(new Colony(marketMock, true).readOwnerId())
                .isEqualTo("pirates");
        }

        @Test
        void reports_no_owner_where_the_market_names_none() {
            // Absorbed rather than refused: an owner nobody can name is compared against whatever
            // a caller compares owners for, and there is nothing here to fail on.
            var colony = new Colony(mock(MarketAPI.class), true);

            assertThat(colony.readOwnerId())
                .isNull();
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
