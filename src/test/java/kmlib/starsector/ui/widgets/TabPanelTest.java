package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.testfixtures.starsector.ui.font.LineWidthMeasurerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link TabPanel}'s pure geometry: box sizing from padding/border/tabs/body, the framed
 * body rectangle, and the footprint and tab hit-tests. The fake measurer reports four width units
 * per character, so every expected rectangle is a hand-checkable multiplication rather than a
 * font-dependent value.
 */
class TabPanelTest {
    // Four width units per character, so a label's measured width is its length times four.
    private final LineWidthMeasurer measurerFake = new LineWidthMeasurerFake(4d);

    // A screen and placement chosen so every derived edge is an integer: box top at 950, content
    // inset by a 2px border, tabs 24 tall snapped past a 16px text pad with a 48px floor.
    private static final float SCREEN_HEIGHT = 1000f;
    private static final float PADDING_TOP = 50f;
    private static final float PADDING_LEFT = 20f;
    private static final float BORDER_WIDTH = 2f;
    private static final float TAB_HEIGHT = 24f;
    private static final float TAB_TEXT_PADDING = 16f;
    private static final float MIN_TAB_WIDTH = 48f;
    private static final double TAB_FONT_SIZE = 15d;

    // "No Layer" (8 chars -> 32 measured -> floored to 48) then "Political Map" (13 -> 52 -> 68).
    private static final List<VanillaTabContent> TWO_TABS = List.of(
            new VanillaTabContent("No Layer", null),
            new VanillaTabContent("Political Map", null));

    private TabPanelPlacement layoutWithBody(TabPanelBodySize bodySize) {
        return TabPanel.layout(SCREEN_HEIGHT, PADDING_TOP, PADDING_LEFT, BORDER_WIDTH, TAB_HEIGHT,
                TAB_TEXT_PADDING, MIN_TAB_WIDTH, TAB_FONT_SIZE, TWO_TABS, bodySize, measurerFake);
    }

    @Nested
    class Layout {
        @Test
        void TabPanel_layout_snapsEachTabToItsLabelFromTheBorderInsetOrigin() {
            var placement = layoutWithBody(TabPanelBodySize.NONE);
            var tabs = placement.tabs();
            assertThat(tabs).hasSize(2);
            // Content origin is paddingLeft + border = 22; row top is boxTop(950) - border = 948,
            // so a 24-tall row bottoms at 924.
            assertThat(tabs.get(0).bounds().x()).isEqualTo(22f);
            assertThat(tabs.get(0).bounds().y()).isEqualTo(924f);
            assertThat(tabs.get(0).bounds().width()).isEqualTo(48f);
            assertThat(tabs.get(1).bounds().x()).isEqualTo(70f);
            assertThat(tabs.get(1).bounds().width()).isEqualTo(68f);
        }

        @Test
        void TabPanel_layout_sizesBoxToTabRowAndBorderWhenBodyIsEmpty() {
            var placement = layoutWithBody(TabPanelBodySize.NONE);
            var box = placement.box();
            // Tab row spans 116 (22..138); box adds the 2px border on each edge -> 120 wide, and is
            // tab-height plus two borders tall -> 28. It pins to paddingLeft and hangs from box top.
            assertThat(box.x()).isEqualTo(20f);
            assertThat(box.width()).isEqualTo(120f);
            assertThat(box.height()).isEqualTo(28f);
            assertThat(box.y()).isEqualTo(922f);
        }

        @Test
        void TabPanel_layout_leavesAZeroSizeBodyWhenBodyIsEmpty() {
            var placement = layoutWithBody(TabPanelBodySize.NONE);
            var body = placement.body();
            assertThat(body.width()).isEqualTo(0f);
            assertThat(body.height()).isEqualTo(0f);
            // The zero body still sits at the tab row's bottom edge, so nothing is reserved beneath.
            assertThat(body.y()).isEqualTo(924f);
        }

        @Test
        void TabPanel_layout_framesTheBodyBeneathTheTabsAtTheGivenSize() {
            var placement = layoutWithBody(new TabPanelBodySize(100f, 60f));
            var body = placement.body();
            // Body hangs from the tab row bottom (924) down its 60px height, left-aligned at 22.
            assertThat(body.x()).isEqualTo(22f);
            assertThat(body.width()).isEqualTo(100f);
            assertThat(body.height()).isEqualTo(60f);
            assertThat(body.y()).isEqualTo(864f);
        }

        @Test
        void TabPanel_layout_growsBoxToTallerFootprintAndWiderOfTabRowOrBody() {
            var placement = layoutWithBody(new TabPanelBodySize(100f, 60f));
            var box = placement.box();
            // Tab row (116) is wider than the body (100), so content width stays 116 -> box 120.
            // Content height is tab height (24) + body (60) = 84 -> box 88 with the borders.
            assertThat(box.width()).isEqualTo(120f);
            assertThat(box.height()).isEqualTo(88f);
            assertThat(box.y()).isEqualTo(862f);
        }

        @Test
        void TabPanel_layout_widensBoxToTheBodyWhenTheBodyIsWiderThanTheTabRow() {
            var placement = layoutWithBody(new TabPanelBodySize(200f, 60f));
            // Body (200) now beats the tab row (116), so content width is 200 -> box 204.
            assertThat(placement.box().width()).isEqualTo(204f);
        }
    }

    @Nested
    class ContainsPoint {
        @Test
        void TabPanel_containsPoint_isTrueForAPointInsideTheFootprint() {
            var placement = layoutWithBody(new TabPanelBodySize(100f, 60f));
            assertThat(TabPanel.containsPoint(placement, 25f, 900f)).isTrue();
        }

        @Test
        void TabPanel_containsPoint_isFalseForAPointLeftOfTheBox() {
            var placement = layoutWithBody(new TabPanelBodySize(100f, 60f));
            assertThat(TabPanel.containsPoint(placement, 10f, 900f)).isFalse();
        }
    }

    @Nested
    class FindTabIndexAt {
        @Test
        void TabPanel_findTabIndexAt_returnsTheTabUnderThePoint() {
            var placement = layoutWithBody(TabPanelBodySize.NONE);
            assertThat(TabPanel.findTabIndexAt(placement, 100f, 930f)).isEqualTo(1);
            assertThat(TabPanel.findTabIndexAt(placement, 30f, 930f)).isEqualTo(0);
        }

        @Test
        void TabPanel_findTabIndexAt_returnsNoTabWhenThePointFallsInNoTab() {
            var placement = layoutWithBody(TabPanelBodySize.NONE);
            assertThat(TabPanel.findTabIndexAt(placement, 300f, 930f)).isEqualTo(TabStrip.NO_TAB);
        }
    }
}
