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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link SystemColoniesIndex#readColoniesIn} and
 * {@link SystemColoniesIndex#readColoniesById}: that the index answers exactly what the direct
 * read answers, and that it pays for a system's walk once however it is asked. Each method's
 * cases live in a {@link Nested} group so the suite reports as a per-method tree; the shared
 * mock builders stay on the outer class.
 */
final class SystemColoniesIndexTest {

    @Nested
    class ReadColoniesIn {

        @Test
        void answers_a_system_exactly_as_the_direct_read_does() {
            // The whole point of the index is that a reader handed it is not reading anything
            // narrower than a reader handed the sector, so the two answers have to be the same.
            var listedColony = buildVisibleColony("independent");
            var unlistedColony = buildVisibleColony("independent");
            var system = buildSystemHolding("galatia", listedColony, unlistedColony);
            var sector = buildSectorListing(system, listedColony);

            assertThat(new SystemColoniesIndex(sector).readColoniesIn(system))
                .isEqualTo(SystemColonies.readColoniesIn(sector, system));
        }

        @Test
        void walks_a_system_once_across_repeated_asks() {
            // A pass asks the same system's colonies several times over - who holds it, how many
            // to draw, what to name in a hover - and paying a traversal for each is what made a
            // rebuild cost two or three walks per system.
            var colony = buildVisibleColony("hegemony");
            var system = buildSystemHolding("corvus", colony);
            var sector = buildSectorListing(system, colony);
            var index = new SystemColoniesIndex(sector);

            index.readColoniesIn(system);
            index.readColoniesIn(system);

            verify(system, times(1)).getAllEntities();
        }

        @Test
        void walks_a_system_carrying_no_id_afresh_on_every_ask() {
            // There is nothing to key the memo on, so the walk is paid again - which is the
            // honest price of an unkeyable system, pooling every one of them under a shared key
            // being the alternative, and that hands one system's colonies to another.
            var colony = buildVisibleColony("hegemony");
            var system = buildSystemHolding(null, colony);
            var sector = buildSectorListing(system, colony);
            var index = new SystemColoniesIndex(sector);

            assertThat(index.readColoniesIn(system).colonies())
                .containsExactly(new SystemColony(colony, true));

            index.readColoniesIn(system);

            verify(system, times(2)).getAllEntities();
        }

        @Test
        void yields_nothing_for_a_null_system() {
            assertThat(new SystemColoniesIndex(mock(SectorAPI.class)).readColoniesIn(null))
                .isEqualTo(SystemColonies.NONE);
        }

        @Test
        void yields_nothing_for_every_system_when_the_sector_is_unreachable() {

            var system = buildSystemHolding("corvus", buildVisibleColony("hegemony"));

            assertThat(new SystemColoniesIndex(null).readColoniesIn(system))
                .isEqualTo(SystemColonies.NONE);
        }
    }

    @Nested
    class ReadColoniesById {

        @Test
        void answers_the_system_carrying_that_id() {

            var colony = buildVisibleColony("hegemony");
            var system = buildSystemHolding("corvus", colony);
            var sector = buildSectorListing(system, colony);

            assertThat(new SystemColoniesIndex(sector).readColoniesById("corvus").colonies())
                .containsExactly(new SystemColony(colony, true));
        }

        @Test
        void answers_off_the_walk_a_read_made_with_the_system_already_paid_for() {
            // A pass keyed by system id and a reader holding the system itself are asking the
            // same question, so the second route must not buy a second traversal.
            var colony = buildVisibleColony("hegemony");
            var system = buildSystemHolding("corvus", colony);
            var sector = buildSectorListing(system, colony);
            var index = new SystemColoniesIndex(sector);

            index.readColoniesIn(system);
            index.readColoniesById("corvus");

            verify(system, times(1)).getAllEntities();
        }

        @Test
        void yields_nothing_for_an_id_no_system_carries() {

            var colony = buildVisibleColony("hegemony");
            var system = buildSystemHolding("corvus", colony);
            var sector = buildSectorListing(system, colony);

            assertThat(new SystemColoniesIndex(sector).readColoniesById("askonia"))
                .isEqualTo(SystemColonies.NONE);
        }

        @Test
        void resolves_the_sector_s_systems_once_across_repeated_asks() {
            // Resolving an id is the index's other walk, and an id no system carries leaves no
            // colony memo to answer off - so without keeping the resolution, a pass asking about
            // absent systems would re-index the whole sector on every ask.
            var colony = buildVisibleColony("hegemony");
            var system = buildSystemHolding("corvus", colony);
            var sector = buildSectorListing(system, colony);
            var index = new SystemColoniesIndex(sector);

            index.readColoniesById("askonia");
            index.readColoniesById("tyle");

            verify(sector, times(1)).getStarSystems();
        }

        @Test
        void yields_nothing_for_a_blank_id() {
            assertThat(new SystemColoniesIndex(mock(SectorAPI.class)).readColoniesById(" "))
                .isEqualTo(SystemColonies.NONE);
        }
    }

    // A system carrying the entities its markets sit on, so an unlisted colony has something to
    // be found on and the entity walk has something to count.
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

        // The entity and the faction each finish their own stubbing before the market's opens,
        // so the two do not nest into an unfinished-stubbing error.
        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.isDiscoverable())
            .thenReturn(false);

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(factionId);

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(factionMock);
        when(marketMock.getPrimaryEntity())
            .thenReturn(entityMock);
        when(entityMock.getMarket())
            .thenReturn(marketMock);

        return marketMock;
    }
}
