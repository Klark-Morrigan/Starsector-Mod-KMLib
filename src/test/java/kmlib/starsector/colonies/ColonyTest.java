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
 * Pins the contracts of {@link Colony#isHidden}, {@link Colony#isDiscoveredByPlayer},
 * {@link Colony#isSighted} and {@link Colony#readOwnerId}, the facts a colony answers
 * about itself rather than storing beside it - three of them straight off its own market, and the
 * sighting off its market read against a register of what has been observed and where. Each
 * method's cases live in a {@link Nested} group so the suite reports as a per-method tree; the
 * shared mock builders stay on the outer class.
 */
final class ColonyTest {

    private static final long OBSERVED_AT = 4_200L;
    private static final String COLONY_ID = "sentinel_gantries";
    private static final String SYSTEM_ID = "kumari_kandam";

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
    class IsSighted {

        @Test
        void reports_a_colony_seen_in_the_system_it_stands_in_as_sighted() {

            var colony = new Colony(buildMarketInSystem(), ColonyKind.COLONY, true);

            assertThat(colony.isSighted(buildSightingIn(SYSTEM_ID)))
                .isTrue();
        }

        @Test
        void reports_a_colony_seen_at_no_stated_moment_as_sighted() {
            // The rule reads the place and never the time, so an observation recorded before the
            // time was kept answers exactly as a timed one does. Anything else would have a save
            // upgrade take colonies off the map.
            var colony = new Colony(buildMarketInSystem(), ColonyKind.COLONY, true);
            ColonySightings sightings = colonyId ->
                ColonyObservation.createUndatedObservation(SYSTEM_ID);

            assertThat(colony.isSighted(sightings))
                .isTrue();
        }

        @Test
        void reports_a_colony_the_player_has_never_seen_as_unsighted() {

            var colony = new Colony(buildMarketInSystem(), ColonyKind.COLONY, true);

            assertThat(colony.isSighted(ColonySightings.NONE))
                .isFalse();
        }

        @Test
        void reports_a_colony_seen_somewhere_it_no_longer_stands_as_unsighted() {
            // The mover. A sighting names where the colony was met, so standing anywhere else is
            // being unseen again - which is the whole of what parts this from vanilla's memory of
            // having entered a system.
            var colony = new Colony(buildMarketInSystem(), ColonyKind.COLONY, true);

            assertThat(colony.isSighted(buildSightingIn("corvus")))
                .isFalse();
        }

        @Test
        void reports_a_colony_standing_in_no_star_system_as_sighted() {
            // A hyperspace colony, which mods build and vanilla does not. There is no system to
            // have been in and none to be settled, so a gate answering otherwise would withhold
            // it for the whole campaign rather than until somebody saw it.
            var colony = new Colony(buildMarketIn(mock(LocationAPI.class)), ColonyKind.COLONY, true);

            assertThat(colony.isSighted(ColonySightings.NONE))
                .isTrue();
        }

        @Test
        void reports_a_colony_as_unsighted_where_no_register_is_stated() {
            // An absent record of the player's travels is not a reason to suppose they travelled,
            // so the unstated case withholds rather than leaks.
            var colony = new Colony(buildMarketInSystem(), ColonyKind.COLONY, true);

            assertThat(colony.isSighted(null))
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

            assertThat(new Colony(marketMock, ColonyKind.COLONY, true).readOwnerId())
                .isEqualTo("pirates");
        }

        @Test
        void reports_no_owner_where_the_market_names_none() {
            // Absorbed rather than refused: an owner nobody can name is compared against the
            // settling ones like any other, and there is nothing here to fail on.
            var colony = new Colony(mock(MarketAPI.class), ColonyKind.COLONY, true);

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

    // A market standing in a star system, under an id of its own so a sighting can name it - or
    // name somewhere else, which is the only way the two answers part company.
    private static MarketAPI buildMarketInSystem() {

        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getId())
            .thenReturn(SYSTEM_ID);

        var marketMock = buildMarketIn(systemMock);

        when(marketMock.getId())
            .thenReturn(COLONY_ID);

        return marketMock;
    }

    // A register holding one sighting of the colony above, in whichever system a case names.
    private static ColonySightings buildSightingIn(String locationId) {

        return colonyId -> COLONY_ID.equals(colonyId)
            ? ColonyObservation.createObservationAt(locationId, OBSERVED_AT)
            : null;
    }

    // A market standing in a given location, which the sighting read is the only consumer of.
    private static MarketAPI buildMarketIn(LocationAPI location) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getContainingLocation())
            .thenReturn(location);

        return marketMock;
    }
}
