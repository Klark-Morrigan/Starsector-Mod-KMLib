package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Pins the contracts of the two projections over a colony set - {@link Colonies#readKnownColonies}
 * and {@link Colonies#readInhabitingColonies}, each beside the emptiness question asked of it -
 * and the fixed-set guarantee its construction makes.
 * Each method's cases live in a {@link Nested} group so the suite reports as a per-method tree;
 * the world they are posed against is {@link ColonyFixture}, shared with the readers' suites.
 *
 * <p>How a set is selected in the first place belongs to whichever reader selected it, and is
 * pinned by {@link SystemColoniesTest} and {@link HyperspaceColoniesTest}.
 */
final class ColoniesTest {

    // The rule as it ships: both leaking shapes held back until somebody has seen them, and
    // nothing admitted that the player has not found.
    private static final ColonyVisibility BOTH_GATES_ON = new ColonyVisibility(
        false,
        Set.of(RevelationGate.ABANDONED_STATIONS, RevelationGate.HIDDEN_COLONIES));

    // One gate apiece, which is how a case shows the two are independent of each other.
    private static final ColonyVisibility ONLY_STATIONS_GATED =
        new ColonyVisibility(false, Set.of(RevelationGate.ABANDONED_STATIONS));

    private static final ColonyVisibility ONLY_HIDDEN_GATED =
        new ColonyVisibility(false, Set.of(RevelationGate.HIDDEN_COLONIES));

    // The reveal, stated with both gates on so a case shows it overriding them rather than
    // merely running where they were off anyway.
    private static final ColonyVisibility REVEAL_EVERYTHING = new ColonyVisibility(
        true,
        Set.of(RevelationGate.ABANDONED_STATIONS, RevelationGate.HIDDEN_COLONIES));

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

            assertThat(buildColoniesOf(buildColony(base)).readKnownColonies(ColonyVisibility.BASE_FOG))
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

            assertThat(buildColoniesOf(buildColony(derelict))
                    .readKnownColonies(ColonyVisibility.BASE_FOG))
                .isEmpty();
        }

        @Test
        void restores_a_colony_the_player_has_not_found_under_the_reveal() {

            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            assertThat(buildColoniesOf(buildColony(base)).readKnownColonies(REVEAL_EVERYTHING))
                .containsExactly(buildColony(base));
        }

        @Test
        void keeps_a_concealed_colony_the_player_has_found() {
            // A raided pirate base stays permanently hidden while being perfectly well known, so
            // concealment alone must not fog it out where no gate asks it to.
            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildFoundConcealedColony("pirates");

            assertThat(buildColoniesOf(buildColony(base)).readKnownColonies(ColonyVisibility.BASE_FOG))
                .containsExactly(buildColony(base));
        }

        @Test
        void keeps_the_set_s_own_order() {

            var fixture = new ColonyFixture("corvus");
            var first = fixture.buildVisibleColony("hegemony");
            var second = fixture.buildVisibleColony("tritachyon");

            assertThat(buildColoniesOf(buildColony(first), buildColony(second))
                    .readKnownColonies(ColonyVisibility.BASE_FOG))
                .containsExactly(buildColony(first), buildColony(second));
        }

        @Test
        void reads_an_unstated_rule_as_the_fog_alone() {
            // A gate nobody asked for must not appear out of a missing argument, and neither
            // must a reveal: a derelict the player has found reads known, and one they have not
            // does not.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildDerelictStation("neutral");

            assertThat(buildColoniesOf(buildDerelict(derelict)).readKnownColonies(null))
                .containsExactly(buildDerelict(derelict));
        }

        @Test
        void excludes_a_derelict_alone_in_a_system_nobody_has_seen() {
            // The Sentinel Gantries reading: found by the fog because nothing hides it, and
            // nothing whatever about it has reached the player.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildDerelictStation("neutral");

            fixture.placeColoniesInSystem(derelict);

            assertThat(buildColoniesOf(buildDerelict(derelict)).readKnownColonies(BOTH_GATES_ON))
                .isEmpty();
        }

        @Test
        void keeps_a_derelict_the_player_has_been_to_the_system_of() {

            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildDerelictStation("neutral");

            fixture.placeColoniesInSystem(derelict);
            fixture.markSystemAsEntered();

            assertThat(buildColoniesOf(buildDerelict(derelict)).readKnownColonies(BOTH_GATES_ON))
                .containsExactly(buildDerelict(derelict));
        }

        @Test
        void keeps_a_derelict_once_a_colony_is_founded_beside_it() {
            // The second route to revelation: a hulk in orbit over an inhabited world is common
            // knowledge there, whether or not the player has ever been.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildDerelictStation("neutral");
            var colony = fixture.buildVisibleColony("hegemony");

            fixture.placeColoniesInSystem(derelict, colony);

            assertThat(buildColoniesOf(buildDerelict(derelict), buildColony(colony))
                    .readKnownColonies(BOTH_GATES_ON))
                .containsExactly(buildDerelict(derelict), buildColony(colony));
        }

        @Test
        void excludes_a_derelict_beside_a_colony_the_player_has_not_found() {
            // A system counts as settled by what the player is shown, not by what is there: an
            // undiscovered colony is no grapevine the player is party to, so the derelict beside
            // it stays unmentioned rather than being vouched for by a place nobody has seen.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildDerelictStation("neutral");
            var unfoundColony = fixture.buildUnfoundOpenColony("hegemony");

            fixture.placeColoniesInSystem(derelict, unfoundColony);

            assertThat(buildColoniesOf(buildDerelict(derelict), buildColony(unfoundColony))
                    .readKnownColonies(BOTH_GATES_ON))
                .isEmpty();
        }

        @Test
        void excludes_a_derelict_vouched_for_only_by_another_derelict() {
            // A derelict cannot settle anything, having never had anybody aboard, so a place
            // holding nothing but hulks reveals none of them.
            var fixture = new ColonyFixture("kumari_kandam");
            var first = fixture.buildDerelictStation("neutral");
            var second = fixture.buildDerelictStation("neutral");

            fixture.placeColoniesInSystem(first, second);

            assertThat(buildColoniesOf(buildDerelict(first), buildDerelict(second))
                    .readKnownColonies(BOTH_GATES_ON))
                .isEmpty();
        }

        @Test
        void keeps_a_derelict_under_its_own_gate_off() {

            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildDerelictStation("neutral");

            fixture.placeColoniesInSystem(derelict);

            assertThat(buildColoniesOf(buildDerelict(derelict)).readKnownColonies(ONLY_HIDDEN_GATED))
                .containsExactly(buildDerelict(derelict));
        }

        @Test
        void excludes_a_concealed_colony_in_a_system_nobody_has_seen() {
            // The Daybreak reading: a colony that hides itself, on an entity that was never
            // discoverable, so the fog admits it and only revelation can hold it back.
            var fixture = new ColonyFixture("kumari_kandam");
            var exoship = fixture.buildFoundConcealedColony("rat_exotech");

            fixture.placeColoniesInSystem(exoship);

            assertThat(buildColoniesOf(buildColony(exoship)).readKnownColonies(BOTH_GATES_ON))
                .isEmpty();
        }

        @Test
        void keeps_a_concealed_colony_a_system_s_own_inhabitants_can_see() {
            // The Galatia Academy reading, and the case that makes the settled route necessary
            // rather than tidy: nothing on the market tells it from the exoship above, and only
            // the Hegemony world in the same system does.
            var fixture = new ColonyFixture("galatia");
            var academy = fixture.buildFoundConcealedColony("independent");
            var ancyra = fixture.buildVisibleColony("hegemony");

            fixture.placeColoniesInSystem(academy, ancyra);

            assertThat(buildColoniesOf(buildColony(academy), buildColony(ancyra))
                    .readKnownColonies(BOTH_GATES_ON))
                .containsExactly(buildColony(academy), buildColony(ancyra));
        }

        @Test
        void keeps_a_concealed_colony_under_its_own_gate_off() {

            var fixture = new ColonyFixture("kumari_kandam");
            var exoship = fixture.buildFoundConcealedColony("rat_exotech");

            fixture.placeColoniesInSystem(exoship);

            assertThat(buildColoniesOf(buildColony(exoship))
                    .readKnownColonies(ONLY_STATIONS_GATED))
                .containsExactly(buildColony(exoship));
        }

        @Test
        void keeps_an_ordinary_colony_under_both_gates() {
            // Neither gate is about an open colony somebody lives on, so the gates that hold the
            // other two kinds back must leave this one exactly where the fog put it.
            var fixture = new ColonyFixture("corvus");
            var colony = fixture.buildVisibleColony("hegemony");

            fixture.placeColoniesInSystem(colony);

            assertThat(buildColoniesOf(buildColony(colony)).readKnownColonies(BOTH_GATES_ON))
                .containsExactly(buildColony(colony));
        }

        @ParameterizedTest
        @MethodSource("kmlib.starsector.colonies.ColoniesTest#buildEveryGateCombination")
        void excludes_an_undiscovered_colony_in_a_settled_system(ColonyVisibility rule) {
            // The conjunction's own case. Revelation is a second condition on top of the fog and
            // never an alternative to it, so a system full of witnesses - entered by the player,
            // and holding a colony that settles it - still shows nothing the player has not
            // found, whichever way the gates are set.
            var fixture = new ColonyFixture("kumari_kandam");
            var unfoundBase = fixture.buildUnfoundConcealedColony("pirates");
            var colony = fixture.buildVisibleColony("hegemony");

            fixture.placeColoniesInSystem(unfoundBase, colony);
            fixture.markSystemAsEntered();

            assertThat(buildColoniesOf(buildColony(unfoundBase), buildColony(colony))
                    .readKnownColonies(rule))
                .containsExactly(buildColony(colony));
        }

        @Test
        void keeps_a_gated_colony_under_the_reveal_though_nobody_has_seen_it() {

            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildDerelictStation("neutral");

            fixture.placeColoniesInSystem(derelict);

            assertThat(buildColoniesOf(buildDerelict(derelict)).readKnownColonies(REVEAL_EVERYTHING))
                .containsExactly(buildDerelict(derelict));
        }

        @Test
        void keeps_a_gated_colony_standing_in_no_star_system() {
            // Hyperspace, where mods put a few. There is no system to have been in and none to be
            // settled, so a gate answering otherwise would withhold it for the whole campaign.
            var derelict = ColonyMarketFixture.buildDerelictStation("neutral");

            ColonyPlacementFixture.placeColonies(mock(LocationAPI.class), derelict);

            assertThat(buildColoniesOf(buildDerelict(derelict)).readKnownColonies(BOTH_GATES_ON))
                .containsExactly(buildDerelict(derelict));
        }
    }

    @Nested
    class HasKnownColony {

        @Test
        void answers_false_for_a_system_holding_nothing() {
            assertThat(Colonies.NONE.hasKnownColony(ColonyVisibility.BASE_FOG))
                .isFalse();
        }

        @Test
        void answers_true_for_an_ordinary_colony() {

            var fixture = new ColonyFixture("corvus");

            assertThat(buildColoniesOf(buildColony(fixture.buildVisibleColony("hegemony")))
                    .hasKnownColony(ColonyVisibility.BASE_FOG))
                .isTrue();
        }

        @Test
        void answers_true_for_a_concealed_colony_the_player_has_found() {
            // A raided base is concealed for good and plainly known, so an emptiness read gated on
            // concealment would call its system empty while the player is standing in it.
            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildFoundConcealedColony("pirates");

            assertThat(buildColoniesOf(buildColony(base)).hasKnownColony(ColonyVisibility.BASE_FOG))
                .isTrue();
        }

        @Test
        void answers_false_for_a_colony_the_player_has_not_found() {
            // Reporting its system as occupied is itself the tell that something is hiding there.
            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            assertThat(buildColoniesOf(buildColony(base)).hasKnownColony(ColonyVisibility.BASE_FOG))
                .isFalse();
        }

        @Test
        void answers_false_for_an_open_colony_on_an_entity_the_player_has_not_found() {
            // The derelict-station shape again, asked of the emptiness read. A system holding
            // nothing but an undiscovered derelict reads as empty, which is what the player has
            // any means of knowing about it.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildUnfoundOpenColony("neutral");

            assertThat(buildColoniesOf(buildColony(derelict))
                    .hasKnownColony(ColonyVisibility.BASE_FOG))
                .isFalse();
        }

        @Test
        void answers_true_for_a_colony_the_player_has_not_found_under_the_reveal() {

            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildUnfoundConcealedColony("pirates");

            assertThat(buildColoniesOf(buildColony(base)).hasKnownColony(REVEAL_EVERYTHING))
                .isTrue();
        }

        @Test
        void agrees_with_the_projection_it_asks_the_emptiness_of() {
            // The claim the second read rests on: not materialising the list must not change the
            // answer, and a set mixing a fogged colony with a visible one is where a filter that
            // had drifted between the two reads would show it.
            var fixture = new ColonyFixture("kumari_kandam");
            var fogged = fixture.buildUnfoundConcealedColony("pirates");
            var visible = fixture.buildVisibleColony("independent");

            var foggedOnly = buildColoniesOf(buildColony(fogged));
            var mixed = buildColoniesOf(buildColony(fogged), buildColony(visible));

            assertThat(foggedOnly.readKnownColonies(ColonyVisibility.BASE_FOG))
                .isEmpty();
            assertThat(foggedOnly.hasKnownColony(ColonyVisibility.BASE_FOG))
                .isFalse();

            assertThat(mixed.readKnownColonies(ColonyVisibility.BASE_FOG))
                .containsExactly(buildColony(visible));
            assertThat(mixed.hasKnownColony(ColonyVisibility.BASE_FOG))
                .isTrue();
        }

        @Test
        void agrees_with_the_projection_over_a_set_holding_only_derelicts() {
            // Nothing here settles the place, so both reads have to reach their answer through
            // the gate itself rather than through anything an ordinary colony vouched for.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildDerelictStation("neutral");

            fixture.placeColoniesInSystem(derelict);

            var colonies = buildColoniesOf(buildDerelict(derelict));

            assertThat(colonies.readKnownColonies(BOTH_GATES_ON))
                .isEmpty();
            assertThat(colonies.hasKnownColony(BOTH_GATES_ON))
                .isFalse();

            assertThat(colonies.readKnownColonies(ONLY_HIDDEN_GATED))
                .containsExactly(buildDerelict(derelict));
            assertThat(colonies.hasKnownColony(ONLY_HIDDEN_GATED))
                .isTrue();
        }

        @Test
        void agrees_with_the_projection_over_a_derelict_a_colony_vouches_for() {
            // A settled place, where the derelict is admitted only by the colony beside it. Both
            // reads must take that settled reading before judging the gate, or they diverge.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildDerelictStation("neutral");
            var colony = fixture.buildVisibleColony("hegemony");

            fixture.placeColoniesInSystem(derelict, colony);

            var colonies = buildColoniesOf(buildDerelict(derelict), buildColony(colony));

            assertThat(colonies.readKnownColonies(BOTH_GATES_ON))
                .containsExactly(buildDerelict(derelict), buildColony(colony));
            assertThat(colonies.hasKnownColony(BOTH_GATES_ON))
                .isTrue();
        }
    }

    @Nested
    class ReadInhabitingColonies {

        @Test
        void excludes_a_derelict_the_player_has_seen() {
            // The whole of the projection's reason for existing: the listing may name a hulk the
            // player has been past, and the place it orbits is still nobody's home.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildDerelictStation("neutral");

            fixture.placeColoniesInSystem(derelict);
            fixture.markSystemAsEntered();

            var colonies = buildColoniesOf(buildDerelict(derelict));

            assertThat(colonies.readKnownColonies(BOTH_GATES_ON))
                .containsExactly(buildDerelict(derelict));
            assertThat(colonies.readInhabitingColonies(BOTH_GATES_ON))
                .isEmpty();
        }

        @Test
        void keeps_a_colony_the_derelict_beside_it_does_not_join() {
            // Both projections speak about one system and say different things: the listing names
            // the derelict the colony's own inhabitants can see, and habitation counts only the
            // colony.
            var fixture = new ColonyFixture("kumari_kandam");
            var colony = fixture.buildVisibleColony("hegemony");
            var derelict = fixture.buildDerelictStation("neutral");

            fixture.placeColoniesInSystem(colony, derelict);

            var colonies = buildColoniesOf(buildColony(colony), buildDerelict(derelict));

            assertThat(colonies.readKnownColonies(BOTH_GATES_ON))
                .containsExactly(buildColony(colony), buildDerelict(derelict));
            assertThat(colonies.readInhabitingColonies(BOTH_GATES_ON))
                .containsExactly(buildColony(colony));
        }

        @Test
        void keeps_every_known_colony_where_no_derelict_is_present() {
            // Nothing is removed where nothing was ever a hulk, so a system of ordinary colonies
            // reads alike under either projection - in the set's own order.
            var fixture = new ColonyFixture("galatia");
            var ancyra = fixture.buildVisibleColony("hegemony");
            var academy = fixture.buildFoundConcealedColony("independent");

            fixture.placeColoniesInSystem(ancyra, academy);

            assertThat(buildColoniesOf(buildColony(ancyra), buildColony(academy))
                    .readInhabitingColonies(BOTH_GATES_ON))
                .containsExactly(buildColony(ancyra), buildColony(academy));
        }

        @Test
        void excludes_a_derelict_its_own_gate_has_let_through() {
            // Kind and gate answer separate questions. Turning the station gate off says the
            // player may be told about a hulk they have found; it does not put anybody aboard it.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildDerelictStation("neutral");

            fixture.placeColoniesInSystem(derelict);

            var colonies = buildColoniesOf(buildDerelict(derelict));

            assertThat(colonies.readKnownColonies(ONLY_HIDDEN_GATED))
                .containsExactly(buildDerelict(derelict));
            assertThat(colonies.readInhabitingColonies(ONLY_HIDDEN_GATED))
                .isEmpty();
        }

        @Test
        void excludes_a_derelict_the_reveal_has_let_through() {
            // The reveal is about the fog and the gates, not about who is aboard. Posed on an
            // entity the player has not found, so it is the reveal alone putting the hulk in the
            // listing - and habitation still declines it.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildUnfoundOpenColony("neutral");

            fixture.placeColoniesInSystem(derelict);

            var colonies = buildColoniesOf(buildDerelict(derelict));

            assertThat(colonies.readKnownColonies(REVEAL_EVERYTHING))
                .containsExactly(buildDerelict(derelict));
            assertThat(colonies.readInhabitingColonies(REVEAL_EVERYTHING))
                .isEmpty();
        }

        @Test
        void follows_the_gate_that_holds_a_concealed_colony_back() {
            // Habitation is the known set with a kind removed and no second reading of the rule,
            // so a colony the hidden gate withholds is absent from both and present in both once
            // the gate is off.
            var fixture = new ColonyFixture("kumari_kandam");
            var exoship = fixture.buildFoundConcealedColony("rat_exotech");

            fixture.placeColoniesInSystem(exoship);

            var colonies = buildColoniesOf(buildColony(exoship));

            assertThat(colonies.readInhabitingColonies(BOTH_GATES_ON))
                .isEmpty();
            assertThat(colonies.readInhabitingColonies(ONLY_STATIONS_GATED))
                .containsExactly(buildColony(exoship));
        }

        @Test
        void reads_an_unstated_rule_as_the_fog_alone() {

            var fixture = new ColonyFixture("corvus");
            var colony = fixture.buildVisibleColony("hegemony");
            var unfoundColony = fixture.buildUnfoundOpenColony("tritachyon");

            fixture.placeColoniesInSystem(colony, unfoundColony);

            assertThat(buildColoniesOf(buildColony(colony), buildColony(unfoundColony))
                    .readInhabitingColonies(null))
                .containsExactly(buildColony(colony));
        }
    }

    @Nested
    class HasInhabitingColony {

        @Test
        void answers_false_for_a_system_holding_nothing() {
            assertThat(Colonies.NONE.hasInhabitingColony(ColonyVisibility.BASE_FOG))
                .isFalse();
        }

        @Test
        void answers_true_for_an_ordinary_colony() {

            var fixture = new ColonyFixture("corvus");

            assertThat(buildColoniesOf(buildColony(fixture.buildVisibleColony("hegemony")))
                    .hasInhabitingColony(ColonyVisibility.BASE_FOG))
                .isTrue();
        }

        @Test
        void answers_false_for_a_system_holding_only_a_derelict_the_player_has_seen() {
            // The reading the map turns on: a system the listing has something to say about, and
            // which is still empty space with a hulk in it.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildDerelictStation("neutral");

            fixture.placeColoniesInSystem(derelict);
            fixture.markSystemAsEntered();

            var colonies = buildColoniesOf(buildDerelict(derelict));

            assertThat(colonies.hasKnownColony(BOTH_GATES_ON))
                .isTrue();
            assertThat(colonies.hasInhabitingColony(BOTH_GATES_ON))
                .isFalse();
        }

        @Test
        void answers_true_for_a_system_a_derelict_shares_with_a_colony() {
            // A settled place, where the derelict must not be what carries the answer - the
            // colony beside it is.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildDerelictStation("neutral");
            var colony = fixture.buildVisibleColony("hegemony");

            fixture.placeColoniesInSystem(derelict, colony);

            assertThat(buildColoniesOf(buildDerelict(derelict), buildColony(colony))
                    .hasInhabitingColony(BOTH_GATES_ON))
                .isTrue();
        }

        @Test
        void answers_false_for_a_derelict_the_reveal_has_let_through() {
            // The emptiness question asked of the same case: the reveal admits the hulk to the
            // listing without making its place anybody's home.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildUnfoundOpenColony("neutral");

            fixture.placeColoniesInSystem(derelict);

            var colonies = buildColoniesOf(buildDerelict(derelict));

            assertThat(colonies.hasKnownColony(REVEAL_EVERYTHING))
                .isTrue();
            assertThat(colonies.hasInhabitingColony(REVEAL_EVERYTHING))
                .isFalse();
        }

        @Test
        void reads_an_unstated_rule_as_the_fog_alone() {
            // No reveal appears out of a missing argument: a colony the player has not found does
            // not inhabit its place for the rule having gone unstated.
            var fixture = new ColonyFixture("corvus");
            var unfoundColony = fixture.buildUnfoundOpenColony("tritachyon");

            fixture.placeColoniesInSystem(unfoundColony);

            assertThat(buildColoniesOf(buildColony(unfoundColony)).hasInhabitingColony(null))
                .isFalse();
        }

        @Test
        void follows_the_gate_that_holds_a_concealed_colony_back() {
            // A concealed colony settles nothing, so the gate is the whole of the answer here -
            // and the emptiness question has to consult it exactly as the listing does.
            var fixture = new ColonyFixture("kumari_kandam");
            var exoship = fixture.buildFoundConcealedColony("rat_exotech");

            fixture.placeColoniesInSystem(exoship);

            var colonies = buildColoniesOf(buildColony(exoship));

            assertThat(colonies.hasInhabitingColony(BOTH_GATES_ON))
                .isFalse();
            assertThat(colonies.hasInhabitingColony(ONLY_STATIONS_GATED))
                .isTrue();
        }

        @Test
        void agrees_with_the_projection_it_asks_the_emptiness_of() {
            // The claim the short-circuit rests on: skipping the list must not change the answer,
            // and a set mixing a derelict with a colony the fog withholds is where a short-circuit
            // that had drifted from the projection would show it.
            var fixture = new ColonyFixture("kumari_kandam");
            var derelict = fixture.buildDerelictStation("neutral");
            var unfoundColony = fixture.buildUnfoundOpenColony("hegemony");

            fixture.placeColoniesInSystem(derelict, unfoundColony);
            fixture.markSystemAsEntered();

            var derelictOnly = buildColoniesOf(buildDerelict(derelict));
            var mixed = buildColoniesOf(buildDerelict(derelict), buildColony(unfoundColony));

            assertThat(derelictOnly.readInhabitingColonies(BOTH_GATES_ON))
                .isEmpty();
            assertThat(derelictOnly.hasInhabitingColony(BOTH_GATES_ON))
                .isFalse();

            assertThat(mixed.readInhabitingColonies(BOTH_GATES_ON))
                .isEmpty();
            assertThat(mixed.hasInhabitingColony(BOTH_GATES_ON))
                .isFalse();
        }
    }

    // Every setting of the two gates, the reveal left off throughout. What a case poses when it
    // is claiming the answer does not depend on the gates at all - which is the shape of the
    // conjunction, where the fog refuses before a gate is ever consulted.
    static Stream<ColonyVisibility> buildEveryGateCombination() {
        return Stream.of(
            BOTH_GATES_ON,
            ONLY_STATIONS_GATED,
            ONLY_HIDDEN_GATED,
            ColonyVisibility.BASE_FOG);
    }

    // A colony set built straight from colonies, for a case about the projection rather than
    // about the walk that gathers the set.
    private static Colonies buildColoniesOf(Colony... colonies) {
        return new Colonies(List.of(colonies));
    }

    // Somewhere people live, listed by the economy - the kind neither gate is about.
    private static Colony buildColony(MarketAPI market) {
        return new Colony(market, ColonyKind.COLONY, true);
    }

    // A derelict nobody has ever lived on. Its kind is stated here rather than resolved off the
    // market, since these cases are about what the projection does with a kind rather than about
    // how a kind is read.
    private static Colony buildDerelict(MarketAPI market) {
        return new Colony(market, ColonyKind.ABANDONED_STATION, true);
    }
}
