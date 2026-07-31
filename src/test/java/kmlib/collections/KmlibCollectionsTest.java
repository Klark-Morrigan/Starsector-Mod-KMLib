package kmlib.collections;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KmlibCollectionsTest {

    @Nested
    class Join {
        @Test
        void joinReturnsEmptyStringForNoItems() {
            assertThat(KmlibCollections.join(List.<String>of(), ", ", value -> value)).isEmpty();
        }

        @Test
        void joinRendersASingleItemWithoutADelimiter() {
            assertThat(KmlibCollections.join(List.of("alpha"), ", ", value -> value))
                .isEqualTo("alpha");
        }

        @Test
        void joinSeparatesRenderedItemsWithTheDelimiter() {
            assertThat(KmlibCollections.join(List.of(1, 2, 3), ", ", String::valueOf))
                .isEqualTo("1, 2, 3");
        }

        @Test
        void joinAppliesTheRendererToEachItem() {
            // The renderer selects a field rather than the element's toString, the
            // reason this exists instead of String.join over a pre-mapped list.
            assertThat(KmlibCollections.join(List.of("alpha", "beta"), "-",
                value -> value.toUpperCase())).isEqualTo("ALPHA-BETA");
        }

        @Test
        void joinKeepsTheDelimiterAfterAnItemThatRendersEmpty() {
            // A first-item flag, not length() > 0, so an empty render between two
            // others still gets its surrounding delimiters.
            assertThat(KmlibCollections.join(List.of("a", "", "b"), ",", value -> value))
                .isEqualTo("a,,b");
        }
    }
}
