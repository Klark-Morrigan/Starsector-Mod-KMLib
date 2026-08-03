package kmlib.starsector.ui.widgets.lists;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the sort resolution and ranking over modes declared outside this package: the stored mode key
 * is matched against whatever mode set the caller passes, the stored direction resolves against that
 * mode's own default, and the comparator ranks the caller's items exactly as the mode does in that
 * direction. Both stored keys arrive as arguments, so the resolution is pinned with no store in
 * reach - the shape that lets a consuming mod keep them wherever it likes.
 */
final class ListSortTest {
    // The fixture vocabulary every resolution here runs against: the mode set bundled with its
    // default, alpha standing in as the fallback the assertions read back.
    private static final AnomalySortMode DEFAULT_MODE = AnomalySortMode.ALPHA;
    private static final ListSortModes<Anomaly> MODES =
        new ListSortModes<>(List.of(AnomalySortMode.values()), DEFAULT_MODE);

    @Nested
    class ResolveStored {

        @Test
        void resolveStoredReadsTheStoredModeAndDirection() {
            var stored = ListSort.resolveStored(
                AnomalySortMode.SEVERITY.persistenceKey(),
                SortDirection.ASCENDING.persistenceKey(),
                MODES);

            assertThat(stored)
                .isEqualTo(new ListSort<>(AnomalySortMode.SEVERITY, SortDirection.ASCENDING));
        }

        @Test
        void resolveStoredFallsBackToTheCallersDefaultWhenNothingIsStored() {
            // A save that never picked a sort holds neither key, so the sort resolves to the
            // caller's default mode in that mode's own natural direction.
            var stored = ListSort.resolveStored(null, null, MODES);

            assertThat(stored)
                .isEqualTo(new ListSort<>(DEFAULT_MODE, DEFAULT_MODE.defaultDirection()));
        }

        @Test
        void resolveStoredFallsBackToTheCallersDefaultWhenTheKeyIsUnrecognised() {
            // A key left by an older or a modded build names no mode in the caller's set, so the
            // sort defaults rather than failing on it.
            var stored = ListSort.resolveStored("no_such_mode", null, MODES);

            assertThat(stored)
                .isEqualTo(new ListSort<>(DEFAULT_MODE, DEFAULT_MODE.defaultDirection()));
        }

        @Test
        void resolveStoredResolvesAnUnstoredDirectionAgainstTheStoredModesDefault() {
            // A save with a mode but no direction (a pre-direction save, or one that never flipped)
            // reads that mode's own default direction rather than some global default.
            var stored = ListSort.resolveStored(
                AnomalySortMode.SEVERITY.persistenceKey(),
                null,
                MODES);

            assertThat(stored)
                .isEqualTo(new ListSort<>(
                    AnomalySortMode.SEVERITY, AnomalySortMode.SEVERITY.defaultDirection()));
        }
    }

    @Nested
    class Comparator {

        @Test
        void comparatorRanksItemsUnderTheSortsModeAndDirection() {
            // The picker sorts under a mode declared outside this package - the proof the ranking
            // mechanism is the caller's to fill. Severity descending leads with the harsher anomaly;
            // the flipped sort reverses the pair.
            var mild = new Anomaly("Mild", 1, 5);
            var harsh = new Anomaly("Harsh", 9, 5);
            var descending = new ListSort<>(AnomalySortMode.SEVERITY, SortDirection.DESCENDING);
            var ascending = new ListSort<>(AnomalySortMode.SEVERITY, SortDirection.ASCENDING);

            assertThat(rankedBy(descending, mild, harsh))
                .containsExactly(harsh, mild);
            assertThat(rankedBy(ascending, mild, harsh))
                .containsExactly(mild, harsh);
        }

        @Test
        void comparatorRanksItemsAsTheModeDoesInThatDirection() {
            // The sort's comparator is the mode's comparator run in the sort's direction, so
            // ranking a list through the sort matches ranking it through the mode directly.
            var near = new Anomaly("Near", 3, 2);
            var far = new Anomaly("Far", 3, 8);
            var sort = new ListSort<>(AnomalySortMode.RADIUS, SortDirection.DESCENDING);

            var rankedByMode = new ArrayList<>(List.of(near, far));
            rankedByMode.sort(AnomalySortMode.RADIUS.comparator(SortDirection.DESCENDING));

            assertThat(rankedBy(sort, near, far))
                .isEqualTo(rankedByMode);
        }
    }

    // The anomalies ranked under the sort's own comparator, so an assertion reads the resulting
    // arrangement without repeating the copy-and-sort plumbing.
    private static List<Anomaly> rankedBy(ListSort<Anomaly> sort, Anomaly... anomalies) {
        var ranked = new ArrayList<>(List.of(anomalies));
        ranked.sort(sort.comparator());
        return ranked;
    }
}
