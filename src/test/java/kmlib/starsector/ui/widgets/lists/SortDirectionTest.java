package kmlib.starsector.ui.widgets.lists;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the direction's two surfaces: the frozen persistence keys a consumer's save round-trips
 * through, and the flip a re-pick applies. The keys are pinned as literals so a rename that would
 * silently reset every save's stored direction fails here rather than shipping.
 */
final class SortDirectionTest {

    @Nested
    class PersistenceKey {

        @Test
        void persistenceKeyIsTheFrozenKeyForEachDirection() {
            // Pinned as literals: renaming a key silently resets every save that stored that direction
            // back to its mode's default, so a change must break this test before it ships.
            assertThat(SortDirection.ASCENDING.persistenceKey())
                .isEqualTo("asc");

            assertThat(SortDirection.DESCENDING.persistenceKey())
                .isEqualTo("desc");
        }
    }

    @Nested
    class Opposite {

        @Test
        void oppositeFlipsEachDirectionToTheOther() {

            assertThat(SortDirection.ASCENDING.opposite())
                .isEqualTo(SortDirection.DESCENDING);

            assertThat(SortDirection.DESCENDING.opposite())
                .isEqualTo(SortDirection.ASCENDING);
        }
    }
}
