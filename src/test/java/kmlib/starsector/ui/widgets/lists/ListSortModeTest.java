package kmlib.starsector.ui.widgets.lists;

import kmlib.testfixtures.starsector.ui.widgets.lists.Anomaly;
import kmlib.testfixtures.starsector.ui.widgets.lists.AnomalySortMode;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the seam's one piece of behaviour of its own: a mode that declares no trailing value answers
 * no runs, so a consumer whose rows carry no number gets a plain list without writing anything for
 * it. Everything else the interface declares is the implementing mod's to fill and is pinned where it
 * is filled.
 */
final class ListSortModeTest {

    // The tone a mode with no colour opinion would draw its runs in. Arbitrary: what this suite reads
    // is that the default answers nothing at all, whatever colour it was offered.
    private static final Color DEFAULT_COLOUR = Color.WHITE;

    @Nested
    class ResolveTrailingRuns {

        @Test
        void resolveTrailingRunsDefaultsToNoRunsForAModeThatDeclaresNone() {
            // The fixture's alpha mode leaves the default in place, so a list ranked by it reads as a
            // plain one.
            assertThat(AnomalySortMode.ALPHA.resolveTrailingRuns(
                    new Anomaly("Mild", 1, 5),
                    DEFAULT_COLOUR))
                .isEmpty();
        }
    }
}
