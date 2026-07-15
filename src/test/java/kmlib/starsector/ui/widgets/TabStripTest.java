package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.SegmentSizing;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.testfixtures.starsector.ui.font.LineWidthMeasurerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link TabStrip}'s text-snapped layout and hit-testing: tabs snap to their measured
 * labels, floor at the minimum width, abut left to right hanging from the row top, and a point
 * resolves to the tab it falls in (the left one on a shared edge).
 */
class TabStripTest {
    // Each character measures 10 wide, so a label's width is a plain multiple of its length.
    private final LineWidthMeasurer measurerFake = new LineWidthMeasurerFake(10d);
    // The tab-sizing rule: 8 padding, 40 minimum, size-14 font, each tab snapped to its own width.
    private static final SegmentSpec SNAPPED = new SegmentSpec(8f, 40f, 14d, SegmentSizing.SNAPPED);

    @Nested
    class LayoutTabs {
        // "AB" measures 20 (+8 padding = 28, floored to 40); "LONGER" measures 60 (+8 = 68).
        private final List<LabeledTab> tabs = TabStrip.layoutTabs(100f, 200f, 24f, SNAPPED,
                List.of("AB", "LONGER"), measurerFake);

        @Test
        void laysTabsLeftToRightFromTheOrigin() {
            assertThat(tabs.get(0).bounds().x()).isEqualTo(100f);
            assertThat(tabs.get(1).bounds().x()).isEqualTo(140f);
        }

        @Test
        void snapsAWideTabToItsMeasuredLabelPlusPadding() {
            assertThat(tabs.get(1).bounds().width()).isEqualTo(68f);
        }

        @Test
        void floorsANarrowTabAtTheMinimumWidth() {
            assertThat(tabs.get(0).bounds().width()).isEqualTo(40f);
        }

        @Test
        void hangsEveryTabDownFromTheRowTopAtTheSharedHeight() {
            assertThat(tabs.get(0).bounds()).isEqualTo(new Rectangle(100f, 176f, 40f, 24f));
            assertThat(tabs.get(1).bounds().y()).isEqualTo(176f);
            assertThat(tabs.get(1).bounds().height()).isEqualTo(24f);
        }

        @Test
        void keepsEachLabelAlongsideItsGeometry() {
            assertThat(tabs.get(0).text()).isEqualTo("AB");
            assertThat(tabs.get(1).text()).isEqualTo("LONGER");
        }
    }

    @Nested
    class MeasureRowWidth {
        // "AB" measures 20 (+8 padding = 28, floored to 40); "LONGER" measures 60 (+8 = 68); the row
        // spans their sum, 108.
        private final List<String> labels = List.of("AB", "LONGER");

        @Test
        void sumsEachTabsSnappedWidth() {
            assertThat(TabStrip.measureRowWidth(labels, SNAPPED, measurerFake)).isEqualTo(108f);
        }

        @Test
        void matchesTheWidthTheLaidOutTabsSpan() {
            // The measured row must equal the summed widths layoutTabs places the same labels at, so the
            // measure and the layout cannot drift on the per-tab snap.
            var tabs = TabStrip.layoutTabs(100f, 200f, 24f, SNAPPED, labels, measurerFake);
            var laidOutTotal = 0f;
            for (var tab : tabs) {
                laidOutTotal += tab.bounds().width();
            }
            assertThat(TabStrip.measureRowWidth(labels, SNAPPED, measurerFake))
                    .isEqualTo(laidOutTotal);
        }

        @Test
        void measuresNoWidthForNoLabels() {
            assertThat(TabStrip.measureRowWidth(List.of(), SNAPPED, measurerFake)).isZero();
        }
    }

    @Nested
    class FindTabIndexAt {
        private final List<LabeledTab> tabs = TabStrip.layoutTabs(100f, 200f, 24f, SNAPPED,
                List.of("AB", "LONGER"), measurerFake);

        @Test
        void findsTheTabAPointFallsIn() {
            assertThat(TabStrip.findTabIndexAt(tabs, 150f, 180f)).isEqualTo(1);
        }

        @Test
        void resolvesASharedEdgeToTheLeftTab() {
            assertThat(TabStrip.findTabIndexAt(tabs, 140f, 180f)).isEqualTo(0);
        }

        @Test
        void reportsNoTabForAPointRightOfTheRow() {
            assertThat(TabStrip.findTabIndexAt(tabs, 300f, 180f)).isEqualTo(TabStrip.NO_TAB);
        }

        @Test
        void reportsNoTabForAPointBelowTheRow() {
            assertThat(TabStrip.findTabIndexAt(tabs, 110f, 100f)).isEqualTo(TabStrip.NO_TAB);
        }
    }
}
