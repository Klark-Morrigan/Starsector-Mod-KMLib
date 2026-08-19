package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.starsector.colonies.Colonies;
import kmlib.starsector.colonies.Colony;
import kmlib.starsector.colonies.ColonyFixture;
import kmlib.starsector.colonies.SystemColonies;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Pins the contracts of {@link SystemColoniesIndex#readColoniesIn},
 * {@link SystemColoniesIndex#readColoniesById} and {@link SystemColoniesIndex#getSector}: that
 * the index answers exactly what the direct read answers, that it pays for a system's walk once
 * however it is asked, and that it names the sector it answers out of. Each method's
 * cases live in a {@link Nested} group so the suite reports as a per-method tree; the world they
 * are posed against is {@link ColonyFixture}, shared with the direct read's suite - which
 * is what lets the two answers be compared at all.
 */
final class SystemColoniesIndexTest {

    @Nested
    class GetSector {

        @Test
        void names_the_sector_the_index_was_opened_over() {
            // A caller holding an index holds no sector beside it, so the index has to be able to
            // name the one its answers came out of.
            var fixture = new ColonyFixture("galatia");

            assertThat(new SystemColoniesIndex(fixture.getSector()).getSector())
                .isSameAs(fixture.getSector());
        }

        @Test
        void names_no_sector_when_the_index_was_opened_over_none() {
            // The unreachable-sector case answers an empty set for every system, and it reports
            // the absence rather than inventing a sector to name.
            assertThat(new SystemColoniesIndex(null).getSector())
                .isNull();
        }
    }

    @Nested
    class ReadColoniesIn {

        @Test
        void answers_a_system_exactly_as_the_direct_read_does() {
            // The whole point of the index is that a reader handed it is not reading anything
            // narrower than a reader handed the sector, so the two answers have to be the same.
            var fixture = new ColonyFixture("galatia");
            var listedColony = fixture.buildVisibleColony("independent");
            var unlistedColony = fixture.buildVisibleColony("independent");

            fixture.placeColoniesInSystem(listedColony, unlistedColony);
            fixture.listColoniesInEconomy(listedColony);

            assertThat(new SystemColoniesIndex(fixture.getSector())
                    .readColoniesIn(fixture.getSystem()))
                .isEqualTo(SystemColonies.readColoniesIn(
                    fixture.getSector(),
                    fixture.getSystem()));
        }

        @Test
        void walks_a_system_once_across_repeated_asks() {
            // A pass asks the same system's colonies several times over - who holds it, how many
            // to draw, what to name in a hover - and paying a traversal for each is what made a
            // rebuild cost two or three walks per system.
            var fixture = buildCorvusHoldingOneColony();
            var index = new SystemColoniesIndex(fixture.getSector());

            index.readColoniesIn(fixture.getSystem());
            index.readColoniesIn(fixture.getSystem());

            verify(fixture.getSystem(), times(1)).getAllEntities();
        }

        @Test
        void walks_a_system_carrying_no_id_afresh_on_every_ask() {
            // There is nothing to key the memo on, so the walk is paid again - which is the
            // honest price of an unkeyable system, pooling every one of them under a shared key
            // being the alternative, and that hands one system's colonies to another.
            var fixture = new ColonyFixture(null);
            var colony = fixture.buildVisibleColony("hegemony");

            fixture.placeColoniesInSystem(colony);
            fixture.listColoniesInEconomy(colony);

            var index = new SystemColoniesIndex(fixture.getSector());

            assertThat(index.readColoniesIn(fixture.getSystem()).colonies())
                .containsExactly(new Colony(colony, true));

            index.readColoniesIn(fixture.getSystem());

            verify(fixture.getSystem(), times(2)).getAllEntities();
        }

        @Test
        void yields_nothing_for_a_null_system() {
            assertThat(new SystemColoniesIndex(mock(SectorAPI.class)).readColoniesIn(null))
                .isEqualTo(Colonies.NONE);
        }

        @Test
        void yields_nothing_for_every_system_when_the_sector_is_unreachable() {

            var fixture = buildCorvusHoldingOneColony();

            assertThat(new SystemColoniesIndex(null).readColoniesIn(fixture.getSystem()))
                .isEqualTo(Colonies.NONE);
        }
    }

    @Nested
    class ReadColoniesById {

        @Test
        void answers_the_system_carrying_that_id() {

            var fixture = new ColonyFixture("corvus");
            var colony = fixture.buildVisibleColony("hegemony");

            fixture.placeColoniesInSystem(colony);
            fixture.listColoniesInEconomy(colony);

            assertThat(new SystemColoniesIndex(fixture.getSector())
                    .readColoniesById("corvus")
                    .colonies())
                .containsExactly(new Colony(colony, true));
        }

        @Test
        void answers_off_the_walk_a_read_made_with_the_system_already_paid_for() {
            // A pass keyed by system id and a reader holding the system itself are asking the
            // same question, so the second route must not buy a second traversal.
            var fixture = buildCorvusHoldingOneColony();
            var index = new SystemColoniesIndex(fixture.getSector());

            index.readColoniesIn(fixture.getSystem());
            index.readColoniesById("corvus");

            verify(fixture.getSystem(), times(1)).getAllEntities();
        }

        @Test
        void yields_nothing_for_an_id_no_system_carries() {

            var fixture = buildCorvusHoldingOneColony();

            assertThat(new SystemColoniesIndex(fixture.getSector()).readColoniesById("askonia"))
                .isEqualTo(Colonies.NONE);
        }

        @Test
        void resolves_the_sector_s_systems_once_across_repeated_asks() {
            // Resolving an id is the index's other walk, and an id no system carries leaves no
            // colony memo to answer off - so without keeping the resolution, a pass asking about
            // absent systems would re-index the whole sector on every ask.
            var fixture = buildCorvusHoldingOneColony();
            var index = new SystemColoniesIndex(fixture.getSector());

            index.readColoniesById("askonia");
            index.readColoniesById("tyle");

            verify(fixture.getSector(), times(1)).getStarSystems();
        }

        @Test
        void yields_nothing_for_a_blank_id() {
            assertThat(new SystemColoniesIndex(mock(SectorAPI.class)).readColoniesById(" "))
                .isEqualTo(Colonies.NONE);
        }
    }

    // The plainest system the index can be posed with: one ordinary colony, both sited and
    // listed, for the cases about how often a walk is paid rather than about what it finds.
    private static ColonyFixture buildCorvusHoldingOneColony() {

        var fixture = new ColonyFixture("corvus");
        var colony = fixture.buildVisibleColony("hegemony");

        fixture.placeColoniesInSystem(colony);
        fixture.listColoniesInEconomy(colony);

        return fixture;
    }
}
