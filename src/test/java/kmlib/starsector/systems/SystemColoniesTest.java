package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link SystemColonies#readColoniesIn} and
 * {@link SystemColonies#readKnownColonies}. Each method's cases live in a {@link Nested} group
 * so the suite reports as a per-method tree; the shared mock builders stay on the outer class.
 */
final class SystemColoniesTest {

    // Colony sizes, named so a case reads as "the bigger of the pair" rather than as two loose
    // numbers, and so nothing depends on the particular values.
    private static final int LARGER_COLONY_SIZE = 6;
    private static final int SMALLER_COLONY_SIZE = 3;

    @Nested
    class ReadColoniesIn {

        @Test
        void yields_the_economy_s_own_colonies_ahead_of_the_ones_it_does_not_list() {
            // Economy order is load-bearing: a caller mirroring vanilla's claim mechanic settles
            // a tied contest on whichever market the economy reaches first, so the listed half
            // has to arrive first and in its own order.
            var ancyra = buildVisibleColony("independent");
            var academy = buildVisibleColony("independent");
            var system = buildSystemHolding("galatia", ancyra, academy);
            var sector = buildSectorListing(system, ancyra);

            assertThat(SystemColonies.readColoniesIn(sector, system).colonies())
                .containsExactly(
                    new SystemColony(ancyra, true),
                    new SystemColony(academy, false));
        }

        @Test
        void marks_a_colony_the_economy_does_not_list() {
            // Galatia Academy: a real market on a real station that vanilla deliberately never
            // registers. A reader that only walked the economy would report the station as
            // nobody's, so it is admitted - and marked, since it carries no economy-fed weight.
            var academy = buildVisibleColony("independent");
            var system = buildSystemHolding("galatia", academy);
            var sector = buildSectorListing(system);

            assertThat(SystemColonies.readColoniesIn(sector, system).colonies())
                .containsExactly(new SystemColony(academy, false));
        }

        @Test
        void excludes_a_planet_s_condition_only_market() {
            // Every uninhabited planet carries one of these to hold its hazard and atmosphere,
            // hung on the entity and never registered with the economy. Admitting them would put
            // a neutral colony on every surveyed rock in the sector.
            var rock = buildConditionOnlyMarket();
            var system = buildSystemHolding("corvus", rock);
            var sector = buildSectorListing(system);

            assertThat(SystemColonies.readColoniesIn(sector, system).colonies())
                .isEmpty();
        }

        @Test
        void admits_a_concealed_colony_and_marks_it_hidden() {

            var base = buildFoundConcealedColony("pirates");
            var system = buildSystemHolding("kumari_kandam", base);
            var sector = buildSectorListing(system, base);

            var colonies = SystemColonies.readColoniesIn(sector, system).colonies();

            assertThat(colonies)
                .containsExactly(new SystemColony(base, true));
            assertThat(colonies.get(0).isHidden())
                .isTrue();
        }

        @Test
        void yields_one_colony_where_two_market_objects_share_a_place_and_owner() {
            // A mod supersedes a colony by adding its own market beside vanilla's on the same
            // station rather than replacing it. Counted per market, that colony is banked twice
            // and its owner reads as holding twice what it holds.
            var vanillaMarket = buildVisibleColonyOfSize("independent", SMALLER_COLONY_SIZE);
            var moddedMarket = buildSiblingMarketOn(vanillaMarket, LARGER_COLONY_SIZE);
            var system = buildSystemHolding("galatia", vanillaMarket);
            var sector = buildSectorListing(system, vanillaMarket, moddedMarket);

            assertThat(SystemColonies.readColoniesIn(sector, system).colonies())
                .containsExactly(new SystemColony(moddedMarket, true));
        }

        @Test
        void yields_a_colony_the_player_has_not_found() {
            // The set is unfogged on purpose: claim scoring weighs colonies the player has never
            // found, and a fogged input would resolve a claimant vanilla does not report.
            var base = buildUnfoundConcealedColony("pirates");
            var system = buildSystemHolding("kumari_kandam", base);
            var sector = buildSectorListing(system, base);

            assertThat(SystemColonies.readColoniesIn(sector, system).colonies())
                .containsExactly(new SystemColony(base, true));
        }

        @Test
        void yields_nothing_for_a_null_sector() {
            assertThat(SystemColonies.readColoniesIn(null, mock(StarSystemAPI.class)))
                .isEqualTo(SystemColonies.NONE);
        }

        @Test
        void yields_nothing_for_a_null_system() {
            assertThat(SystemColonies.readColoniesIn(mock(SectorAPI.class), null))
                .isEqualTo(SystemColonies.NONE);
        }
    }

    @Nested
    class Construct {

        @Test
        void reads_absent_colonies_as_an_empty_set() {
            assertThat(new SystemColonies(null))
                .isEqualTo(SystemColonies.NONE);
        }

        @Test
        void keeps_the_colonies_it_was_built_with_when_the_source_list_changes_later() {
            // A set is read repeatedly across a pass, so a caller able to see it change - or
            // change it - would be reading a different system each time it looked.
            var colonies = new ArrayList<SystemColony>();
            colonies.add(new SystemColony(buildVisibleColony("hegemony"), true));

            var set = new SystemColonies(colonies);

            colonies.clear();

            assertThat(set.colonies())
                .hasSize(1);
        }
    }

    @Nested
    class ReadKnownColonies {

        @Test
        void excludes_a_colony_the_player_has_not_found() {
            // Concealed and on an undiscovered entity: the one shape that fails both arms of the
            // known read, and the one the fog has to keep back - naming its owner in a box would
            // tell the player exactly what is hiding out there.
            var base = buildUnfoundConcealedColony("pirates");
            var colonies = buildColoniesOf(base);

            assertThat(colonies.readKnownColonies(false))
                .isEmpty();
        }

        @Test
        void restores_a_colony_the_player_has_not_found_under_the_reveal() {

            var base = buildUnfoundConcealedColony("pirates");
            var colonies = buildColoniesOf(base);

            assertThat(colonies.readKnownColonies(true))
                .containsExactly(new SystemColony(base, true));
        }

        @Test
        void keeps_a_concealed_colony_the_player_has_found() {
            // A raided pirate base stays permanently hidden while being perfectly well known, so
            // concealment alone must not fog it out.
            var base = buildFoundConcealedColony("pirates");
            var colonies = buildColoniesOf(base);

            assertThat(colonies.readKnownColonies(false))
                .containsExactly(new SystemColony(base, true));
        }

        @Test
        void keeps_the_set_s_own_order() {

            var first = buildVisibleColony("hegemony");
            var second = buildVisibleColony("tritachyon");
            var colonies = buildColoniesOf(first, second);

            assertThat(colonies.readKnownColonies(false))
                .containsExactly(
                    new SystemColony(first, true),
                    new SystemColony(second, true));
        }
    }

    // A colony set built straight from markets, for a case about the projection rather than
    // about the walk that gathers the set.
    private static SystemColonies buildColoniesOf(MarketAPI... markets) {

        var colonies = new ArrayList<SystemColony>();
        for (var market : markets) {
            colonies.add(new SystemColony(market, true));
        }
        return new SystemColonies(colonies);
    }

    // A system carrying the entities its markets sit on - the second half of "what is in this
    // system", beside the economy's own listing, and the only way an unlisted colony is reached.
    private static StarSystemAPI buildSystemHolding(String systemId, MarketAPI... markets) {

        var entities = new ArrayList<SectorEntityToken>();
        for (var market : markets) {
            entities.add(market.getPrimaryEntity());
        }

        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getId())
            .thenReturn(systemId);
        when(systemMock.getAllEntities())
            .thenReturn(entities);

        return systemMock;
    }

    // A sector whose economy lists exactly these markets for the system. What is left out is
    // what the unlisted read has to find on the system's entities.
    private static SectorAPI buildSectorListing(
            StarSystemAPI system,
            MarketAPI... listedMarkets) {

        var economyMock = mock(EconomyAPI.class);

        when(economyMock.getMarkets(system))
            .thenReturn(List.of(listedMarkets));

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getEconomy())
            .thenReturn(economyMock);
        when(sectorMock.getStarSystems())
            .thenReturn(List.of(system));

        return sectorMock;
    }

    // An ordinary colony: owned, open, on an entity the player has found.
    private static MarketAPI buildVisibleColony(String factionId) {
        return buildVisibleColonyOfSize(factionId, SMALLER_COLONY_SIZE);
    }

    private static MarketAPI buildVisibleColonyOfSize(String factionId, int size) {
        return buildColonyOnItsOwnEntity(factionId, size, false, false, false);
    }

    // A base once raided: its entity is discovered, its market stays hidden for good.
    private static MarketAPI buildFoundConcealedColony(String factionId) {
        return buildColonyOnItsOwnEntity(factionId, SMALLER_COLONY_SIZE, true, false, false);
    }

    // A base still to be found: concealed and on a discoverable entity, so it fails both arms
    // of the known read.
    private static MarketAPI buildUnfoundConcealedColony(String factionId) {
        return buildColonyOnItsOwnEntity(factionId, SMALLER_COLONY_SIZE, true, true, false);
    }

    // A bare planet's placeholder: owned by nobody in particular and condition-only, which is
    // the arm the ownership rule rejects it on.
    private static MarketAPI buildConditionOnlyMarket() {
        return buildColonyOnItsOwnEntity("neutral", SMALLER_COLONY_SIZE, false, false, true);
    }

    // A market on an entity of its own, wired both ways: the market names the entity as its
    // place, and the entity carries the market - which is how an unlisted colony is found at
    // all. Reached through the named builders above; the flags say nothing at a call site.
    private static MarketAPI buildColonyOnItsOwnEntity(
            String factionId,
            int size,
            boolean isHidden,
            boolean isEntityDiscoverable,
            boolean isConditionOnly) {

        // The entity and the faction each stub their own mock, so they are finished before the
        // market's stubbing opens and the two do not nest into an unfinished-stubbing error.
        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.isDiscoverable())
            .thenReturn(isEntityDiscoverable);

        var factionMock = buildFaction(factionId);
        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(factionMock);
        when(marketMock.getPrimaryEntity())
            .thenReturn(entityMock);
        when(marketMock.getSize())
            .thenReturn(size);
        when(marketMock.isHidden())
            .thenReturn(isHidden);
        when(marketMock.isPlanetConditionMarketOnly())
            .thenReturn(isConditionOnly);
        when(entityMock.getMarket())
            .thenReturn(marketMock);

        return marketMock;
    }

    // A second market object on an existing colony's entity, under the same owner - the shape a
    // mod builds when it supersedes a colony by adding beside vanilla's rather than replacing.
    private static MarketAPI buildSiblingMarketOn(MarketAPI market, int size) {

        // Read off the sibling before the new mock's stubbing opens, for the same reason the
        // builder above finishes its entity first.
        var faction = market.getFaction();
        var entity = market.getPrimaryEntity();
        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(faction);
        when(marketMock.getPrimaryEntity())
            .thenReturn(entity);
        when(marketMock.getSize())
            .thenReturn(size);

        return marketMock;
    }

    private static FactionAPI buildFaction(String id) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(id);

        return factionMock;
    }
}
