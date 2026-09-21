package kmlib.starsector.markets.colonies;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.testfixtures.starsector.markets.colonies.ColonyFixture;

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
        void yieldsTheEconomySOwnColoniesAheadOfTheOnesItDoesNotList() {
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
                    new Colony(ancyra, true),
                    new Colony(academy, false));
        }

        @Test
        void marksAColonyTheEconomyDoesNotList() {
            // Galatia Academy: a real market on a real station that vanilla deliberately never
            // registers. A reader that only walked the economy would report the station as
            // nobody's, so it is admitted - and marked, since it carries no economy-fed weight.
            var fixture = new ColonyFixture("galatia");
            var academy = fixture.buildVisibleColony("independent");

            fixture.placeColoniesInSystem(academy);

            assertThat(readColonies(fixture))
                .containsExactly(new Colony(academy, false));
        }

        @Test
        void excludesAPlanetSConditionOnlyMarket() {
            // Every uninhabited planet carries one of these, hung on the entity and never
            // registered. Admitting them would put a neutral colony on every surveyed rock.
            var fixture = new ColonyFixture("corvus");

            fixture.placeColoniesInSystem(fixture.buildConditionOnlyMarket());

            assertThat(readColonies(fixture))
                .isEmpty();
        }

        @Test
        void admitsAConditionOnlyMarketCarryingTheDecivilisedCondition() {
            // The one condition-only market a colony set holds, and the narrowest admission that
            // reaches it: a decivilised world is stripped of its owner as it collapses, so
            // ownership refuses it along with every bare rock, and the decivilised condition is
            // what parts it from those.
            var fixture = new ColonyFixture("kumari_kandam");
            var decivilisedWorld = fixture.buildDecivilisedWorld();

            fixture.placeColoniesInSystem(decivilisedWorld);

            assertThat(readColonies(fixture))
                .containsExactly(new Colony(decivilisedWorld, false));
        }

        @Test
        void admitsADecivilisedWorldThePlayerHasNotSurveyed() {
            // The set is unfogged, a mechanic mirrored from vanilla having to see what vanilla
            // sees, so what the player may be told about such a world is decided over the set
            // rather than by leaving it out of it.
            var fixture = new ColonyFixture("kumari_kandam");
            var decivilisedWorld = fixture.buildUnsurveyedDecivilisedWorld();

            fixture.placeColoniesInSystem(decivilisedWorld);

            assertThat(readColonies(fixture))
                .containsExactly(new Colony(decivilisedWorld, false));
        }

        @Test
        void admitsAConcealedColonyAndMarksItHidden() {

            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildFoundConcealedColony("pirates");

            fixture.placeColoniesInSystem(base);
            fixture.listColoniesInEconomy(base);

            var colonies = readColonies(fixture);

            assertThat(colonies)
                .containsExactly(new Colony(base, true));
            assertThat(colonies.get(0).isHidden())
                .isTrue();
        }

        @Test
        void yieldsOneColonyWhereTwoMarketObjectsShareAPlaceAndOwner() {
            // A mod supersedes a colony by adding its own market beside vanilla's on the same
            // station rather than replacing it. Counted per market, that colony is banked twice
            // and its owner reads as holding twice what it holds.
            var fixture = new ColonyFixture("galatia");
            var vanillaMarket = fixture.buildVisibleColony("independent");
            var moddedMarket = fixture.buildSiblingMarketOn(vanillaMarket, LARGER_COLONY_SIZE);

            fixture.placeColoniesInSystem(vanillaMarket);
            fixture.listColoniesInEconomy(vanillaMarket, moddedMarket);

            assertThat(readColonies(fixture))
                .containsExactly(new Colony(moddedMarket, true));
        }

        @Test
        void marksADerelictStationAsAnAbandonedStation() {
            // The set admits it like any other owned market - what changes is that the colony
            // says what it is, so a reader downstream is not left to take a derelict for a town.
            var fixture = new ColonyFixture("corvus");
            var derelict = fixture.buildDerelictStation();

            fixture.placeColoniesInSystem(derelict);

            assertThat(readColonies(fixture))
                .containsExactly(new Colony(derelict, false));
        }

        @Test
        void takesACollidingPairSKindFromTheMarketThatWinsThePlace() {
            // Kind and place have to be settled by the same market. The loser is named first
            // here, so a resolution reading the kind off anything but the winner reports a plain
            // colony where the winner is a station.
            //
            // The winner comes out an outpost rather than a derelict, and that is the staging
            // rather than the rule: two markets can only share one place through the economy's
            // listing, and being listed is itself one of the two things that make a station
            // somebody's.
            var fixture = new ColonyFixture("corvus");
            var derelict = fixture.buildDerelictStation();
            var supersededMarket = fixture.buildSiblingMarketOn(derelict, SMALLER_COLONY_SIZE);

            fixture.placeColoniesInSystem(derelict);
            fixture.listColoniesInEconomy(supersededMarket, derelict);

            assertThat(readColonies(fixture))
                .containsExactly(new Colony(derelict, true));
        }

        @Test
        void yieldsAColonyThePlayerHasNotFound() {
            // The set is unfogged on purpose: claim scoring weighs colonies the player has never
            // found, and a fogged input would resolve a claimant vanilla does not report.
            var fixture = new ColonyFixture("kumari_kandam");
            var base = fixture.buildUndiscoveredConcealedColony("pirates");

            fixture.placeColoniesInSystem(base);
            fixture.listColoniesInEconomy(base);

            assertThat(readColonies(fixture))
                .containsExactly(new Colony(base, true));
        }

        @Test
        void yieldsNothingForANullSector() {
            assertThat(SystemColonies.readColoniesIn(null, mock(StarSystemAPI.class)))
                .isEqualTo(Colonies.NONE);
        }

        @Test
        void yieldsNothingForANullSystem() {
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
