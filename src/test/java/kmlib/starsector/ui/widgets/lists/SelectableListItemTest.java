package kmlib.starsector.ui.widgets.lists;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one answer the picker seam gives on a consumer's behalf. The three values a row is drawn
 * from are required and so cannot drift, but the receding state is optional, and what that default
 * is worth is a promise to every list already implementing this: adding the question must leave a
 * consumer that never heard of it drawing exactly as it did.
 *
 * <p>Run against an item declaring nothing but the three required values, since the fixtures the
 * rest of this package uses all state the flag outright and so would pass whatever the default was.
 */
final class SelectableListItemTest {

    @Nested
    class IsDimmed {

        @Test
        void isDimmedIsFalseForAnItemThatDeclaresNoRule() {
            // Receding is opt-in: a list whose every item is equally worth picking implements the
            // three required values and says nothing further, and its rows stay at full strength.
            assertThat(new PlainItem().isDimmed())
                .isFalse();
        }
    }

    // The barest thing that can be a picker row: the three values the seam requires and no answer to
    // anything optional. Declared here rather than reusing a fixture so that the case cannot start
    // passing because some shared fixture happened to state the flag as false.
    private record PlainItem() implements SelectableListItem {

        @Override
        public String itemId() {
            return "plain";
        }

        @Override
        public String displayName() {
            return "Plain";
        }

        @Override
        public String crestSpritePath() {
            return null;
        }
    }
}
