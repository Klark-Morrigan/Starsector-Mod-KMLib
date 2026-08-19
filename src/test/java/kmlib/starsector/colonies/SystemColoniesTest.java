package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Pins the contract of {@link SystemColonies#readColoniesIn}. The cases live in a {@link Nested}
 * group so the suite reports as a per-method tree; the world they are posed against is
 * {@link ColonyFixture}, shared with the index's suite.
 *
 * <p>The selection rule this exercises is {@link LocationColonies}', reached through the
 * star-system reader because that is the surface callers hold. What the same rule yields
 * somewhere other than a system is {@link HyperspaceColoniesTest}'s.
 */
final class SystemColoniesTest {

    // The bigger of a colliding pair, so the case reads as "the larger wins" rather than as two
    // loose numbers. Its partner is the fixture's own default size.
    private static final int LARGER_COLONY_SIZE = 6;

    // The smaller of a colliding pair, for the cases posing the loser rather than the winner.
    private static final int SMALLER_COLONY_SIZE = 1;

    @Nested
    class ReadColoniesIn {

        @Test
        void yields_the_economy_s_own_colonies_ahead_of_the_ones_it_does_not_list() {
            // Economy order is load-bearing: a caller mirroring vanilla's claim mechanic settles
            // a tied contest on whichever market the economy reaches first, so the listed half
            // has to arrive first and in its own order.
            var fixture = new ColonyFixture("galatia");
            var ancyra = fixture.buildVisibleColony("independent");
            var academy = fixture.buildVisibleColony("independent");

            fixture.placeColoniesInSystem(ancyra, academy);
            fixture.listColoniesInEconomy(ancyra);

            assertThat(readColonies(fixture))
                .containsExactly(
                    new Colony(ancyra, ColonyKind.COLONY, true),
                    new Colony(academy, ColonyKind.COLONY, false));
        }

        @Test
        void marks_a_colony_the_economy_does_not_list() {
            // Galatia Academy: a real market on a real station that vanilla deliberately never
            // registers. A reader that only walked the economy would report the station as
            // nobody's, so it is admitted - and marked, since it carries no economy-fed weight.
            var fixture = new ColonyFixture("galatia");
            var academy = fixture.buildVisibleColony("independent");

            fixture.placeColoniesInSystem(academy);

            assertThat(readColonies(fixture))
                .containsExactly(new Colony(academy, ColonyKind.COLONY, false));
        }

        @Test
        void excludes_a_planet_s_condition_only_market() {
            // Every uninhabited planet carries one of these, hung on the entity and never
            // registered. Admitting them would put a neutral colony on every surveyed rock.
            var fixture = new ColonyFixture("corvus");

            fixture.placeColoniesInSystem(fixture.buildConditionOnlyMarket());

            assertThat(readColonies(fixture))
                .isEmpty();
        }

        @Test
        void admits_a_concealed_colony_and_marks_it_hidden() {

            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildFoundConcealedColony("pirates");

            fixture.placeColoniesInSystem(base);
            fixture.listColoniesInEconomy(base);

            var colonies = readColonies(fixture);

            assertThat(colonies)
                .containsExactly(new Colony(base, ColonyKind.COLONY, true));
            assertThat(colonies.get(0).isHidden())
                .isTrue();
        }

        @Test
        void yields_one_colony_where_two_market_objects_share_a_place_and_owner() {
            // A mod supersedes a colony by adding its own market beside vanilla's on the same
            // station rather than replacing it. Counted per market, that colony is banked twice
            // and its owner reads as holding twice what it holds.
            var fixture = new ColonyFixture("galatia");
            var vanillaMarket = fixture.buildVisibleColony("independent");
            var moddedMarket = fixture.buildSiblingMarketOn(vanillaMarket, LARGER_COLONY_SIZE);

            fixture.placeColoniesInSystem(vanillaMarket);
            fixture.listColoniesInEconomy(vanillaMarket, moddedMarket);

            assertThat(readColonies(fixture))
                .containsExactly(new Colony(moddedMarket, ColonyKind.COLONY, true));
        }

        @Test
        void marks_a_derelict_station_as_an_abandoned_station() {
            // The set admits it like any other owned market - what changes is that the colony
            // says what it is, so a reader downstream is not left to take a hulk for a town.
            var fixture = new ColonyFixture("corvus");
            var derelict = fixture.buildDerelictStation("neutral");

            fixture.placeColoniesInSystem(derelict);

            assertThat(readColonies(fixture))
                .containsExactly(new Colony(derelict, ColonyKind.ABANDONED_STATION, false));
        }

        @Test
        void takes_a_colliding_pair_s_kind_from_the_market_that_wins_the_place() {
            // Kind and place have to be settled by the same market. The loser is named first
            // here, so a resolution reading the kind off anything but the winner reports the
            // derelict as a settlement.
            var fixture = new ColonyFixture("corvus");
            var derelict = fixture.buildDerelictStation("neutral");
            var supersededMarket = fixture.buildSiblingMarketOn(derelict, SMALLER_COLONY_SIZE);

            fixture.placeColoniesInSystem(derelict);
            fixture.listColoniesInEconomy(supersededMarket, derelict);

            assertThat(readColonies(fixture))
                .containsExactly(new Colony(derelict, ColonyKind.ABANDONED_STATION, true));
        }

        @Test
        void yields_a_colony_the_player_has_not_found() {
            // The set is unfogged on purpose: claim scoring weighs colonies the player has never
            // found, and a fogged input would resolve a claimant vanilla does not report.
            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            fixture.placeColoniesInSystem(base);
            fixture.listColoniesInEconomy(base);

            assertThat(readColonies(fixture))
                .containsExactly(new Colony(base, ColonyKind.COLONY, true));
        }

        @Test
        void yields_nothing_for_a_null_sector() {
            assertThat(SystemColonies.readColoniesIn(null, mock(StarSystemAPI.class)))
                .isEqualTo(Colonies.NONE);
        }

        @Test
        void yields_nothing_for_a_null_system() {
            assertThat(SystemColonies.readColoniesIn(mock(SectorAPI.class), null))
                .isEqualTo(Colonies.NONE);
        }
    }

    private static List<Colony> readColonies(ColonyFixture fixture) {
        return SystemColonies
            .readColoniesIn(fixture.getSector(), fixture.getSystem())
            .colonies();
    }
}
