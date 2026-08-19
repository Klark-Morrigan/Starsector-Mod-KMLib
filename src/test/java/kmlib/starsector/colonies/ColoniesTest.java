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
            colonies.add(new Colony(mock(MarketAPI.class), ColonyKind.COLONY, true));

            var set = new Colonies(colonies);

            colonies.clear();

            assertThat(set.colonies())
                .hasSize(1);
        }

        @Test
        void rejects_an_attempt_to_change_the_colonies() {
            // A set is memoised for a whole pass and handed to every reader in it, so one reader
            // able to change it would be rewriting the system underneath all the others.
            var set = new Colonies(List.of(new Colony(mock(MarketAPI.class), ColonyKind.COLONY, true)));

            assertThatThrownBy(() -> set.colonies().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class ReadKnownColonies {

        @Test
        void excludes_a_colony_the_player_has_not_found() {
            // Concealed and on an undiscovered entity: the shape the fog has to keep back on both
            // counts - naming its owner in a box would tell the player exactly what is hiding out
            // there.
            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).readKnownColonies(false))
                .isEmpty();
        }

        @Test
        void excludes_an_open_colony_on_an_entity_the_player_has_not_found() {
            // A derelict station: the sector's most common undiscovered colony, and the one shape
            // whose concealment and discovery disagree. Nothing hides it, so a projection reading
            // concealment would paint its system as settled from the first frame of a campaign,
            // for a place no fleet has been near.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildUnfoundOpenColony("neutral");

            assertThat(buildColoniesOf(derelict).readKnownColonies(false))
                .isEmpty();
        }

        @Test
        void restores_a_colony_the_player_has_not_found_under_the_reveal() {

            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).readKnownColonies(true))
                .containsExactly(new Colony(base, ColonyKind.COLONY, true));
        }

        @Test
        void keeps_a_concealed_colony_the_player_has_found() {
            // A raided pirate base stays permanently hidden while being perfectly well known, so
            // concealment alone must not fog it out.
            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildFoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).readKnownColonies(false))
                .containsExactly(new Colony(base, ColonyKind.COLONY, true));
        }

        @Test
        void keeps_the_set_s_own_order() {

            var fixture = new ColonyFixture("corvus");
            var first = fixture.buildVisibleColony("hegemony");
            var second = fixture.buildVisibleColony("tritachyon");

            assertThat(buildColoniesOf(first, second).readKnownColonies(false))
                .containsExactly(
                    new Colony(first, ColonyKind.COLONY, true),
                    new Colony(second, ColonyKind.COLONY, true));
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
            // concealment would call its system empty while the player is standing in it.
            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildFoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).hasKnownColony(false))
                .isTrue();
        }

        @Test
        void answers_false_for_a_colony_the_player_has_not_found() {
            // Reporting its system as occupied is itself the tell that something is hiding there.
            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            assertThat(buildColoniesOf(base).hasKnownColony(false))
                .isFalse();
        }

        @Test
        void answers_false_for_an_open_colony_on_an_entity_the_player_has_not_found() {
            // The derelict-station shape again, asked of the emptiness read. A system holding
            // nothing but an undiscovered derelict reads as empty, which is what the player has
            // any means of knowing about it.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildUnfoundOpenColony("neutral");

            assertThat(buildColoniesOf(derelict).hasKnownColony(false))
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
                .containsExactly(new Colony(visible, ColonyKind.COLONY, true));
            assertThat(mixed.hasKnownColony(false))
                .isTrue();
        }
    }

    // A colony set built straight from markets, for a case about the projection rather than about
    // the walk that gathers the set.
    private static Colonies buildColoniesOf(MarketAPI... markets) {

        var colonies = new ArrayList<Colony>();
        for (var market : markets) {
            colonies.add(new Colony(market, ColonyKind.COLONY, true));
        }
        return new Colonies(colonies);
    }
}
