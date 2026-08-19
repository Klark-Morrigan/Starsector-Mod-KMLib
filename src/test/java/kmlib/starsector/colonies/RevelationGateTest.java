package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what each {@link RevelationGate} covers. The cases live in a {@link Nested} group so the
 * suite reports as a per-method tree, and the colonies they are posed against are
 * {@link ColonyMarketFixture}'s.
 *
 * <p>Pinned apart from the rule that applies them because the two gates read different facts -
 * one the kind of place, the other whether the market conceals itself - and a gate quietly
 * covering the other's shape would hold back a colony no setting was asked to hide, while every
 * case in {@link ColoniesTest} that poses both gates together went on passing.
 */
final class RevelationGateTest {

    @Nested
    class CoversColony {

        @Test
        void abandoned_stations_covers_a_derelict() {

            assertThat(RevelationGate.ABANDONED_STATIONS.coversColony(
                    buildDerelict(ColonyMarketFixture.buildDerelictStation("neutral"))))
                .isTrue();
        }

        @Test
        void abandoned_stations_passes_over_a_colony_somebody_lives_on() {

            assertThat(RevelationGate.ABANDONED_STATIONS.coversColony(
                    buildColony(ColonyMarketFixture.buildVisibleColony("hegemony"))))
                .isFalse();
        }

        @Test
        void abandoned_stations_passes_over_a_concealed_colony() {
            // The other gate's shape. A derelict gate that covered concealment would hide a
            // raided pirate base for the player who raided it.
            assertThat(RevelationGate.ABANDONED_STATIONS.coversColony(
                    buildColony(ColonyMarketFixture.buildFoundConcealedColony("pirates"))))
                .isFalse();
        }

        @Test
        void hidden_colonies_covers_a_concealed_colony() {

            assertThat(RevelationGate.HIDDEN_COLONIES.coversColony(
                    buildColony(ColonyMarketFixture.buildFoundConcealedColony("rat_exotech"))))
                .isTrue();
        }

        @Test
        void hidden_colonies_passes_over_a_colony_held_in_the_open() {

            assertThat(RevelationGate.HIDDEN_COLONIES.coversColony(
                    buildColony(ColonyMarketFixture.buildVisibleColony("hegemony"))))
                .isFalse();
        }

        @Test
        void hidden_colonies_passes_over_a_derelict_nothing_conceals() {
            // The ordinary derelict: open, and covered by the other gate alone. Concealment and
            // kind are separate facts, and this is the case that says so.
            assertThat(RevelationGate.HIDDEN_COLONIES.coversColony(
                    buildDerelict(ColonyMarketFixture.buildDerelictStation("neutral"))))
                .isFalse();
        }
    }

    // Somewhere people live, listed by the economy.
    private static Colony buildColony(MarketAPI market) {
        return new Colony(market, ColonyKind.COLONY, true);
    }

    // A derelict nobody has ever lived on. Its kind is stated rather than resolved off the
    // market, these cases being about what a gate does with a kind rather than how one is read.
    private static Colony buildDerelict(MarketAPI market) {
        return new Colony(market, ColonyKind.ABANDONED_STATION, true);
    }
}
