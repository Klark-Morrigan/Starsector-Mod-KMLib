package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.starsector.WalkCountCapture;
import kmlib.starsector.markets.colonies.Colonies;
import kmlib.starsector.markets.colonies.Colony;
import kmlib.starsector.markets.colonies.SystemColonies;
import kmlib.testfixtures.starsector.markets.colonies.ColonyFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Pins the contracts of {@link SystemColoniesIndex#readColoniesIn},
 * {@link SystemColoniesIndex#readColoniesById}, {@link SystemColoniesIndex#readSystemsById} and
 * {@link SystemColoniesIndex#getSector}: that the index answers exactly what the direct read
 * answers, that it pays for a system's walk once however it is asked, that one traversal of the
 * sector serves every reader of it, and that it names the sector it answers out of. Each method's
 * cases live in a {@link Nested} group so the suite reports as a per-method tree; the world they
 * are posed against is {@link ColonyFixture}, shared with the direct read's suite - which
 * is what lets the two answers be compared at all.
 */
final class SystemColoniesIndexTest {

    @Nested
    class GetSector {

        @Test
        void namesTheSectorTheIndexWasOpenedOver() {
            // A caller holding an index holds no sector beside it, so the index has to be able to
            // name the one its answers came out of.
            var fixture = new ColonyFixture("galatia");

            assertThat(new SystemColoniesIndex(fixture.getSector()).getSector())
                .isSameAs(fixture.getSector());
        }

        @Test
        void namesNoSectorWhenTheIndexWasOpenedOverNone() {
            // The unreachable-sector case answers an empty set for every system, and it reports
            // the absence rather than inventing a sector to name.
            assertThat(new SystemColoniesIndex(null).getSector())
                .isNull();
        }
    }

    @Nested
    class ReadColoniesIn {

        @Test
        void answersASystemExactlyAsTheDirectReadDoes() {
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
        void walksASystemOnceAcrossRepeatedAsks() {
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
        void countsOneSystemVisitedHoweverOftenItIsAskedAbout() {
            // The counter a pass reads to check the one-walk-per-system rule it was given the
            // index for - and it is the walk that is counted, not the ask, so a memo hit adds
            // nothing.
            var fixture = buildCorvusHoldingOneColony();
            var index = new SystemColoniesIndex(fixture.getSector());

            var counts = WalkCountCapture.captureCountsOf(() -> {
                index.readColoniesIn(fixture.getSystem());
                index.readColoniesIn(fixture.getSystem());
            });

            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(1L);
        }

        @Test
        void walksASystemCarryingNoIdAfreshOnEveryAsk() {
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
        void yieldsNothingForANullSystem() {
            assertThat(new SystemColoniesIndex(mock(SectorAPI.class)).readColoniesIn(null))
                .isEqualTo(Colonies.NONE);
        }

        @Test
        void yieldsNothingForEverySystemWhenTheSectorIsUnreachable() {

            var fixture = buildCorvusHoldingOneColony();

            assertThat(new SystemColoniesIndex(null).readColoniesIn(fixture.getSystem()))
                .isEqualTo(Colonies.NONE);
        }
    }

    @Nested
    class ReadColoniesById {

        @Test
        void answersTheSystemCarryingThatId() {

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
        void answersOffTheWalkAReadMadeWithTheSystemAlreadyPaidFor() {
            // A pass keyed by system id and a reader holding the system itself are asking the
            // same question, so the second route must not buy a second traversal.
            var fixture = buildCorvusHoldingOneColony();
            var index = new SystemColoniesIndex(fixture.getSector());

            index.readColoniesIn(fixture.getSystem());
            index.readColoniesById("corvus");

            verify(fixture.getSystem(), times(1)).getAllEntities();
        }

        @Test
        void yieldsNothingForAnIdNoSystemCarries() {

            var fixture = buildCorvusHoldingOneColony();

            assertThat(new SystemColoniesIndex(fixture.getSector()).readColoniesById("askonia"))
                .isEqualTo(Colonies.NONE);
        }

        @Test
        void resolvesTheSectorSSystemsOnceAcrossRepeatedAsks() {
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
        void yieldsNothingForABlankId() {
            assertThat(new SystemColoniesIndex(mock(SectorAPI.class)).readColoniesById(" "))
                .isEqualTo(Colonies.NONE);
        }
    }

    @Nested
    class ReadSystemsById {

        @Test
        void answersEachOfTheSectorSSystemsKeyedByItsId() {

            var fixture = buildCorvusHoldingOneColony();

            assertThat(new SystemColoniesIndex(fixture.getSector()).readSystemsById())
                .containsExactly(entry("corvus", fixture.getSystem()));
        }

        @Test
        void traversesTheSectorOnceAcrossRepeatedAsks() {
            // This is what a reader resolving many ids takes instead of indexing the sector for
            // itself, so it has to be cheaper than doing so - otherwise the reader has bought the
            // traversal it came here to avoid.
            var fixture = buildCorvusHoldingOneColony();
            var index = new SystemColoniesIndex(fixture.getSector());

            index.readSystemsById();
            index.readSystemsById();

            verify(fixture.getSector(), times(1)).getStarSystems();
        }

        @Test
        void sharesThatTraversalWithAColonyReadMadeById() {
            // The index's two id-keyed answers are one traversal between them: a pass that resolves
            // a system here and then asks who lives in one it never held has walked the sector
            // once, which is what the row reporting that pass is allowed.
            var fixture = buildCorvusHoldingOneColony();
            var index = new SystemColoniesIndex(fixture.getSector());

            index.readSystemsById();
            index.readColoniesById("askonia");

            verify(fixture.getSector(), times(1)).getStarSystems();
        }

        @Test
        void countsOneSectorWalkHoweverOftenItIsAskedFor() {
            // The counter the frame's bound on a rebuild is stated against: a second traversal
            // opened for the same pass is the breach, so this answer must never add one.
            var fixture = buildCorvusHoldingOneColony();
            var index = new SystemColoniesIndex(fixture.getSector());

            var counts = WalkCountCapture.captureCountsOf(() -> {
                index.readSystemsById();
                index.readSystemsById();
            });

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
        }

        @Test
        void yieldsNoSystemsWhenTheSectorIsUnreachable() {

            assertThat(new SystemColoniesIndex(null).readSystemsById())
                .isEmpty();
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
