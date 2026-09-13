package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.profiling.ProfileSection;
import kmlib.starsector.SectorWalkCounters;
import kmlib.starsector.WalkCountCapture;
import kmlib.testfixtures.logging.LogAppenderFake;
import kmlib.testfixtures.starsector.systems.StarSystemFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link SectorStarSystems#collectHyperspacePositions},
 * {@link SectorStarSystems#getPlayerStarSystem}, {@link SectorStarSystems#indexById},
 * {@link SectorStarSystems#indexHeldSystemsById}, {@link SectorStarSystems#indexByKey},
 * {@link SectorStarSystems#findSystemById} and {@link SectorStarSystems#findSystemByKey}. Each
 * method's cases live in a {@link Nested} group so the suite reports as a per-method tree.
 *
 * <p>Every read keyed on something gets a case posing two systems that share an ID, since a live
 * modded sector holds several such pairs and the two addresses answer differently: the key index
 * holds both, the ID index and the ID lookup hold the first.
 */
final class SectorStarSystemsTest {

    @Nested
    class CollectHyperspacePositions {

        @Test
        void collectsEachSystemPositionAsXy() {

            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.buildSystemAt("corvus", 10, 20),
                StarSystemFixture.buildSystemAt("yma", -5, 7));

            var positions = SectorStarSystems.collectHyperspacePositions(sector);

            assertThat(positions)
                .hasSize(2);
            assertThat(positions.get(0))
                .containsExactly(10.0, 20.0);
            assertThat(positions.get(1))
                .containsExactly(-5.0, 7.0);
        }

        @Test
        void nullSectorYieldsNoPositions() {
            assertThat(SectorStarSystems.collectHyperspacePositions(null))
                .isEmpty();
        }

        @Test
        void systemsWithoutALocationAreSkipped() {

            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.buildSystemAt("located", 1, 2),
                StarSystemFixture.buildSystem("unlocated"));

            assertThat(SectorStarSystems.collectHyperspacePositions(sector))
                .hasSize(1);
        }

        @Test
        void countsOneWalkOverEverySystemOnTheOpenSection() {
            // The traversal is charged to whoever asked for the layout, so a pass resolving it
            // twice reads as two walks without either caller having written a profiling line.
            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.buildSystemAt("corvus", 10, 20),
                StarSystemFixture.buildSystemAt("yma", -5, 7));

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorStarSystems.collectHyperspacePositions(sector));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(2L);
        }
    }

    @Nested
    class GetPlayerStarSystem {

        @Test
        void returnsTheFleetsSystem() {

            var systemMock = StarSystemFixture.buildSystem("corvus");
            var fleetMock = mock(CampaignFleetAPI.class);

            when(fleetMock.getStarSystem())
                .thenReturn(systemMock);

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getPlayerFleet())
                .thenReturn(fleetMock);

            assertThat(SectorStarSystems.getPlayerStarSystem(sectorMock))
                .isSameAs(systemMock);
        }

        @Test
        void returnsNullForANullSector() {
            assertThat(SectorStarSystems.getPlayerStarSystem(null))
                .isNull();
        }

        @Test
        void returnsNullWithoutAPlayerFleet() {

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getPlayerFleet())
                .thenReturn(null);

            assertThat(SectorStarSystems.getPlayerStarSystem(sectorMock))
                .isNull();
        }

        @Test
        void returnsNullWhenTheFleetIsInHyperspace() {

            var fleetMock = mock(CampaignFleetAPI.class);

            when(fleetMock.getStarSystem())
                .thenReturn(null);

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getPlayerFleet())
                .thenReturn(fleetMock);

            assertThat(SectorStarSystems.getPlayerStarSystem(sectorMock))
                .isNull();
        }
    }

    @Nested
    class FindSystemByKey {

        @Test
        void returnsTheSystemWhoseKeyMatches() {

            var wanted = StarSystemFixture.buildKeyedSystem("corvus", "corvus_star", "893");
            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.buildKeyedSystem("yma", "yma_star", "89a"),
                wanted);

            assertThat(SectorStarSystems.findSystemByKey(
                    sector,
                    new SystemKey("corvus", "corvus_star", "893")))
                .isSameAs(wanted);
        }

        @Test
        void answersTheNamedSystemOfAPairSharingAnId() {
            // What the key lookup is for: the ID lookup answers the first of the pair whichever of
            // them was asked about, so the second is unreachable by ID and reachable by key.
            var second = StarSystemFixture.buildKeyedSystem("deep space", null, "38d53");
            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.buildKeyedSystem("deep space", null, "8b3"),
                second);

            assertThat(SectorStarSystems.findSystemByKey(
                    sector,
                    new SystemKey("deep space", null, "38d53")))
                .isSameAs(second);
        }

        @Test
        void returnsNullWhenNoSystemCarriesThatKey() {

            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.buildKeyedSystem("corvus", "corvus_star", "893"));

            assertThat(SectorStarSystems.findSystemByKey(
                    sector,
                    new SystemKey("corvus", "corvus_star", "999")))
                .isNull();
        }

        @Test
        void returnsNullForANullSector() {
            assertThat(SectorStarSystems.findSystemByKey(null, new SystemKey("corvus", null, null)))
                .isNull();
        }

        @Test
        void returnsNullForAKeyStatingNothing() {
            // The blank key equals every other blank one, so answering with a system would name
            // whichever the sector happens to list first rather than the one asked about.
            assertThat(SectorStarSystems.findSystemByKey(
                    mock(SectorAPI.class),
                    new SystemKey(null, null, null)))
                .isNull();
        }

        @Test
        void countsTheWalkAtTheSystemsItExaminedBeforeTheMatch() {
            // Charged as the ID lookup charges its own: a search that stopped at the first system
            // did not visit the sector.
            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.buildKeyedSystem("corvus", "corvus_star", "893"),
                StarSystemFixture.buildKeyedSystem("yma", "yma_star", "89a"));

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorStarSystems.findSystemByKey(
                    sector,
                    new SystemKey("corvus", "corvus_star", "893")));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(1L);
        }
    }

    @Nested
    class FindSystemById {

        @Test
        void returnsTheSystemWhoseIdMatches() {

            var wanted = StarSystemFixture.buildSystem("corvus");
            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.buildSystem("yma"),
                wanted);

            assertThat(SectorStarSystems.findSystemById(sector, "corvus"))
                .isSameAs(wanted);
        }

        @Test
        void answersTheFirstSystemFoundUnderARepeatedId() {
            // The invariant the ID index is built to match: an ID no system holds alone resolves to
            // one system, whichever way a caller asks. An override table's ID or a saved preference
            // would otherwise address one system through the lookup and another through the index.
            var first = StarSystemFixture.buildKeyedSystem("deep space", null, "8b3");
            var sector = StarSystemFixture.buildSectorOf(
                first,
                StarSystemFixture.buildKeyedSystem("deep space", null, "38d53"));

            assertThat(SectorStarSystems.findSystemById(sector, "deep space"))
                .isSameAs(first);
        }

        @Test
        void returnsNullWhenNoSystemHasThatId() {

            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.buildSystem("corvus"));

            assertThat(SectorStarSystems.findSystemById(sector, "nowhere"))
                .isNull();
        }

        @Test
        void returnsNullForANullSector() {
            assertThat(SectorStarSystems.findSystemById(null, "corvus"))
                .isNull();
        }

        @Test
        void countsTheWalkAtTheSystemsItExaminedBeforeTheMatch() {
            // A lookup that stops at the first system did not visit the sector. Counting the whole
            // list would hide what this counter is for: many lookups each walking from the start.
            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.buildSystem("corvus"),
                StarSystemFixture.buildSystem("yma"));

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorStarSystems.findSystemById(sector, "corvus"));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(1L);
        }

        @Test
        void countsTheWholeListForAnIdNoSystemCarries() {
            // The other half of the same rule: a lookup that matched nothing did go over every
            // system, and that is the expensive case a row has to be able to show.
            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.buildSystem("corvus"),
                StarSystemFixture.buildSystem("yma"));

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorStarSystems.findSystemById(sector, "nowhere"));

            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(2L);
        }

        @Test
        void returnsNullForABlankId() {
            // A blank ID short-circuits before the walk, so a posed system list is not even
            // needed - a blank query matches nothing rather than the first system by accident.
            assertThat(SectorStarSystems.findSystemById(mock(SectorAPI.class), " "))
                .isNull();
        }
    }

    @Nested
    class IndexById {

        @Test
        void keysEverySystemByItsOwnIdInTheSectorsOrder() {
            // The bulk lookup a pass resolving many IDs reaches for instead of walking the system
            // list once per id. The sector's own order is kept, so a caller iterating the index
            // sees the systems in the order the sector lists them rather than a hash's.
            var corvus = StarSystemFixture.buildSystem("corvus");
            var yma = StarSystemFixture.buildSystem("yma");

            assertThat(SectorStarSystems.indexById(StarSystemFixture.buildSectorOf(yma, corvus)))
                .containsExactly(
                    entry("yma", yma),
                    entry("corvus", corvus));
        }

        @Test
        void keepsTheFirstSystemFoundUnderARepeatedId() {
            // An ID is not unique in a modded sector, and the system kept is the one findSystemById
            // answers with, so both ID reads name one system.
            var first = StarSystemFixture.buildKeyedSystem("deep space", null, "8b3");
            var sector = StarSystemFixture.buildSectorOf(
                first,
                StarSystemFixture.buildKeyedSystem("deep space", null, "38d53"));

            assertThat(SectorStarSystems.indexById(sector))
                .containsExactly(entry("deep space", first));
        }

        @Test
        void statesTheIdThatLeftASystemOutOfTheIndex() {
            // The line that turns "a system is missing from the map" into a one-line diagnosis.
            // Silently dropping the second system is what made the defect invisible for as long as
            // it lasted, so the saying of it is pinned rather than left to the reader of the code.
            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.nameSystem(
                    StarSystemFixture.buildKeyedSystem("deep space", null, "8b3"), "Deep Space"),
                StarSystemFixture.nameSystem(
                    StarSystemFixture.buildKeyedSystem("deep space", null, "38d53"), "Deep Space"));

            var log = LogAppenderFake.captureLogOf(
                SectorStarSystems.class,
                () -> SectorStarSystems.indexById(sector));

            assertThat(log.getMessages())
                .hasSize(1);
            assertThat(log.getMessages().get(0))
                .contains("deep space")
                .contains("Deep Space");
        }

        @Test
        void returnsAnEmptyIndexForANullSector() {
            assertThat(SectorStarSystems.indexById(null))
                .isEmpty();
        }

        @Test
        void chargesNoWalkForANullSector() {
            // There was no list to go over, so a row that charged one would price a traversal that
            // never happened. Pinned here for the walk every bulk read shares.
            var counts = WalkCountCapture.captureCountsOf(() -> SectorStarSystems.indexById(null));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(0L);
            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(0L);
        }

        @Test
        void countsAnIndexAskedForTwiceAsTwoWalks() {
            // The row a pass reads to find out it is resolving the same index twice - which is
            // the whole contract this counter is here to make checkable.
            var sector = StarSystemFixture.buildSectorOf(StarSystemFixture.buildSystem("corvus"));

            var counts = WalkCountCapture.captureCountsOf(() -> {
                SectorStarSystems.indexById(sector);
                SectorStarSystems.indexById(sector);
            });

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(2L);
        }

        @Test
        void keepsAWalkMadeWithNothingOpenUnderTheReservedRow() {
            // A traversal from a path nobody profiled is seen rather than dropped, which is what
            // makes an unattributed walk findable at all.
            var sector = StarSystemFixture.buildSectorOf(StarSystemFixture.buildSystem("corvus"));

            var counts = WalkCountCapture.captureUnscopedCountsOf(
                () -> SectorStarSystems.indexById(sector));

            assertThat(counts.getSection())
                .isSameAs(ProfileSection.UNSCOPED_COUNTS);
            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
        }
    }

    @Nested
    class IndexHeldSystemsById {

        @Test
        void keysSystemsACallerAlreadyHoldsByTheirOwnIds() {
            // The address a caller takes when it has traversed the sector already: the same index
            // over the systems it hands over, in the order it hands them over.
            var corvus = StarSystemFixture.buildSystem("corvus");
            var yma = StarSystemFixture.buildSystem("yma");

            assertThat(SectorStarSystems.indexHeldSystemsById(List.of(yma, corvus)))
                .containsExactly(
                    entry("yma", yma),
                    entry("corvus", corvus));
        }

        @Test
        void keepsTheFirstSystemHeldUnderARepeatedId() {
            // The ID rule is the sector read's, whichever way the systems arrive - so a caller
            // re-addressing systems it holds gets the system every other ID read answers with.
            var first = StarSystemFixture.buildKeyedSystem("deep space", null, "8b3");

            assertThat(SectorStarSystems.indexHeldSystemsById(List.of(
                    first,
                    StarSystemFixture.buildKeyedSystem("deep space", null, "38d53"))))
                .containsExactly(entry("deep space", first));
        }

        @Test
        void chargesNoWalkForSystemsACallerAlreadyHolds() {
            // The whole reason this address exists: the traversal was the caller's, so charging one
            // here would report a pass as having read the sector twice for addressing it twice.
            var counts = WalkCountCapture.captureCountsOf(() -> SectorStarSystems
                .indexHeldSystemsById(List.of(StarSystemFixture.buildSystem("corvus"))));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(0L);
        }

        @Test
        void returnsAnEmptyIndexForNoSystemsToIndex() {
            assertThat(SectorStarSystems.indexHeldSystemsById(null))
                .isEmpty();
        }
    }

    @Nested
    class IndexByKey {

        @Test
        void keysEverySystemByItsOwnKeyInTheSectorsOrder() {
            // The key is the three arms the sector states, so a caller holding one can be handed
            // back the system without the ID having had to be unique for it to work.
            var corvus = StarSystemFixture.buildKeyedSystem("corvus", "corvus_star", "893");
            var yma = StarSystemFixture.buildKeyedSystem("yma", "yma_star", "89a");

            assertThat(SectorStarSystems.indexByKey(StarSystemFixture.buildSectorOf(yma, corvus)))
                .containsExactly(
                    entry(new SystemKey("yma", "yma_star", "89a"), yma),
                    entry(new SystemKey("corvus", "corvus_star", "893"), corvus));
        }

        @Test
        void holdsBothSystemsOfAPairSharingAnId() {
            // The whole point of the key index, and the defect the ID index carries: a sector
            // holding two systems under one ID has both of them here, so a pass built on this one
            // accounts for every system the sector lists.
            var first = StarSystemFixture.buildKeyedSystem("deep space", null, "8b3");
            var second = StarSystemFixture.buildKeyedSystem("deep space", null, "38d53");

            assertThat(SectorStarSystems.indexByKey(StarSystemFixture.buildSectorOf(first, second)))
                .containsExactly(
                    entry(new SystemKey("deep space", "", "8b3"), first),
                    entry(new SystemKey("deep space", "", "38d53"), second));
        }

        @Test
        void keepsTheFirstOfTwoSystemsTheSectorStatesNothingAbout() {
            // The one shape a key cannot part: neither system offers an ID, a centre or an anchor,
            // so both carry the blank key and the sector has said nothing that tells them apart.
            // The first is kept for the same reason every other read here keeps the first.
            var first = StarSystemFixture.buildKeyedSystem(null, null, null);
            var sector = StarSystemFixture.buildSectorOf(
                first,
                StarSystemFixture.buildKeyedSystem(null, null, null));

            assertThat(SectorStarSystems.indexByKey(sector))
                .containsExactly(entry(new SystemKey(null, null, null), first));
        }

        @Test
        void statesTheSystemThatLeftTheKeyIndexUnsaid() {
            // This index is the one promised to hold every system the sector lists, so a system
            // dropped from it is dropped from everything derived from it. Saying so is what turns
            // the cell it never got into a one-line diagnosis rather than a hunt.
            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.nameSystem(
                    StarSystemFixture.buildKeyedSystem(null, null, null), "Unknown Location"),
                StarSystemFixture.nameSystem(
                    StarSystemFixture.buildKeyedSystem(null, null, null), "Uncharted Space"));

            var log = LogAppenderFake.captureLogOf(
                SectorStarSystems.class,
                () -> SectorStarSystems.indexByKey(sector));

            assertThat(log.getMessages())
                .hasSize(1);
            assertThat(log.getMessages().get(0))
                .contains("Unknown Location")
                .contains("Uncharted Space");
        }

        @Test
        void returnsAnEmptyIndexForANullSector() {
            assertThat(SectorStarSystems.indexByKey(null))
                .isEmpty();
        }

        @Test
        void countsOneWalkOverEverySystem() {
            // Keyed differently, priced the same: the key index walks the sector's list once, the
            // extra arms being reads off a system already in hand rather than a second traversal.
            var sector = StarSystemFixture.buildSectorOf(
                StarSystemFixture.buildKeyedSystem("corvus", "corvus_star", "893"));

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorStarSystems.indexByKey(sector));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(1L);
        }
    }
}
