package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.profiling.ProfileSection;
import kmlib.starsector.SectorWalkCounters;
import kmlib.starsector.WalkCountCapture;
import kmlib.testfixtures.logging.LogAppenderFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link SectorStarSystems#getHyperspacePositions},
 * {@link SectorStarSystems#collectPositionsById},
 * {@link SectorStarSystems#getPlayerStarSystem}, {@link SectorStarSystems#indexById},
 * {@link SectorStarSystems#indexByKey} and {@link SectorStarSystems#findById}. Each method's cases
 * live in a {@link Nested} group so the suite reports as a per-method tree; the shared mock
 * builders stay on the outer class.
 *
 * <p>Every read keyed on something gets a case posing two systems that share an id, since a live
 * modded sector holds several such pairs and each read answers differently: the key index holds
 * both, the id index holds the first and says so, the id-keyed positions hold one.
 */
final class SectorStarSystemsTest {

    @Nested
    class GetHyperspacePositions {

        @Test
        void collectsEachSystemPositionAsXy() {

            var sector = buildSectorWithSystemsAt(new float[] {10, 20}, new float[] {-5, 7});
            var positions = SectorStarSystems.getHyperspacePositions(sector);

            assertThat(positions)
                .hasSize(2);
            assertThat(positions.get(0))
                .containsExactly(10.0, 20.0);
            assertThat(positions.get(1))
                .containsExactly(-5.0, 7.0);
        }

        @Test
        void nullSectorYieldsNoPositions() {
            assertThat(SectorStarSystems.getHyperspacePositions(null))
                .isEmpty();
        }

        @Test
        void systemsWithoutALocationAreSkipped() {

            var locatedMock = mock(StarSystemAPI.class);

            when(locatedMock.getLocation())
                .thenReturn(new Vector2f(1, 2));

            var unlocatedMock = mock(StarSystemAPI.class);

            when(unlocatedMock.getLocation())
                .thenReturn(null);

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(locatedMock, unlocatedMock));

            assertThat(SectorStarSystems.getHyperspacePositions(sectorMock))
                .hasSize(1);
        }

        @Test
        void countsOneWalkOverEverySystemOnTheOpenSection() {
            // The traversal is charged to whoever asked for the layout, so a pass resolving it
            // twice reads as two walks without either caller having written a profiling line.
            var sector = buildSectorWithSystemsAt(new float[] {10, 20}, new float[] {-5, 7});
            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorStarSystems.getHyperspacePositions(sector));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(2L);
        }
    }

    @Nested
    class CollectPositionsById {

        @Test
        void keysEachSelectedSystemByIdWithItsPosition() {

            var a = buildSystemAt("a", 10, 20);
            var b = buildSystemAt("b", -5, 7);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(a, b));

            var positions = SectorStarSystems.collectPositionsById(sectorMock, system -> true);

            assertThat(positions.get("a"))
                .containsExactly(10.0, 20.0);
            assertThat(positions.get("b"))
                .containsExactly(-5.0, 7.0);
        }

        @Test
        void excludesSystemsThePredicateRejects() {

            var kept = buildSystemAt("kept", 1, 1);
            var rejected = buildSystemAt("rejected", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(kept, rejected));

            var positions = SectorStarSystems.collectPositionsById(
                sectorMock,
                system -> system.getId().equals("kept"));

            assertThat(positions)
                .containsOnlyKeys("kept");
        }

        @Test
        void skipsASelectedSystemWithoutALocation() {

            var located = buildSystemAt("located", 1, 1);
            var unlocatedMock = mock(StarSystemAPI.class);

            when(unlocatedMock.getLocation())
                .thenReturn(null);

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(located, unlocatedMock));

            var positions = SectorStarSystems.collectPositionsById(sectorMock, system -> true);

            assertThat(positions)
                .containsOnlyKeys("located");
        }

        @Test
        void aNullPredicateKeepsEveryLocatedSystem() {

            var a = buildSystemAt("a", 1, 1);
            var b = buildSystemAt("b", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(a, b));

            var positions = SectorStarSystems.collectPositionsById(sectorMock, null);

            assertThat(positions)
                .containsOnlyKeys("a", "b");
        }

        @Test
        void nullSectorYieldsNoPositions() {
            assertThat(SectorStarSystems.collectPositionsById(null, system -> true))
                .isEmpty();
        }

        @Test
        void keepsTheLastPositionOfSystemsSharingAnId() {
            // Two systems under one id are one entry, the later position standing for both. The
            // co-located pair this happens to in a live sector is exactly the one a motion poll
            // then reads as a single system moving.
            var first = buildKeyedSystemAt("deep space", null, "8b3", 1, 1);
            var second = buildKeyedSystemAt("deep space", null, "38d53", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(first, second));

            var positions = SectorStarSystems.collectPositionsById(sectorMock, null);

            assertThat(positions)
                .containsOnlyKeys("deep space");
            assertThat(positions.get("deep space"))
                .containsExactly(2.0, 2.0);
        }

        @Test
        void countsTheSystemsItWentOverRatherThanTheOnesItKept() {
            // What the walk cost is what it reached: a filter keeping one system of two did not
            // make the traversal any shorter, and a row saying it did would price it wrong.
            var kept = buildSystemAt("kept", 1, 1);
            var rejected = buildSystemAt("rejected", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(kept, rejected));

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorStarSystems.collectPositionsById(
                    sectorMock,
                    system -> system.getId().equals("kept")));

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

            var systemMock = mock(StarSystemAPI.class);
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
    class FindById {

        @Test
        void returnsTheSystemWhoseIdMatches() {

            var wanted = buildSystemAt("corvus", 1, 1);
            var other = buildSystemAt("yma", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(other, wanted));

            assertThat(SectorStarSystems.findById(sectorMock, "corvus"))
                .isSameAs(wanted);
        }

        @Test
        void returnsNullWhenNoSystemHasThatId() {

            var only = buildSystemAt("corvus", 1, 1);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(only));

            assertThat(SectorStarSystems.findById(sectorMock, "nowhere"))
                .isNull();
        }

        @Test
        void returnsNullForANullSector() {
            assertThat(SectorStarSystems.findById(null, "corvus"))
                .isNull();
        }

        @Test
        void countsTheWalkAtTheSystemsItExaminedBeforeTheMatch() {
            // A lookup that stops at the first system did not visit the sector. Counting the whole
            // list would hide what this counter is for: many lookups each walking from the start.
            var wanted = buildSystemAt("corvus", 1, 1);
            var other = buildSystemAt("yma", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(wanted, other));

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorStarSystems.findById(sectorMock, "corvus"));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(1L);
        }

        @Test
        void countsTheWholeListForAnIdNoSystemCarries() {
            // The other half of the same rule: a lookup that matched nothing did go over every
            // system, and that is the expensive case a row has to be able to show.
            var corvus = buildSystemAt("corvus", 1, 1);
            var yma = buildSystemAt("yma", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(corvus, yma));

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorStarSystems.findById(sectorMock, "nowhere"));

            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(2L);
        }

        @Test
        void returnsNullForABlankId() {
            // A blank id short-circuits before the walk, so a stubbed system list is not even
            // needed - a blank query matches nothing rather than the first system by accident.
            assertThat(SectorStarSystems.findById(mock(SectorAPI.class), " "))
                .isNull();
        }
    }

    @Nested
    class IndexById {

        @Test
        void keysEverySystemByItsOwnIdInTheSectorsOrder() {
            // The bulk lookup a pass resolving many ids reaches for instead of walking the system
            // list once per id. The sector's own order is kept, so a caller iterating the index
            // sees the systems in the order the sector lists them rather than a hash's.
            var corvus = buildSystemAt("corvus", 1, 1);
            var yma = buildSystemAt("yma", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(yma, corvus));

            var indexed = SectorStarSystems.indexById(sectorMock);

            assertThat(indexed)
                .containsExactly(
                    entry("yma", yma),
                    entry("corvus", corvus));
        }

        @Test
        void keepsTheFirstSystemFoundUnderARepeatedId() {
            // An id is not unique in a modded sector. Whichever system this index answers with has
            // to be the one findById answers with, or an id written in an override table or a saved
            // preference addresses one system through the lookup and another through the index.
            var first = buildKeyedSystemAt("deep space", null, "8b3", 1, 1);
            var second = buildKeyedSystemAt("deep space", null, "38d53", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(first, second));

            assertThat(SectorStarSystems.indexById(sectorMock))
                .containsExactly(entry("deep space", first));
        }

        @Test
        void statesTheIdThatLeftASystemOutOfTheIndex() {
            // The line that turns "a system is missing from the map" into a one-line diagnosis.
            // Silently dropping the second system is what made the defect invisible for as long as
            // it lasted, so the saying of it is pinned rather than left to the reader of the code.
            var first = buildKeyedSystemAt("deep space", null, "8b3", 1, 1);

            when(first.getName())
                .thenReturn("Deep Space");

            var second = buildKeyedSystemAt("deep space", null, "38d53", 2, 2);

            when(second.getName())
                .thenReturn("Deep Space");

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(first, second));

            var log = LogAppenderFake.captureLogOf(
                SectorStarSystems.class,
                () -> SectorStarSystems.indexById(sectorMock));

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
        void countsAnIndexAskedForTwiceAsTwoWalks() {
            // The row a pass reads to find out it is resolving the same index twice - which is
            // the whole contract this counter is here to make checkable.
            // The system finishes its own stubbing before the sector's opens, so the two do not
            // nest into an unfinished-stubbing error.
            var corvus = buildSystemAt("corvus", 1, 1);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(corvus));

            var counts = WalkCountCapture.captureCountsOf(() -> {
                SectorStarSystems.indexById(sectorMock);
                SectorStarSystems.indexById(sectorMock);
            });

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(2L);
        }

        @Test
        void keepsAWalkMadeWithNothingOpenUnderTheReservedRow() {
            // A traversal from a path nobody profiled is seen rather than dropped, which is what
            // makes an unattributed walk findable at all.
            var corvus = buildSystemAt("corvus", 1, 1);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(corvus));

            var counts = WalkCountCapture.captureUnscopedCountsOf(
                () -> SectorStarSystems.indexById(sectorMock));

            assertThat(counts.getSection())
                .isSameAs(ProfileSection.UNSCOPED_COUNTS);
            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
        }
    }

    @Nested
    class IndexByKey {

        @Test
        void keysEverySystemByItsOwnKeyInTheSectorsOrder() {
            // The key is the three arms the sector states, so a caller holding one can be handed
            // back the system without the id having had to be unique for it to work.
            var corvus = buildKeyedSystemAt("corvus", "corvus_star", "893", 1, 1);
            var yma = buildKeyedSystemAt("yma", "yma_star", "89a", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(yma, corvus));

            assertThat(SectorStarSystems.indexByKey(sectorMock))
                .containsExactly(
                    entry(new SystemKey("yma", "yma_star", "89a"), yma),
                    entry(new SystemKey("corvus", "corvus_star", "893"), corvus));
        }

        @Test
        void holdsBothSystemsOfAPairSharingAnId() {
            // The whole point of the key index, and the defect the id index carries: a sector
            // holding two systems under one id has both of them here, so a pass built on this one
            // accounts for every system the sector lists.
            var first = buildKeyedSystemAt("deep space", null, "8b3", 1, 1);
            var second = buildKeyedSystemAt("deep space", null, "38d53", 2, 2);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(first, second));

            assertThat(SectorStarSystems.indexByKey(sectorMock))
                .containsExactly(
                    entry(new SystemKey("deep space", "", "8b3"), first),
                    entry(new SystemKey("deep space", "", "38d53"), second));
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
            var corvus = buildKeyedSystemAt("corvus", "corvus_star", "893", 1, 1);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(List.of(corvus));

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorStarSystems.indexByKey(sectorMock));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(1L);
        }
    }

    private static StarSystemAPI buildSystemAt(String id, float x, float y) {

        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getId())
            .thenReturn(id);
        when(systemMock.getLocation())
            .thenReturn(new Vector2f(x, y));

        return systemMock;
    }

    // A system carrying the entities a key is read off, for the cases posing two systems that share
    // an id - what tells those apart is nothing else.
    private static StarSystemAPI buildKeyedSystemAt(
            String id,
            String centreEntityId,
            String anchorEntityId,
            float x,
            float y) {

        // Both entity mocks are built before any stubbing opens, since building one inside a
        // when(...) call leaves Mockito's stubbing half finished.
        var centreMock = buildEntityMock(centreEntityId);
        var anchorMock = buildEntityMock(anchorEntityId);
        var systemMock = buildSystemAt(id, x, y);

        when(systemMock.getCenter())
            .thenReturn(centreMock);
        when(systemMock.getHyperspaceAnchor())
            .thenReturn(anchorMock);

        return systemMock;
    }

    // A null id stands for the entity being absent altogether, which a system may well be without.
    private static SectorEntityToken buildEntityMock(String entityId) {

        if (entityId == null) {
            return null;
        }
        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.getId())
            .thenReturn(entityId);

        return entityMock;
    }

    private static SectorAPI buildSectorWithSystemsAt(float[]... points) {

        var systems = new ArrayList<StarSystemAPI>();

        for (float[] point : points) {

            var systemMock = mock(StarSystemAPI.class);

            when(systemMock.getLocation())
                .thenReturn(new Vector2f(point[0], point[1]));

            systems.add(systemMock);
        }

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getStarSystems())
            .thenReturn(systems);

        return sectorMock;
    }
}
