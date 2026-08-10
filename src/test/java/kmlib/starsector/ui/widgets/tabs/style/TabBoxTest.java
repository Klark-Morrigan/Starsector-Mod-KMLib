package kmlib.starsector.ui.widgets.tabs.style;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the box a tab stands in: which rows count as fixed or parted, and how a box shorter than its
 * band resolves against it.
 */
class TabBoxTest {

    // Vanilla's own map-tab box (com.fs.starfarer.coreui.A.G): 130 x 18, parted by one pixel.
    private static final TabBox VANILLA_MAP_TAB = new TabBox(130f, 18f, 1f);

    @Nested
    class IsFixedWidth {

        @Test
        void isTrueForABoxStatingAWidth() {
            assertThat(VANILLA_MAP_TAB.isFixedWidth())
                .isTrue();
        }

        @Test
        void isFalseForTheSnappedBox() {
            // The claim that carries the whole snapped path: a row asking this and hearing false never
            // reaches the fixed width rule and so still measures its labels.
            assertThat(TabBox.SNAPPED.isFixedWidth())
                .isFalse();
        }
    }

    @Nested
    class IsParted {

        @Test
        void isTrueForABoxStatingAGap() {
            assertThat(VANILLA_MAP_TAB.isParted())
                .isTrue();
        }

        @Test
        void isFalseForABoxWhoseTabsAbut() {
            // A fixed box need not be a parted one, so this is asked apart from the width: an abutting
            // fixed row still has seams to rule.
            assertThat(new TabBox(130f, 18f, 0f).isParted())
                .isFalse();
        }
    }

    @Nested
    class ResolveTabHeight {

        @Test
        void standsTheStatedHeightInsideATallerBand() {
            // 18 in a 19 band, which leaves the pixel the vanilla row keeps for the line its tabs sit on.
            assertThat(VANILLA_MAP_TAB.resolveTabHeight(19f))
                .isEqualTo(18f);
        }

        @Test
        void fillsTheBandWhenNoHeightIsStated() {
            assertThat(TabBox.SNAPPED.resolveTabHeight(19f))
                .isEqualTo(19f);
        }

        @Test
        void neverStandsTallerThanItsBand() {
            // A collapsing panel narrows its band toward nothing, and a tab that kept its stated height
            // through that would hang out of the band it was supposed to be wiped with.
            assertThat(VANILLA_MAP_TAB.resolveTabHeight(4f))
                .isEqualTo(4f);
        }
    }

    @Nested
    class Construction {

        @Test
        void floorsEveryNegativeDimensionAtZero() {
            // Floored rather than rejected, so a caller handed a bad number lays a snapped row instead of
            // tabs drawn backwards through their neighbours.
            var negative = new TabBox(-130f, -18f, -1f);

            assertThat(negative)
                .isEqualTo(TabBox.SNAPPED);
        }
    }
}
