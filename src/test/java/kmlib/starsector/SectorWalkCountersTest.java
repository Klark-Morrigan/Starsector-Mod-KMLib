package kmlib.starsector;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what the sector's shared reads report: each count lands on whichever section is open, a
 * walk states both the traversal and its size, and an amount of nothing opens no counter. Where
 * a count with nothing open at all lands is pinned against a real walk, in
 * {@code SectorStarSystemsTest}.
 */
final class SectorWalkCountersTest {

    @Nested
    class CountColoniesRead {

        @Test
        void countsTheColoniesOnTheOpenSection() {

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorWalkCounters.countColoniesRead(4));

            assertThat(counts.readCount(SectorWalkCounters.COLONIES_READ))
                .isEqualTo(4L);
        }
    }

    @Nested
    class CountEntitiesVisited {

        @Test
        void countsTheEntitiesOnTheOpenSection() {

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorWalkCounters.countEntitiesVisited(2100));

            assertThat(counts.readCount(SectorWalkCounters.ENTITIES_VISITED))
                .isEqualTo(2100L);
        }
    }

    @Nested
    class CountMarketsRead {

        @Test
        void countsTheMarketsOnTheOpenSection() {

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorWalkCounters.countMarketsRead(12));

            assertThat(counts.readCount(SectorWalkCounters.MARKETS_READ))
                .isEqualTo(12L);
        }
    }

    @Nested
    class CountSectorWalk {

        @Test
        void countsOneWalkAndTheSystemsItWentOver() {
            // Both, since neither answers the other's question: the walk is what a budget bounds,
            // and the systems are what a duration is read against.
            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorWalkCounters.countSectorWalk(48));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(48L);
        }

        @Test
        void countsASecondWalkAsASecondWalk() {
            // The one thing the counter exists to catch: a pass answering one question with two
            // traversals says so on the row that asked for them.
            var counts = WalkCountCapture.captureCountsOf(() -> {
                SectorWalkCounters.countSectorWalk(48);
                SectorWalkCounters.countSectorWalk(48);
            });

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(2L);
        }

        @Test
        void countsAWalkOverAnEmptySectorWithoutOpeningASystemsColumn() {
            // A walk that found nothing still cost a traversal, while a systems column reading
            // zero is a column every other row then has to be read past.
            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorWalkCounters.countSectorWalk(0));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
            assertThat(counts.hasCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isFalse();
        }
    }

    @Nested
    class CountSystemsVisited {

        @Test
        void countsTheSystemsWithoutCountingAWalk() {
            // A system handed over one at a time is a visit and not a traversal, so a budget
            // stated on walks is not spent by a reader that walked nothing.
            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorWalkCounters.countSystemsVisited(1));

            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(1L);
            assertThat(counts.hasCount(SectorWalkCounters.SECTOR_WALKS))
                .isFalse();
        }
    }
}
