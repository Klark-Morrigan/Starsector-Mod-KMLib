package kmlib.starsector.ui.widgets.lists;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the seam's one piece of behaviour of its own: a mode that declares no trailing value shows
 * blank, so a consumer whose rows carry no number gets a plain list without writing anything for it.
 * Everything else the interface declares is the implementing mod's to fill and is pinned where it is
 * filled.
 */
final class ListSortModeTest {

    @Nested
    class ResolveTrailingValue {

        @Test
        void resolveTrailingValueDefaultsToBlankForAModeThatDeclaresNone() {
            // The fixture's severity mode leaves the default in place, so a list ranked by it reads
            // as a plain one.
            assertThat(AnomalySortMode.SEVERITY.resolveTrailingValue(new Anomaly("Mild", 1, 5)))
                .isEmpty();
        }
    }
}
