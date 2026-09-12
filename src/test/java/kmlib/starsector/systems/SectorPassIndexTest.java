package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.starsector.WalkCountCapture;
import kmlib.starsector.markets.colonies.Colonies;
import kmlib.starsector.markets.colonies.Colony;
import kmlib.starsector.markets.colonies.SystemColonies;
import kmlib.testfixtures.starsector.markets.colonies.ColonyFixture;
import kmlib.testfixtures.starsector.markets.colonies.ColonyMarketFixture;
import kmlib.testfixtures.starsector.markets.colonies.ColonyPlacementFixture;
import kmlib.testfixtures.starsector.systems.StarSystemFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link SectorPassIndex#readColoniesIn},
 * {@link SectorPassIndex#readColoniesById}, {@link SectorPassIndex#readSystemsById},
 * {@link SectorPassIndex#readSystemsByKey} and {@link SectorPassIndex#getSector}: that the
 * index answers exactly what the direct read answers, that it pays for a system's walk once however
 * it is asked, that one traversal of the sector serves every reader keyed the same way, and that it
 * names the sector it answers out of. Each method's cases live in a {@link Nested} group so the
 * suite reports as a per-method tree; the world they are posed against is {@link ColonyFixture},
 * shared with the direct read's suite - which is what lets the two answers be compared at all - and
 * {@link StarSystemFixture} where a case needs more systems than that one holds.
 */
final class SectorPassIndexTest {

    @Nested
    class GetSector {

        @Test
        void namesTheSectorTheIndexWasOpenedOver() {
            // A caller holding an index holds no sector beside it, so the index has to be able to
            // name the one its answers came out of.
            var fixture = new ColonyFixture("galatia");

            assertThat(new SectorPassIndex(fixture.getSector()).getSector())
                .isSameAs(fixture.getSector());
        }

        @Test
        void namesNoSectorWhenTheIndexWasOpenedOverNone() {
            // The unreachable-sector case answers an empty set for every system, and it reports
            // the absence rather than inventing a sector to name.
            assertThat(new SectorPassIndex(null).getSector())
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

            assertThat(new SectorPassIndex(fixture.getSector())
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
            var index = new SectorPassIndex(fixture.getSector());

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
            var index = new SectorPassIndex(fixture.getSector());

            var counts = WalkCountCapture.captureCountsOf(() -> {
                index.readColoniesIn(fixture.getSystem());
                index.readColoniesIn(fixture.getSystem());
            });

            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(1L);
        }

        @Test
        void answersEachOfTwoSystemsSharingAnIdItsOwnColonies() {
            // The defect a memo keyed on the id carries: a sector holds two systems under one id,
            // so the pair is a single entry and the second system is handed the first's colonies.
            // The key separates them, an anchor id being minted per system.
            var world = buildTwoSystemsSharingAnId();
            var index = new SectorPassIndex(world.sector());

            assertThat(index.readColoniesIn(world.first()).colonies())
                .containsExactly(new Colony(world.firstColony(), false));
            assertThat(index.readColoniesIn(world.second()).colonies())
                .containsExactly(new Colony(world.secondColony(), false));
        }

        @Test
        void memoisesASystemCarryingNoIdButAnAnchor() {
            // What the key buys over keying on the id: an id is only one of three arms, so a system
            // the sector never named is still remembered by the entity the engine minted for it.
            var system = StarSystemFixture.buildKeyedSystem(null, null, "8b3");
            var colony = ColonyMarketFixture.buildVisibleColony("hegemony");

            ColonyPlacementFixture.placeColonies(system, colony);

            var index = new SectorPassIndex(buildSectorHoldingSystems(system));

            assertThat(index.readColoniesIn(system).colonies())
                .containsExactly(new Colony(colony, false));

            index.readColoniesIn(system);

            verify(system, times(1)).getAllEntities();
        }

        @Test
        void walksASystemStatingNoArmAtAllAfreshOnEveryAsk() {
            // No id and neither entity, so the key is blank and equals every other blank one. The
            // walk is paid again, which is the honest price: pooling every such system under the
            // one key would hand the first one's colonies to the second.
            var fixture = new ColonyFixture(null);
            var colony = fixture.buildVisibleColony("hegemony");

            fixture.placeColoniesInSystem(colony);
            fixture.listColoniesInEconomy(colony);

            var index = new SectorPassIndex(fixture.getSector());

            assertThat(index.readColoniesIn(fixture.getSystem()).colonies())
                .containsExactly(new Colony(colony, true));

            index.readColoniesIn(fixture.getSystem());

            verify(fixture.getSystem(), times(2)).getAllEntities();
        }

        @Test
        void keepsTwoSystemsStatingNoArmAtAllApart() {
            // The conflation the blank key would cause, posed outright: two systems the sector
            // names with nothing are two systems, and each has to answer with its own colonies.
            var firstColony = ColonyMarketFixture.buildVisibleColony("hegemony");
            var secondColony = ColonyMarketFixture.buildVisibleColony("tritachyon");
            var first = StarSystemFixture.buildKeyedSystem(null, null, null);
            var second = StarSystemFixture.buildKeyedSystem(null, null, null);

            ColonyPlacementFixture.placeColonies(first, firstColony);
            ColonyPlacementFixture.placeColonies(second, secondColony);

            var index = new SectorPassIndex(buildSectorHoldingSystems(first, second));

            assertThat(index.readColoniesIn(first).colonies())
                .containsExactly(new Colony(firstColony, false));
            assertThat(index.readColoniesIn(second).colonies())
                .containsExactly(new Colony(secondColony, false));
        }

        @Test
        void yieldsNothingForANullSystem() {
            assertThat(new SectorPassIndex(mock(SectorAPI.class)).readColoniesIn(null))
                .isEqualTo(Colonies.NONE);
        }

        @Test
        void yieldsNothingForEverySystemWhenTheSectorIsUnreachable() {

            var fixture = buildCorvusHoldingOneColony();

            assertThat(new SectorPassIndex(null).readColoniesIn(fixture.getSystem()))
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

            assertThat(new SectorPassIndex(fixture.getSector())
                    .readColoniesById("corvus")
                    .colonies())
                .containsExactly(new Colony(colony, true));
        }

        @Test
        void answersOffTheWalkAReadMadeWithTheSystemAlreadyPaidFor() {
            // A pass keyed by system id and a reader holding the system itself are asking the
            // same question, so the second route must not buy a second traversal.
            var fixture = buildCorvusHoldingOneColony();
            var index = new SectorPassIndex(fixture.getSector());

            index.readColoniesIn(fixture.getSystem());
            index.readColoniesById("corvus");

            verify(fixture.getSystem(), times(1)).getAllEntities();
        }

        @Test
        void yieldsNothingForAnIdNoSystemCarries() {

            var fixture = buildCorvusHoldingOneColony();

            assertThat(new SectorPassIndex(fixture.getSector()).readColoniesById("askonia"))
                .isEqualTo(Colonies.NONE);
        }

        @Test
        void resolvesTheSectorSSystemsOnceAcrossRepeatedAsks() {
            // Resolving an id is the index's other walk, and an id no system carries leaves no
            // colony memo to answer off - so without keeping the resolution, a pass asking about
            // absent systems would re-index the whole sector on every ask.
            var fixture = buildCorvusHoldingOneColony();
            var index = new SectorPassIndex(fixture.getSector());

            index.readColoniesById("askonia");
            index.readColoniesById("tyle");

            verify(fixture.getSector(), times(1)).getStarSystems();
        }

        @Test
        void answersTheFirstOfTwoSystemsSharingAnId() {
            // The id arm is what an override table or a saved preference writes, so it has to name
            // one system - the one the id index holds, which is what this resolves through.
            var world = buildTwoSystemsSharingAnId();

            assertThat(new SectorPassIndex(world.sector())
                    .readColoniesById("deep space")
                    .colonies())
                .containsExactly(new Colony(world.firstColony(), false));
        }

        @Test
        void yieldsNothingForABlankId() {
            assertThat(new SectorPassIndex(mock(SectorAPI.class)).readColoniesById(" "))
                .isEqualTo(Colonies.NONE);
        }
    }

    @Nested
    class ReadSystemsById {

        @Test
        void answersEachOfTheSectorSSystemsKeyedByItsId() {

            var fixture = buildCorvusHoldingOneColony();

            assertThat(new SectorPassIndex(fixture.getSector()).readSystemsById())
                .containsExactly(entry("corvus", fixture.getSystem()));
        }

        @Test
        void traversesTheSectorOnceAcrossRepeatedAsks() {
            // This is what a reader resolving many ids takes instead of indexing the sector for
            // itself, so it has to be cheaper than doing so - otherwise the reader has bought the
            // traversal it came here to avoid.
            var fixture = buildCorvusHoldingOneColony();
            var index = new SectorPassIndex(fixture.getSector());

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
            var index = new SectorPassIndex(fixture.getSector());

            index.readSystemsById();
            index.readColoniesById("askonia");

            verify(fixture.getSector(), times(1)).getStarSystems();
        }

        @Test
        void countsOneSectorWalkHoweverOftenItIsAskedFor() {
            // The counter the frame's bound on a rebuild is stated against: a second traversal
            // opened for the same pass is the breach, so this answer must never add one.
            var fixture = buildCorvusHoldingOneColony();
            var index = new SectorPassIndex(fixture.getSector());

            var counts = WalkCountCapture.captureCountsOf(() -> {
                index.readSystemsById();
                index.readSystemsById();
            });

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
        }

        @Test
        void yieldsNoSystemsWhenTheSectorIsUnreachable() {

            assertThat(new SectorPassIndex(null).readSystemsById())
                .isEmpty();
        }
    }

    @Nested
    class ReadSystemsByKey {

        @Test
        void answersEachOfTheSectorSSystemsKeyedByItsKey() {

            var corvus = StarSystemFixture.buildKeyedSystem("corvus", "corvus_star", "893");
            var index = new SectorPassIndex(buildSectorHoldingSystems(corvus));

            assertThat(index.readSystemsByKey())
                .containsExactly(entry(new SystemKey("corvus", "corvus_star", "893"), corvus));
        }

        @Test
        void holdsBothSystemsOfAPairSharingAnId() {
            // Why a pass takes this read instead of the id one: the four systems a live sector
            // loses to a repeated id are here, so everything derived from this index accounts for
            // every system the sector lists.
            var world = buildTwoSystemsSharingAnId();

            assertThat(new SectorPassIndex(world.sector()).readSystemsByKey())
                .containsExactly(
                    entry(new SystemKey("deep space", "", "8b3"), world.first()),
                    entry(new SystemKey("deep space", "", "38d53"), world.second()));
        }

        @Test
        void traversesTheSectorOnceAcrossRepeatedAsks() {
            // The same bargain the id index offers, and the reason a pass reaches for either: a
            // reader that had to index the sector itself has bought the traversal it came to avoid.
            var sector = buildSectorHoldingSystems(StarSystemFixture.buildSystem("corvus"));
            var index = new SectorPassIndex(sector);

            index.readSystemsByKey();
            index.readSystemsByKey();

            verify(sector, times(1)).getStarSystems();
        }

        @Test
        void countsAPassAskingBothWaysAsOneWalk() {
            // A rebuild addresses its systems both ways at once - the cells by key, the readers
            // holding a bare id by id - and its bound is one traversal for the whole of it. So the
            // id index is taken off the systems this one holds rather than off a walk of its own,
            // the rule for which system a repeated id names still being the sector read's.
            var sector = buildSectorHoldingSystems(StarSystemFixture.buildSystem("corvus"));
            var index = new SectorPassIndex(sector);

            var counts = WalkCountCapture.captureCountsOf(() -> {
                index.readSystemsByKey();
                index.readSystemsById();
            });

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
        }

        @Test
        void countsAPassAskingByIdFirstAndThenByKeyAsOneWalkToo() {
            // The same bound from the other side: which way a pass asked first cannot decide what
            // it pays, or a reader added to a rebuild would move the cost of one already there.
            var sector = buildSectorHoldingSystems(StarSystemFixture.buildSystem("corvus"));
            var index = new SectorPassIndex(sector);

            var counts = WalkCountCapture.captureCountsOf(() -> {
                index.readSystemsById();
                index.readSystemsByKey();
            });

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
        }

        @Test
        void yieldsNoSystemsWhenTheSectorIsUnreachable() {

            assertThat(new SectorPassIndex(null).readSystemsByKey())
                .isEmpty();
        }
    }

    // The pair of unnamed deep space systems the sector really holds under one id, each with a
    // colony of its own, and separated only by the anchor the engine minted for each. Built here
    // rather than taken from ColonyFixture because that one's sector holds a single system, and a
    // collision cannot be posed with one. The ids are the ones a live install reports.
    private static CollidingSystemPair buildTwoSystemsSharingAnId() {

        var firstColony = ColonyMarketFixture.buildVisibleColony("hegemony");
        var secondColony = ColonyMarketFixture.buildVisibleColony("tritachyon");
        var first = StarSystemFixture.buildKeyedSystem("deep space", null, "8b3");
        var second = StarSystemFixture.buildKeyedSystem("deep space", null, "38d53");

        ColonyPlacementFixture.placeColonies(first, firstColony);
        ColonyPlacementFixture.placeColonies(second, secondColony);

        return new CollidingSystemPair(
            buildSectorHoldingSystems(first, second),
            first,
            firstColony,
            second,
            secondColony);
    }

    // A sector holding the given systems, with an economy that lists nothing anywhere - so what a
    // colony read finds is what was sited in a system, which is the half these cases pose.
    private static SectorAPI buildSectorHoldingSystems(StarSystemAPI... systems) {

        var sectorMock = StarSystemFixture.buildSectorOf(systems);

        when(sectorMock.getEconomy())
            .thenReturn(mock(EconomyAPI.class));

        return sectorMock;
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

    // Two systems under one id and the sector listing them, each system beside the colony sited in
    // it. The colonies travel with the systems because what the cases turn on is which system's
    // colonies an answer came from - an assertion that cannot be made without naming both.
    private record CollidingSystemPair(
        SectorAPI sector,
        StarSystemAPI first,
        MarketAPI firstColony,
        StarSystemAPI second,
        MarketAPI secondColony) {
    }
}
