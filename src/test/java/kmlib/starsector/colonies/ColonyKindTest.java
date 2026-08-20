package kmlib.starsector.colonies;

import com.fs.starfarer.api.impl.campaign.ids.Factions;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link ColonyKind#resolveKind} - which kind of place a market stands for.
 * The cases live in a {@link Nested} group so the suite reports as a per-method tree, and the
 * markets they are posed against are {@link ColonyMarketFixture}'s.
 *
 * <p>What makes a market wear the derelict shape is the market read's own contract and is pinned
 * with it; what this adds is that the classification routes to the right kind - including which of
 * the two derelict-shaped kinds an owner or a listing makes it - and that anything unrecognised
 * falls to the ordinary one rather than being erased.
 */
final class ColonyKindTest {

    // What the economy's own listing said of the market being classified, which the walk that
    // selected it decides rather than the market. Named so a case reads as the listing it poses
    // instead of as a bare boolean at the end of a call.
    private static final boolean LISTED_BY_ECONOMY = true;
    private static final boolean UNLISTED_BY_ECONOMY = false;

    @Nested
    class ResolveKind {

        @Test
        void reads_a_derelict_station_held_by_nobody_as_a_space_derelict() {

            var derelict = ColonyMarketFixture.buildDerelictStation();

            assertThat(ColonyKind.resolveKind(derelict, UNLISTED_BY_ECONOMY))
                .isEqualTo(ColonyKind.SPACE_DERELICT);
        }

        @Test
        void reads_a_derelict_conditioned_market_a_faction_holds_as_an_outpost() {
            // The condition is not vanilla's alone - a mod may hang it on a station it means to be
            // manned - so the owner is one of the two things that part a kept station from a hulk.
            // Posed against the case above, which differs in the owner and in nothing else.
            var outpost = ColonyMarketFixture.buildOutpost("hegemony");

            assertThat(ColonyKind.resolveKind(outpost, UNLISTED_BY_ECONOMY))
                .isEqualTo(ColonyKind.OUTPOST);
        }

        @Test
        void reads_an_unowned_derelict_the_economy_lists_as_an_outpost() {
            // The other of the two, and the case that says either alone is enough. The routine
            // that builds a derelict pointedly does not register one, so a hulk trading anyway was
            // made economically real on purpose - and reading the owner alone left it taking a
            // dominance weight while counting toward nobody living there.
            var derelict = ColonyMarketFixture.buildDerelictStation();

            assertThat(ColonyKind.resolveKind(derelict, LISTED_BY_ECONOMY))
                .isEqualTo(ColonyKind.OUTPOST);
        }

        @Test
        void reads_a_derelict_conditioned_market_the_neutral_faction_holds_as_a_space_derelict() {
            // Neutral is an owner on paper and nobody in fact, which is the whole reason the kind
            // read asks the faction rather than merely asking whether one is set.
            var derelict = ColonyMarketFixture.buildOutpost(Factions.NEUTRAL);

            assertThat(ColonyKind.resolveKind(derelict, UNLISTED_BY_ECONOMY))
                .isEqualTo(ColonyKind.SPACE_DERELICT);
        }

        @Test
        void reads_an_ordinary_colony_as_a_colony() {

            var colony = ColonyMarketFixture.buildVisibleColony("hegemony");

            assertThat(ColonyKind.resolveKind(colony, LISTED_BY_ECONOMY))
                .isEqualTo(ColonyKind.COLONY);
        }

        @Test
        void reads_a_null_market_as_a_colony() {
            // The default arm, posed at its extreme: nothing at all to read still yields the kind
            // that keeps a place on the map, because misfiling a settlement as a hulk erases it.
            assertThat(ColonyKind.resolveKind(null, UNLISTED_BY_ECONOMY))
                .isEqualTo(ColonyKind.COLONY);
        }
    }
}
