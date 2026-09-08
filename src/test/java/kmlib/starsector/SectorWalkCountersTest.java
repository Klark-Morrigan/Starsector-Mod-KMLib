package kmlib.starsector;

import kmlib.profiling.ProfileSection;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what the sector's shared reads report: each count lands on whichever section is open, a
 * walk states both the traversal and its size, an amount of nothing opens no counter, and a count
 * made with nothing open is kept under the profiler's reserved row rather than dropped.
 */
final class SectorWalkCountersTest {

    @Nested
    class CountColoniesRead {

        @Test
        void counts_the_colonies_on_the_open_section() {

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorWalkCounters.countColoniesRead(4));

            assertThat(counts.readCount(SectorWalkCounters.COLONIES_READ))
                .isEqualTo(4L);
        }
    }

    @Nested
    class CountEntitiesVisited {

        @Test
        void counts_the_entities_on_the_open_section() {

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorWalkCounters.countEntitiesVisited(2100));

            assertThat(counts.readCount(SectorWalkCounters.ENTITIES_VISITED))
                .isEqualTo(2100L);
        }
    }

    @Nested
    class CountMarketsRead {

        @Test
        void counts_the_markets_on_the_open_section() {

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorWalkCounters.countMarketsRead(12));

            assertThat(counts.readCount(SectorWalkCounters.MARKETS_READ))
                .isEqualTo(12L);
        }
    }

    @Nested
    class CountSectorWalk {

        @Test
        void counts_one_walk_and_the_systems_it_went_over() {
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
        void counts_a_second_walk_as_a_second_walk() {
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
        void counts_a_walk_over_an_empty_sector_without_opening_a_systems_column() {
            // A walk that found nothing still cost a traversal, while a systems column reading
            // zero is a column every other row then has to be read past.
            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorWalkCounters.countSectorWalk(0));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
            assertThat(counts.hasCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isFalse();
        }

        @Test
        void keeps_a_walk_made_with_nothing_open_under_the_reserved_row() {
            // A traversal from a path nobody profiled is the first thing a reader hunting stray
            // walks looks for, so it is kept rather than charged to nobody.
            var counts = WalkCountCapture.captureUnscopedCountsOf(
                () -> SectorWalkCounters.countSectorWalk(48));

            assertThat(counts.getSection())
                .isSameAs(ProfileSection.UNSCOPED_COUNTS);
            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
        }
    }

    @Nested
    class CountSystemsVisited {

        @Test
        void counts_the_systems_without_counting_a_walk() {
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
