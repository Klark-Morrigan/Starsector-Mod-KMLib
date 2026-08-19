package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link Colony#isHidden}, {@link Colony#isDiscoveredByPlayer} and
 * {@link Colony#isSightedByPlayer}, the three facts a colony answers off its own market rather
 * than storing beside it. Each method's cases live in a {@link Nested} group so the suite reports
 * as a per-method tree; the shared mock builders stay on the outer class.
 */
final class ColonyTest {

    @Nested
    class IsHidden {

        @Test
        void reports_a_concealed_market_as_hidden() {

            var colony = new Colony(buildMarket(true, false), ColonyKind.COLONY, true);

            assertThat(colony.isHidden())
                .isTrue();
        }

        @Test
        void reports_a_publicly_listed_market_as_not_hidden() {

            var colony = new Colony(buildMarket(false, false), ColonyKind.COLONY, true);

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
            var colony = new Colony(buildMarket(true, false), ColonyKind.COLONY, true);

            assertThat(colony.isDiscoveredByPlayer())
                .isTrue();
        }

        @Test
        void reports_a_concealed_colony_on_an_unfound_entity_as_unfound() {

            var colony = new Colony(buildMarket(true, true), ColonyKind.COLONY, true);

            assertThat(colony.isDiscoveredByPlayer())
                .isFalse();
        }

        @Test
        void reports_an_open_colony_on_an_unfound_entity_as_unfound() {
            // The case concealment and the fog part company on. Being publicly listed is not
            // being seen: a derelict station declares itself to an economy the player has no
            // sight of, so listing alone must not carry a colony past the fog.
            var colony = new Colony(buildMarket(false, true), ColonyKind.COLONY, true);

            assertThat(colony.isDiscoveredByPlayer())
                .isFalse();
        }
    }

    @Nested
    class IsSightedByPlayer {

        @Test
        void reports_a_colony_in_a_system_the_player_has_entered_as_sighted() {

            var colony = new Colony(buildMarketInSystem(true), ColonyKind.COLONY, true);

            assertThat(colony.isSightedByPlayer())
                .isTrue();
        }

        @Test
        void reports_a_colony_in_a_system_the_player_has_never_entered_as_unsighted() {

            var colony = new Colony(buildMarketInSystem(false), ColonyKind.COLONY, true);

            assertThat(colony.isSightedByPlayer())
                .isFalse();
        }

        @Test
        void reports_a_colony_standing_in_no_star_system_as_sighted() {
            // A hyperspace colony, which mods build and vanilla does not. There is no system to
            // have been in and none to be settled, so a gate answering otherwise would withhold
            // it for the whole campaign rather than until somebody saw it.
            var colony = new Colony(buildMarketIn(mock(LocationAPI.class)), ColonyKind.COLONY, true);

            assertThat(colony.isSightedByPlayer())
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

    // A market standing in a star system the player has or has not been to.
    private static MarketAPI buildMarketInSystem(boolean isEnteredByPlayer) {

        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.isEnteredByPlayer())
            .thenReturn(isEnteredByPlayer);

        return buildMarketIn(systemMock);
    }

    // A market standing in a given location, which the sighting read is the only consumer of.
    private static MarketAPI buildMarketIn(LocationAPI location) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getContainingLocation())
            .thenReturn(location);

        return marketMock;
    }
}
