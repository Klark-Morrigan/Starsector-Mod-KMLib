package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Pins the contracts of {@link Colonies#readKnownColonies} and
 * {@link Colonies#hasKnownColony}, and the fixed-set guarantee its construction makes.
 * Each method's cases live in a {@link Nested} group so the suite reports as a per-method tree;
 * the world they are posed against is {@link ColonyFixture}, shared with the readers' suites.
 *
 * <p>How a set is selected in the first place belongs to whichever reader selected it, and is
 * pinned by {@link SystemColoniesTest} and {@link HyperspaceColoniesTest}.
 */
final class ColoniesTest {

    @Nested
    class Construct {

        @Test
        void reads_absent_colonies_as_an_empty_set() {
            assertThat(new Colonies(null))
                .isEqualTo(Colonies.NONE);
        }

        @Test
        void keeps_the_colonies_it_was_built_with_when_the_source_list_changes_later() {

            var colonies = new ArrayList<Colony>();
            colonies.add(new Colony(mock(MarketAPI.class), true));

            var set = new Colonies(colonies);

            colonies.clear();

            assertThat(set.colonies())
                .hasSize(1);
        }

        @Test
        void rejects_an_attempt_to_change_the_colonies() {
            // A set is memoised for a whole pass and handed to every reader in it, so one reader
            // able to change it would be rewriting the system underneath all the others.
            var set = new Colonies(List.of(new Colony(mock(MarketAPI.class), true)));

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
            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).readKnownColonies(false))
                .isEmpty();
        }

        @Test
        void restores_a_colony_the_player_has_not_found_under_the_reveal() {

            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).readKnownColonies(true))
                .containsExactly(new Colony(base, true));
        }

        @Test
        void keeps_a_concealed_colony_the_player_has_found() {
            // A raided pirate base stays permanently hidden while being perfectly well known, so
            // concealment alone must not fog it out.
            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildFoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).readKnownColonies(false))
                .containsExactly(new Colony(base, true));
        }

        @Test
        void keeps_the_set_s_own_order() {

            var fixture = new ColonyFixture("corvus");
            var first = fixture.buildVisibleColony("hegemony");
            var second = fixture.buildVisibleColony("tritachyon");

            assertThat(buildColoniesOf(first, second).readKnownColonies(false))
                .containsExactly(
                    new Colony(first, true),
                    new Colony(second, true));
        }
    }

    @Nested
    class HasKnownColony {

        @Test
        void answers_false_for_a_system_holding_nothing() {
            assertThat(Colonies.NONE.hasKnownColony(false))
                .isFalse();
        }

        @Test
        void answers_true_for_an_ordinary_colony() {

            var fixture = new ColonyFixture("corvus");

            assertThat(buildColoniesOf(fixture.buildVisibleColony("hegemony")).hasKnownColony(false))
                .isTrue();
        }

        @Test
        void answers_true_for_a_concealed_colony_the_player_has_found() {
            // A raided base is concealed for good and plainly known, so an emptiness read gated on
            // public listing would call its system empty while the player is standing in it.
            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildFoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).hasKnownColony(false))
                .isTrue();
        }

        @Test
        void answers_false_for_a_colony_the_player_has_not_found() {
            // The one shape the fog keeps back. Reporting its system as occupied is itself the
            // tell that something is hiding there.
            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).hasKnownColony(false))
                .isFalse();
        }

        @Test
        void answers_true_for_a_colony_the_player_has_not_found_under_the_reveal() {

            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).hasKnownColony(true))
                .isTrue();
        }

        @Test
        void agrees_with_the_projection_it_asks_the_emptiness_of() {
            // The claim the short-circuit rests on: skipping the list must not change the answer,
            // and a set mixing a fogged colony with a visible one is where a filter that had
            // drifted between the two reads would show it.
            var fixture = new ColonyFixture("kumari_kandam");
            var fogged = fixture.buildUnfoundConcealedColony("pirates");
            var visible = fixture.buildVisibleColony("independent");

            var foggedOnly = buildColoniesOf(fogged);
            var mixed = buildColoniesOf(fogged, visible);

            assertThat(foggedOnly.readKnownColonies(false))
                .isEmpty();
            assertThat(foggedOnly.hasKnownColony(false))
                .isFalse();

            assertThat(mixed.readKnownColonies(false))
                .containsExactly(new Colony(visible, true));
            assertThat(mixed.hasKnownColony(false))
                .isTrue();
        }
    }

    // A colony set built straight from markets, for a case about the projection rather than about
    // the walk that gathers the set.
    private static Colonies buildColoniesOf(MarketAPI... markets) {

        var colonies = new ArrayList<Colony>();
        for (var market : markets) {
            colonies.add(new Colony(market, true));
        }
        return new Colonies(colonies);
    }
}
