package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Pins the contracts of {@link SystemColonies#readColoniesIn},
 * {@link SystemColonies#readKnownColonies} and {@link SystemColonies#hasKnownColony}, and the
 * fixed-set guarantee its construction makes.
 * Each method's cases live in a {@link Nested} group so the suite reports as a per-method tree;
 * the world they are posed against is {@link SystemColonyFixture}, shared with the index's suite.
 */
final class SystemColoniesTest {

    // The bigger of a colliding pair, so the case reads as "the larger wins" rather than as two
    // loose numbers. Its partner is the fixture's own default size.
    private static final int LARGER_COLONY_SIZE = 6;

    @Nested
    class ReadColoniesIn {

        @Test
        void yields_the_economy_s_own_colonies_ahead_of_the_ones_it_does_not_list() {
            // Economy order is load-bearing: a caller mirroring vanilla's claim mechanic settles
            // a tied contest on whichever market the economy reaches first, so the listed half
            // has to arrive first and in its own order.
            var fixture = new SystemColonyFixture("galatia");
            var ancyra = fixture.buildVisibleColony("independent");
            var academy = fixture.buildVisibleColony("independent");

            fixture.placeColoniesInSystem(ancyra, academy);
            fixture.listColoniesInEconomy(ancyra);

            assertThat(readColonies(fixture))
                .containsExactly(
                    new SystemColony(ancyra, true),
                    new SystemColony(academy, false));
        }

        @Test
        void marks_a_colony_the_economy_does_not_list() {
            // Galatia Academy: a real market on a real station that vanilla deliberately never
            // registers. A reader that only walked the economy would report the station as
            // nobody's, so it is admitted - and marked, since it carries no economy-fed weight.
            var fixture = new SystemColonyFixture("galatia");
            var academy = fixture.buildVisibleColony("independent");

            fixture.placeColoniesInSystem(academy);

            assertThat(readColonies(fixture))
                .containsExactly(new SystemColony(academy, false));
        }

        @Test
        void excludes_a_planet_s_condition_only_market() {
            // Every uninhabited planet carries one of these, hung on the entity and never
            // registered. Admitting them would put a neutral colony on every surveyed rock.
            var fixture = new SystemColonyFixture("corvus");

            fixture.placeColoniesInSystem(fixture.buildConditionOnlyMarket());

            assertThat(readColonies(fixture))
                .isEmpty();
        }

        @Test
        void admits_a_concealed_colony_and_marks_it_hidden() {

            var fixture = new SystemColonyFixture("kumari_kandam");
            var base = fixture.buildFoundConcealedColony("pirates");

            fixture.placeColoniesInSystem(base);
            fixture.listColoniesInEconomy(base);

            var colonies = readColonies(fixture);

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
            var fixture = new SystemColonyFixture("galatia");
            var vanillaMarket = fixture.buildVisibleColony("independent");
            var moddedMarket = fixture.buildSiblingMarketOn(vanillaMarket, LARGER_COLONY_SIZE);

            fixture.placeColoniesInSystem(vanillaMarket);
            fixture.listColoniesInEconomy(vanillaMarket, moddedMarket);

            assertThat(readColonies(fixture))
                .containsExactly(new SystemColony(moddedMarket, true));
        }

        @Test
        void yields_a_colony_the_player_has_not_found() {
            // The set is unfogged on purpose: claim scoring weighs colonies the player has never
            // found, and a fogged input would resolve a claimant vanilla does not report.
            var fixture = new SystemColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            fixture.placeColoniesInSystem(base);
            fixture.listColoniesInEconomy(base);

            assertThat(readColonies(fixture))
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

            var colonies = new ArrayList<SystemColony>();
            colonies.add(new SystemColony(mock(MarketAPI.class), true));

            var set = new SystemColonies(colonies);

            colonies.clear();

            assertThat(set.colonies())
                .hasSize(1);
        }

        @Test
        void rejects_an_attempt_to_change_the_colonies() {
            // A set is memoised for a whole pass and handed to every reader in it, so one reader
            // able to change it would be rewriting the system underneath all the others.
            var set = new SystemColonies(List.of(new SystemColony(mock(MarketAPI.class), true)));

            assertThatThrownBy(() -> set.colonies().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class ReadKnownColonies {

        @Test
        void excludes_a_colony_the_player_has_not_found() {
            // Concealed and on an undiscovered entity: the one shape that fails both arms of the
            // known read, and the one the fog has to keep back - naming its owner in a box would
            // tell the player exactly what is hiding out there.
            var fixture = new SystemColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).readKnownColonies(false))
                .isEmpty();
        }

        @Test
        void restores_a_colony_the_player_has_not_found_under_the_reveal() {

            var fixture = new SystemColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).readKnownColonies(true))
                .containsExactly(new SystemColony(base, true));
        }

        @Test
        void keeps_a_concealed_colony_the_player_has_found() {
            // A raided pirate base stays permanently hidden while being perfectly well known, so
            // concealment alone must not fog it out.
            var fixture = new SystemColonyFixture("kumari_kandam");
            var base = fixture.buildFoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).readKnownColonies(false))
                .containsExactly(new SystemColony(base, true));
        }

        @Test
        void keeps_the_set_s_own_order() {

            var fixture = new SystemColonyFixture("corvus");
            var first = fixture.buildVisibleColony("hegemony");
            var second = fixture.buildVisibleColony("tritachyon");

            assertThat(buildColoniesOf(first, second).readKnownColonies(false))
                .containsExactly(
                    new SystemColony(first, true),
                    new SystemColony(second, true));
        }
    }

    @Nested
    class HasKnownColony {

        @Test
        void answers_false_for_a_system_holding_nothing() {
            assertThat(SystemColonies.NONE.hasKnownColony(false))
                .isFalse();
        }

        @Test
        void answers_true_for_an_ordinary_colony() {

            var fixture = new SystemColonyFixture("corvus");

            assertThat(buildColoniesOf(fixture.buildVisibleColony("hegemony")).hasKnownColony(false))
                .isTrue();
        }

        @Test
        void answers_true_for_a_concealed_colony_the_player_has_found() {
            // A raided base is concealed for good and plainly known, so an emptiness read gated on
            // public listing would call its system empty while the player is standing in it.
            var fixture = new SystemColonyFixture("kumari_kandam");
            var base = fixture.buildFoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).hasKnownColony(false))
                .isTrue();
        }

        @Test
        void answers_false_for_a_colony_the_player_has_not_found() {
            // The one shape the fog keeps back. Reporting its system as occupied is itself the
            // tell that something is hiding there.
            var fixture = new SystemColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).hasKnownColony(false))
                .isFalse();
        }

        @Test
        void answers_true_for_a_colony_the_player_has_not_found_under_the_reveal() {

            var fixture = new SystemColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).hasKnownColony(true))
                .isTrue();
        }

        @Test
        void agrees_with_the_projection_it_asks_the_emptiness_of() {
            // The claim the short-circuit rests on: skipping the list must not change the answer,
            // and a set mixing a fogged colony with a visible one is where a filter that had
            // drifted between the two reads would show it.
            var fixture = new SystemColonyFixture("kumari_kandam");
            var fogged = fixture.buildUnfoundConcealedColony("pirates");
            var visible = fixture.buildVisibleColony("independent");

            var foggedOnly = buildColoniesOf(fogged);
            var mixed = buildColoniesOf(fogged, visible);

            assertThat(foggedOnly.readKnownColonies(false))
                .isEmpty();
            assertThat(foggedOnly.hasKnownColony(false))
                .isFalse();

            assertThat(mixed.readKnownColonies(false))
                .containsExactly(new SystemColony(visible, true));
            assertThat(mixed.hasKnownColony(false))
                .isTrue();
        }
    }

    private static List<SystemColony> readColonies(SystemColonyFixture fixture) {
        return SystemColonies
            .readColoniesIn(fixture.getSector(), fixture.getSystem())
            .colonies();
    }

    // A colony set built straight from markets, for a case about the projection rather than about
    // the walk that gathers the set.
    private static SystemColonies buildColoniesOf(MarketAPI... markets) {

        var colonies = new ArrayList<SystemColony>();
        for (var market : markets) {
            colonies.add(new SystemColony(market, true));
        }
        return new SystemColonies(colonies);
    }
}
