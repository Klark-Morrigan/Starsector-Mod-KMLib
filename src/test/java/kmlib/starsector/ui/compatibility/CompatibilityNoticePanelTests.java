package kmlib.starsector.ui.compatibility;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the half of the raise that decides whether to attempt one at all.
 *
 * <p>Standing the panel up needs a running game, so what is pinned is the gate in front of it: a
 * screen that is not showing answers no, and nothing reaches for the game to find that out. The
 * gate failing open would put a panel in a tree nothing is drawing, which reports success and shows
 * the player nothing. That the read behind it fails closed on a tree that cannot be walked is
 * {@code ShownMapTab}'s to pin.
 */
final class CompatibilityNoticePanelTests {

    @Nested
    class RaiseNoticeOnScreen {

        @Test
        void standsNoNoticeWhereNoScreenIsShowing() {

            assertThat(CompatibilityNoticePanel.raiseNoticeOnScreen(() -> false))
                .isNull();
        }
    }
}
