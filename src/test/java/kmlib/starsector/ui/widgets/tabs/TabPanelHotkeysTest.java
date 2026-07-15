package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link TabPanelHotkeys}'s pure key-to-tab mapping: matching a press to its bound tab,
 * skipping unbound tabs, and rejecting a press no tab claims.
 */
class TabPanelHotkeysTest {
    // Two bound tabs (keycodes 36, 25) with a third left unbound (0), in tab order.
    private static final List<Integer> KEYCODES = List.of(36, 25, 0);

    @Nested
    class FindTabForKey {
        @Test
        void TabPanelHotkeys_findTabForKey_returnsTheTabWhoseKeycodeMatches() {
            assertThat(TabPanelHotkeys.findTabForKey(25, KEYCODES)).isEqualTo(1);
        }

        @Test
        void TabPanelHotkeys_findTabForKey_returnsNoTabWhenNoBoundTabMatches() {
            assertThat(TabPanelHotkeys.findTabForKey(99, KEYCODES)).isEqualTo(TabStrip.NO_TAB);
        }

        @Test
        void TabPanelHotkeys_findTabForKey_skipsAnUnboundTabEvenForAZeroPress() {
            // The third tab stores 0 (unbound); a stray 0 press must not resolve to it.
            assertThat(TabPanelHotkeys.findTabForKey(0, KEYCODES)).isEqualTo(TabStrip.NO_TAB);
        }

        @Test
        void TabPanelHotkeys_findTabForKey_skipsANegativeKeycodeAsUnbound() {
            var keycodes = Arrays.asList(-1, 25);
            assertThat(TabPanelHotkeys.findTabForKey(-1, keycodes)).isEqualTo(TabStrip.NO_TAB);
            assertThat(TabPanelHotkeys.findTabForKey(25, keycodes)).isEqualTo(1);
        }
    }
}
